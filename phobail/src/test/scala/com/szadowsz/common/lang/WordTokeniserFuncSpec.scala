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
package com.szadowsz.common.lang

import org.scalatest.LoneElement._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
  * Simple Unit Tests for the word tokeniser.
  *
  * Created on 28/07/2016.
  */
class WordTokeniserFuncSpec extends AnyFunSpec with Matchers {

  describe("Tokenisation Function"){

    it("should only return a single element if no delimiters are found"){
      val list = WordTokeniser.tokenise("NASA")
      list.loneElement should be ("NASA")
    }

    it("should only use spaces as a delimiter by default"){
      WordTokeniser.tokenise("Anti-Aircraft Gun") should contain theSameElementsAs List("Anti-Aircraft","Gun")
    }

    it("should be able to use user-defined delimiters"){
      WordTokeniser.tokenise("Anti-Aircraft Gun",List(' ','-')) should contain theSameElementsAs List("Anti","Aircraft","Gun")
    }
  }
}
