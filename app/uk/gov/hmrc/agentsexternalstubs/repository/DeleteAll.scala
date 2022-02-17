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

import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

trait DeleteAll[E] {
  self: ReactiveRepository[E, BSONObjectID] =>

  import ImplicitBSONHandlers._

  val UPDATED: String

  def deleteAll(lastUpdatedBefore: Long)(implicit ec: ExecutionContext): Future[Int] =
    collection
      .delete()
      .element(
        q = BSONDocument(UPDATED -> BSONDocument("$lt" -> BSONLong(lastUpdatedBefore))),
        limit = None,
        collation = None
      )
      .flatMap(e => collection.delete(ordered = false).many(Seq(e)))
      .map(_.n)

}
