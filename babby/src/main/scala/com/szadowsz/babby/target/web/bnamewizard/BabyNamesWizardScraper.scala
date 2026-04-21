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
package com.szadowsz.babby.target.web.bnamewizard

import java.io.File

import com.szadowsz.common.io.explore.{ExtensionFilter, FileFinder}
import com.szadowsz.common.io.read.FReader
import com.szadowsz.common.io.zip.ZipperUtil
import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.MaeveConf
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.actions.{NopExecutor, WaitExecutor}
import com.szadowsz.maeve.core.instruction.target.multi.{PathTarget, RelativeUriTarget}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created on 05/06/2015.
  */
object BabyNamesWizardScraper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val urlBase = Uri("http://www.babynamewizard.com/baby-name/")
  private val target = PathTarget(urlBase,('a' to 'z').toSeq.map(_.toString))
  private val conf = MaeveConf().setJavaScriptEnabled(false).setThrowExceptionOnScriptError(false)


  def main(args : Array[String]): Unit = {
    val scraper = new MaeveDriver(conf)
    scraper.setRecoveryDirectory("./recovery/")
    val filter = new BNWizNamePageExtractor()
    val actions = new WaitExecutor(2000) // try not throttle the website

    val instruction1 = MaeveInstruction("babynamewizard", target, actions, filter, "./data/web/babynamewizard/", true, true)


    scraper.feedInstruction(instruction1)
    scraper.scrapeUsingCurrInstruction()

    val urlFiles = FileFinder.search("./data/web/babynamewizard/", Option(new ExtensionFilter(".txt",false)))

    val urls = urlFiles.flatMap { f =>
      val read = new FReader(f.getAbsolutePath)

      val buff = ArrayBuffer[String]()
      var l: Option[String] = None
      do {
        l = read.readLineOpt()
        l.foreach(s => buff += s)
      } while (l.isDefined)
      buff.toList.distinct
    }
    val target2 = RelativeUriTarget(Uri("http://www.babynamewizard.com/"),urls.map(Uri(_)))
    val instruction2 = MaeveInstruction("babynamewizardnames", target2, actions, new BNWizNameDataExtractor, "./data/web/babynamewizard/", true, true)

    scraper.feedInstruction(instruction2)
    scraper.scrapeUsingCurrInstruction()
    urlFiles.foreach(_.delete())

  //  ZipperUtil.zip(new File("./data/web/babynamewizard/babynamewizardnames.csv"),new File("./archives/web/babynamewizard/"))
  }
}
