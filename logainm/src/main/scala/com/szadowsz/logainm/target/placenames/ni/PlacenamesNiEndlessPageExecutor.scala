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
import com.szadowsz.maeve.core.browser.MaeveBrowser
import com.szadowsz.maeve.core.instruction.actions.ActionExecutor
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.LoggerFactory

/**
  * Executor for when no javascript interaction is called for, but you need to wait a set period of time after each load.
  *
  * Created on 18/10/2016.
  */
final class PlacenamesNiEndlessPageExecutor(timeInMS : Long, state: PlacenamesNiListState) extends ActionExecutor {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // The ArcGIS Experience list widget container that holds the rendered place-name records.
  private val listSelector = "div[class=\"widget-list d-flex\"]"
  // The virtualised (react-window) scroll container inside the list widget.
  private val scrollSelector = "div.widget-list-list"
  // Spinner the ArcGIS Experience app shows while it is still fetching / rendering the list.
  private val loadingSelector = "div.jimu-secondary-loading"
  // Overlap one row height between scroll steps so react-window virtualisation can never skip a record.
  private val rowOverlapPx = 71L
  // Total time we are prepared to wait for the JS app to finish rendering the list.
  private val maxWaitMs = math.max(timeInMS * 15, 30000L)
  private val pollIntervalMs = 500L

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
     // brief settle to let the ArcGIS Experience app bootstrap before we start polling for data
     Thread.sleep(timeInMS)
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
      scrollSelector, java.lang.Long.valueOf(rowOverlapPx))
  }

  /**
    * @return whether the list container is scrolled to the bottom, and its current total scroll height.
    */
  private def scrollMetrics(js: JavascriptExecutor): (Boolean, Long) = {
    js.executeScript(
      "var c = document.querySelector(arguments[0]);" +
        "if (!c) { return null; }" +
        "return {atBottom: (c.scrollTop + c.clientHeight) >= (c.scrollHeight - 2), scrollHeight: c.scrollHeight};",
      scrollSelector) match {
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
