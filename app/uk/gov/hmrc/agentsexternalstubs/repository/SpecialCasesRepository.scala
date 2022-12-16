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
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.libs.json.{OWrites => _, _}
import uk.gov.hmrc.agentsexternalstubs.models.SpecialCase.internal
import uk.gov.hmrc.agentsexternalstubs.models.{Id, SpecialCase}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import SpecialCasesRepositoryMongo._
import org.bson.types.ObjectId
import uk.gov.hmrc.agentsexternalstubs.syntax.|>

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

object SpecialCasesRepositoryMongo {
  private final val PLANET_ID = "planetId"
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
class SpecialCasesRepositoryMongo @Inject() (mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[SpecialCase](
      mongoComponent = mongo,
      collectionName = "specialCases",
      domainFormat = Format(internal.reads, internal.writes),
      indexes = Seq(
        IndexModel(Indexes.ascending(SpecialCase.UNIQUE_KEY), IndexOptions().name("SpecialCasesByKey").unique(true)),
        IndexModel(Indexes.ascending(Id.ID, PLANET_ID), IndexOptions().name("SpecialCaseId").unique(true))
      ),
      replaceIndexes = true
    ) with StrictlyEnsureIndexes[SpecialCase] with SpecialCasesRepository {

  final val UPDATED = "_last_updated_at"

  def findById(id: String, planetId: String): Future[Option[SpecialCase]] =
    collection
      .find(
        Filters.and(
          Filters.equal(Id.ID, new ObjectId(id)),
          Filters.equal(PLANET_ID, planetId)
        )
      )
      .headOption

  def findByMatchKey(key: String, planetId: String): Future[Option[SpecialCase]] =
    collection
      .find(
        Filters.equal(SpecialCase.UNIQUE_KEY, SpecialCase.uniqueKey(key, planetId))
      )
      .toFuture
      .map(_.headOption)

  def findByPlanetId(planetId: String)(limit: Int): Future[Seq[SpecialCase]] =
    collection
      .find(Filters.equal(PLANET_ID, planetId))
      .|>(o => if (limit >= 0) o.limit(limit) else o)
      .toFuture()

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
                  replacement = specialCase.copy(planetId = Some(planetId)),
                  ReplaceOptions().upsert(false)
                )
                .toFuture
                .map((_, id.value))
                .flatMap(MongoHelper.interpretUpdateResult)
            case None =>
              val newId = ObjectId.get().toString
              collection
                .find(
                  Filters.equal(SpecialCase.UNIQUE_KEY, SpecialCase.uniqueKey(specialCase.requestMatch.toKey, planetId))
                )
                .toFuture
                .map(_.headOption)
                .flatMap {
                  case Some(sc) =>
                    collection
                      .replaceOne(
                        filter = Filters.and(
                          Filters.equal(Id.ID, new ObjectId(sc.id.map(_.value).get)),
                          Filters.equal(PLANET_ID, planetId)
                        ),
                        replacement = specialCase.copy(planetId = Some(planetId)),
                        options = ReplaceOptions().upsert(false)
                      )
                      .toFuture
                      .map((_, sc.id.map(_.value).get))
                      .flatMap(MongoHelper.interpretUpdateResult)
                  case None =>
                    collection
                      .insertOne(
                        specialCase.copy(planetId = Some(planetId), id = Some(Id(newId)))
                      )
                      .toFuture
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
      .toFuture
      .map(_ => ())

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(Filters.equal(PLANET_ID, planetId))
      .toFuture()
      .map(_ => ())
}
