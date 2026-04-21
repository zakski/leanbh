package com.szadowsz.babby.target.web.meanOfNames

import com.gargoylesoftware.htmlunit.html.{DomAttr, HtmlElement, HtmlPage}
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor
import com.szadowsz.maeve.core.instruction.extractor.util.TxtFileLineWriter

import scala.collection.JavaConverters._


/**
  * Created on 01/11/2016.
  */
class MeaningOfNamesPageExtractor extends HtmlExtractor with TxtFileLineWriter {
  private var isMore = false

  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: HtmlPage): Unit = {
    val fileName = queryUrl.path.substring(queryUrl.path.lastIndexOf('/')+1,queryUrl.path.lastIndexOf('/')+2)
    val urls = page.getByXPath("//div[@class='brownwide0']//a/@href|//div[@class='brownwide1']//a/@href").asScala.toList.map{ case (url : DomAttr) =>
      url.getValue}

    write(inst.dPath, fileName, urls, true)
    isMore = page.getFirstByXPath("//a[text()='>']") != null
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = isMore
}
