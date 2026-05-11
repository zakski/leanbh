package com.szadowsz.babby.target.web.bcenter

import org.htmlunit.html.{HtmlElement, HtmlPage}
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.jdk.CollectionConverters._

/**
  * Created on 01/11/2016.
  */
class BCenterDataExtractor extends HtmlExtractor {
  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: HtmlPage): Unit = {
    val titleEl = page.getByXPath("//main[contains(@class,'mobileMarginProvider')]//h1").asInstanceOf[java.util.List[HtmlElement]].asScala.head
    val title = titleEl.asNormalizedText().trim

    val meanOpt = page.getByXPath("//main[contains(@class,'mobileMarginProvider')]//div[@class='spacer200 stats']//div[contains(.,'Meaning')]")
      .asInstanceOf[java.util.List[HtmlElement]].asScala.headOption

    val orgOpt = page.getByXPath("//main[contains(@class,'mobileMarginProvider')]//div[@class='spacer200 stats']//div[@class='labelAndText'][contains(.,'Origin')]")
      .asInstanceOf[java.util.List[HtmlElement]].asScala.headOption

    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    writer.write(title, "",orgOpt.map(_.asNormalizedText().replace("Origin:","").trim).getOrElse(""),meanOpt.map(_.asNormalizedText().replace("Meaning:","").trim).getOrElse(""))
    writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
