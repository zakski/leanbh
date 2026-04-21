package com.szadowsz.babby.target.web.meanOfNames

import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.browser.MaeveBrowser
import com.szadowsz.maeve.core.instruction.actions.ActionExecutor

// TODO replace this with non-stopgap code
class MeaningOfNamesPageExecutor extends ActionExecutor {
  private val incrementPattern = "(\\d+)".r

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
  override def doInitialPageAction(browser: MaeveBrowser): Unit = {}

  /**
    * Function to execute actions before extraction is called.
    *
    * @param browser the browser to interact with.
    */
  override def doBeforeExtractAction(browser: MaeveBrowser): Unit = {}

  /**
    * Function to execute actions after extraction is called.
    *
    * @param browser the browser to interact with.
    */
  override def doAfterExtractAction(browser: MaeveBrowser): Unit = {
    val url = browser.getURI
    val path = url.path
    if (browser.getPageAsHtml.getFirstByXPath("//a[text()='>']") != null) {
      val n = incrementPattern.findFirstIn(path).get.toInt + 1
      val nPath = incrementPattern.replaceFirstIn(path, n.toString)
      browser.get(url.withPath(nPath))
    }
  }

  /**
    * Function to execute final cleanup actions after a scrape.
    *
    * @param browser the browser to interact with.
    */
  override def doFinalExecutionAction(browser: MaeveBrowser): Unit = {}
}
