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
package com.szadowsz.logainm.target.placenames.ni

import com.szadowsz.common.net.Uri
import com.szadowsz.maeve.core.MaeveDriver
import com.szadowsz.maeve.core.browser.MaeveConf
import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.maeve.core.instruction.target.single.SingleTarget
import org.slf4j.LoggerFactory

import java.util.Locale

/**
  * Created on 05/06/2015.
  */
object PlacenamesNiScraper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val url = Uri("https://experience.arcgis.com/experience/9b31e0501b744154b4584b1dce1f859b/page/Place-Name-Search#data_s=id%3AdataSource_1-PlaceNames_Gazeteer_No_Global_IDs_3734%3A10258")
  private val target = SingleTarget(url)
  private val conf = new MaeveConf().setJavaScriptEnabled(true).setThrowExceptionOnScriptError(false)

  def enableChromeDriver(): Unit = {
    val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
    val arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT)

    val platform = if (osName.contains("win")) {
      if (arch.contains("64")) "win64" else "win32"
    } else if (osName.contains("mac")) {
      if (arch.contains("aarch64") || arch.contains("arm64")) "mac_arm64" else "mac_x64"
    } else if (osName.contains("linux")) {
      "linux64"
    } else {
      throw new RuntimeException(s"Unsupported OS '${osName}'")
    }
    if (platform.contains("win")) {
      System.setProperty("webdriver.chrome.driver", s".\\chromedriver_${platform}\\chromedriver.exe")
    } else {
      System.setProperty("webdriver.chrome.driver", s".\\chromedriver_${platform}\\chromedriver")
    }

  }

  def main(args : Array[String]): Unit = {
    enableChromeDriver()
    val scraper = new MaeveDriver(conf)
    scraper.setRecoveryDirectory("./recovery/")
    val filter = new PlacenamesNiEndlessPageExtractor()
    val actions = new PlacenamesNiEndlessPageExecutor(2000) // try not throttle the website

    val instruction1 = MaeveInstruction("placenamesNI", target, actions, filter, "./data/web/placenamesNI/", isHeadless = false, recovEnabled = true)


    scraper.feedInstruction(instruction1)
    scraper.scrapeUsingCurrInstruction()

//    val urlFiles = FileFinder.search("./data/web/behindthename/", Option(new ExtensionFilter(".txt",false)))
//
//    val urls = urlFiles.sortBy(_.getName).flatMap { f =>
//      val read = new FReader(f.getAbsolutePath)
//
//      val buff = ArrayBuffer[String]()
//      var l: Option[String] = None
//      do {
//        l = read.readLineOpt()
//        l.foreach(s => buff += s)
//      } while (l.isDefined)
//      buff.toList.distinct
//    }
//    val target2 = PathTarget(Uri("http://www.behindthename.com/"),urls)
//    val filter2 =  new BehindTheNameFirstDataExtractor
//    val instruction2 = MaeveInstruction("behindthenameFirstnames", target2, actions, filter2, "./data/web/behindthename/", isHeadless = true, recovEnabled = true)
//
//    scraper.feedInstruction(instruction2)
//    scraper.scrapeUsingCurrInstruction()
//    urlFiles.foreach(_.delete())

//    ZipperUtil.zip(new File("./data/web/babynamewizard/behindthenameFirstnames.csv"),new File("./archives/web/behindthename/"))
  }
}
