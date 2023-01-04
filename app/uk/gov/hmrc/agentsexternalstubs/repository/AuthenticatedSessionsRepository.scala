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

package uk.gov.hmrc.agentsexternalstubs.repository

import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.result.DeleteResult

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatedSessionsRepository @Inject() (mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[AuthenticatedSession](
      mongoComponent = mongo,
      collectionName = "authenticated-sessions",
      domainFormat = AuthenticatedSession.formats,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("authToken"),
          IndexOptions().unique(true).name("AuthenticatedSessions").expireAfter(900, TimeUnit.SECONDS)
        ),
        IndexModel(Indexes.ascending("sessionId"), IndexOptions().unique(true).name("SessionIds")),
        IndexModel(Indexes.ascending("userId"), IndexOptions().name("AuthenticatedUsers"))
      ),
      replaceIndexes = true
    ) {

  final val UPDATED = "createdAt"

  def findByAuthToken(authToken: String): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("authToken" -> authToken))

  def findBySessionId(sessionId: String): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("sessionId" -> sessionId))

  def findByPlanetId(planetId: String): Future[Option[AuthenticatedSession]] =
    one[AuthenticatedSession](Seq("planetId" -> planetId))

  def findByUserId(userId: String): Future[Seq[AuthenticatedSession]] =
    collection
      .find(Filters.equal("userId", userId))
      .limit(1000)
      .toFuture

  def create(authenticatedSession: AuthenticatedSession): Future[Unit] =
    collection
      .insertOne(authenticatedSession)
      .toFuture()
      .map(_ => ())

  def delete(sessionId: String): Future[DeleteResult] =
    collection
      .deleteOne(Filters.equal("authToken", sessionId))
      .toFuture()

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(Filters.equal("planetId", planetId))
      .toFuture()
      .map(_ => ())

  private def one[T](
    query: Seq[(String, String)]
  ): Future[Option[AuthenticatedSession]] =
    collection
      .find(Filters.and(query.map { case (field, value) => Filters.equal(field, value) }: _*))
      .toFuture
      .map(_.headOption)
}
