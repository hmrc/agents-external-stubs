/*
 * Copyright 2022 HM Revenue & Customs
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

import scala.concurrent.Future
import scala.io.Source
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import concurrent.Await.result

object UserIdGenerator {

  val userIdSeries = new ConcurrentHashMap[String, Iterator[String]]

  val userIds: Future[Seq[String]] = Future {
    Source
      .fromResource("names.txt")
      .getLines()
      .toIndexedSeq
  }

  val addSuffix: String => String = s => s + "_" + Random.nextInt(10000)

  val defaultUserIds: Future[Seq[String]] = userIds.map(shuffle).map(_.take(1000).map(addSuffix))

  private def nextUserId: Future[String] = userIds.map(users => addSuffix(users(Random.nextInt(users.size))))

  private def shuffle(strings: Seq[String]): Seq[String] =
    strings.zip(Stream.continually(Random.nextInt())).sortBy(_._2).map(_._1)

  def nextUserIdFor(planetId: String, userIdFromPool: Boolean): String =
    if (userIdFromPool)
      Option(userIdSeries.get(planetId)) match {
        case Some(iterator) =>
          if (iterator.hasNext) iterator.next() else result(nextUserId, 60.seconds)
        case None =>
          val iterator = new UserIdIterator(result(defaultUserIds.map(shuffle), 60.seconds))
          userIdSeries.put(planetId, iterator)
          iterator.next()
      }
    else result(nextUserId, 60.seconds)

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
