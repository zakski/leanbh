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

import javax.xml.parsers.DocumentBuilderFactory

import org.openqa.selenium.chrome.ChromeDriver
import org.w3c.{dom => w3c}

/**
  * This is an extension to the Chrome Driver to bring it under the Maeve Browser Umbrella. It basically provides the additional page options that we require
  * to the chrome driver. Was originally built to be used with the Firefox Driver, however firefox is in a state of transition and the new driver library
  * does not currently work with the latest firefox version.
  *
  * Created on 16/10/2016.
  */
class MaeveRemoteBrowser(private val conf: MaeveConf) extends ChromeDriver(conf.buildChromeProfile) with MaeveBrowser {
  private lazy val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

  /**
    * Get the currently accessed web page as Jsoup Document.
    *
    * @return A HTML document.
    */
  override def getPageAsDom: w3c.Document =  docBuilder.parse(getPageSource)
}
