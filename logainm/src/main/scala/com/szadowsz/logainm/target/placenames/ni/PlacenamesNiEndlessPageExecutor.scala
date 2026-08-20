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
import com.szadowsz.logainm.target.placenames.ni.PlacenamesNiEndlessPageExecutor.{PLACENAME_DETAIL_MARKER, PLACENAME_LIST_MARKER, PLACENAME_SCROLL_STEP_PX, PLACENAME_SCROLL_STEP_ROWS, SCROLL_SELECTOR_JS, SCROLL_SELECTOR_NAME}
import com.szadowsz.maeve.core.browser.MaeveBrowser
import com.szadowsz.maeve.core.instruction.actions.ActionExecutor
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.ListHasAsScala


object PlacenamesNiEndlessPageExecutor {

  val PLACENAME_LIST_MARKER = "Place-Name-Search" // url fragment for the placename search list web page

  val PLACENAME_DETAIL_MARKER = "Place-Name-Info" // url fragment for a single place-name's detail ("More Info") page

  val PLACENAME_ROW_HEIGHT_PX = 71L // Nominal row height of the search list. Rows can vary in height, this is standard

  val PLACENAME_SCROLL_STEP_ROWS = 8 // How far to advance the scroll in rows. Kept below a viewport of rows so the downward crawl never jumps past (and misses) a target row.

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
  // Shorter settle taken between scroll steps: after a scroll we only need react-window to render the new window (and,
  // if it appears, the lazy-load spinner to be picked up) - not the full page-load poll. Speeds up long scrolls.
  private val scrollSettleMs = 200L
  // Scroll position of the results list when the current record was opened, restored after returning from its detail
  // page so the crawl does not have to re-scroll from the top for every record.
  private var lastListScrollTop = 0L
  // Place-name of the record most recently opened/extracted. Used to re-locate our position in the list (which can
  // re-render, and so shift react-window indices, when we return to it) rather than blindly trusting index + 1.
  private var lastProcessedName = ""

  // Full-reload the page after this many records to reset the ArcGIS Experience app's renderer memory (the tab was
  // observed to crash around 900 records of client-side list<->detail navigation). Set to 0 to disable.
  private val reloadEvery = 150


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
    val js = browser.asInstanceOf[JavascriptExecutor]
    js.executeScript(
      "return document.querySelector(arguments[0]) != null;",
      SCROLL_SELECTOR_JS + " div[data-react-window-index='" + index + "']") match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
    }
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
    val js = browser.asInstanceOf[JavascriptExecutor]
    js.executeScript(
      "var rows = document.querySelectorAll(arguments[0] + ' div[data-react-window-index]');" +
        "for (var i = 0; i < rows.length; i++) {" +
        "  var t = rows[i].textContent || '';" +
        "  if (t.indexOf('{') !== -1 && t.indexOf('}') !== -1) { return true; }" +
        "}" +
        "return false;",
      SCROLL_SELECTOR_JS) match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
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
    val js = browser.asInstanceOf[JavascriptExecutor]
    js.executeScript(
      "var row = document.querySelector(arguments[0]); if (!row) { return null; }" +
        "var cell = row.querySelector(\"div[data-layoutitemid='1'] div[data-testid='rich-displayer']\");" +
        "if (!cell) { cell = row.querySelector(\"div[data-testid='rich-displayer']\"); }" +
        "return cell ? (cell.textContent || '') : '';",
      SCROLL_SELECTOR_JS + " div[data-react-window-index='" + index + "']") match {
      case s: String => Some(s.trim)
      case _ => None
    }
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
   * Blocks until the detail page is loaded and its record fields are bound. Because the ArcGIS Experience app swaps the
   * detail content in asynchronously (the URL fragment and the bound fields can flip to "ready" a beat before the app
   * has actually finished re-rendering the record), readiness is double-checked: the "loaded" condition must hold on two
   * consecutive polls (pollIntervalMs apart) before we treat the detail page as settled. Throws on timeout.
   */
  private def waitForDetailPage(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val deadline = System.currentTimeMillis() + maxWaitMs

    def loaded: Boolean = urlContains(browser, PLACENAME_DETAIL_MARKER) && detailBound(js)

    var confirmations = 0
    while (confirmations < 2 && System.currentTimeMillis() < deadline) {
      if (loaded) {
        confirmations += 1
      } else {
        confirmations = 0
      }
      if (confirmations < 2) {
        Thread.sleep(pollIntervalMs)
      }
    }
    if (confirmations < 2) {
      throw new IllegalStateException(s"Place-Name-Info detail page did not load within ${maxWaitMs}ms")
    }
  }

  /**
   * Scrolls the virtualised list container down by one viewport, overlapping one row so no record is skipped.
   */
  private def scrollDown(js: JavascriptExecutor): Unit = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]);" +
        "if (c) { c.scrollTop = c.scrollTop + Math.max(1, c.clientHeight - arguments[1]); }",
      SCROLL_SELECTOR_NAME, java.lang.Long.valueOf(rowOverlapPx))
  }

  /**
   * @return whether the list container is scrolled to the bottom, and its current total scroll height.
   */
  private def scrollMetrics(js: JavascriptExecutor): (Boolean, Long) = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]);" +
        "if (!c) { return null; }" +
        "return {atBottom: (c.scrollTop + c.clientHeight) >= (c.scrollHeight - 2), scrollHeight: c.scrollHeight};",
      SCROLL_SELECTOR_NAME) match {
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
   * @return true once the detail page has a bound "Place-Name ID" field (i.e. the record has finished loading).
   */
  private def detailBound(js: JavascriptExecutor): Boolean = {
    js.executeScript(
      "var els = document.querySelectorAll('div[data-testid=\"rich-displayer\"]');" +
        "for (var i = 0; i < els.length; i++) { var t = (els[i].textContent || '').trim();" +
        " if (t.indexOf('Place-Name ID:') === 0 && /[0-9]/.test(t)) { return true; } }" +
        "return false;") match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
    }
  }

  /**
   * Dispatches a full mouse-event sequence at the element so EXB's React handler fires. Uses events rather than
   * element.click() so a virtualised row that is only partially in view is still actionable.
   *
   * @return true if the element was found, false otherwise.
   */
  private def clickElement(js: JavascriptExecutor, selector: String): Boolean = {
    js.executeScript(
      "var el = document.querySelector(arguments[0]); if (!el) { return false; }" +
        "['pointerdown','mousedown','mouseup','click'].forEach(function(t){" +
        " el.dispatchEvent(new MouseEvent(t, {bubbles: true, cancelable: true, view: window})); });" +
        "return true;",
      selector) match {
      case b: java.lang.Boolean => b.booleanValue()
      case _ => false
    }
  }

  /**
   * Opens the detail page for the given row and confirms it is the correct record before returning: the list
   * row's place-name is read first, then - after navigating - the detail page's title is compared against it.
   * On a mismatch the browser returns to the list and the open is retried, so a mis-selection can never silently
   * write a wrong/duplicate record. Throws if it cannot reconcile after several attempts rather than proceeding.
   *
   * @return true once the browser is on the confirmed detail page; false if the row could not be opened at all
   *         (e.g. it is past the end of the list).
   */
  private def openAndVerify(browser: MaeveBrowser, js: JavascriptExecutor, index: Int): Boolean = {
    val maxAttempts = 3
    var attempt = 0
    while (attempt < maxAttempts) {
      attempt += 1
      getBoundRowName(browser, index) match {
        case None =>
          logger.warn("Row {} has no bound place-name yet (attempt {}/{}); retrying",
            Integer.valueOf(index), Integer.valueOf(attempt), Integer.valueOf(maxAttempts))
          waitForRowsBound(browser)
        case Some(expected) =>
          if (throttleMs > 0) {
            Thread.sleep(throttleMs) // deliberately slow down so we do not hammer the site
          }
          if (!openRow(js, index)) {
            return false // the row/button is not present - treated as the end of the list by the caller
          }
          waitForDetailPage(browser)
          val shown = detailTitle(js)
          if (namesMatch(expected, shown)) {
            logger.info("Opened detail page '{}' for row {}", shown, Integer.valueOf(index))
            lastProcessedName = expected // remembered so we can re-locate our position after returning to the list
            state.currentListName = expected // the clean list-page name the extractor writes (no county suffix)
            return true // confirmed correct record; leave the browser here for the extractor
          }
          logger.warn("'More Info' opened the wrong record for row {} (expected '{}' but detail shows '{}'); recovering",
            Integer.valueOf(index), expected, shown)
          goBackToList(browser)
          setScrollTop(js, 0L)
          waitForRowsBound(browser)
          if (!scrollToIndex(browser)) {
            return false
          }
      }
    }
    throw new IllegalStateException(
      s"'More Info' kept opening the wrong record for index $index after $maxAttempts attempts; aborting rather " +
        s"than writing misaligned data.")
  }

  /**
   * Selects the target row and clicks its "More Info". Every row's "More Info" button carries the same static
   * href, so the record actually opened is whichever list row the widget currently has selected. We therefore
   * first select the target row via its (full-height, untransformed) role="option" element - keyed by
   * data-react-window-index so we always act on the intended row - and only then click its "More Info" button.
   * This removes the off-by-one that occurred when clicking the (vertically offset) button without an explicit
   * selection.
   *
   * @return true if the row was selected and its "More Info" clicked; false if the elements were not found.
   */
  private def openRow(js: JavascriptExecutor, index: Int): Boolean = {
    val rowSelector = SCROLL_SELECTOR_NAME + " div[data-react-window-index='" + index + "']"
    // Select the row so it becomes the widget's current feature.
    if (!clickElement(js, rowSelector + " div[role='option']")) {
      return false
    }
    // Give the selection a brief moment to register before navigating. aria-selected is not reliably reflected on
    // these result rows, but the click still updates the widget's selected record (which is what the shared static
    // "More Info" href resolves against - openAndVerify confirms the opened detail page by name regardless). So an
    // unconfirmed selection is not an error: we just settle briefly (exiting early when it *is* observable, e.g. the
    // pre-selected first row) rather than burning the full page-load budget polling for a signal that never comes.
    val deadline = System.currentTimeMillis() + 1500L
    while (!isRowSelected(js, index) && System.currentTimeMillis() < deadline) {
      Thread.sleep(pollIntervalMs)
    }
    if (!isRowSelected(js, index)) {
      logger.debug("Row {} selection not observable via aria-selected; proceeding (the opened detail page is verified by name)", Integer.valueOf(index))
    }
    clickMoreInfo(js, index)
  }

  /**
   * @return the detail page's title (the place-name heading in widget_829), or an empty string if not present.
   */
  private def detailTitle(js: JavascriptExecutor): String = {
    js.executeScript(
      "var el = document.querySelector('div[data-widgetid=\"widget_829\"] div[data-testid=\"rich-displayer\"]');" +
        "return el ? (el.textContent || '').trim() : '';") match {
      case s: String => s.trim
      case _ => ""
    }
  }

  /**
   * Scrolls the virtualised list down until the row at the given index is rendered. Scrolling proceeds in steps,
   * pausing for lazy-loaded records between each, so far-down rows (e.g. when resuming) become available. The
   * list starts at the top after every load / go-back, so the walk is always downward.
   *
   * @return true once the row is present in the DOM, false if the index is past the end of the list.
   */
  private def scrollToIndex(browser: MaeveBrowser): Boolean = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val index = state.currentIndex
    if (isRowPresent(browser, index)) {
      return true // already rendered (the list often keeps its position after returning from a detail page)
    }
    // Scale the deadline with the distance: reaching the index takes (index / rows-per-step) scroll steps, and each
    // step can cost a short settle plus, occasionally, a full lazy-load wait - so budget that worst case per step,
    // with maxWaitMs as a floor for short scrolls.
    val stepRows = math.max(1L, PLACENAME_SCROLL_STEP_ROWS)
    val perStepMs = scrollSettleMs + math.max(timeInMS, 3000L)
    val budgetMs = math.max(maxWaitMs, (index.toLong / stepRows + 1L) * perStepMs)
    logger.info("Scrolling to row {}, budget {}ms", Integer.valueOf(index), java.lang.Long.valueOf(budgetMs))
    val deadline = System.currentTimeMillis() + budgetMs
    var endChecks = 0
    var lastTop = -1L
    // Step down by a fixed amount (never a pixel target computed from the index, which drifts on variable-height
    // rows) until the row's react-window element renders, or we are clamped at the bottom of the list.
    while (System.currentTimeMillis() < deadline) {
      setScrollTop(js, getScrollTop(js) + PLACENAME_SCROLL_STEP_PX)
      waitForRowsBound(browser) // let react-window render / any lazily loaded records bind before checking again
      Thread.sleep(scrollSettleMs)
      if (isRowPresent(browser, index)) {
        logger.info("Found Row, Stopping Scrolling")
        return true
      }
      val top = getScrollTop(js)
      // Only count towards "end of list" when we are clamped at the bottom, made no progress, AND the app is not
      // mid lazy-load - so the shortened settle cannot mistake an in-flight fetch for the true end of the list.
      if (atListBottom(js) && top == lastTop && !isLoading(js)) {
        endChecks += 1
        if (endChecks >= 3) {
          logger.info("Did Not Find Row, Stopping Scrolling")
          return false // clamped at the bottom and the row never appeared -> end of list
        }
      } else {
        endChecks = 0
      }
      lastTop = top
    }
    logger.info("Ran Out Of Time, Stopping Scrolling")
    isRowPresent(browser, index)
  }

  /**
   * Clicks the "More Info" button of the given row to navigate to its detail page.
   *
   * @return true if the button was found and clicked.
   */
  private def clickMoreInfo(js: JavascriptExecutor, index: Int): Boolean = {
    clickElement(js, SCROLL_SELECTOR_NAME + " div[data-react-window-index='" + index + "'] a[aria-label='More Info']")
  }

  /**
   * Returns to the search list, clicking the detail page's "Go Back" button (falling back to browser history).
   */
  private def goBackToList(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    if (!clickElement(js, "a[aria-label='Go Back']")) {
      browser.navigate().back()
    }
    waitForList(browser)
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

    val perStepMs = scrollSettleMs + math.max(timeInMS, 3000L)
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
        Thread.sleep(scrollSettleMs)
        if (!isRowPresent(browser, idx) && atListBottom(js) && getScrollTop(js) == before && !isLoading(js)) {
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
    if (state.complete) {
      return
    }
    val js = browser.asInstanceOf[JavascriptExecutor]

    waitForData(browser)

    if (!scrollToIndex(browser)) {
      state.complete = true
      logger.info("Reached end of place-name list after {} records", Integer.valueOf(state.currentIndex))
      return
    }

    if (!openAndVerify(browser, js, state.currentIndex)) {
      state.complete = true
      logger.warn("Could not open the detail page for row {}; stopping", Integer.valueOf(state.currentIndex))
      return
    }
  }

  /**
    * @return true while the ArcGIS Experience app is still showing its loading spinner.
    */
  private def isLoading(js: JavascriptExecutor): Boolean = {
    js.executeScript("return document.querySelectorAll(arguments[0]).length;", loadingSelector) match {
      case n: java.lang.Number => n.longValue() > 0
      case _ => false
    }
  }

  private def renderedRowCount(js: JavascriptExecutor): Long = {
    js.executeScript(
      "return document.querySelectorAll(arguments[0] + ' div[data-react-window-index]').length;",
      listSelector) match {
      case n: java.lang.Number => n.longValue()
      case _ => 0L
    }
  }

  /**
   * @return the (react-window index, place-name) of every results-list row currently rendered, read from a single
   *         live-DOM snapshot. Used to re-locate our position after the list re-renders.
   */
  private def renderedRows(browser: MaeveBrowser): Seq[(Int, String)] = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    js.executeScript(
      "var rows = document.querySelectorAll(arguments[0] + ' div[data-react-window-index]');" +
        "var out = [];" +
        "for (var i = 0; i < rows.length; i++) {" +
        "  var idx = rows[i].getAttribute('data-react-window-index');" +
        "  var cell = rows[i].querySelector(\"div[data-layoutitemid='1'] div[data-testid='rich-displayer']\");" +
        "  if (!cell) { cell = rows[i].querySelector(\"div[data-testid='rich-displayer']\"); }" +
        "  var name = cell ? (cell.textContent || '') : '';" +
        "  out.push([idx, name]);" +
        "}" +
        "return out;",
      SCROLL_SELECTOR_JS) match {
      case list: java.util.List[_] =>
        list.asScala.toSeq.flatMap {
          case pair: java.util.List[_] if pair.size() >= 2 =>
            val idx = Option(pair.get(0)).map(_.toString).flatMap(_.toIntOption)
            val name = Option(pair.get(1)).map(_.toString.trim).getOrElse("")
            idx.map(_ -> name)
          case _ => None
        }
      case _ => Seq.empty
    }
  }

  /**
   * Re-aligns [[state.currentIndex]] after returning to the (possibly re-rendered) search list. Scrolls the row we
   * just read back into view and re-locates the just-read place-name, then continues from the row immediately after
   * it. This anchors the index to the live list, so a record removed on reload (the one just read, or an earlier
   * one) shifts our position rather than causing a skipped or duplicated record. Falls back to a plain advance when
   * there is nothing to match against.
   */
  private def resyncIndex(browser: MaeveBrowser): Unit = {
    val expectedIndex = state.currentIndex

    // Bring the just-read row (and its neighbours) back into the rendered window so their names can be compared.
    scrollToIndex(browser)

    if (lastProcessedName.isEmpty) {
      logger.warn(s"Resync has no just-read place-name to match against at index $expectedIndex; advancing to ${expectedIndex + 1}")
      state.currentIndex = expectedIndex + 1
      return
    }

    val matches = renderedRows(browser).collect {
      case (i, n) if n.nonEmpty && !isUnbound(n) && namesMatch(lastProcessedName, n) => i
    }

    matches.sortBy(i => math.abs(i - expectedIndex)).headOption match {
      case Some(found) =>
        if (found != expectedIndex) {
          logger.info(s"Resynced after reload: '$lastProcessedName' now at index $found (was $expectedIndex); " +
            s"continuing from ${found + 1}")
        }
        state.currentIndex = found + 1
      case None =>
        val here = getBoundRowName(browser, expectedIndex).getOrElse("")
        if (here.nonEmpty && namesMatch(lastProcessedName, here)) {
          // renderedRows missed it (e.g. a row transiently unbound during the snapshot) but the slot still holds it.
          logger.info(s"Resync: '$lastProcessedName' still at index $expectedIndex; continuing from ${expectedIndex + 1}")
          state.currentIndex = expectedIndex + 1
        } else if (here.isEmpty) {
          logger.warn(s"Resync could not find or confirm '$lastProcessedName' near index $expectedIndex " +
            s"(row not rendered/bound); advancing to ${expectedIndex + 1}")
          state.currentIndex = expectedIndex + 1
        } else {
          logger.warn(s"Resync: just-read '$lastProcessedName' is no longer in the list near index $expectedIndex " +
            s"(row now shows '$here'); treating it as removed and continuing from that record")
          state.currentIndex = expectedIndex // the next record has slid into this slot
        }
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
    if (state.complete) {
      return
    }
    goBackToList(browser)

    // The search list can re-render when we return to it; if the record we just read (or one before it) was dropped,
    // react-window indices shift. Re-locate the just-read place-name and continue from the row after it, so the index
    // stays aligned to the live list rather than to a stale running count.
    resyncIndex(browser)

    if (reloadEvery > 0 && state.currentIndex % reloadEvery == 0) {
      logger.info("Reloading the page after {} records to reset renderer memory", Integer.valueOf(state.currentIndex))
      browser.navigate().refresh()
      waitForList(browser)
    }
  }

  /**
    * Function to execute final cleanup actions after a scrape.
    *
    * @param browser the browser to interact with.
    */
  override def doFinalExecutionAction(browser: MaeveBrowser): Unit = {}

}
