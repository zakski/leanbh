package com.szadowsz.babby.target.web.behindthename

import org.htmlunit.html.{DomNode, DomText, HtmlAnchor, HtmlElement, HtmlPage, HtmlSpan}
import com.szadowsz.babby.data.gender.GenderUtil
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * Created on 01/11/2016.
  */
class BehindTheNameFirstDataExtractor extends HtmlExtractor {


  private def convertToMap(varsList: Iterable[DomNode]): Map[String, List[String]] = {
    val map = mutable.Map[String, List[String]]()
    var sameOrigin = List[String]()
    for (node <- varsList) {
      node match {
        case anchor: HtmlAnchor =>
          sameOrigin = sameOrigin :+ anchor.asNormalizedText()
        case span : HtmlSpan =>
          map.put(span.asNormalizedText(), sameOrigin)
          sameOrigin = List[String]()
      }
    }
    map.toMap
  }

  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl    the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst        the current maeve instruction.
    * @param page        the webpage in whatever format is being provided.
    */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: HtmlPage): Unit = {
    val title = page.getFirstByXPath("//div[@id='body']//h1[@class='namebanner-title']").asInstanceOf[HtmlElement].asNormalizedText().trim

    val genderEl = page.getFirstByXPath("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Gender')]/span[@class='infoname-info']")
      .asInstanceOf[HtmlElement].asNormalizedText()

    val gender = GenderUtil.forName(genderEl).toString

    val usages = page.getByXPath[HtmlElement]("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Usage')]/span[@class='infoname-info']//a")
      .asScala
      .map(_.asNormalizedText())

    val scripts = page.getByXPath[HtmlElement]("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Script')]/span[@class='infoname-info']//span[@class='infoname-unit']")
      .asScala
      .map(_.asNormalizedText())

    val pronunciations = page.getByXPath[HtmlElement]("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Pronounced')]/span[@class='infoname-info']//span[@class='infoname-unit']")
      .asScala
      .map(_.asNormalizedText())

    val tags = Option(
          page.getFirstByXPath("//div[@id='body']//article//section//div[@class='nameheading wide' and contains(.,'People think this name is')]")
          .asInstanceOf[HtmlElement]
    ).map(e => e.getNextElementSibling.getByXPath[HtmlElement](".//span").asScala)
      .getOrElse(mutable.Buffer.empty[HtmlElement])
      .map(_.asNormalizedText())

    val rootsOpt =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Roots')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]).map(_.asNormalizedText().replaceAll("[\r\n]*","").replaceAll("Expand Name Links",""))

    val varsList =  Option(
          page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Variant')]//span[@class='inforel-info']")
          .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val varsMap = convertToMap(varsList)

    val dimsList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Diminutive')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val dimsMap = convertToMap(dimsList)

    val othersList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Other Languages & Cultures')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val othersMap = convertToMap(othersList)

    val sursList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Surname Descendants')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val sursMap = convertToMap(sursList)

    val histOpt = Option(page.getFirstByXPath("//div[@id='body']//article//section//div[@class='nameheading wide' and contains(.,'Meaning & History')]")
      .asInstanceOf[HtmlElement]).map(e => e.getNextElementSibling.asNormalizedText().replaceAll("[\r\n]*","").replaceAll("Expand Name Links",""))

    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    writer.write(
      title,
      gender,
      scripts.mkString("|"),
      usages.mkString("|"),
      pronunciations.mkString("|"),
      rootsOpt.getOrElse(""),
      varsMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      dimsMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      othersMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      sursMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      tags.mkString("|"),
      histOpt.getOrElse("")
    )
    writer.close()
  }

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  override def shouldContinue(): Boolean = false
}
