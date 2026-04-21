package com.szadowsz.babby.target.web.oxfordref

import com.gargoylesoftware.htmlunit.html.{DomAttr, DomElement, HtmlPage}
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.{HtmlExtractor, JsoupExtractor}
import com.szadowsz.maeve.core.instruction.extractor.util.TxtFileLineWriter
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._


/**
  * Created on 01/11/2016.
  */
class OxfordIndexExtractor extends JsoupExtractor with TxtFileLineWriter {
  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: Document): Unit = {
    val urls = page.select("div#searchContent h2[class=itemTitle] a").asScala.toList.map{
      case (url : Element) => s"${url.text()}|${url.attr("href")}"
    }

    write(inst.dPath, inst.name, urls, true)
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
