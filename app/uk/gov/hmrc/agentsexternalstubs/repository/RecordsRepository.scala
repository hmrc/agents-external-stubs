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
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.Record.TYPE
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import uk.gov.hmrc.agentsexternalstubs.syntax.|>

@ImplementedBy(classOf[RecordsRepositoryMongo])
trait RecordsRepository {

  def store[T <: Record](entity: T, planetId: String)(
    implicit ec: ExecutionContext,
    recordType: RecordMetaData[T]): Future[Unit]

  def cursor[T <: Record](
    key: String,
    planetId: String)(implicit reads: Reads[T], ec: ExecutionContext, recordType: RecordMetaData[T]): Cursor[T]

  def findAll(planetId: String)(implicit ec: ExecutionContext): Cursor[Record]
}

@Singleton
class RecordsRepositoryMongo @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[Record, BSONObjectID](
      "records",
      mongoComponent.mongoConnector.db,
      Record.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[Record, BSONObjectID] with RecordsRepository {

  import ImplicitBSONHandlers._

  private final val PLANET_ID = "_planetId"
  private final val UNIQUE_KEY = "_uniqueKey"
  private final val KEYS = "_keys"

  override def indexes =
    Seq(
      Index(Seq(KEYS       -> Ascending), Some("Keys")),
      Index(Seq(UNIQUE_KEY -> Ascending), Some("UniqueKey"), unique = true, sparse = true),
      Index(
        Seq(PLANET_ID -> Ascending),
        Some("TTL"),
        sparse = true,
        options = BSONDocument("expireAfterSeconds" -> BSONLong(2592000)) // 30 days
      )
    )

  override def store[T <: Record](entity: T, planetId: String)(
    implicit ec: ExecutionContext,
    recordType: RecordMetaData[T]): Future[Unit] = {
    val json = Json
      .toJson[Record](entity)
      .as[JsObject]
      .+(PLANET_ID -> JsString(planetId))
      .+(TYPE -> JsString(recordType.typeName))
      .+(
        KEYS -> JsArray(
          entity.uniqueKey
            .map(key => entity.lookupKeys :+ key)
            .getOrElse(entity.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, recordType)))))
      .|> { obj =>
        entity.uniqueKey
          .map(uniqueKey => obj.+(UNIQUE_KEY -> JsString(keyOf(uniqueKey, planetId, recordType))))
          .getOrElse(obj)
      }

    (entity.id match {
      case None     => collection.insert(json)
      case Some(id) => collection.update(Json.obj("_id" -> Json.obj("$oid" -> JsString(id))), json, upsert = true)
    }).flatMap(MongoHelper.interpretWriteResult)

  }

  override def cursor[T <: Record](
    key: String,
    planetId: String)(implicit reads: Reads[T], ec: ExecutionContext, recordType: RecordMetaData[T]): Cursor[T] =
    collection
      .find(
        JsObject(Seq(KEYS -> JsString(keyOf(key, planetId, recordType)))),
        Json.obj(recordType.fieldNames.map(option => option -> toJsFieldJsValueWrapper(JsNumber(1))): _*)
      )
      .cursor[T](ReadPreference.primaryPreferred)(
        implicitly[collection.pack.Reader[Record]].map(_.asInstanceOf[T]),
        ec,
        implicitly[CursorProducer[T]])

  override def findAll(planetId: String)(implicit ec: ExecutionContext): Cursor[Record] =
    collection
      .find(
        JsObject(Seq(PLANET_ID -> JsString(planetId)))
      )
      .cursor[Record](ReadPreference.primaryPreferred)(
        implicitly[collection.pack.Reader[Record]],
        ec,
        implicitly[CursorProducer[Record]])

  private def keyOf[T <: Record](key: String, planetId: String, recordType: RecordMetaData[T]): String =
    s"${recordType.typeName}:${key.replace(" ", "").toLowerCase}@$planetId"
}
