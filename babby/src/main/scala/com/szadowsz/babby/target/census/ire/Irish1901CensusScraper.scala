package com.szadowsz.babby.target.census.ire

import com.szadowsz.maeve.core.browser.MaeveConf

/**
  * Created on 09/11/2016.
  */
object Irish1901CensusScraper {
  private val conf = new MaeveConf().setJavaScriptEnabled(false).setThrowExceptionOnScriptError(false).setHTTPProxy("proxy", 8080, Nil)

  def main(args: Array[String]): Unit = {
    val scraper = new IrishCensusScraper(1901, 1000, 100, 100000, 4429866, conf)
    scraper.setRecoveryDirectory("./recovery/")
    scraper.scrapeUsingCurrInstruction()
  }
}
