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
import org.mongodb.scala.model.*
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{OWrites as _, *}
import uk.gov.hmrc.agentsexternalstubs.models.Record.TYPE
import uk.gov.hmrc.agentsexternalstubs.models.*
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository.*
import uk.gov.hmrc.agentsexternalstubs.syntax.*
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object RecordsRepository {
  final val PLANET_ID = "_planetId"
  final val UNIQUE_KEY = "_uniqueKey"
  final val KEYS = "_keys"
  final val UPDATED = "lastUpdatedAt"
}

@ImplementedBy(classOf[RecordsRepositoryMongo])
trait RecordsRepository {

  def store[T <: Record](entity: T, planetId: String)(using writes: Writes[T]): Future[String]

  def findByKey[T](key: String, planetId: String, limit: Option[Int])(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]]

  def findByKeys[T](keys: Seq[String], planetId: String, limit: Option[Int])(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]]

  /** Finds records matching any of the given keys, regardless of which planet they belong to, up to `limit`. Only safe
    * to rely on returning a single, unambiguous match when the identifier the keys are derived from is expected to be
    * globally unique (e.g. a safeId minted uniquely per test run) - callers should treat more than one result as a sign
    * that assumption doesn't hold. Returns the owning planetId alongside each record, since there is no session to
    * source it from. Deliberately returns raw matches rather than picking a "winner" or logging here -
    * `uk.gov.hmrc.agentsexternalstubs.repository` is configured ERROR-only in logback.xml, so a warning logged from
    * this layer is silently swallowed; callers (e.g. RecordsService) are responsible for deciding how to handle/log
    * ambiguity.
    */
  def findByKeysAnyPlanet[T](keys: Seq[String], limit: Int)(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[(String, T)]]

  def findById[T](id: String, planetId: String)(using reads: Reads[T]): Future[Option[T]]

  def findByPlanetId(planetId: String, limit: Option[Int] = None): Future[Seq[Record]]

  def remove(id: String, planetId: String): Future[Unit]

  def removeByKey[T](key: String, planetId: String)(using recordType: RecordMetaData[T]): Future[Unit]

  def destroyPlanet(planetId: String): Future[Unit]
}

@Singleton
class RecordsRepositoryMongo @Inject() (mongo: MongoComponent, appConfig: AppConfig)(using ExecutionContext)
    extends PlayMongoRepository[JsonAbuse[Record]](
      mongoComponent = mongo,
      collectionName = "records",
      domainFormat = JsonAbuse.format[Record](true),
      indexes = Seq(
        IndexModel(Indexes.ascending(KEYS), IndexOptions().name("Keys")),
        IndexModel(Indexes.ascending(UNIQUE_KEY), IndexOptions().name("UniqueKey").unique(true).sparse(true)),
        IndexModel(Indexes.ascending(PLANET_ID)),
        IndexModel(
          Indexes.ascending(UPDATED),
          IndexOptions().name("TtlIndex").expireAfter(appConfig.collectionsTtl.toLong, TimeUnit.DAYS)
        )
      ),
      extraCodecs = Seq(Codecs.playFormatCodec(Record.formats)),
      replaceIndexes = true
    ) with RecordsRepository {

  override def store[T <: Record](entity: T, planetId: String)(using writes: Writes[T]): Future[String] = {
    val typeName = Record.typeOf(entity)
    val entityWithExtraJson = JsonAbuse(entity: Record)
      .addField(PLANET_ID, JsString(planetId))
      .addField(TYPE, JsString(typeName))
      .addField(
        KEYS,
        JsArray(
          entity.uniqueKey
            .map(key => entity.lookupKeys :+ key)
            .getOrElse(entity.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, typeName)))
        )
      )
      .|> { obj =>
        entity.uniqueKey
          .map(uniqueKey => obj.addField(UNIQUE_KEY, JsString(keyOf(uniqueKey, planetId, typeName))))
          .getOrElse(obj)
      }

    (entity.id, entity.uniqueKey) match {
      case (None, None) =>
        val newId = ObjectId.get().toString
        collection
          .insertOne(
            entityWithExtraJson
              .addField(Record.ID, Json.toJson(Id(newId))(using Id.internalFormats))
              .addField(UPDATED, Json.toJson(Instant.now())(using MongoJavatimeFormats.instantFormat))
          )
          .toFuture()
          .map((_, newId))
          .flatMap(MongoHelper.interpretInsertOneResult)
      case (Some(id), _) =>
        collection
          .replaceOne(
            filter = Filters.equal(Record.ID, new ObjectId(id)),
            replacement = entityWithExtraJson
              .addField(UPDATED, Json.toJson(Instant.now())(using MongoJavatimeFormats.instantFormat)),
            ReplaceOptions().upsert(true)
          )
          .toFuture()
          .map((_, id))
          .flatMap(MongoHelper.interpretUpdateResult)
      case (None, Some(uniqueKey)) =>
        collection
          .find(Filters.equal(UNIQUE_KEY, keyOf(uniqueKey, planetId, typeName)))
          .headOption()
          .map(_.flatMap(_.value.id))
          .flatMap {
            case Some(recordId) =>
              collection
                .replaceOne(
                  Filters.equal(Record.ID, new ObjectId(recordId)),
                  entityWithExtraJson
                    .addField(Record.ID, Json.obj("$oid" -> JsString(recordId)))
                    .addField(UPDATED, Json.toJson(Instant.now())(using MongoJavatimeFormats.instantFormat)),
                  ReplaceOptions().upsert(true)
                )
                .toFuture()
                .map(_ => recordId)
            case None =>
              val newId = ObjectId.get().toString
              collection
                .insertOne(
                  entityWithExtraJson
                    .addField(Record.ID, Json.obj("$oid" -> JsString(newId)))
                    .addField(UPDATED, Json.toJson(Instant.now())(using MongoJavatimeFormats.instantFormat))
                )
                .toFuture()
                .map(_ => newId)
          }
    }
  }

  def rawStore(record: JsonAbuse[Record]): Future[String] = {
    val newId = ObjectId.get().toString
    collection
      .insertOne(
        record
          .addField(Record.ID, Json.obj("$oid" -> JsString(newId)))
          .addField(UPDATED, Json.toJson(Instant.now())(using MongoJavatimeFormats.instantFormat))
      )
      .toFuture()
      .map((_, newId))
      .flatMap(MongoHelper.interpretInsertOneResult)
  }

  override def findByKey[T](key: String, planetId: String, limit: Option[Int])(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]] =
    collection
      .find(Filters.equal(KEYS, keyOf(key, planetId, recordType.typeName)))
      .|>(o => if (limit.exists(_ >= 0)) o.limit(limit.get) else o)
      .toFuture()
      .map(_.map(_.value.asInstanceOf[T]))

  override def findByKeys[T](keys: Seq[String], planetId: String, limit: Option[Int])(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]] =
    collection
      .find(Filters.in(KEYS, keys.map(key => keyOf(key, planetId, recordType.typeName))*))
      .|>(o => if (limit.exists(_ >= 0)) o.limit(limit.get) else o)
      .toFuture()
      .map(_.map(_.value.asInstanceOf[T]))

  override def findByKeysAnyPlanet[T](keys: Seq[String], limit: Int)(using
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[(String, T)]] = {
    // Same normalisation as keyOf, minus the "@planetId" suffix - anchored-prefix regex against the
    // existing KEYS index, so this doesn't need a new index or a schema change.
    val prefixPattern = keys
      .map(key => "^" + java.util.regex.Pattern.quote(s"${recordType.typeName}:${key.replace(" ", "").toLowerCase}@"))
      .mkString("|")
    collection
      .find(Filters.regex(KEYS, prefixPattern))
      .limit(limit)
      .toFuture()
      .map(_.map(doc => (doc.extraFields(PLANET_ID).as[String], doc.value.asInstanceOf[T])))
  }

  override def findById[T](id: String, planetId: String)(using reads: Reads[T]): Future[Option[T]] =
    collection
      .find(
        Filters.and(
          Filters.equal(Record.ID, new ObjectId(id)),
          Filters.equal(PLANET_ID, planetId)
        )
      )
      .map(_.value.asInstanceOf[T]) // TODO asInstanceOf is bad
      .headOption()

  override def findByPlanetId(planetId: String, limit: Option[Int] = None): Future[Seq[Record]] =
    collection
      .find(Filters.equal(PLANET_ID, planetId))
      .|>(o => if (limit.exists(_ >= 0)) o.limit(limit.get) else o)
      .toFuture()
      .map(_.map(_.value))

  override def remove(id: String, planetId: String): Future[Unit] =
    collection
      .deleteOne(
        Filters.and(
          Filters.equal("_id", new ObjectId(id)),
          Filters.equal(PLANET_ID, planetId)
        )
      )
      .toFuture()
      .map(_ => ())

  def removeByKey[T](key: String, planetId: String)(using recordType: RecordMetaData[T]): Future[Unit] =
    collection
      .deleteOne(
        Filters.equal(KEYS, keyOf(key, planetId, recordType.typeName))
      )
      .toFuture()
      .map(_ => ())

  private def keyOf(key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

  def destroyPlanet(planetId: String): Future[Unit] =
    collection.deleteMany(Filters.equal(PLANET_ID, planetId)).toFuture().map(_ => ())
}
