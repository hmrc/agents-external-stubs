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
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatedSessionsRepository @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[AuthenticatedSession, BSONObjectID](
      "authenticated-sessions",
      mongoComponent.mongoConnector.db,
      AuthenticatedSession.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[AuthenticatedSession, BSONObjectID] {

  import ImplicitBSONHandlers._

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("authToken" -> Option(authToken)))(AuthenticatedSession.formats)

  def findBySessionId(sessionId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("sessionId" -> Option(sessionId)))(AuthenticatedSession.formats)

  def findByPlanetId(planetId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("planetId" -> Option(planetId)))(AuthenticatedSession.formats)

  def findByUserId(userId: String)(implicit ec: ExecutionContext): Future[List[AuthenticatedSession]] =
    cursor[AuthenticatedSession](Seq("userId" -> Option(userId)))(AuthenticatedSession.formats)
      .collect[List](maxDocs = 1000, err = Cursor.FailOnError())

  override def indexes = Seq(
    Index(
      Seq("authToken" -> Ascending),
      Some("AuthenticatedSessions"),
      unique = true,
      options = BSONDocument("expireAfterSeconds" -> 900)),
    Index(Seq("sessionId" -> Ascending), Some("SessionIds"), unique = true),
    Index(Seq("userId"    -> Ascending), Some("AuthenticatedUsers"))
  )

  def create(authenticatedSession: AuthenticatedSession)(implicit ec: ExecutionContext): Future[Unit] =
    insert(authenticatedSession).map(_ => ())

  def delete(sessionId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("authToken" -> sessionId)

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove("planetId" -> Option(planetId)).map(_ => ())

  private val toJsWrapper: PartialFunction[(String, Option[String]), (String, Json.JsValueWrapper)] = {
    case (name, Some(value)) => name -> toJsFieldJsValueWrapper(value)
  }

  private def one[T](query: Seq[(String, Option[String])], projection: Seq[(String, Int)] = Seq.empty)(
    reader: collection.pack.Reader[T])(implicit ec: ExecutionContext): Future[Option[T]] =
    collection
      .find(
        Json.obj(query.collect(toJsWrapper): _*),
        if (projection.isEmpty) None
        else Some(Json.obj(projection.map(option => option._1 -> toJsFieldJsValueWrapper(option._2)): _*))
      )
      .one[T](readPreference = ReadPreference.nearest)(reader, ec)

  private def cursor[T](query: Seq[(String, Option[String])], projection: Seq[(String, Int)] = Seq.empty)(
    reader: collection.pack.Reader[T])(implicit ec: ExecutionContext): Cursor[T] =
    collection
      .find(
        Json.obj(query.collect(toJsWrapper): _*),
        if (projection.isEmpty) None
        else Some(Json.obj(projection.map(option => option._1 -> toJsFieldJsValueWrapper(option._2)): _*))
      )
      .cursor[T](readPreference = ReadPreference.nearest)(reader, implicitly[CursorProducer[T]])

}
