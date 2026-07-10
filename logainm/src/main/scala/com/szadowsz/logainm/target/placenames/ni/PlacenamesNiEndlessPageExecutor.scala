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
import com.szadowsz.maeve.core.browser.{MaeveBrowser, MaeveRemoteBrowser}
import com.szadowsz.maeve.core.instruction.actions.ActionExecutor
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.LoggerFactory

/**
  * Executor for when no javascript interaction is called for, but you need to wait a set period of time after each load.
  *
  * Created on 18/10/2016.
  */
final class PlacenamesNiEndlessPageExecutor(timeInMS : Long) extends ActionExecutor {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // The ArcGIS Experience list widget container that holds the rendered place-name records.
  private val listSelector = "div[class=\"widget-list d-flex\"]"
  // Spinner the ArcGIS Experience app shows while it is still fetching / rendering the list.
  private val loadingSelector = "div.jimu-secondary-loading"
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
    * Blocks until the JavaScript-driven list widget has finished loading and actually rendered its
    * records. Readiness is gated on the ArcGIS 'jimu-secondary-loading' spinner disappearing and the
    * list container having content. Without this the extractor can run against a still-loading, empty
    * DOM. A timeout throws so that MaeveDriver's retry/refresh logic reacts instead of silently
    * extracting nothing.
    *
    * @param browser the browser to interact with.
    */
  private def waitForData(browser: MaeveBrowser): Unit = {
    val js = browser.asInstanceOf[JavascriptExecutor]
    val deadline = System.currentTimeMillis() + maxWaitMs

    var ready = false
    while (!ready && System.currentTimeMillis() < deadline) {
      if (!isLoading(js) && renderedContentCount(js) > 0) {
        ready = true
      } else {
        Thread.sleep(pollIntervalMs)
      }
    }

    if (!ready) {
      throw new IllegalStateException(
        s"Place-name list ('$listSelector') did not finish loading within ${maxWaitMs}ms")
    }
    logger.info("Place-name list finished loading and rendered content")
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
    * Counts the descendant elements of the list widget container. A count of zero means the widget has
    * not rendered any records yet.
    */
  private def renderedContentCount(js: JavascriptExecutor): Long = {
    val result = js.executeScript(
      "var el = document.querySelector(arguments[0]); return el ? el.getElementsByTagName('*').length : 0;",
      listSelector)
    result match {
      case n: java.lang.Number => n.longValue()
      case _ => 0L
    }
  }

  /**
    * Function to execute actions after extraction is called.
    *
    * @param browser the browser to interact with.
    */
  override def doAfterExtractAction(browser: MaeveBrowser): Unit = {
    val headless = browser.asInstanceOf[MaeveRemoteBrowser]

    val screenHeight = headless.executeScript("return window.screen.height;")
    println(s"Screen Height: $screenHeight")
  }

  /**
    * Function to execute final cleanup actions after a scrape.
    *
    * @param browser the browser to interact with.
    */
  override def doFinalExecutionAction(browser: MaeveBrowser): Unit = {}

}
