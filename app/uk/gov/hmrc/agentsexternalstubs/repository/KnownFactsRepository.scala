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

import com.google.inject.ImplementedBy
import org.mongodb.scala.model._
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, KnownFacts}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[KnownFactsRepositoryMongo])
trait KnownFactsRepository {

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[KnownFacts]]

  def upsert(knownFacts: KnownFacts, planetId: String): Future[Unit]

  def delete(enrolmentKey: EnrolmentKey, planetId: String): Future[Unit]

  def destroyPlanet(planetId: String): Future[Unit]
}

@Singleton
class KnownFactsRepositoryMongo @Inject() (mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[KnownFacts](
      mongoComponent = mongo,
      collectionName = "knownFacts",
      domainFormat = KnownFacts.formats,
      indexes = Seq(
        IndexModel(
          Indexes.ascending(KnownFacts.UNIQUE_KEY),
          IndexOptions().name("KnownFactsByEnrolmentKey").unique(true)
        )
      ),
      replaceIndexes = true
    ) with StrictlyEnsureIndexes[KnownFacts] with KnownFactsRepository {

  private final val PLANET_ID = "planetId"
  final val UPDATED = "_last_updated_at"

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[KnownFacts]] =
    collection
      .find(Filters.equal(KnownFacts.UNIQUE_KEY, KnownFacts.uniqueKey(enrolmentKey.tag, planetId)))
      .toFuture
      .map(_.headOption)

  def upsert(knownFacts: KnownFacts, planetId: String): Future[Unit] =
    KnownFacts
      .validate(knownFacts)
      .fold(
        errors => Future.failed(new Exception(s"KnownFacts validation failed because $errors")),
        _ =>
          collection
            .replaceOne(
              filter =
                Filters.equal(KnownFacts.UNIQUE_KEY, KnownFacts.uniqueKey(knownFacts.enrolmentKey.tag, planetId)),
              replacement = knownFacts.copy(planetId = Some(planetId)),
              options = ReplaceOptions().upsert(true)
            )
            .toFuture
            .flatMap(MongoHelper.interpretUpdateResultUnit)
      )

  def delete(enrolmentKey: EnrolmentKey, planetId: String): Future[Unit] =
    collection
      .deleteOne(Filters.equal(KnownFacts.UNIQUE_KEY, KnownFacts.uniqueKey(enrolmentKey.tag, planetId)))
      .toFuture
      .map(_ => ())

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(Filters.equal(PLANET_ID, planetId))
      .toFuture
      .map(_ => ())
}
