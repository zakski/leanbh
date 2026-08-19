package com.szadowsz.logainm.target.placenames.ni

import org.slf4j.LoggerFactory
import org.supercsv.io.CsvListReader
import org.supercsv.prefs.CsvPreference

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Using

/**
  * Shared mutable state for scraping the virtualised (react-window) place-name list. Because only a handful of
  * rows exist in the DOM at any moment, the executor scrolls through the list while the extractor accumulates
  * rows across scroll steps. This holder lets the two coordinate: which rows have already been written and
  * whether the whole list has been exhausted.
  */
final class PlacenamesNiListState(csvFile: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** place-name of every already-written record, indexed by its list (react-window) index. */
  private val writtenNames: ListBuffer[String] = loadWrittenNames()

  /** indices already written (seeded on resume with everything already in the CSV) to guard against duplicates.*/
  val seen: mutable.Set[Int] = mutable.LinkedHashSet.empty[Int] ++= writtenNames.indices

  /** index of the search-list row currently being processed. */
  var currentIndex: Int = writtenNames.length

  /** set once the list can no longer be scrolled and all lazily-loaded records are present. */
  var complete: Boolean = false

  /** true when the crawl was resumed from previously written CSV data. */
  var resuming: Boolean = writtenNames.nonEmpty

  /** total scroll height observed on the previous scroll step, used to detect when lazy loading has finished. */
  var lastScrollHeight: Long = -1L


  /**
   * Reads the CSV, returning the place-name of every record in order. The place-name is the first column with any
   * trailing ", County ..." suffix stripped, matching the leading name shown in the search list. Parsing is
   * tolerant of a partially written final row (e.g. after a crash).
   */
  private def loadWrittenNames(): ListBuffer[String] = {
    val file = new File(csvFile)
    if (!file.isFile) {
      logger.info("No records recovered from CSV {}; starting from the beginning", csvFile)
      return ListBuffer[String]()
    }

    Using.resource(new CsvListReader(
      new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)),
      CsvPreference.STANDARD_PREFERENCE)) { reader =>
      val names = ListBuffer[String]()
      var read = 0
      try {
        var row = reader.read()
        while (row != null) {
          read += 1
          val title = if (row.size() == 0) "" else Option(row.get(0)).getOrElse("")
          names += title
          row = reader.read()
        }
      } catch {
        case e: Exception =>
          logger.warn("Stopped reading CSV {} after {} rows: {}", csvFile, Integer.valueOf(read), e.getMessage)
      }
      logger.info("Recovered {} records from CSV {}, resuming at index {} (last written '{}')", names.length, csvFile, names.length, names.last)
      names
    }
  }


  /**
   * Claims the given index for writing, guarding against duplicate rows (e.g. when a write succeeds but a later
   * step fails and MaeveDriver retries the same record, or when resuming over already-written rows).
   *
   * @param index the index about to be written.
   * @return true if this index has not been written before and should be written now; false if it was already seen.
   */
  def markWritten(index: Int): Boolean = seen.add(index)

  def getWrittenName(index: Int): Option[String] = {
    if (index >= 0 && index < writtenNames.length) Some(writtenNames(index)) else None
  }

  def getWrittenCount(): Int = writtenNames.length
}
