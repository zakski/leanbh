package com.szadowsz.logainm.target.placenames.ni

import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.JsoupExtractor
import org.jsoup.nodes.Document

import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

class PlacenamesNiEndlessPageExtractor(state: PlacenamesNiListState) extends JsoupExtractor {
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
    val rows = page.select("div[class=\"widget-list d-flex\"] div[class=\"widget-list-list\"] div[class=\"list-card-content d-flex\"] div[class=\"layout fixed-layout d-flex\"] > div[class=\"app-root-emotion-cache-ltr-1nvu187\"]").asScala

    val writer = new CsvWriter(inst.dPath + s"${inst.name}.csv", "UTF-8", true)
    for (row <- rows) {
      val place = row.children().select("div[data-testid=\"rich-displayer\"]").asScala.map(_.text()).toList :+ row.select("a.jimu-button[aria-label=\"More Info\"]").attr("href")

      // Key each row by its react-window index so overlapping scroll steps do not write the same record twice.
      val wrapper = row.closest("[data-react-window-index]")
      val key = if (wrapper != null) wrapper.attr("data-react-window-index") else place.mkString("\u0001")

      // Skip rows the JS app has rendered but not yet bound (still showing {TEMPLATE} tokens or missing cells);
      // these are the "extracted too early" rows. Also skip rows already written on a previous scroll step.
      if (place.length >= 6 && !PlacenamesNiEndlessPageExtractor.hasUnboundTemplate(place) && state.seen.add(key)) {
        writer.write(place.head, place(1), place(2), place(3), place(4), place(5))
      }
    }
    writer.close()
  }

  /**
   * Function to check if retrieval is finished for the current page.
   *
   * @return true if we should continue to extract data from the current page, false otherwise.
   */
  override def shouldContinue(): Boolean = {
    !state.complete
  }
}

object PlacenamesNiEndlessPageExtractor {
  /** Matches an unresolved ArcGIS field expression such as {PLACE_NAME} or {COUNTY}. */
  private val unboundTemplate: Regex = "\\{[A-Z0-9_]+\\}".r

  private def hasUnboundTemplate(values: Seq[String]): Boolean =
    values.exists(value => unboundTemplate.findFirstIn(value).isDefined)
}
