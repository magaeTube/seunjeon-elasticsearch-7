/**
 * Copyright 2015 youngho yu, yongwoon lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.bitbucket.eunjeon.seunjeon

import scala.collection.mutable

/**
 * Created by parallels on 7/28/15.
 */

case class LatticeNode(term:Term, startPos:Int, endPos:Int, var accumulatedCost:Int = 9999) {
  var leftNode:LatticeNode = null

  // TODO: hashCode 랑 equals 구현안해도 Set에 중복없이 잘 들어가나?
  override def equals(o: Any) = o match {
    case that:LatticeNode => (that.startPos == startPos) && (that.endPos == endPos)
    case _ => false
  }

  override def hashCode = startPos.hashCode()<<20 + endPos.hashCode()
}

// TODO: connectionCostDict 클래스로 빼자
class Lattice(length:Int, connectingCostDict:ConnectionCostDict) {
  var startingNodes = build2DimNodes(length+2)  // for BOS + EOS
  var endingNodes = build2DimNodes(length+2)    // for BOS + EOS
  var bos = new LatticeNode(new Term("BOS", 0, 0, 0, "BOS"), 0, 0, 0)
  var eos = new LatticeNode(new Term("EOS", 0, 0, 0, "EOS"), length, length)
  startingNodes.head += bos
  endingNodes.head += bos
  startingNodes.last += eos
  endingNodes.last += eos

  private def build2DimNodes(length:Int)
  : mutable.ArraySeq[mutable.MutableList[LatticeNode]] = {
    val temp = new mutable.ArraySeq(length)
    temp.map(l => new mutable.MutableList[LatticeNode])
  }

  def add(latticeNode:LatticeNode): Unit = {
    startingNodes(latticeNode.startPos+1) += latticeNode
    endingNodes(latticeNode.endPos+1) += latticeNode
  }

  def addAll(latticeNodes:Seq[LatticeNode]): Unit = {
    latticeNodes.foreach(node => add(node))
  }

  def getBestPath: Seq[Term] = {
    for (idx <- 1 until startingNodes.length) {
      startingNodes(idx).foreach{ startingNode:LatticeNode =>
        updateCost(endingNodes(idx-1), startingNode)
      }
    }

    var result = new mutable.ListBuffer[Term]
    var node = eos
    while (node != null) {
      result += node.term
      node = node.leftNode
    }
    result.reverse
  }


  private def updateCost(endingNodes:Seq[LatticeNode], startingNode:LatticeNode): Unit = {
    var minTotalCost:Int = 2147483647
    endingNodes.foreach{ endingNode =>
      val totalCost: Int = getCost(endingNode, startingNode)
      if (totalCost < minTotalCost) {
        minTotalCost = totalCost
        startingNode.accumulatedCost = totalCost
        startingNode.leftNode = endingNode
      }
    }
  }

  private def getCost(endingNode: LatticeNode, startingNode: LatticeNode): Int = {
    endingNode.accumulatedCost +
      endingNode.term.cost +
      connectingCostDict.getCost(endingNode.term.rightId, startingNode.term.leftId)
  }
}

