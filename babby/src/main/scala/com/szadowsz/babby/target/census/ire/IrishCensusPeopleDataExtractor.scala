package com.szadowsz.babby.target.census.ire

import com.gargoylesoftware.htmlunit.html.{DomAttr, HtmlElement, HtmlPage}
import com.szadowsz.common.io.write.{CsvWriter, FWriter}
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.{HtmlExtractor, JsoupExtractor}
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Created on 07/11/2016.
  */
class IrishCensusPeopleDataExtractor(var num: Int, var count: Int, val limit: Int = 100000) extends JsoupExtractor {
  private val logger = LoggerFactory.getLogger(this.getClass)
  var writer: CsvWriter = _

  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: Document): Unit = {
    val peopleRawData = page.select("tbody > tr.even,tbody > tr.odd").asScala.toList
    peopleRawData.foreach { e =>
      val personDomCols = e.children().asScala.toList
      val personTextCols = personDomCols.map(_.text())
      val personParsedCols = personTextCols :+ e.select("a[href]").asScala.head.attr("href")

      count = count + 1
      if (count > limit) {
        writer.close()
        num = num + 1
        writer = new CsvWriter(inst.dPath + "census-" + num + ".csv", "UTF-8", false)
        count = 1
      } else if (writer == null) {
        writer = new CsvWriter(inst.dPath + "census-" + num + ".csv", "UTF-8", false)
      }
      writer.write(personParsedCols)
    }

  }

  def close() = {
    writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
