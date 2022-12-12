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

package uk.gov.hmrc.agentsexternalstubs.repository

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.{ExecutionContext, Future}

trait DeleteAll[E] {
  self: PlayMongoRepository[E] =>

  implicit val ec: ExecutionContext

  val UPDATED: String

  def deleteAll(lastUpdatedBefore: Long): Future[Long] =
    collection
      .deleteMany(Filters.lt(UPDATED, lastUpdatedBefore))
      .toFuture()
      .map(_.getDeletedCount)
}
