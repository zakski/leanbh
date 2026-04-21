package com.szadowsz.babby.target.web.oxfordref

import com.szadowsz.common.io.explore.{ExtensionFilter, FileFinder}
import com.szadowsz.common.io.read.FReader
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.MaeveConf
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.actions.WaitExecutor
import com.szadowsz.maeve.core.instruction.target.multi.{PathTarget, QueryTarget, RelativeUriTarget}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

/**
  * Created on 18/11/2016.
  */
object OxfordSurnameIndexScraper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val urlBase = Uri("http://www.oxfordreference.com/view/10.1093/acref/9780199677764.001.0001/acref-9780199677764?pageSize=100")
  private val target = QueryTarget(urlBase, "page", (1 to 457).toSeq.map(_.toString))
  private val conf = MaeveConf().setJavaScriptEnabled(false).setThrowExceptionOnScriptError(false).setHTTPProxy("proxy", 8080, Nil)
  private val group = "oxfordsurnames"

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
