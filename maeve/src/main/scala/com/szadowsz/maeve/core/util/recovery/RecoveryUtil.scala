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
package com.szadowsz.maeve.core.util.recovery

import java.io.File

import com.szadowsz.maeve.core.instruction.MaeveInstruction
import com.szadowsz.common.io.read.FReader
import com.szadowsz.common.io.write.FWriter

/**
  * Created on 13/10/2016.
  */
object RecoveryUtil {


  def doRecovery(path: String, instr: MaeveInstruction[_]): MaeveInstruction[_] = {
    //    instr.recovMode match {
    //      case RecoveryMode.PAGES =>
    instr.applyLinkHistory(doRecovery(path, instr.name))
    //      case RecoveryMode.PARTIAL_RESULT => instr.applyWorkerHistory(doRecovery(path, instr.name))
    //      case _ =>
    //        instr.applyLinkHistory(doRecovery(path, instr.name + "-PAGES"))
    //          .applyWorkerHistory(doRecovery(path, instr.name + "-Workers"))
    //    }
  }

  def doRecovery(path: String, name: String): List[String] = {
    val filepath = path + name + ".recov"
    if (new File(filepath).exists) {
      val reader = new FReader(filepath)
      Iterator.continually(reader.readLineOpt()).takeWhile(_.isDefined).map(_.get).toList
    } else {
      Nil
    }
  }

  def appendRecovery(path: String, name: String, line: String): Unit = {
    val filepath = path + name + ".recov"
    val writer = new FWriter(filepath, true)
    writer.writeLine(line)
    writer.close()
  }
}
