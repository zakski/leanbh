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
package com.szadowsz.maeve.core.instruction.target.multi.feeder

import com.szadowsz.common.net.Uri

import scala.collection.mutable.ArrayBuffer

/**
  * Class for Uri targeting during the scraping process, if we are expecting to differentiate web pages by their fragment.
  *
  * Created on 18/10/2016.
  */
case class FragmentFeederTarget(
                        override val baseUrl: Uri,
                        override val seq: ArrayBuffer[String] = ArrayBuffer(),
                        override val hist: ArrayBuffer[String] = ArrayBuffer()
                      ) extends FeederTarget[String,FragmentFeederTarget] {


  /**
    * Builds and returns the current Uri
    *
    * @return the current uri
    */
  def currentUrl():Uri = baseUrl.withFragment(seq.head)

  /**
    * Method to update the url to the next in sequence.
    *
    * @return the instance, updated to the next target in the sequence
    */
  def next():FragmentFeederTarget = {
    hist += seq.remove(0)
    this
  }

  /**
    * Method to reset the target.
    *
    * @return a version of the target sequence with a cleared history.
    */
  def reset():FragmentFeederTarget = {
    seq.clear()
    hist.clear()
    this
  }
}
