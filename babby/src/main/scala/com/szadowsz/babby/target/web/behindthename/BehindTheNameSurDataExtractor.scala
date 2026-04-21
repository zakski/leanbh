package com.szadowsz.babby.target.web.behindthename

import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage}
import com.szadowsz.babby.data.gender.GenderUtil
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.collection.JavaConverters._

/**
  * Created on 01/11/2016.
  */
class BehindTheNameSurDataExtractor extends HtmlExtractor {

   /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: HtmlPage): Unit = {
    val titleEl = page.getFirstByXPath("//div[@class='body']//div[@class='namemain']").asInstanceOf[HtmlElement].asText()
    val title = titleEl.replaceAll("Given Name","").trim

    val source = page.getFirstByXPath("//div[@class='body']//div[@class='nameinfo'][1]/div[@class='namesub'][1]/span[@class='info']")
      .asInstanceOf[HtmlElement].asText()

    val usage = page.getFirstByXPath("//div[@class='body']//div[@class='nameinfo'][1]/div[@class='namesub'][2]/span[@class='info']").asInstanceOf[HtmlElement]
      .asText()

    val proXpath = "//div[@class='body']//div[@class='nameinfo'][1]/div[@class='namesub']/span[text()='PRONOUNCED:']"
    val proOpt = Option(page.getFirstByXPath(proXpath).asInstanceOf[HtmlElement]).map(e => e.getNextElementSibling.asText())

    val histOpt = Option(page.getFirstByXPath("//div[@class='body']//div[text()='Meaning & History']")
      .asInstanceOf[HtmlElement]).map(e => e.getNextElementSibling.asText().replaceAll("[\r\n]*","").replaceAll("Expand Name Links",""))

    val varOpt = page.getByXPath("//div[@class='body']//div[@class='nameinfo'][2]/div[@class='namesub']/span[@class='info']").asInstanceOf[java.util
    .List[HtmlElement]].asScala


    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    writer.write(title, source,usage,proOpt.getOrElse(""),varOpt.map(_.asText()).mkString(","),histOpt.getOrElse(""))
    writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
