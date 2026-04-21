package com.szadowsz.babby.target.census.ire

import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.{MaeveConf, MaeveHeadlessBrowser, MaeveRemoteBrowser}
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.actions.WaitExecutor
import com.szadowsz.maeve.core.instruction.target.multi.QueryTarget
import com.szadowsz.maeve.core.instruction.target.single.SingleTarget
import com.szadowsz.maeve.core.util.recovery.RecoveryUtil
import org.slf4j.LoggerFactory

/**
  * Created on 08/11/2016.
  */
class IrishCensusScraper(
                          val year: Int,
                          val waitTime: Long,
                          val pageIncrement: Int,
                          val fileLimit: Int,
                          val censusSize: Int,
                          conf: MaeveConf
                        ) extends MaeveDriver(conf) {

  private val logger = LoggerFactory.getLogger(classOf[IrishCensusScraper])
  private var extractor : IrishCensusPeopleDataExtractor = _

  protected def buildInstruction(): MaeveInstruction[_] = {
    val name = s"irishCensus-$year"
    logger.info("Attempting Recovery")
    var history = RecoveryUtil.doRecovery(recovPath, name).distinct
    val numofUrlsPerPage = fileLimit / pageIncrement
    val completedPageCount = history.length / numofUrlsPerPage
    val currentPageRemainder = history.length % numofUrlsPerPage
    if (currentPageRemainder != 0) {
      history = history.dropRight(currentPageRemainder)
    }

    val baseUrl = Uri("http://www.census.nationalarchives.ie/search/results.jsp?" +
      s"searchMoreVisible=true&search=Search&surname=&firstname=&census_year=$year&pageSize=100&sort=surnameSort")
    // val maxUrls = censusSize / pageIncrement + (if (censusSize % pageIncrement > 0) 1 else 0)
    val tagrets = (0 until censusSize by pageIncrement).toList.map(_.toString)
    val target = QueryTarget(baseUrl, "pager.offset", tagrets)

    val actions = new WaitExecutor(waitTime)

    extractor = new IrishCensusPeopleDataExtractor(completedPageCount, 0, fileLimit)
    val instruction = MaeveInstruction(name, target, actions, extractor, s"./data/census/ireland/$year/", true, true,true)
    instruction.applyLinkHistory(history)
  }


  def doRecovery(path: String, instr: MaeveInstruction[_]): MaeveInstruction[_] = {
    var info = RecoveryUtil.doRecovery(path, instr.name)
    val numofUrlsPerPage = fileLimit / pageIncrement
    val remainder = info.length % numofUrlsPerPage
    if (remainder != 0) {
      info = info.dropRight(remainder)
    }
    instr.applyLinkHistory(info)
  }

  override def feedInstruction(instruction: MaeveInstruction[_]) = {
    logger.info("Initialising Browser...")
    if (instruction.isHeadless) {
      browser = new MaeveHeadlessBrowser(instruction.overrideConf(defaultConf))
      logger.info("Initialised Headless Browser")
    } else {
      browser = new MaeveRemoteBrowser(instruction.overrideConf(defaultConf))
      logger.info("Initialised Remote Browser")
    }

    instr = instruction
  }

  override def scrapeUsingCurrInstruction(): Unit = {
    feedInstruction(buildInstruction())
    logger.info("Scraping Against Current Instruction: {}", instr.name)
    if (!instr.isDone || instr.target.isInstanceOf[SingleTarget]) {
      instr.doBefore(browser)
      do {
        scrapePage()
        instr = instr.update()
      } while (!instr.isDone)
      instr.doAfter(browser)
    }
    browser.quit()
    extractor.close()
  }
}
