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
  val seen: mutable.Set[String] = mutable.LinkedHashSet.empty[String]

  /** set once the list can no longer be scrolled and all lazily-loaded records are present. */
  var complete: Boolean = false

  /** total scroll height observed on the previous scroll step, used to detect when lazy loading has finished. */
  var lastScrollHeight: Long = -1L
}
