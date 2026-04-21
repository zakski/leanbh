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
package com.szadowsz.maeve.core.util

import scala.collection.JavaConverters._
import org.jsoup.nodes.{Document, Element}
import org.jsoup.parser.Tag

/**
  * Created on 18/05/2016.
  */
object PageDeltaBuilder {

  def build(base: Document, current: Document): Document = {
    val delta = new Document(current.location())

    if (!base.hasSameValue(current)) {
      build(base, current, delta)
    }
val y = delta.text()
    delta
  }


  def build(base: Element, current: Element, delta: Element): Unit = {
    val childNodes = if(base != null){
      base.children().asScala.zipAll(current.children().asScala, null, null)
    } else {
      current.children().asScala.map((null.asInstanceOf[Element],_))
    }

    val changedNodes = childNodes.filter { case (b, c) => c != null && !c.hasSameValue(b)}

    changedNodes.foreach { case (b, c) =>
      val child = new Element(Tag.valueOf(c.tagName), c.baseUri, c.attributes)
      child.appendText(c.ownText())
      build(b, c, child)
      delta.appendChild(child)
    }
  }
}