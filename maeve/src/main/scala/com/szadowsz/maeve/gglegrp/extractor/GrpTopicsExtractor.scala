// Copyright 2016 zakski.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.szadowsz.maeve.gglegrp.extractor

import java.text.SimpleDateFormat
import java.util.Calendar

import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.JsoupExtractor
import com.szadowsz.maeve.core.instruction.extractor.util.TxtFileLineWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.util.JsoupHtmlToPlainText
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.{Document, Element}
import org.jsoup.safety.Whitelist
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/**
  * Created on 18/10/2016.
  */
case class GrpTopicsExtractor(maxWidth: Int = JsoupHtmlToPlainText.terminalLength * 2) extends JsoupExtractor with TxtFileLineWriter {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val contSettings = new OutputSettings()
    .prettyPrint(false)

  def buildFileName(instr: MaeveInstruction[_], timeElement: Element, fullTitle: String, length: Int = 20): String = {

    val time = Try(new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd/MM/yyyy").parse(timeElement.text()))) match {
      case Success(d) => d
      case Failure(_) =>
        val d = new SimpleDateFormat("dd MMM").parse(timeElement.text())
        val c = Calendar.getInstance()
        c.setTimeInMillis(System.currentTimeMillis())
        c.get(Calendar.YEAR) + "-" + new SimpleDateFormat("MM-dd").format(d)
    }

    val smallTitle = fullTitle.replaceAll("[^a-zA-Z0-9]", "")
    var count = 1
    var tmp = time + "-" + smallTitle.substring(0, Math.min(smallTitle.length, length))
    val org = tmp
    val path = instr.dPath + s"/${tmp.substring(0, 4)}/"
    while (exists(path, tmp)) {
      count += 1
      tmp = org + s"-$count"
    }
    tmp
  }

  private def writeContent(instr: MaeveInstruction[_], returnedUrl: Uri, title: String, posts: List[(Element, String, String)]): Unit = {
    val postDateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm:ss zzz")
    val sortedPosts = posts.sortBy { case (time, user, post) => postDateFormat.parse(time.attr("title")).getTime }
    val fileName = buildFileName(instr, sortedPosts.head._1, title)
    logger.info(s"Writing to file $fileName")
    val content: ArrayBuffer[String] = ArrayBuffer(s"URL: $returnedUrl")
    content += s"TITLE: $title \n"

    sortedPosts.foreach { case (timeElement, userStr, post) =>
      content += "----------------------------------------------------------------------------\n"
      val user = if (userStr.length > 0) userStr else "UNKNOWN"
      val time = timeElement.attr("title")
      content += (time + " - " + user + ":")

      var cleanedUp = post
      do {
        cleanedUp = cleanedUp.trim
        cleanedUp = if (cleanedUp.startsWith("\n")) cleanedUp.substring(1) else cleanedUp
      } while (cleanedUp.startsWith("\n") || cleanedUp.startsWith(" "))
      content += cleanedUp
    }
    write(instr.dPath + s"/${fileName.substring(0, 4)}/", fileName, content.toList, false)
  }


  override def extract(queryUrl: Uri, returnedUrl: Uri, instr: MaeveInstruction[_], page: Document): Unit = {
    val titleElement = page.select("span[id='t-t']").asScala.toList.headOption
    val timeElements = page.select("span[class='IVILX2C-tb-Q IVILX2C-b-Cb'],span[class='IVILX2C-nb-Q IVILX2C-b-Cb']").asScala.toList
    val contentElements = page.select("div[class='IVILX2C-tb-W'] div[class='IVILX2C-tb-P'][tabindex=0],div[class='IVILX2C-nb-W'] div[class='IVILX2C-nb-P'][tabindex=0]")
      .asScala.toList
    var userElements = page.select("span[class='IVILX2C-D-a']").asScala.toList.map(_.text())

    val nonEmpty = titleElement.nonEmpty && timeElements.nonEmpty && contentElements.nonEmpty

    if (nonEmpty && timeElements.length == contentElements.length) {
      val posts = (timeElements, userElements, contentElements.map(c => JsoupHtmlToPlainText.convertToPlainText(c, maxWidth))).zipped.toList
      writeContent(instr, returnedUrl, titleElement.get.text(), posts)
    } else {
      throw new Exception(s"Missing Information: Time=${timeElements.length} Contents=${contentElements.length}")
    }
  }

  override def shouldContinue(): Boolean = false
}
