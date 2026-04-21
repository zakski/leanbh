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

import java.util

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.{Capabilities, Platform, Proxy}

import collection.JavaConverters._

object MaeveConf {
  val PROXY = "proxy"
  val JAVASCRIPT = "enableJS"
  val INSECURE_SSL = "useInsecureSSL"
  val ENABLE_REDIRECT = "enableRedirect"
  val ENABLE_CSS = "enableCSS"
  val ENABLE_APPLET = "enableApplet"
  val ENABLE_POPUP_BLOCK = "enablePopupBlock"
  val ENABLE_GEO_TRACK = "enableGeoTrack"
  val ENABLE_DONT_TRACK = "enableDoNotTrack"
  val ENABLE_NATIVE_ACTIVEX = "enableActiveXNative"
  val THROW_FAIL_STATUS = "throwOnFailStatCode"
  val THROW_SCRIPT_ERROR = "throwOnScriptError"
  val PRINT_FAIL_STATUS = "printOnFailStatCode"

  val CHROME_DONT_TRACK = "enable_do_not_track"
  val CHROME_PREFS = "prefs"

  val SKIP_TEST_PROXY = "skipProxyTest"

}

/**
  * Created on 13/05/2016.
  */
case class MaeveConf(protected val caps: Map[String, AnyRef] = Map()) extends Capabilities {

  override def is(capabilityName: String): Boolean = {
    val cap = caps.get(capabilityName)
    cap.isDefined && cap.contains(true: java.lang.Boolean)
  }

  override def getCapability(capabilityName: String): AnyRef = caps.get(capabilityName).orNull

  override def isJavascriptEnabled: Boolean = caps.getOrElse(MaeveConf.JAVASCRIPT, true).asInstanceOf[Boolean]

  def shouldSkipProxyTest: java.lang.Boolean = caps.getOrElse(MaeveConf.SKIP_TEST_PROXY, false).asInstanceOf[Boolean]

  def isUseInsecureSSL: java.lang.Boolean = caps.getOrElse(MaeveConf.INSECURE_SSL, false).asInstanceOf[Boolean]

  def isRedirectEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_REDIRECT, true).asInstanceOf[Boolean]

  def isCssEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_CSS, true).asInstanceOf[Boolean]

  def isAppletEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_APPLET, false).asInstanceOf[Boolean]

  def isPopupBlockerEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_POPUP_BLOCK, true).asInstanceOf[Boolean]

  def isGeolocationEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_GEO_TRACK, false).asInstanceOf[Boolean]

  def isDoNotTrackEnabled: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_DONT_TRACK, true).asInstanceOf[Boolean]

  def isThrowExceptionOnFailingStatusCode: java.lang.Boolean = caps.getOrElse(MaeveConf.THROW_FAIL_STATUS, false).asInstanceOf[Boolean]

  def isPrintContentOnFailingStatusCode: java.lang.Boolean = caps.getOrElse(MaeveConf.PRINT_FAIL_STATUS, true).asInstanceOf[Boolean]

  def isThrowExceptionOnScriptError: java.lang.Boolean = caps.getOrElse(MaeveConf.THROW_SCRIPT_ERROR, false).asInstanceOf[Boolean]

  def isActiveXNative: java.lang.Boolean = caps.getOrElse(MaeveConf.ENABLE_NATIVE_ACTIVEX, false).asInstanceOf[Boolean]

  override def getVersion: String = "38"

  override def getPlatform: Platform = Platform.getCurrent

  override def getBrowserName: String = "firefox"

  def getProxy: Proxy = caps.getOrElse(MaeveConf.PROXY, new Proxy()).asInstanceOf[Proxy]

  def setNoProxy(): MaeveConf = {
    copy(caps = caps + (MaeveConf.PROXY -> new Proxy()))
  }

  def setHTTPProxy(host: String, port: Int, noProxyHosts: List[String]): MaeveConf = {
    val proxy = new Proxy()
    proxy.setHttpProxy(host + ":" + port)
    copy(caps = caps + (MaeveConf.PROXY -> proxy))
  }

  def setCapability(key : String, value : AnyRef) = copy(caps = caps + (key -> value))

  def setJavaScriptEnabled(enableJS: java.lang.Boolean): MaeveConf = copy(caps = caps + (MaeveConf.JAVASCRIPT -> enableJS))

  def setUseInsecureSSL(useInsecureSSL: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.INSECURE_SSL -> useInsecureSSL))

  def setRedirectEnabled(enableRedirect: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_REDIRECT -> enableRedirect))

  def setSkipProxyTestEnabled(enable: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.SKIP_TEST_PROXY -> enable))

  def setCssEnabled(enableCSS: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_CSS -> enableCSS))

  def setAppletEnabled(enableApplet: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_APPLET -> enableApplet))

  def setPopupBlockerEnabled(enablePopupBlock: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_POPUP_BLOCK -> enablePopupBlock))

  def setGeolocationEnabled(enableGeoTrack: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_GEO_TRACK -> enableGeoTrack))

  def setDoNotTrackEnabled(enableDoNotTrack: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_DONT_TRACK -> enableDoNotTrack))

  def setThrowExceptionOnFailingStatusCode(throwOnFailStatCode: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.THROW_FAIL_STATUS -> throwOnFailStatCode))

  def setPrintContentOnFailingStatusCode(printOnFailStatCode: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.PRINT_FAIL_STATUS -> printOnFailStatCode))

  def setThrowExceptionOnScriptError(throwOnScriptError: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.THROW_SCRIPT_ERROR -> throwOnScriptError))

  def setActiveXNative(enableActiveXNative: java.lang.Boolean): MaeveConf = copy(caps + (MaeveConf.ENABLE_NATIVE_ACTIVEX -> enableActiveXNative))

  override def asMap(): util.Map[String, Object] = caps.asJava

  def overrideConf(defaultConf : MaeveConf): MaeveConf = {
    caps.foldLeft(defaultConf){case (conf,(key,value)) => conf.setCapability(key,value)}
  }

  def buildChromeProfile: Capabilities = {
    val profile = DesiredCapabilities.chrome()
    val opts = new ChromeOptions()

    if (getProxy.getHttpProxy != null) {
      profile.setCapability(MaeveConf.PROXY, getProxy)
      opts.addArguments("--proxy-server=" + getProxy.getHttpProxy)
    }

    val preferences = new util.HashMap[String, Object]()
    // TODO translate more preferences
    preferences.put(MaeveConf.CHROME_DONT_TRACK, isDoNotTrackEnabled)
    opts.setExperimentalOption(MaeveConf.CHROME_PREFS, preferences)

    profile.setCapability(ChromeOptions.CAPABILITY, opts)
    profile
  }

  //  def buildFirefoxProfile() ={
  //    val profile = new FirefoxProfile()
  //
  //    val proxy = getProxyConfig
  //    if (proxy.getProxyHost != null) {
  //      profile.setPreference("network.proxy.type", 1)
  //      profile.setPreference("network.proxy.http", proxy.getProxyHost)
  //      profile.setPreference("network.proxy.http_port", proxy.getProxyPort)
  //    } else if (proxy.getProxyAutoConfigUrl != null){
  //      profile.setPreference("network.proxy.type", 2)
  //      profile.setPreference("network.proxy.autoconfig_url",proxy.getProxyAutoConfigUrl)
  //    }
  //
  //    profile.setPreference("privacy.donottrackheader.enabled", true)
  //    if(isDoNotTrackEnabled) {
  //      profile.setPreference("privacy.donottrackheader.value", 1)
  //    } else {
  //      profile.setPreference("privacy.donottrackheader.value", 0)
  //    }
  //
  //    profile
  //  }
}
