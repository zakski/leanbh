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

import org.htmlunit.{BrowserVersion, ProxyConfig}
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.htmlunit.options.{HtmlUnitDriverOptions, HtmlUnitOptionNames}
import org.openqa.selenium.{Platform, Proxy}

import java.util

object MaeveConf {
  val PROXY = "proxy"

  val SKIP_TEST_PROXY = "skipProxyTest"

  val CHROME_DONT_TRACK = "enable_do_not_track"
  val CHROME_PREFS = "prefs"
}

/**
 * Created on 13/05/2016.
 */
class MaeveConf extends HtmlUnitDriverOptions {

  override def getBrowserName: String = "firefox"

  override def getBrowserVersion: String = BrowserVersion.FIREFOX_ESR.toString

  override def getPlatformName: Platform = Platform.getCurrent

  def getProxy: Proxy = Option(getCapability(MaeveConf.PROXY).asInstanceOf[Proxy]).getOrElse(new Proxy())

  def getProxyConfig: ProxyConfig = getCapability(HtmlUnitOptionNames.optProxyConfig).asInstanceOf[ProxyConfig]

  def isUseInsecureSSL: Boolean = getCapability(HtmlUnitOptionNames.optUseInsecureSSL).asInstanceOf[Boolean]

  def isRedirectEnabled: Boolean =  getCapability(HtmlUnitOptionNames.optIsRedirectEnabled).asInstanceOf[Boolean]

  def isCssEnabled: Boolean = getCapability(HtmlUnitOptionNames.optCssEnabled).asInstanceOf[Boolean]

  def isPopupBlockerEnabled: Boolean = getCapability(HtmlUnitOptionNames.optPopupBlockerEnabled).asInstanceOf[Boolean]

  def isGeolocationEnabled: Boolean = getCapability(HtmlUnitOptionNames.optGeolocationEnabled).asInstanceOf[Boolean]

  def isDoNotTrackEnabled: Boolean = getCapability(HtmlUnitOptionNames.optDoNotTrackEnabled).asInstanceOf[Boolean]

  def isThrowExceptionOnFailingStatusCode: Boolean = getCapability(HtmlUnitOptionNames.optThrowExceptionOnFailingStatusCode).asInstanceOf[Boolean]

  def isPrintContentOnFailingStatusCode: Boolean = getCapability(HtmlUnitOptionNames.optPrintContentOnFailingStatusCode).asInstanceOf[Boolean]

  def isThrowExceptionOnScriptError: Boolean = getCapability(HtmlUnitOptionNames.optThrowExceptionOnScriptError).asInstanceOf[Boolean]

  def shouldSkipProxyTest: Boolean = {
    val opt = Option(getCapability(MaeveConf.SKIP_TEST_PROXY)).map(_.asInstanceOf[Boolean])
    opt.getOrElse(true)
  }

  override def setJavaScriptEnabled(enableJavascript: Boolean): MaeveConf = super.setJavaScriptEnabled(enableJavascript).asInstanceOf[MaeveConf]

  def setUseInsecureSSL(useInsecureSSL: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optUseInsecureSSL,useInsecureSSL)
    this
  }

  def setRedirectEnabled(enableRedirect: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optIsRedirectEnabled,enableRedirect)
    this
  }

  def setCssEnabled(enableCSS: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optCssEnabled,enableCSS)
    this
  }

  def setPopupBlockerEnabled(enablePopupBlock: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optPopupBlockerEnabled,enablePopupBlock)
    this
  }
  
  def setGeolocationEnabled(enableGeoTrack: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optGeolocationEnabled,enableGeoTrack)
    this
  }

  def setDoNotTrackEnabled(enableDoNotTrack: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optDoNotTrackEnabled,enableDoNotTrack)
    this
  }
  
  def setThrowExceptionOnFailingStatusCode(throwOnFailStatCode: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optThrowExceptionOnFailingStatusCode,throwOnFailStatCode)
    this
  }
  
  def setPrintContentOnFailingStatusCode(printOnFailStatCode: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optPrintContentOnFailingStatusCode,printOnFailStatCode)
    this
  }
  
  def setThrowExceptionOnScriptError(throwOnScriptError: java.lang.Boolean): MaeveConf = {
    setCapability(HtmlUnitOptionNames.optThrowExceptionOnScriptError,throwOnScriptError)
    this
  }

  def setNoProxy(): MaeveConf = {
    setCapability(MaeveConf.PROXY,new Proxy())
    this
  }

  def setHTTPProxy(host: String, port: Int, noProxyHosts: List[String]): MaeveConf = {
    val proxy = new Proxy()
    proxy.setHttpProxy(host + ":" + port)
    setCapability(MaeveConf.PROXY,proxy)
    this
  }

  def setSkipProxyTestEnabled(enable: java.lang.Boolean): MaeveConf = {
    setCapability(MaeveConf.SKIP_TEST_PROXY,enable)
    this
  }


  def buildChromeProfile: ChromeOptions = {
    val opts = new ChromeOptions()
    if (getProxy.getHttpProxy != null) {
      opts.setProxy(getProxy)
      opts.addArguments("--proxy-server=" + getProxy.getHttpProxy)
    }

    val preferences = new util.HashMap[String, Object]()
    // TODO translate more preferences
    preferences.put(MaeveConf.CHROME_DONT_TRACK, isDoNotTrackEnabled.asInstanceOf[java.lang.Boolean])
    opts.setExperimentalOption(MaeveConf.CHROME_PREFS, preferences)

    opts
  }

  def overrideConf(defaultConf : MaeveConf): MaeveConf = {
    defaultConf.getCapabilityNames.forEach(cap => {
      setCapability(cap,defaultConf.getCapability(cap))
    })
    defaultConf.getExtraCapabilityNames.forEach(cap => {
      setCapability(cap,defaultConf.getExtraCapability(cap))
    })
    this
  }
}
