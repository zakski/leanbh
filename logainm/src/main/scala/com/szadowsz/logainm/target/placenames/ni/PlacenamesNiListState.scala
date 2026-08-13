package com.szadowsz.logainm.target.placenames.ni

import scala.collection.mutable

/**
  * Shared mutable state for scraping the virtualised (react-window) place-name list. Because only a handful of
  * rows exist in the DOM at any moment, the executor scrolls through the list while the extractor accumulates
  * rows across scroll steps. This holder lets the two coordinate: which rows have already been written and
  * whether the whole list has been exhausted.
  */
final class PlacenamesNiListState {
  /** react-window indices of rows already written, so overlapping scroll steps do not duplicate records. */
  val seen: mutable.Set[Int] = mutable.LinkedHashSet.empty[Int]

  /** set once the list can no longer be scrolled and all lazily-loaded records are present. */
  var complete: Boolean = false

  /** total scroll height observed on the previous scroll step, used to detect when lazy loading has finished. */
  var lastScrollHeight: Long = -1L

  /**
   * Claims the given index for writing, guarding against duplicate rows (e.g. when a write succeeds but a later
   * step fails and MaeveDriver retries the same record, or when resuming over already-written rows).
   *
   * @param index the index about to be written.
   * @return true if this index has not been written before and should be written now; false if it was already seen.
   */
  def markWritten(index: Int): Boolean = seen.add(index)

}
