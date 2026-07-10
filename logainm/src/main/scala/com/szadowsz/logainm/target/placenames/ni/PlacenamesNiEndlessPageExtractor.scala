package com.szadowsz.logainm.target.placenames.ni

import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.JsoupExtractor
import org.jsoup.nodes.Document

import scala.jdk.CollectionConverters._

class PlacenamesNiEndlessPageExtractor extends JsoupExtractor {
  /**
   * Generic method to extract data from a webpage.
   *
   * @param queryUrl    the expected url of the page.
   * @param returnedUrl the actual url of the page.
   * @param inst        the current maeve instruction.
   * @param page        the webpage in whatever format is being provided.
   */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: Document): Unit = {
    val fileName = returnedUrl.path.substring(returnedUrl.path.lastIndexOf('/') - 1, returnedUrl.path.lastIndexOf('/'))
    val places = page.select("div[class=\"widget-list d-flex\"] div[class=\"widget-list-list\"] div[class=\"list-card-content d-flex\"] div[class=\"layout fixed-layout d-flex\"] > div[class=\"app-root-emotion-cache-ltr-1nvu187\"]")
      .asScala.map { place => place.children().select("div[data-testid=\"rich-displayer\"]").asScala.map(_.text()).toList :+ place.select("a.jimu-button[aria-label=\"More Info\"]").attr("href")}//.asScala.map(_.text()).toList }

    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    for (place <- places) {
      writer.write(place.head, place(1), place(2), place(3), place(4), place(5))
    }
    writer.close()
  }

  /**
   * Function to check if retrieval is finished for the current page.
   *
   * @return true if we should continue to extract data from the current page, false otherwise.
   */
  override def shouldContinue(): Boolean = {
    false
  }
}
