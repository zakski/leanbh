package com.szadowsz.babby.target.web.meanOfNames

import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage}
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.collection.JavaConverters._

/**
  * Created on 01/11/2016.
  */
class MeaningOFNamesDataExtractor extends HtmlExtractor {
  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: HtmlPage): Unit = {
    val titleEl = page.getByXPath("//div[@id='content-content']//div[@class='title clear-block']/h2/span").asInstanceOf[java.util.List[HtmlElement]].asScala
      .head

    val title = titleEl.asText()
    val gender = titleEl.getAttribute("class")

    val pro = page.getByXPath("//div[@id='content-content']//div[@id='name-pronunciation']").asInstanceOf[java.util.List[HtmlElement]].asScala.head.asText()
      .replaceAll("Pronunciation:", "").trim

    val orgOpt = page.getByXPath("//div[@id='content-content']//div[@id='name-origin']/p").asInstanceOf[java.util.List[HtmlElement]].asScala.headOption
    val varOpt = page.getByXPath("//div[@id='content-content']//div[@id='variant']/p").asInstanceOf[java.util.List[HtmlElement]].asScala.headOption


    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    writer.write(title, gender, pro,orgOpt.map(_.asText()).getOrElse(""),varOpt.map(_.asText()).getOrElse(""))
    writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
