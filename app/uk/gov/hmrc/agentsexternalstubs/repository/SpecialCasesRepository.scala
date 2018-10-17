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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{CursorProducer, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.SpecialCase.internal
import uk.gov.hmrc.agentsexternalstubs.models.{Id, SpecialCase}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
@ImplementedBy(classOf[SpecialCasesRepositoryMongo])
trait SpecialCasesRepository {

  def findById(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[SpecialCase]]

  def findByMatchKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[SpecialCase]]

  def findByPlanetId(planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[SpecialCase]]

  def upsert(specialCase: SpecialCase, planetId: String)(implicit ec: ExecutionContext): Future[String]

  def delete(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit]
}

@Singleton
class SpecialCasesRepositoryMongo @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[SpecialCase, BSONObjectID](
      "specialCases",
      mongoComponent.mongoConnector.db,
      Format(internal.reads, internal.writes),
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[SpecialCase, BSONObjectID]
    with SpecialCasesRepository {

  private final val PLANET_ID = "planetId"

  override def indexes = Seq(
    Index(Seq(SpecialCase.UNIQUE_KEY -> Ascending), Some("SpecialCasesByKey"), unique = true),
    Index(Seq(Id.ID                  -> Ascending, PLANET_ID -> Ascending), Some("SpecialCaseId"), unique = true)
  )

  import ImplicitBSONHandlers._

  implicit val writes: OWrites[SpecialCase] = SpecialCase.internal.writes

  def findById(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[SpecialCase]] =
    collection
      .find(
        Json.obj(Id.ID -> Json.obj("$oid" -> JsString(id)), PLANET_ID -> JsString(planetId))
      )
      .cursor[SpecialCase](ReadPreference.primaryPreferred)(
        implicitly[collection.pack.Reader[SpecialCase]],
        ec,
        implicitly[CursorProducer[SpecialCase]])
      .headOption

  def findByMatchKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[SpecialCase]] =
    collection
      .find(
        Json.obj(SpecialCase.UNIQUE_KEY -> SpecialCase.uniqueKey(key, planetId))
      )
      .cursor[SpecialCase](ReadPreference.primaryPreferred)(
        implicitly[collection.pack.Reader[SpecialCase]],
        ec,
        implicitly[CursorProducer[SpecialCase]])
      .headOption

  def findByPlanetId(planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[SpecialCase]] =
    collection
      .find(Json.obj(PLANET_ID -> planetId))
      .cursor[SpecialCase]
      .collect[Seq](limit)

  def upsert(specialCase: SpecialCase, planetId: String)(implicit ec: ExecutionContext): Future[String] =
    SpecialCase
      .validate(specialCase)
      .fold(
        errors => Future.failed(new Exception(s"SpecialCase validation failed because $errors")),
        _ =>
          specialCase.id match {
            case Some(id) =>
              collection
                .update(
                  Json.obj(Id.ID -> Json.obj("$oid" -> JsString(id.value)), PLANET_ID -> JsString(planetId)),
                  specialCase.copy(planetId = Some(planetId)),
                  upsert = false
                )
                .map((_, id.value))
                .flatMap(MongoHelper.interpretWriteResult)
            case None =>
              val newId = BSONObjectID.generate().stringify
              collection
                .find(
                  Json.obj(
                    SpecialCase.UNIQUE_KEY -> JsString(SpecialCase.uniqueKey(specialCase.requestMatch.toKey, planetId)))
                )
                .one[SpecialCase]
                .flatMap {
                  case Some(sc) =>
                    collection
                      .update(
                        Json.obj(
                          Id.ID     -> Json.obj("$oid" -> JsString(sc.id.map(_.value).get)),
                          PLANET_ID -> JsString(planetId)),
                        specialCase.copy(planetId = Some(planetId)),
                        upsert = false
                      )
                      .map((_, sc.id.map(_.value).get))
                      .flatMap(MongoHelper.interpretWriteResult)
                  case None =>
                    collection
                      .insert(
                        specialCase.copy(planetId = Some(planetId), id = Some(Id(newId)))
                      )
                      .map((_, newId))
                      .flatMap(MongoHelper.interpretWriteResult)
                }
        }
      )

  def delete(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    this
      .remove(
        Id.ID     -> Json.obj("$oid" -> JsString(id)),
        PLANET_ID -> JsString(planetId)
      )(ec)
      .map(_ => ())

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove(PLANET_ID -> Option(planetId)).map(_ => ())
}
