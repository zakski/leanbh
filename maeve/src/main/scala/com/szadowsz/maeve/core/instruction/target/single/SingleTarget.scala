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
package com.szadowsz.maeve.core.instruction.target.single

import com.szadowsz.maeve.core.instruction.target.Target
import com.szadowsz.common.net.Uri

/**
  * A single url target; designed to scrape against one uri then die.
  *
  * Created on 16/10/2016.
  */
case class SingleTarget(link: Uri) extends Target[SingleTarget] {

  /**
    * Builds and returns the current Uri
    *
    * @return the current uri
    */
  override def currentUrl(): Uri = link

  /**
    * Method to update the url to the next in sequence.
    *
    * @return a fresh instance, updated to the next target in the sequence
    */
  override def next(): SingleTarget = this

  /**
    * Method to reset the target.
    *
    * @return a fresh version of the target sequence with no history.
    */
  override def reset(): SingleTarget = this

  /**
    * Checker Function to see if we are at the beginning of the target sequence
    *
    * @return true if it is at the beginning, false otherwise.
    */
  override def atBeginning: Boolean = true

  /**
    * Checker Function to see if we are at the end of the target sequence
    *
    * @return true if it is at the end, false otherwise.
    */
  override def isDone: Boolean = true
}
