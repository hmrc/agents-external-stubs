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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDocument, BSONObjectID}
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

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    find(Seq("authToken" -> Option(authToken)).map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*)
      .map(_.headOption)

  def findBySessionId(sessionId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    find(Seq("sessionId" -> Option(sessionId)).map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*)
      .map(_.headOption)

  def findByUserId(userId: String)(implicit ec: ExecutionContext): Future[List[AuthenticatedSession]] =
    find(Seq("userId" -> Option(userId)).map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*)

  override def indexes = Seq(
    Index(
      Seq("authToken" -> Ascending),
      Some("AuthenticatedSessions"),
      unique = true,
      options = BSONDocument("expireAfterSeconds" -> 900)),
    Index(Seq("sessionId" -> Ascending), Some("SessionIds"), unique = true),
    Index(Seq("userId"    -> Ascending), Some("AuthenticatedUsers"))
  )

  def create(userId: String, authToken: String, providerType: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Unit] =
    insert(AuthenticatedSession(UUID.randomUUID().toString, userId, authToken, providerType, planetId)).map(_ => ())

  def delete(sessionId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("authToken" -> sessionId)

}
