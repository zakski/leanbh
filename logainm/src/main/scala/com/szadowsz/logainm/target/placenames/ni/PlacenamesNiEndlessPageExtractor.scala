package com.szadowsz.logainm.target.placenames.ni

import com.szadowsz.common.io.write.CsvWriter
import com.szadowsz.common.net.Uri
import com.szadowsz.logainm.target.placenames.ni.PlacenamesNiEndlessPageExtractor.LABELS
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.extractor.JsoupExtractor
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

object PlacenamesNiEndlessPageExtractor {
  /**
   * Detail fields, in output order. Each is rendered as a "Label: value" rich-displayer on the detail
   * page (the heading-style fields such as Suggested Origin simply have their value on the line(s) after the colon).
   */
  val LABELS: List[String] = List(
    "Barony",
    "Parish (1851)",
    "Parish (1961)",
    "Townland",
    "Place-Name ID",
    "Type",
    "Suggested Origin",
    "Irish Form",
    "Background",
    "References",
    "Additional Information"
  )

  /**
   * Get the Title of the Placename Detail Page
   *
   * @param page the page to read
   * @return the placename page's title shown at the top of the detail page.
   */
  private def getTitle(page: Document): String = {
    val heading = page.select("div[data-widgetid=\"widget_829\"] div[data-testid=\"rich-displayer\"]").text().trim
    if (heading.nonEmpty) {
      heading
    } else {
      page.select("div[data-testid=\"rich-displayer\"]").asScala.headOption.map(_.text().trim).getOrElse("")
    }
  }

  /**
   * Finds the rich-displayer whose text begins with "<label>:" and returns the value after the colon.
   *
   * @param page the page to read
   * @param label the field to read
   * @return the field value, or an empty string if the field is not present.
   */
  private def getField(page: Document, label: String): String = {
    val marker = label + ":"
    page.select("div[data-testid=\"rich-displayer\"]").asScala
      .map(_.text().trim)
      .find(_.startsWith(marker))
      .map(_.substring(marker.length).trim)
      .getOrElse("")
  }

  /**
   * Collects the names of the historical forms listed on the detail page
   *
   * @param page the page to read
   * @return Returns a string of the historical forms, joined with " | "; These live virtually, so only what is currently rendered are captured.
   */
  private def getHistoricalForms(page: Document): String = {
    // Scope to the detail page's own Historical Forms list (widget_927). During the ArcGIS Experience page
    // transition the search results list (widget_895) can still be in the DOM, and an unscoped selector would
    // otherwise capture those search place-names instead of this record's historical forms.
    page.select("div.list-widget-widget_927 div[data-react-window-index] div[data-layoutitemid=\"1\"] div[data-testid=\"rich-displayer\"]")
      .asScala
      .map(_.text().trim)
      .filter(_.nonEmpty)
      .mkString(" | ")
  }
}

class PlacenamesNiEndlessPageExtractor(state: PlacenamesNiListState) extends JsoupExtractor {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Extracts a single place-name record from its detail ("Place-Name Info") page. The executor has already
   * navigated the browser to this page for the current row, so the supplied document is the detail page.
   *
   * @param queryUrl    the expected url of the page.
   * @param returnedUrl the actual url of the page.
   * @param inst        the current maeve instruction.
   * @param page        the detail page document.
   */
  override def extract(queryUrl: Uri, returnedUrl: Uri, inst: MaeveInstruction[_], page: Document): Unit = {
    if (state.complete) {
      return
    }

    val title = PlacenamesNiEndlessPageExtractor.getTitle(page)
    if (!state.markWritten(state.currentIndex, title)) {
      logger.warn("Skipping write for index {} ('{}'): already written this run/resume (no CSV line appended)",
        Integer.valueOf(state.currentIndex), title)
      return
    } // already written (retry / resume) - do not duplicate

    val fields = LABELS.map(label => PlacenamesNiEndlessPageExtractor.getField(page, label))
    val historical = PlacenamesNiEndlessPageExtractor.getHistoricalForms(page)

    val row: Seq[String] = (title +: fields) :+ historical

    val csvPath = inst.dPath + s"${inst.name}.csv"
    val writer = new CsvWriter(csvPath, "UTF-8", true)
    writer.write(row)
    writer.close()
    logger.info("Appended CSV line for index {} ('{}') to {}", Integer.valueOf(state.currentIndex), title, csvPath)
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
