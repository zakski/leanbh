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
package com.szadowsz.maeve.core.instruction.target

import com.szadowsz.common.net.Uri

/**
  * Base Trait for Uri targeting during the scraping process
  *
  * Created on 15/05/2016.
  */
trait Target[P <: Target[P]] {

  /**
    * Builds and returns the current Uri
    *
    * @return the current uri
    */
  def currentUrl(): Uri

  /**
    * Method to update the url to the next in sequence.
    *
    * @return a fresh instance, updated to the next target in the sequence
    */
  def next(): P

  /**
    * Method to reset the target.
    *
    * @return a fresh version of the target sequence with no history.
    */
  def reset(): P

  /**
    * Checker Function to see if we are at the beginning of the target sequence
    *
    * @return true if it is at the beginning, false otherwise.
    */
  def atBeginning: Boolean

  /**
    * Checker Function to see if we are at the end of the target sequence
    *
    * @return true if it is at the end, false otherwise.
    */
  def isDone: Boolean
}