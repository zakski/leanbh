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

import com.szadowsz.common.io.explore.{ExtensionFilter, FileFinder}
import com.szadowsz.maeve.core.util.recovery.RecoveryUtil
import com.szadowsz.common.io.read.FReader
import com.szadowsz.common.net.Uri

/**
  * Created on 21/10/2016.
  */
object GrpComErrorDelta {

  def calcDelta(groupDir: String, recoveDir: String, group : String): List[String] = {
    val topicFiles = FileFinder.search(groupDir, Option(new ExtensionFilter(".txt", true)))
    val topicUrls = topicFiles.map { file =>
      val read = new FReader(file.getAbsolutePath)
      val line = read.readLine()
      line.substring(line.indexOf(" ")).trim
    }
    val history = RecoveryUtil.doRecovery(recoveDir,group)
    history.filterNot(topicUrls.contains).distinct.map(Uri(_).fragment)
  }
}
