/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.models
import java.util.concurrent.ConcurrentHashMap

import scala.io.Source
import scala.util.Random

object UserIdGenerator {

  val userIdSeries = new ConcurrentHashMap[String, Iterator[String]]

  val userIds: Seq[String] =
    Source
      .fromResource("names.txt")
      .getLines()
      .toIndexedSeq

  val addSuffix: String => String = s => s + "_" + Random.nextInt(10000)

  val defaultUserIds: Seq[String] = shuffle(userIds).take(1000).map(addSuffix)

  private def nextUserId: String = addSuffix(userIds(Random.nextInt(userIds.size)))

  private def shuffle(strings: Seq[String]): Seq[String] =
    strings.zip(Stream.continually(Random.nextInt())).sortBy(_._2).map(_._1)

  def nextUserIdFor(planetId: String, userIdFromPool: Boolean): String =
    if (userIdFromPool)
      Option(userIdSeries.get(planetId)) match {
        case Some(iterator) =>
          if (iterator.hasNext) iterator.next() else nextUserId
        case None =>
          val iterator = new UserIdIterator(shuffle(defaultUserIds))
          userIdSeries.put(planetId, iterator)
          iterator.next()
      }
    else nextUserId

  def destroyPlanetId(planetId: String): Unit = userIdSeries.remove(planetId)

  private class UserIdIterator(userIds: Seq[String]) extends Iterator[String] {
    @volatile var i = 0
    override def hasNext: Boolean = i < userIds.length - 1
    override def next(): String = {
      val id = userIds(i)
      i = i + 1
      id
    }
  }

}
