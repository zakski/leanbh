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
package com.szadowsz.maeve.core.error

import org.openqa.selenium.WebDriverException

/**
  * General Root Exception to build on for all scraping specific issues that can occur.
  *
  * Created on 12/10/2016.
  */
class MaeveException(message: String, cause: Throwable) extends WebDriverException(message, cause) {

  def this() {
    this(null, null)
  }

  def this(message: String) {
    this(message, null)
  }

  def this(cause: Throwable) {
    this(cause.toString, cause)
  }
}
