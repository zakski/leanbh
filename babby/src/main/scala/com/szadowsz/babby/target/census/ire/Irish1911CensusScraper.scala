package com.szadowsz.babby.target.census.ire

import com.szadowsz.maeve.core.browser.MaeveConf

/**
  * Created on 09/11/2016.
  */
object Irish1911CensusScraper {
  private val conf = MaeveConf().setJavaScriptEnabled(false).setThrowExceptionOnScriptError(false).setHTTPProxy("proxy", 8080, Nil)

  def main(args: Array[String]): Unit = {
    val scraper = new IrishCensusScraper(1911, 1000, 100, 100000, 4384519, conf)
    scraper.setRecoveryDirectory("./recovery/")
    scraper.scrapeUsingCurrInstruction()
  }
}
