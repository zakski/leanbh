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
package com.szadowsz.maeve.core.instruction.extractor.util

import java.io.File

import com.szadowsz.maeve.core.instruction.extractor.DataExtractor
import com.szadowsz.common.io.write.FWriter

/**
  * Trait to provide support to write lines to text files.
  *
  * Created on 17/10/2016.
  */
trait TxtFileLineWriter {
  this: DataExtractor[_] =>

  /**
    * Function to check if file already exists.
    *
    * @param path the file path of the file.
    * @param name the name of the file, minus its extension.
    * @return true if it exists, false otherwise.
    */
  def exists(path : String, name : String):Boolean = new File(path + name + ".txt").exists()

  /**
    * Function to add lines to a file.
    *
    * @param path the file path of the file.
    * @param name the name of the file, minus its extension.
    * @param lines the lines to add.
    * @param append whether or not to append to the file or overwrite it.
    */
  def write(path : String, name : String, lines: List[String], append : Boolean): Unit = {
    val writer = FWriter(path + name + ".txt", append)
    lines.foreach(writer.writeLine)
    writer.close()
  }
}
