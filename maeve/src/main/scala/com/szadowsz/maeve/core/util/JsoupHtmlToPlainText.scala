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

import org.jsoup.helper.StringUtil
import org.jsoup.nodes.{Element, Node, TextNode}
import org.jsoup.select.{NodeTraversor, NodeVisitor}

/**
  * This is a port of a jsoup project example from java to scala. taken from
  * https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java
  *
  * Created on 02/11/2016.
  */
object JsoupHtmlToPlainText {
  val terminalLength = 80
  private val headList = Set("p", "h1", "h2", "h3", "h4", "h5", "tr")
  private val tailSet = Set("br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")

  /**
    * Function to walk the provided Dom Element and translate it into plain text.
    *
    * @param element the dom element.
    * @return string translation.
    */
  def convertToPlainText(element : Element,maxWidth : Int = terminalLength): String = {
    val formatter = FormattingVisitor(maxWidth)
    val traversor = new NodeTraversor(formatter)
    traversor.traverse(element) // walk the DOM, and call .head() and .tail() for each node
    formatter.toString()
  }

  // the formatting rules, implemented in a breadth-first DOM traverse
  private case class FormattingVisitor(maxWidth : Int) extends NodeVisitor {
    private var width = 0
    private val accumulator = new StringBuilder() // holds the accumulated text

    /**
      * hit when the node is first seen.
      *
      * @param node the element we are examining.
      * @param depth current depth (not used).
      */
    override def head(node : Node, depth : Int) {
      val name = node.nodeName()
      if (node.isInstanceOf[TextNode]) {
        append(node.asInstanceOf[TextNode].text()); // TextNodes carry all user-readable text in the DOM.
      } else if (name.equals("li")) {
        append("\n * ")
      } else if (name.equals("dt")) {
        append("  ")
      } else if (headList.contains(name)) {
        append("\n")
      }
    }

    /**
      * hit when all of the node's children (if any) have been visited.
      *
      * @param node the element we are examining.
      * @param depth current depth (not used).
      */
    override def tail(node : Node, depth : Int):Unit = {
      val name = node.nodeName()
      if (JsoupHtmlToPlainText.tailSet.contains(name)) {
        append("\n")
      } else if (name.equals("a")) {
        append(String.format(" <%s>", node.absUrl("href")))
      }
    }

    /**
      * appends text to the string builder with a simple word wrap method
      *
      * @param text the text to append.
      */
    private def append(text: String):Unit =  {
      if (text.startsWith("\n"))
        width = 0; // reset counter if starts with a newline. only from formats above, not in natural text
      // / don't accumulate long runs of empty spaces
      if (!(text.equals(" ") &&  (accumulator.isEmpty || StringUtil.in(accumulator.substring(accumulator.length - 1), " ", "\n")))) {
        if (text.length() + width > maxWidth) {
          // won't fit, needs to wrap
          val words = text.split("\\s+")
          words.zipWithIndex.foreach { case (nword, i) =>
            var word = nword
            val last = i == words.length - 1
            if (!last) // insert a space if not the last word
              word = word + " "
            if (word.length() + width > maxWidth) {
              // wrap and reset counter
              accumulator.append("\n").append(word)
              width = word.length()
            } else {
              accumulator.append(word)
              width += word.length()
            }
          }
        } else {
          // fits as is, without need to wrap text
          accumulator.append(text)
          width += text.length()
        }
      }
    }

    override def toString: String = {
      accumulator.toString()
    }
  }
}
