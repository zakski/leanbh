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
  * Shared mutable state for the place-name crawl. Records are visited one at a time by their react-window index:
  * the executor navigates into each record's detail page, the extractor scrapes it, and the executor returns to
  * the (sorted, deterministic) search list and advances the index. Navigating away resets the list scroll, so the
  * index is the stable handle used to re-locate each row.
  *
  * The already-written CSV is the single source of truth for recovery: on construction its rows are read (one per
  * written record, in list order) so the crawl resumes at the next index, and every written place-name is kept so
  * the executor can verify each already-processed row against the (static) list while scrolling back to the resume
  * point. This is index-driven, not pixel-driven, so variable row heights cannot knock the resume out of step.
  *
  * @param csvFile path of the output CSV; its row count / contents drive resume.
  */
final class PlacenamesNiListState(csvFile: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** place-name of every already-written record, indexed by its list (react-window) index. */
  private val writtenNames: ListBuffer[String] = loadWrittenNames()

  /**
   * (list index, place-name) pairs already written, seeded on resume with everything already in the CSV, to guard
   * against duplicate writes. The place-name is part of the key because the react-window index is reused when a
   * record disappears on reload and the list shifts up: the same index can then legitimately point at a different
   * (not-yet-written) record, which must still be written.
   */
  val seen: mutable.Set[(Int, String)] =
    mutable.LinkedHashSet.empty[(Int, String)] ++= writtenNames.zipWithIndex.map { case (name, index) => (index, name) }

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
   * Claims the given (index, place-name) for writing, guarding against duplicate rows (e.g. when a write succeeds
   * but a later step fails and MaeveDriver retries the same record, or when resuming over already-written rows).
   * Keying on the place-name as well as the index means a react-window index reused for a different record (after
   * an earlier record disappears and the list shifts up) is still written rather than skipped.
   *
   * @param index     the list index about to be written.
   * @param placename the place-name (CSV title) about to be written.
   * @return true if this (index, place-name) has not been written before and should be written now; false if it was
   *         already seen.
   */
  def markWritten(index: Int, placename: String): Boolean = seen.add((index, placename))

  def getWrittenName(index: Int): Option[String] = {
    if (index >= 0 && index < writtenNames.length) Some(writtenNames(index)) else None
  }

  def getWrittenCount(): Int = writtenNames.length
}
