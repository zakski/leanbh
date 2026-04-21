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
package com.szadowsz.maeve.core.instruction.extractor

import javax.xml.xpath.{XPathConstants, XPathFactory}

import org.w3c.dom.{Document, Element, Node, NodeList}

/**
  * Base trait for extraction of Data from an xml Dom page.
  *
  * Created on 14/10/2016.
  */
trait DomExtractor extends DataExtractor[Document]{

  protected val xPath = XPathFactory.newInstance.newXPath

  /**
    * Utility Function to extract data via xpath
    *
    * @param thePath the xpath command
    * @param theElement the base element to use it on
    * @return a list of nodes that satisfy the criteria.
    */
  protected def getXPath(thePath: String, theElement: Element): List[Node] = {
    val nodes = xPath.evaluate(thePath, theElement, XPathConstants.NODESET).asInstanceOf[NodeList]
    (0 until nodes.getLength).map(nodes.item).toList
  }
}
