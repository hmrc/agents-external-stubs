/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.services
import com.github.blemale.scaffeine.Scaffeine

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AsyncCache[K, V](
  maximumSize: Int,
  expireAfterWrite: Option[Duration] = None,
  expireAfterAccess: Option[Duration] = None,
  keys: V => Seq[K]
) {

  private val cache = {
    val s = Scaffeine().maximumSize(maximumSize)
    val s1 = expireAfterWrite.map(d => s.expireAfterWrite(FiniteDuration(d.length, d.unit))).getOrElse(s)
    val s2 = expireAfterAccess.map(d => s1.expireAfterAccess(FiniteDuration(d.length, d.unit))).getOrElse(s1)
    s2.build[K, V]()
  }

  def get(key: K, load: Future[V])(implicit ec: ExecutionContext): Future[V] =
    cache.getIfPresent(key) match {
      case Some(value) => Future.successful[V](value)
      case None =>
        load.map { value =>
          keys(value).foreach(cache.put(_, value))
          value
        }
    }

  def getOption(key: K, load: => Future[Option[V]])(implicit ec: ExecutionContext): Future[Option[V]] =
    cache.getIfPresent(key) match {
      case Some(value) => Future.successful[Option[V]](Some(value))
      case None =>
        load.map {
          _.map { value =>
            keys(value).foreach(cache.put(_, value))
            value
          }
        }
    }

  def put(value: V): Future[Unit] = {
    keys(value).foreach(cache.put(_, value))
    Future.successful(())
  }

  def put(value: Option[V]): Future[Unit] = {
    value.foreach(v => keys(v).foreach(cache.put(_, v)))
    Future.successful(())
  }

  def invalidate(key: K): Future[Unit] = {
    cache
      .getIfPresent(key)
      .foreach(v => keys(v).foreach(cache.invalidate))
    Future.successful(())
  }
}
