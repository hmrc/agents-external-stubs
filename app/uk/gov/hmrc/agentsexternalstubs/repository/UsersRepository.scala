/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersRepository @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[User, BSONObjectID](
      "users",
      mongoComponent.mongoConnector.db,
      User.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[User, BSONObjectID] {

  import ImplicitBSONHandlers._

  def findByUserId(userId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    find(Seq("userId" -> Option(userId)).map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case x :: xs  => throw new IllegalStateException(s"Duplicated userId $userId")
    }

  override def indexes = Seq(
    Index(Seq("userId" -> Ascending), Some("Users"), unique = true)
  )

  def create(user: User)(implicit ec: ExecutionContext): Future[Unit] =
    insert(user).map(_ => ())

  def update(user: User)(implicit ec: ExecutionContext): Future[Unit] =
    (User.formats.writes(user) match {
      case d @ JsObject(_) => collection.update(Json.obj("userId" -> user.userId), d, upsert = true)
      case _ =>
        Future.failed[WriteResult](new Exception("cannot write object"))
    }).map(_ => ())

  def delete(userId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("userId" -> userId)

}
