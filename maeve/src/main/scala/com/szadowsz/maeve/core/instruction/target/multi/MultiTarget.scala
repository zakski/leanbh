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
package com.szadowsz.maeve.core.instruction.target.multi

import com.szadowsz.maeve.core.instruction.target.Target
import com.szadowsz.common.net.Uri

/**
  * Trait for the common scenario in which we need to construct and target multiple uris from some provided sequence.
  *
  * Created on 16/10/2016.
  */
trait MultiTarget[T <: Any , P <: MultiTarget[T,P]] extends Target[P] {

  val baseUrl: Uri

  val hist: Seq[T]

  val seq: Seq[T]

  /**
    * Checker Function to see if we are at the beginning of the target sequence
    *
    * @return true if it is at the beginning, false otherwise.
    */
  override def atBeginning:Boolean = hist.isEmpty

  /**
    * Checker Function to see if we are at the end of the target sequence
    *
    * @return true if it is at the end, false otherwise.
    */
  override def isDone: Boolean = seq.isEmpty
}
