/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{CursorProducer, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, KnownFact, KnownFacts}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
@ImplementedBy(classOf[KnownFactsRepositoryMongo])
trait KnownFactsRepository {

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[KnownFacts]]

  def upsert(knownFacts: KnownFacts, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def delete(enrolmentKey: EnrolmentKey, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def deleteAll(createdBefore: Long)(implicit ec: ExecutionContext): Future[Int]
}

@Singleton
class KnownFactsRepositoryMongo @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[KnownFacts, BSONObjectID](
      "knownFacts",
      mongoComponent.mongoConnector.db,
      KnownFacts.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[KnownFacts, BSONObjectID]
    with KnownFactsRepository with DeleteAll[KnownFacts] {

  import ImplicitBSONHandlers._

  private final val PLANET_ID = "planetId"
  final val UPDATED = "_last_updated_at"

  override def indexes = Seq(
    Index(Seq(KnownFacts.UNIQUE_KEY -> Ascending), Some("KnownFactsByEnrolmentKey"), unique = true)
  )

  def findByEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[KnownFacts]] =
    collection
      .find[JsObject, JsObject](
        Json.obj(KnownFacts.UNIQUE_KEY -> KnownFacts.uniqueKey(enrolmentKey.tag, planetId)),
        None
      )
      .cursor[KnownFacts](ReadPreference.primary)(
        implicitly[collection.pack.Reader[KnownFacts]],
        implicitly[CursorProducer[KnownFacts]])
      .headOption

  def upsert(knownFacts: KnownFacts, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    KnownFacts
      .validate(knownFacts)
      .fold(
        errors => Future.failed(new Exception(s"KnownFacts validation failed because $errors")),
        _ =>
          collection
            .update(
              Json.obj(KnownFacts.UNIQUE_KEY -> KnownFacts.uniqueKey(knownFacts.enrolmentKey.tag, planetId)),
              Json
                .toJson(knownFacts.copy(planetId = Some(planetId)))
                .as[JsObject]
                .+(UPDATED -> JsNumber(System.currentTimeMillis())),
              upsert = true
            )
            .flatMap(MongoHelper.interpretWriteResultUnit)
      )

  def delete(enrolmentKey: EnrolmentKey, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    this
      .remove(
        KnownFacts.UNIQUE_KEY -> toJsFieldJsValueWrapper(KnownFacts.uniqueKey(enrolmentKey.tag, planetId))
      )(ec)
      .map(_ => ())

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove(PLANET_ID -> Option(planetId)).map(_ => ())
}
