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

import com.google.inject.ImplementedBy
import org.mongodb.scala.model._
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.models.KnownFacts.{identifierKey, verifierKey}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Identifier, KnownFact, KnownFacts}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepositoryMongo.{PLANET_ID, UPDATED}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[KnownFactsRepositoryMongo])
trait KnownFactsRepository {

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[KnownFacts]]

  def findByIdentifier(identifier: Identifier, planetId: String): Future[Option[KnownFacts]]

  def findByVerifier(knownFacts: KnownFact, planetId: String): Future[Option[KnownFacts]]

  def upsert(knownFacts: KnownFacts, planetId: String): Future[Unit]

  def delete(enrolmentKey: EnrolmentKey, planetId: String): Future[Unit]

  def destroyPlanet(planetId: String): Future[Unit]
}

object KnownFactsRepositoryMongo {
  private final val PLANET_ID = "planetId"
  private final val UPDATED = "lastUpdatedAt"
}

@Singleton
class KnownFactsRepositoryMongo @Inject() (mongo: MongoComponent, appConfig: AppConfig)(implicit
  val ec: ExecutionContext
) extends PlayMongoRepository[JsonAbuse[KnownFacts]](
      mongoComponent = mongo,
      collectionName = "knownFacts",
      domainFormat = JsonAbuse.format[KnownFacts](extractExtraFieldsOnRead = false),
      indexes = Seq(
        IndexModel(Indexes.ascending("planetId")),
        IndexModel(
          Indexes.ascending(KnownFacts.UNIQUE_KEY),
          IndexOptions().name("KnownFactsByEnrolmentKey").unique(true)
        ),
        IndexModel(
          Indexes.ascending(UPDATED),
          IndexOptions().name("TtlIndex").expireAfter(appConfig.collectionsTtl, TimeUnit.HOURS)
        )
      ),
      replaceIndexes = true
    ) with KnownFactsRepository {

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[KnownFacts]] =
    collection
      .find(Filters.equal(KnownFacts.UNIQUE_KEY, KnownFacts.uniqueKey(enrolmentKey.tag, planetId)))
      .toFuture()
      .map(_.headOption)
      .map(_.map(_.value))

  override def findByIdentifier(identifier: Identifier, planetId: String): Future[Option[KnownFacts]] =
    collection
      .find(Filters.equal(KnownFacts.IDENTIFIER_KEYS, identifierKey(identifier, planetId)))
      .toFuture()
      .map(_.headOption)
      .map(_.map(_.value))

  override def findByVerifier(knownFacts: KnownFact, planetId: String): Future[Option[KnownFacts]] =
    collection
      .find(Filters.equal(KnownFacts.VERIFIERS_KEYS, verifierKey(knownFacts, planetId)))
      .toFuture()
      .map(_.headOption)
      .map(_.map(_.value))

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
              replacement = JsonAbuse(knownFacts.copy(planetId = Some(planetId)))
                .addField(UPDATED, Json.toJson(Instant.now())(MongoJavatimeFormats.instantFormat)),
              options = ReplaceOptions().upsert(true)
            )
            .toFuture()
            .flatMap(MongoHelper.interpretUpdateResultUnit)
      )

  def delete(enrolmentKey: EnrolmentKey, planetId: String): Future[Unit] =
    collection
      .deleteOne(Filters.equal(KnownFacts.UNIQUE_KEY, KnownFacts.uniqueKey(enrolmentKey.tag, planetId)))
      .toFuture()
      .map(_ => ())

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(Filters.equal(PLANET_ID, planetId))
      .toFuture()
      .map(_ => ())
}
