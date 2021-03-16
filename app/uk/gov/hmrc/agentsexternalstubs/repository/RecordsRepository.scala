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
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{OWrites => _, _}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.agentsexternalstubs.models.Record.TYPE
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.syntax.|>
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RecordsRepositoryMongo])
trait RecordsRepository {

  def store[T <: Record](entity: T, planetId: String)(implicit reads: Reads[T], ec: ExecutionContext): Future[String]

  def cursor[T <: Record](key: String, planetId: String)(implicit
    reads: Reads[T],
    ec: ExecutionContext,
    recordType: RecordMetaData[T]
  ): Cursor[T]

  def cursor[T <: Record](keys: Seq[String], planetId: String)(implicit
    reads: Reads[T],
    ec: ExecutionContext,
    recordType: RecordMetaData[T]
  ): Cursor[T]

  def findById[T <: Record](id: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[T]]

  def findByPlanetId(planetId: String)(implicit ec: ExecutionContext): Cursor[Record]

  def remove(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def deleteAll(createdBefore: Long)(implicit ec: ExecutionContext): Future[Int]
}

@Singleton
class RecordsRepositoryMongo @Inject() (mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[Record, BSONObjectID](
      "records",
      mongoComponent.mongoConnector.db,
      Record.formats,
      ReactiveMongoFormats.objectIdFormats
    ) with StrictlyEnsureIndexes[Record, BSONObjectID] with RecordsRepository with DeleteAll[Record] {

  final val PLANET_ID = "_planetId"
  final val UNIQUE_KEY = "_uniqueKey"
  final val KEYS = "_keys"
  final val UPDATED = "_last_updated_at"

  override def indexes =
    Seq(
      Index(Seq(KEYS -> Ascending), Some("Keys")),
      Index(Seq(UNIQUE_KEY -> Ascending), Some("UniqueKey"), unique = true, sparse = true)
    )

  override def store[T <: Record](entity: T, planetId: String)(implicit
    reads: Reads[T],
    ec: ExecutionContext
  ): Future[String] = {
    val typeName = Record.typeOf(entity)
    val json = Json
      .toJson[Record](entity)
      .as[JsObject]
      .+(PLANET_ID -> JsString(planetId))
      .+(TYPE -> JsString(typeName))
      .+(
        KEYS -> JsArray(
          entity.uniqueKey
            .map(key => entity.lookupKeys :+ key)
            .getOrElse(entity.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, typeName)))
        )
      )
      .|> { obj =>
        entity.uniqueKey
          .map(uniqueKey => obj.+(UNIQUE_KEY -> JsString(keyOf(uniqueKey, planetId, typeName))))
          .getOrElse(obj)
      }

    ((entity.id, entity.uniqueKey) match {
      case (None, None) =>
        val newId = BSONObjectID.generate().stringify
        collection
          .insert(ordered = false)
          .one(
            json + (Record.ID -> Json.obj("$oid" -> JsString(newId))) + (UPDATED -> JsNumber(
              System.currentTimeMillis()
            ))
          )
          .map((_, newId))
      case (Some(id), _) =>
        collection
          .update(ordered = false)
          .one(
            Json.obj(Record.ID -> Json.obj("$oid" -> JsString(id))),
            json + (UPDATED -> JsNumber(System.currentTimeMillis())),
            upsert = true
          )
          .map((_, id))
      case (None, Some(uniqueKey)) =>
        collection
          .find(Json.obj(UNIQUE_KEY -> JsString(keyOf(uniqueKey, planetId, typeName))), projection = None)
          .one[JsObject]
          .map(recordOpt =>
            recordOpt.flatMap(r => (r \ "_id" \ "$oid").asOpt[String]).getOrElse(BSONObjectID.generate().stringify)
          )
          .flatMap(recordId =>
            collection
              .update(ordered = false)
              .one(
                Json.obj(Record.ID -> Json.obj("$oid" -> JsString(recordId))),
                json
                  .+(Record.ID -> Json.obj("$oid" -> JsString(recordId)))
                  .+(UPDATED -> JsNumber(System.currentTimeMillis())),
                upsert = true
              )
              .map((_, recordId))
          )
    }).flatMap(MongoHelper.interpretWriteResult)

  }

  override def cursor[T <: Record](key: String, planetId: String)(implicit
    reads: Reads[T],
    ec: ExecutionContext,
    recordType: RecordMetaData[T]
  ): Cursor[T] =
    collection
      .find(
        JsObject(Seq(KEYS -> JsString(keyOf(key, planetId, recordType.typeName)))),
        Some(Json.obj(recordType.fieldNames.map(option => option -> toJsFieldJsValueWrapper(JsNumber(1))): _*))
      )
      .cursor[T](ReadPreference.primary)(
        implicitly[collection.pack.Reader[Record]].map(_.asInstanceOf[T]),
        implicitly[CursorProducer[T]]
      )

  override def cursor[T <: Record](keys: Seq[String], planetId: String)(implicit
    reads: Reads[T],
    ec: ExecutionContext,
    recordType: RecordMetaData[T]
  ): Cursor[T] =
    collection
      .find(
        JsObject(
          Seq(
            KEYS -> JsObject(
              Seq("$in" -> JsArray(keys.map(key => JsString(keyOf(key, planetId, recordType.typeName)))))
            )
          )
        ),
        Some(Json.obj(recordType.fieldNames.map(option => option -> toJsFieldJsValueWrapper(JsNumber(1))): _*))
      )
      .cursor[T](ReadPreference.primary)(
        implicitly[collection.pack.Reader[Record]].map(_.asInstanceOf[T]),
        implicitly[CursorProducer[T]]
      )

  override def findById[T <: Record](id: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[T]] =
    collection
      .find[JsObject, JsObject](
        Json.obj(Record.ID -> Json.obj("$oid" -> JsString(id)), PLANET_ID -> JsString(planetId)),
        None
      )
      .cursor[T](ReadPreference.primary)(
        implicitly[collection.pack.Reader[Record]].map(_.asInstanceOf[T]),
        implicitly[CursorProducer[T]]
      )
      .headOption

  override def findByPlanetId(planetId: String)(implicit ec: ExecutionContext): Cursor[Record] =
    collection
      .find[JsObject, JsObject](
        JsObject(Seq(PLANET_ID -> JsString(planetId))),
        None
      )
      .cursor[Record](ReadPreference.primary)(
        implicitly[collection.pack.Reader[Record]],
        implicitly[CursorProducer[Record]]
      )

  override def remove(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    this
      .remove(
        Record.ID -> toJsFieldJsValueWrapper(Json.obj("$oid" -> JsString(id))),
        PLANET_ID -> toJsFieldJsValueWrapper(JsString(planetId))
      )
      .map(_ => ())

  private def keyOf[T <: Record](key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove(PLANET_ID -> Option(planetId)).map(_ => ())
}
