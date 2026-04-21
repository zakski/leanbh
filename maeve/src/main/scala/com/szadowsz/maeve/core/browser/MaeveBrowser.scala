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

import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.szadowsz.common.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.WebDriver
import org.w3c.{dom => w3c}

/**
  * Trait to cover both Headless and Chrome driver situations. While Headless is preferable from a resource perspective, certain things are impossible to
  * scrape without a physical browser; particularly web pages heavily reliant on Ajax calls.
  *
  * Created on 16/10/2016.
  */
trait MaeveBrowser extends WebDriver {


  /**
    * Function to access a page
    *
    * @param url the uri to access.
    */
  def get(url : Uri) : Unit = get(url.toString)

  /**
    * Gets the true currently accessed uri.
    *
    * @return the uri the driver is currently accessing.
    */
  def getURI: Uri = Uri(getCurrentUrl)

  /**
    * Get the currently accessed web page.
    *
    * @return A representation of an HTML page returned from a server.
    */
  def getPageAsHtml: HtmlPage = throw new UnsupportedOperationException("Cannot Provide HtmlUnit HTML Page")

  /**
    * Get the currently accessed web page as Jsoup Document.
    *
    * @return A HTML document.
    */
  def getPageAsJsoup: Document = Jsoup.parse(getPageSource)

  /**
    * Get the currently accessed web page as Jsoup Document.
    *
    * @return A HTML document.
    */
  def getPageAsDom: w3c.Document

  /**
    * Function to clear all broswer history.
    */
  def clearHistory() : Unit = {
    manage().deleteAllCookies()
  }
}
