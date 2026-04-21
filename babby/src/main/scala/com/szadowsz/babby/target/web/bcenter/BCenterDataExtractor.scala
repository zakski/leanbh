package com.szadowsz.babby.target.web.bcenter

import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage}
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.collection.JavaConverters._

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
     val titleEl = page.getByXPath("//div[@class='mainContent']//h1[@itemprop='headline']").asInstanceOf[java.util.List[HtmlElement]].asScala.head

    val title = titleEl.getFirstChild.asText().trim
    val gender = if (titleEl.getByXPath("./span[@class='bgGenderIconM genderIcon']").size() > 0){
      "m"
    }else if (titleEl.getByXPath("./span[@class='bgGenderIconF genderIcon']").size() > 0) {
      "f"
    } else {
      throw new RuntimeException("Gender not found")
    }
    val meanOpt = page.getByXPath("//div[@class='mainContent']//div[@class='babyNameSubhead'][contains(text(),'What does')]/following-sibling::p[1]")
      .asInstanceOf[java.util.List[HtmlElement]].asScala.headOption

    val orgOpt = page.getByXPath("//div[@class='mainContent']//div[@class='babyNameSubhead'][text()='Origin']/following-sibling::p[1]")
      .asInstanceOf[java.util.List[HtmlElement]].asScala.headOption

         val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
        writer.write(title, gender,orgOpt.map(_.asText()).getOrElse(""),meanOpt.map(_.asText()).getOrElse(""))
        writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
