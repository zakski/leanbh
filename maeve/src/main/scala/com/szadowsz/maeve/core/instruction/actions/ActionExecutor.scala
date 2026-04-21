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
package com.szadowsz.maeve.core.instruction.actions

import com.szadowsz.maeve.core.browser.MaeveBrowser
import com.szadowsz.common.net.Uri

/**
  * Executor for Javascript Actions; provides context points at the start of a scrape, after a page load, before extraction is called, after extraction is
  * called, as a final cleanup action after a scrape.
  *
  * Created on 13/10/2016.
  */
trait ActionExecutor {

  /**
    * Function to execute actions at the start of a scrape.
    *
    * @param browser     the browser to interact with.
    * @param firstTarget the expected first url that will be accessed after this.
    */
  def doFirstExecutionAction(browser: MaeveBrowser, firstTarget: Uri): Unit

  /**
    * Function to execute actions after a page load.
    *
    * @param browser the browser to interact with.
    */
  def doInitialPageAction(browser: MaeveBrowser): Unit

  /**
    * Function to execute actions before extraction is called.
    *
    * @param browser the browser to interact with.
    */
  def doBeforeExtractAction(browser: MaeveBrowser): Unit

  /**
    * Function to execute actions after extraction is called.
    *
    * @param browser the browser to interact with.
    */
  def doAfterExtractAction(browser: MaeveBrowser): Unit

  /**
    * Function to execute final cleanup actions after a scrape.
    *
    * @param browser the browser to interact with.
    */
  def doFinalExecutionAction(browser: MaeveBrowser): Unit
}
