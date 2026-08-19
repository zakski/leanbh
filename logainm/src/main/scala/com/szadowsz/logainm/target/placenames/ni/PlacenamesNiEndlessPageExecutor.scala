// Copyright 2016 zakski.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.szadowsz.logainm.target.placenames.ni

import com.szadowsz.common.net.Uri
import com.szadowsz.logainm.target.placenames.ni.PlacenamesNiEndlessPageExecutor.{PLACENAME_LIST_MARKER, PLACENAME_SCROLL_STEP_PX, PLACENAME_SCROLL_STEP_ROWS, SCROLL_SELECTOR_JS, SCROLL_SELECTOR_NAME}
import com.szadowsz.maeve.core.browser.MaeveBrowser
import com.szadowsz.maeve.core.instruction.actions.ActionExecutor
import org.htmlunit.html.HtmlElement
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.ListHasAsScala


object PlacenamesNiEndlessPageExecutor {

  val PLACENAME_LIST_MARKER = "Place-Name-Search" // title for the placename search list web page

  val PLACENAME_ROW_HEIGHT_PX = 71L // Nominal row height of the search list. Rows can vary in height, this is standard

  val PLACENAME_SCROLL_STEP_ROWS = 5 // How far to advance the scroll in rows. Kept below a viewport of rows.

  val PLACENAME_SCROLL_STEP_PX = PLACENAME_ROW_HEIGHT_PX * PLACENAME_SCROLL_STEP_ROWS // How far to advance the scroll. Kept below a viewport of rows.

  private val RESULTS_LIST_SELECTOR = "div.list-widget-widget_895"

  private val SCROLL_SELECTOR_NAME = "div.widget-list-list"

  private val SCROLL_SELECTOR_JS = RESULTS_LIST_SELECTOR + " " + SCROLL_SELECTOR_NAME

}

/**
 * Drives the place-name crawl. The search list is a virtualised (react-window) list, and each record's detail is
 * on a separate "Place-Name Info" page reached by clicking the row's "More Info" button. For every row (by
 * react-window index) this executor scrolls it into view, clicks through to the detail page (leaving the browser
 * there so the extractor can scrape it), and afterwards returns to the search list and advances to the next index.
 *
 * The list is scrolled incrementally (rather than jumped) so the widget can lazily load records as it goes. This
 * also drives recovery: after a restart the state resumes at the row after the last written one, and the executor
 * simply scrolls down to reach it - passing over, but never re-opening or re-writing, the already-processed rows.
 *
 * @param timeInMS   base wait/settle time, in milliseconds, used for page/list loads and lazy-load pauses.
 * @param throttleMs deliberate pause, in milliseconds, taken before opening each record to slow the crawl down.
 * @param state      shared crawl state (current index / completion / resume progress).
 *
 * Created on 18/10/2016.
 */
final class PlacenamesNiEndlessPageExecutor(timeInMS : Long, throttleMs : Long, state: PlacenamesNiListState) extends ActionExecutor {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // The ArcGIS Experience list widget container that holds the rendered place-name records.
  private val listSelector = "div[class=\"widget-list d-flex\"]"
   // Spinner the ArcGIS Experience app shows while it is still fetching / rendering the list.
  private val loadingSelector = "div.jimu-secondary-loading"
  // Overlap one row height between scroll steps so react-window virtualisation can never skip a record.
  private val rowOverlapPx = 71L
  // Total time we are prepared to wait for the JS app to finish rendering the list.
  private val maxWaitMs = math.max(timeInMS * 15, 30000L)
  private val pollIntervalMs = 500L


  /**
   * @return true if the row at the given index is currently marked selected (aria-selected="true").
   */
  private def isRowSelected(js: JavascriptExecutor, index: Int): Boolean = {
    js.executeScript(
      "return document.querySelector(arguments[0]) != null;",
      SCROLL_SELECTOR_JS + " div[data-react-window-index='" + index + "'] div[role='option'][aria-selected='true']"
    ) match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
    }
  }

  private def isRowPresent(browser: MaeveBrowser, index: Int): Boolean = {
    val row = Option(browser.getPageAsHtml.getFirstByXPath[HtmlElement](s"//$SCROLL_SELECTOR_NAME//div[data-react-window-index='$index']"))
    return row.nonEmpty
  }

  /**
   * Check if the retrieved text is a placeholder
   *
   * @return true if the text is an unbound template placeholder such as "{PLACE_NAME}", false otherwise
   * */
  private def isUnbound(text: String): Boolean = text.contains("{") && text.contains("}")

  /**
   * @return true if any currently rendered row still shows an unbound template placeholder (e.g. "{PLACE_NAME}")
   *         in its leading cell, i.e. the record data has not been populated yet.
   */
  private def hasUnboundRows(browser: MaeveBrowser): Boolean = {
    val rowElements = browser.getPageAsHtml.getByXPath[HtmlElement](s"//$SCROLL_SELECTOR_NAME//div[data-react-window-index]'").asScala
    rowElements.map(e => e.getTextContent.trim).exists(isUnbound)
  }

  private def urlContains(browser: MaeveBrowser, marker: String): Boolean =
    Option(browser.getCurrentUrl).exists(_.contains(marker))


  /**
   * Reads the place-name (the leading column, data-layoutitemid="1") of the list row with the given react-window
   * index. Mirrors the extractor's column selector so it reads the same field, falling back to the row's first
   * rich-displayer if the column markup ever differs.
   *
   * @return the row's place-name text, or None if the row is not currently rendered.
   */
  private def getRowName(browser: MaeveBrowser, index: Int): Option[String] = {
    val nameElement = browser.getPageAsHtml.getFirstByXPath[HtmlElement](s"//$SCROLL_SELECTOR_NAME//div[data-react-window-index='$index']'")
    Some(nameElement).map(_.getTextContent.trim())
  }

  /**
   * Reads the given row's place-name repeatedly until two consecutive reads agree on a bound (non-template,
   * non-empty) value, so a briefly stale/rebinding react-window cell settles before we act on it.
   *
   * @return the stable place-name, or None if the row never produced a bound value.
   */
  private def getBoundRowName(browser: MaeveBrowser, index: Int): Option[String] = {
    var previous: Option[String] = None
    var attempt = 0
    while (attempt < 6) {
      val current = getRowName(browser, index).map(_.trim).filter(s => s.nonEmpty && !isUnbound(s))
      if (current.isDefined && current == previous) {
        return current
      }
      previous = current.orElse(previous)
      Thread.sleep(pollIntervalMs)
      attempt += 1
    }
    previous
  }

  private def getScrollTop(js: JavascriptExecutor): Long = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]); return c ? c.scrollTop : 0;",
      SCROLL_SELECTOR_JS) match {
      case n: java.lang.Number => n.longValue()
      case _ => 0L
    }
  }

  private def setScrollTop(js: JavascriptExecutor, top: Long): Unit = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]); if (c) { c.scrollTop = arguments[1]; }",
      SCROLL_SELECTOR_JS, java.lang.Long.valueOf(top))
  }
  /**
   * Blocks until the search list page is loaded with rendered rows. Throws on timeout so MaeveDriver can react.
   *
   * @param browser the browser to interact with.
   */
  private def waitForList(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val deadline = System.currentTimeMillis() + maxWaitMs
    logger.info("Page Load: Load for {} initialised",browser.getURI)
    while (System.currentTimeMillis() < deadline &&
      !(urlContains(browser, PLACENAME_LIST_MARKER) && !isLoading(js) && renderedRowCount(js) > 0 && !hasUnboundRows(browser))) {
      Thread.sleep(pollIntervalMs)
    }
    if (!(urlContains(browser, PLACENAME_LIST_MARKER) && renderedRowCount(js) > 0 && !hasUnboundRows(browser))) {
      throw new IllegalStateException(s"Place-Name-Search list did not load (or bind its rows) within ${maxWaitMs}ms")
    }
    logger.info("Page Load: Load for {} complete",browser.getURI)
  }

  /**
   * Blocks (bounded) until the rendered list rows have bound their data: the spinner is gone, at least one row is
   * rendered, and no rendered row still shows an unbound template placeholder (e.g. "{PLACE_NAME}"). This prevents
   * reading/verifying rows before the ArcGIS Experience app has populated them.
   */
  private def waitForRowsBound(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val deadline = System.currentTimeMillis() + math.max(timeInMS, 3000L)
    while (System.currentTimeMillis() < deadline &&
      (renderedRowCount(js) == 0 || isLoading(js) || hasUnboundRows(browser))) {
      Thread.sleep(pollIntervalMs)
    }
  }

  private def atListBottom(js: JavascriptExecutor): Boolean = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]); return c ? (c.scrollTop + c.clientHeight) >= (c.scrollHeight - 2) : true;",
      SCROLL_SELECTOR_JS) match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => true
    }
  }

  /**
   * Tolerant comparison of two place-names: case-insensitive, ignoring punctuation/whitespace, and matching when
   * either is contained in the other (the list column and the detail title often differ by a county suffix etc.).
   */
  private def namesMatch(expected: String, actual: String): Boolean = {
    def normalise(s: String): String = s.toLowerCase//.replaceAll("[^a-z0-9]", "")
    val a = normalise(expected)
    val b = normalise(actual)
    a.nonEmpty && b.nonEmpty && (a.contains(b) || b.contains(a))
  }

  /**
   * Confirms the rendered row at the given index carries the expected place-name, re-reading a few times so a
   * transiently stale or still-binding react-window cell has a chance to settle. Returns true as soon as a read
   * matches; false only if the row stays bound to a different (non-empty) name across all attempts.
   */
  private def verifyRowName(browser: MaeveBrowser, index: Int, expected: String): Boolean = {
    var attempt = 0
    while (attempt < 5) {
      getRowName(browser, index) match {
        case Some(actual) if actual.nonEmpty && !isUnbound(actual) && namesMatch(expected, actual) => return true
        case _ => // not rendered / not yet bound / stale - wait and re-read
      }
      Thread.sleep(pollIntervalMs)
      attempt += 1
    }
    getRowName(browser, index).exists(a => a.nonEmpty && !isUnbound(a) && namesMatch(expected, a))
  }
  /**
   * Resumes a crawl against the CSV. Starting at the top, this walks the virtualised list downward one index at a
   * time: for each already-written index it scrolls the matching react-window row into view and compares that
   * exact row's place-name (the leading column, data-layoutitemid="1") to the value previously written for that
   * index. Comparison is index-driven (not pixel-driven, so variable row heights cannot knock it out of step) and
   * tolerant of react-window's transient recycling: a row is re-read a few times so a briefly stale/rebinding cell
   * settles before it is judged. Throws on a persistent mismatch, or if the list ends before every written record
   * is accounted for, so we never resume writing misaligned data.
   */
  private def verifyAndScrollToResume(browser: MaeveBrowser): Unit = {
    if (state.getWrittenCount() <= 0) {
      return
    }
    val js = browser.asInstanceOf[JavascriptExecutor]


    logger.info("Resuming: verifying {} already-written records against the list", state.getWrittenCount())

    setScrollTop(js, 0L)
    waitForRowsBound(browser)

    val perStepMs = pollIntervalMs + math.max(timeInMS, 3000L)
    val budgetMs = math.max(maxWaitMs, (state.getWrittenCount().toLong / PLACENAME_SCROLL_STEP_ROWS + 1L) * perStepMs)
    val deadline = System.currentTimeMillis() + budgetMs

    var idx = 0
    var endChecks = 0
    while (idx < state.getWrittenCount() && System.currentTimeMillis() < deadline) {
      if (!isRowPresent(browser, idx)) {
        // The row is not rendered yet: step the virtualised list down so react-window renders it.
        val before = getScrollTop(js)
        setScrollTop(js, before + PLACENAME_SCROLL_STEP_PX)
        waitForRowsBound(browser)
        Thread.sleep(pollIntervalMs)
        if (!isRowPresent(browser, idx) && atListBottom(js) && getScrollTop(js) == before) {
          endChecks += 1
          if (endChecks >= 3) {
            throw new IllegalStateException(
              s"Resume verification reached the end of the list before index $idx, but the CSV has ${state.getWrittenCount()} records. " +
                s"The list content may have changed; fix or remove the CSV to restart.")
          }
        } else {
          endChecks = 0
        }
      } else {
        endChecks = 0
        val expected = state.getWrittenName(idx).getOrElse("")
        if (expected.nonEmpty && !verifyRowName(browser, idx, expected)) {
          throw new IllegalStateException(
            s"Resume verification failed at index $idx: CSV place-name '$expected' but the list row shows " +
              s"'${getRowName(browser, idx).getOrElse("")}'. The list order/content may have changed; fix or remove the CSV " +
              s"to restart.")
        }
        if (idx == state.getWrittenCount() - 1 || (idx + 1) % 100 == 0) {
          logger.info("Resume verification progress: matched {} of {} records", Integer.valueOf(idx + 1),state.getWrittenCount())
        }
        idx += 1
      }
    }

    if (idx < state.getWrittenCount()) {
      throw new IllegalStateException(s"Resume verification timed out after verifying $idx of ${state.getWrittenCount()} records.")
    }
    logger.info("Resume verification passed: {} records match the list; continuing from index {}",idx, state.getWrittenCount())
  }

  /**
    * Function to execute actions at the start of a scrape.
    *
    * @param browser     the browser to interact with.
    * @param firstTarget the expected first url that will be accessed after this.
    */
  override def doFirstExecutionAction(browser: MaeveBrowser, firstTarget: Uri): Unit = {}

  /**
   * Function to execute actions after a page load.
   *
   * @param browser the browser to interact with.
   */
  override def doInitialPageAction(browser: MaeveBrowser): Unit = {
    // Validation tp ensure the page has loaded before proceeding
    Thread.sleep(timeInMS)
    waitForList(browser)

    if (state.resuming) {
      verifyAndScrollToResume(browser)
      state.resuming = false
    }
  }

  /**
    * Function to execute actions before extraction is called.
    *
    * @param browser the browser to interact with.
    */
  override def doBeforeExtractAction(browser: MaeveBrowser): Unit = {
    waitForData(browser)
  }

  /**
    * Blocks until the JavaScript-driven list widget has finished loading and actually rendered its records with
    * their data bound. Readiness is gated on the ArcGIS 'jimu-secondary-loading' spinner disappearing, at least
    * one row being present, and no row still showing an unbound {TEMPLATE} expression. Without this the extractor
    * can run against a still-loading DOM (empty, or rows showing {PLACE_NAME} etc.). A timeout throws so that
    * MaeveDriver's retry/refresh logic reacts instead of silently extracting nothing.
    *
    * @param browser the browser to interact with.
    */
  private def waitForData(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val deadline = System.currentTimeMillis() + maxWaitMs

    var ready = false
    while (!ready && System.currentTimeMillis() < deadline) {
      if (!isLoading(js) && renderedRowCount(js) > 0 && !hasUnboundTemplates(js)) {
        ready = true
      } else {
        Thread.sleep(pollIntervalMs)
      }
    }

    if (!ready) {
      throw new IllegalStateException(
        s"Place-name list ('$listSelector') did not finish loading within ${maxWaitMs}ms")
    }
    logger.info("Place-name list finished loading and rendered bound rows")
  }

  /**
    * @return true while the ArcGIS Experience app is still showing its loading spinner.
    */
  private def isLoading(js: JavascriptExecutor): Boolean = {
    val result = js.executeScript(
      "return document.querySelectorAll(arguments[0]).length;",
      loadingSelector)
    result match {
      case n: java.lang.Number => n.longValue() > 0
      case _ => false
    }
  }

  /**
    * @return the number of list rows currently rendered in the virtualised list. Zero means the list widget has
    *         rendered its chrome (toolbar, search) but not yet any record rows.
    */
  private def renderedRowCount(js: JavascriptExecutor): Long = {
    val result = js.executeScript(
      "return document.querySelectorAll(arguments[0] + ' div[data-react-window-index]').length;",
      listSelector)
    result match {
      case n: java.lang.Number => n.longValue()
      case _ => 0L
    }
  }

  /**
    * @return true while any currently rendered row still shows an unbound {TEMPLATE} expression, i.e. the JS app
    *         has injected the row markup but not yet resolved the record's field values.
    */
  private def hasUnboundTemplates(js: JavascriptExecutor): Boolean = {
    val result = js.executeScript(
      "var els = document.querySelectorAll(arguments[0] + ' div[data-testid=\"rich-displayer\"]');" +
        "for (var i = 0; i < els.length; i++) { if (/\\{[A-Z0-9_]+\\}/.test(els[i].textContent)) { return true; } }" +
        "return false;",
      listSelector)
    result match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
    }
  }

  /**
    * Function to execute actions after extraction is called. For this virtualised (react-window) list only a
    * handful of rows exist in the DOM at once, so we scroll the list container down one viewport to render the
    * next batch. When the container can no longer scroll and its height has stopped growing (all lazily-loaded
    * records are present) we flag the scrape as complete so [[com.szadowsz.logainm.target.placenames.ni.PlacenamesNiEndlessPageExtractor.shouldContinue]]
    * can stop the mining loop.
    *
    * @param browser the browser to interact with.
    */
  override def doAfterExtractAction(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val previousHeight = state.lastScrollHeight

    scrollDown(js)
    Thread.sleep(timeInMS) // allow the list to lazily fetch the next batch of records and re-render

    val (atBottom, scrollHeight) = scrollMetrics(js)
    state.lastScrollHeight = scrollHeight
    if (atBottom && scrollHeight <= previousHeight) {
      state.complete = true
      logger.info("Reached end of place-name list; {} unique rows captured", Integer.valueOf(state.seen.size))
    }
  }

  /**
    * Scrolls the virtualised list container down by one viewport, overlapping one row so no record is skipped.
    */
  private def scrollDown(js: JavascriptExecutor): Unit = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]);" +
        "if (c) { c.scrollTop = c.scrollTop + Math.max(1, c.clientHeight - arguments[1]); }",
      SCROLL_SELECTOR_JS, java.lang.Long.valueOf(rowOverlapPx))
  }

  /**
    * @return whether the list container is scrolled to the bottom, and its current total scroll height.
    */
  private def scrollMetrics(js: JavascriptExecutor): (Boolean, Long) = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]);" +
        "if (!c) { return null; }" +
        "return {atBottom: (c.scrollTop + c.clientHeight) >= (c.scrollHeight - 2), scrollHeight: c.scrollHeight};",
      SCROLL_SELECTOR_JS) match {
      case m: java.util.Map[_, _] =>
        val jm = m.asInstanceOf[java.util.Map[String, AnyRef]]
        val atBottom = jm.get("atBottom") match {
          case b: java.lang.Boolean => b.booleanValue()
          case _ => false
        }
        val height = jm.get("scrollHeight") match {
          case n: java.lang.Number => n.longValue()
          case _ => -1L
        }
        (atBottom, height)
      case _ => (true, -1L)
    }
  }

  /**
    * Function to execute final cleanup actions after a scrape.
    *
    * @param browser the browser to interact with.
    */
  override def doFinalExecutionAction(browser: MaeveBrowser): Unit = {}

}
