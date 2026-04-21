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
package com.szadowsz.maeve.core.browser

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.w3c.{dom => w3c}

/**
  * Extension of Htmlunit web browser to allow for a bigger variety of page formats to be returned and configuration options applied.
  *
  * Created on 13/05/2016.
  */
class MaeveHeadlessBrowser(private val conf: MaeveConf) extends HtmlUnitDriver(conf) with MaeveBrowser {

  /**
    * We override this method to allow for the customisation of the WebClient that the HtmlUnit driver uses.
    *
    * @param client The client to modify
    * @return The modified client
    */
  override protected def modifyWebClient(client: WebClient): WebClient = {
    val opt = client.getOptions
    opt.setUseInsecureSSL(conf.isUseInsecureSSL)
    opt.setRedirectEnabled(conf.isRedirectEnabled)
    opt.setJavaScriptEnabled(conf.isJavascriptEnabled)
    opt.setCssEnabled(conf.isCssEnabled)
    opt.setAppletEnabled(conf.isAppletEnabled)
    opt.setPopupBlockerEnabled(conf.isPopupBlockerEnabled)
    opt.setGeolocationEnabled(conf.isGeolocationEnabled)
    opt.setDoNotTrackEnabled(conf.isDoNotTrackEnabled)
    opt.setThrowExceptionOnFailingStatusCode(conf.isThrowExceptionOnFailingStatusCode)
    opt.setPrintContentOnFailingStatusCode(conf.isPrintContentOnFailingStatusCode)
    opt.setThrowExceptionOnScriptError(conf.isThrowExceptionOnScriptError)
    opt.setActiveXNative(conf.isActiveXNative)

    if (conf.isJavascriptEnabled){
      client.setAjaxController(new NicelyResynchronizingAjaxController())
    }

    client
  }

  /**
    * Expose the Webclient.
    *
    * @return the web client the driver is using.
    */
  def getClient: WebClient = getWebClient

  /**
    * Expose the browser window.
    *
    * @return the window the driver is using.
    */
  def getWindow: WebWindow = getCurrentWindow

  /**
    * Get the currently accessed page.
    *
    * @return An abstract page that represents some content returned from a server.
    */
  def getPage: Page = getCurrentWindow.getEnclosedPage

  /**
    * Get the currently accessed web page.
    *
    * @return A representation of an HTML page returned from a server.
    */
  override def getPageAsHtml: HtmlPage = getCurrentWindow.getEnclosedPage.asInstanceOf[HtmlPage]

  /**
    * Get the currently accessed web page as Jsoup Document.
    *
    * @return A HTML document.
    */
  override def getPageAsDom: w3c.Document =  getPageAsHtml

  /**
    * Expose the browser history.
    *
    * @return the history of the driver.
    */
  def getHistory: History = getWindow.getHistory

  /**
    * Function to clear all browser history.
    */
  override def clearHistory(): Unit = {
    val hist = getHistory
    (0 until hist.getLength).foreach(i => hist.removeCurrent())
    super.clearHistory()
  }
}
