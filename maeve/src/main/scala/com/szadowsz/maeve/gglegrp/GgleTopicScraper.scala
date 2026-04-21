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
package com.szadowsz.maeve.gglegrp

import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.MaeveConf
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.actions.WaitExecutor
import com.szadowsz.maeve.core.instruction.target.multi.FragmentTarget
import com.szadowsz.maeve.gglegrp.extractor.GrpTopicsExtractor
import com.szadowsz.common.io.read.FReader
import com.szadowsz.common.net.Uri
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Rendered Necessary Unfortunately.
  *
  * Created on 19/10/2016.
  */
class GgleTopicScraper(private val link: String, private val dir: String, private val recoveryDir: Option[String], private val deltaMode: Boolean) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val urlBase = Uri("https://groups.google.com/forum/")
  private val group: String = link.substring(link.lastIndexOf("/") + 1)
  private val groupDir: String = dir + s"${group.replaceAll("\\.", "/")}/"

  def execute(): Unit = {
    val conf = MaeveConf()
    val scraper = new MaeveDriver(conf)
    recoveryDir.foreach(p => scraper.setRecoveryDirectory(p))

    val topics = if (!deltaMode) {
      val read = new FReader(dir + s"$group.txt")

      val buff = ArrayBuffer[String]()
      var l: Option[String] = None
      do {
        l = read.readLineOpt()
        l.foreach(s => buff += s)
      } while (l.isDefined)
      buff.toList.distinct
    } else {
      GrpComErrorDelta.calcDelta(groupDir, recoveryDir.getOrElse("./recovery/"), group)
    }
    logger.info(s"Discovered ${topics.length} Topics")
    val target = FragmentTarget(urlBase, topics)
    val filter = new GrpTopicsExtractor()
    val actions = new WaitExecutor(5000)

    val instruction = MaeveInstruction(group, target, actions, filter, groupDir, false, !deltaMode, false, MaeveConf().setNoProxy())

    scraper.feedInstruction(instruction)
    scraper.scrapeUsingCurrInstruction()
  }
}
