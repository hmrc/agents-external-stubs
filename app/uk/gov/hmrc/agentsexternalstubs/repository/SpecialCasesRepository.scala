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
import org.bson.types.ObjectId
import org.mongodb.scala.model._
import play.api.libs.json.{OWrites => _, _}
import uk.gov.hmrc.agentsexternalstubs.models.SpecialCase.internal
import uk.gov.hmrc.agentsexternalstubs.models.{Id, SpecialCase}
import uk.gov.hmrc.agentsexternalstubs.repository.SpecialCasesRepositoryMongo._
import uk.gov.hmrc.agentsexternalstubs.syntax.|>
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object SpecialCasesRepositoryMongo {
  private final val PLANET_ID = "planetId"
  private final val UPDATED = "lastUpdatedAt"
  implicit val specialCaseFormat: OFormat[SpecialCase] = Json.format[SpecialCase]
}

@ImplementedBy(classOf[SpecialCasesRepositoryMongo])
trait SpecialCasesRepository {

  def findById(id: String, planetId: String): Future[Option[SpecialCase]]

  def findByMatchKey(key: String, planetId: String): Future[Option[SpecialCase]]

  def findByPlanetId(planetId: String)(limit: Int): Future[Seq[SpecialCase]]

  def upsert(specialCase: SpecialCase, planetId: String): Future[String]

  def delete(id: String, planetId: String): Future[Unit]

  def destroyPlanet(planetId: String): Future[Unit]
}

@Singleton
class SpecialCasesRepositoryMongo @Inject() (mongo: MongoComponent, appConfig: AppConfig)(implicit
  val ec: ExecutionContext
) extends PlayMongoRepository[JsonAbuse[SpecialCase]](
      mongoComponent = mongo,
      collectionName = "specialCases",
      domainFormat = JsonAbuse.format()(OFormat[SpecialCase](internal.reads, internal.writes)),
      indexes = Seq(
        IndexModel(Indexes.ascending(SpecialCase.UNIQUE_KEY), IndexOptions().name("SpecialCasesByKey").unique(true)),
        IndexModel(Indexes.ascending(PLANET_ID)),
        IndexModel(Indexes.ascending(Id.ID, PLANET_ID), IndexOptions().name("SpecialCaseId").unique(true)),
        IndexModel(
          Indexes.ascending(UPDATED),
          IndexOptions().name("TtlIndex").expireAfter(appConfig.collectionsTtl, TimeUnit.DAYS)
        )
      ),
      replaceIndexes = true
    ) with SpecialCasesRepository {

  def findById(id: String, planetId: String): Future[Option[SpecialCase]] =
    collection
      .find(
        Filters.and(
          Filters.equal(Id.ID, new ObjectId(id)),
          Filters.equal(PLANET_ID, planetId)
        )
      )
      .headOption()
      .map(_.map(_.value))

  def findByMatchKey(key: String, planetId: String): Future[Option[SpecialCase]] =
    collection
      .find(
        Filters.equal(SpecialCase.UNIQUE_KEY, SpecialCase.uniqueKey(key, planetId))
      )
      .toFuture()
      .map(_.headOption)
      .map(_.map(_.value))

  def findByPlanetId(planetId: String)(limit: Int): Future[Seq[SpecialCase]] =
    collection
      .find(Filters.equal(PLANET_ID, planetId))
      .|>(o => if (limit >= 0) o.limit(limit) else o)
      .toFuture()
      .map(_.map(_.value))

  def upsert(specialCase: SpecialCase, planetId: String): Future[String] =
    SpecialCase
      .validate(specialCase)
      .fold(
        errors => Future.failed(new Exception(s"SpecialCase validation failed because $errors")),
        _ =>
          specialCase.id match {
            case Some(id) =>
              collection
                .replaceOne(
                  filter = Filters.and(
                    Filters.equal(Id.ID, new ObjectId(id.value)),
                    Filters.equal(PLANET_ID, planetId)
                  ),
                  replacement = JsonAbuse(specialCase.copy(planetId = Some(planetId)))
                    .addField(UPDATED, Json.toJson(Instant.now())(MongoJavatimeFormats.instantFormat)),
                  ReplaceOptions().upsert(false)
                )
                .toFuture()
                .map((_, id.value))
                .flatMap(MongoHelper.interpretUpdateResult)
            case None =>
              val newId = ObjectId.get().toString
              collection
                .find(
                  Filters.equal(SpecialCase.UNIQUE_KEY, SpecialCase.uniqueKey(specialCase.requestMatch.toKey, planetId))
                )
                .toFuture()
                .map(_.headOption)
                .flatMap {
                  case Some(sc) =>
                    collection
                      .replaceOne(
                        filter = Filters.and(
                          Filters.equal(Id.ID, new ObjectId(sc.value.id.map(_.value).get)),
                          Filters.equal(PLANET_ID, planetId)
                        ),
                        replacement = JsonAbuse(specialCase.copy(planetId = Some(planetId)))
                          .addField(UPDATED, Json.toJson(Instant.now())(MongoJavatimeFormats.instantFormat)),
                        options = ReplaceOptions().upsert(false)
                      )
                      .toFuture()
                      .map((_, sc.value.id.map(_.value).get))
                      .flatMap(MongoHelper.interpretUpdateResult)
                  case None =>
                    collection
                      .insertOne(
                        JsonAbuse(specialCase.copy(planetId = Some(planetId), id = Some(Id(newId))))
                          .addField(UPDATED, Json.toJson(Instant.now())(MongoJavatimeFormats.instantFormat))
                      )
                      .toFuture()
                      .map((_, newId))
                      .flatMap(MongoHelper.interpretInsertOneResult)
                }
          }
      )

  def delete(id: String, planetId: String): Future[Unit] =
    collection
      .deleteOne(
        Filters.and(
          Filters.equal(Id.ID, new ObjectId(id)),
          Filters.equal(PLANET_ID, planetId)
        )
      )
      .toFuture()
      .map(_ => ())

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(Filters.equal(PLANET_ID, planetId))
      .toFuture()
      .map(_ => ())
}
