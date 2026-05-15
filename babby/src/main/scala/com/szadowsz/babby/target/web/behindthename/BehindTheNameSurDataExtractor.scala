package com.szadowsz.babby.target.web.behindthename

import org.htmlunit.html.{DomNode, DomText, HtmlAnchor, HtmlElement, HtmlPage, HtmlSpan}
import com.szadowsz.babby.data.gender.GenderUtil
import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.HtmlExtractor

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * Created on 01/11/2016.
  */
class BehindTheNameSurDataExtractor extends HtmlExtractor {

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
    if (sameOrigin.nonEmpty){
      map.put("_", sameOrigin)
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

    val typeOf = page.getFirstByXPath("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Type')]/span[@class='infoname-info']")
      .asInstanceOf[HtmlElement].asNormalizedText()

    val usages = page.getByXPath[HtmlElement]("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Usage')]/span[@class='infoname-info']//a")
      .asScala
      .map(_.asNormalizedText())

    val pronunciations = page.getByXPath[HtmlElement]("//div[@id='body']//article//div[@class='infogroup']/div[@class='infoname' and contains(.,'Pronounced')]/span[@class='infoname-info']//span[@class='infoname-unit']")
      .asScala
      .map(_.asNormalizedText())

    val rootsOpt =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Roots')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]).map(_.asNormalizedText().replaceAll("[\r\n]*","").replaceAll("Expand Name Links",""))

    val histOpt = Option(page.getFirstByXPath("//div[@id='body']//article//section//div[@class='nameheading wide' and contains(.,'Meaning & History')]")
      .asInstanceOf[HtmlElement]).map(e => e.getNextElementSibling.asNormalizedText().replaceAll("[\r\n]*","").replaceAll("Expand Name Links",""))

    val varsList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Variant')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val varsMap = convertToMap(varsList)


    val othersList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Other Languages & Cultures')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val othersMap = convertToMap(othersList)

    val firstList =  Option(
      page.getFirstByXPath("//div[@id='body']//article//section[contains(.,'Related Names')]//div[@class='inforel' and contains(.,'Given Name Descendant')]//span[@class='inforel-info']")
        .asInstanceOf[HtmlElement]
    ).map(e => e.getChildren.asScala)
      .getOrElse(mutable.Buffer.empty[DomNode])
      .filter(!_.isInstanceOf[DomText])

    val firstMap = convertToMap(firstList)

    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    writer.write(
      title,
      typeOf,
      usages.mkString("|"),
      pronunciations.mkString("|"),
      rootsOpt.getOrElse(""),
      varsMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      othersMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
      firstMap.map{case (k,v) => k + "=" + v.mkString("[","|","]")}.mkString("|"),
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
