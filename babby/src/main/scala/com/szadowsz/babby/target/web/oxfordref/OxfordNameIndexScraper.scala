package com.szadowsz.babby.target.web.oxfordref

import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.MaeveConf
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.actions.WaitExecutor
import com.szadowsz.maeve.core.instruction.target.multi.QueryTarget
import org.slf4j.LoggerFactory

/**
  * Created on 18/11/2016.
  */
object OxfordNameIndexScraper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val urlBase = Uri("http://www.oxfordreference.com/view/10.1093/acref/9780198610601.001.0001/acref-9780198610601?pageSize=100")
  private val target = QueryTarget(urlBase, "page", (1 to 78).toSeq.map(_.toString))
  private val conf = MaeveConf().setJavaScriptEnabled(false).setThrowExceptionOnScriptError(false).setHTTPProxy("proxy", 8080, Nil)
  private val group = "oxfordnames"

  def main(args: Array[String]): Unit = {
    System.setProperty("webdriver.chrome.driver", ".\\chromedriver_win32\\chromedriver.exe")
    val scraper = new MaeveDriver(conf)
    scraper.setRecoveryDirectory("./recovery/")
    val filter = new OxfordIndexExtractor()
    val actions = new WaitExecutor(5000)

    val instruction1 = MaeveInstruction(group, target, actions, filter, s"./data/web/$group/", false, true)

    scraper.scrapeUsingInstruction(instruction1)
  }
}
