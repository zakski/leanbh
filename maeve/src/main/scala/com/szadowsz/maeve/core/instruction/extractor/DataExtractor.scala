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
package com.szadowsz.maeve.core.instruction.extractor

import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.common.net.Uri

/**
  * Base trait for extraction of Data from a Webpage.
  *
  * Created on 14/10/2016.
  */
private[maeve] trait DataExtractor[P <: Any] {

  /**
    * Generic method to extract data from a webpage.
    *
    * @param queryUrl the expected url of the page.
    * @param returnedUrl the actual url of the page.
    * @param inst the current maeve instruction.
    * @param page the webpage in whatever format is being provided.
    */
  def extract(queryUrl: Uri, returnedUrl: Uri, inst : MaeveInstruction[_], page : P)

  /**
    * Function to check if retrieval is finished for the current page.
    *
    * @return true if we should continue to extract data from the current page, false otherwise.
    */
  def shouldContinue():Boolean
}
