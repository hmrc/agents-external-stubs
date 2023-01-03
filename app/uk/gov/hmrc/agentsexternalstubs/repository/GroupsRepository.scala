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
import play.api.Logger
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Group, Identifier, User}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class DuplicateGroupException(msg: String, key: Option[String] = None) extends IllegalStateException(msg)

@ImplementedBy(classOf[GroupsRepositoryMongo])
trait GroupsRepository {
  def findByGroupId(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[Group]]
  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[Group]]
  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[Group]]
  def findByAgentCode(agentCode: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[Group]]
  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[Group]]
  def updateFriendlyNameForEnrolment(
    group: Group,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    friendlyName: String
  )(implicit
    ec: ExecutionContext
  ): Future[Option[Group]]
  def create(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def update(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def delete(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult]
  def syncRecordId(groupId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def reindexAllGroups(implicit ec: ExecutionContext): Future[String]
}

@Singleton
class GroupsRepositoryMongo @Inject() (mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[Group, BSONObjectID](
      "groups",
      mongoComponent.mongoConnector.db,
      Group.format,
      ReactiveMongoFormats.objectIdFormats
    ) with StrictlyEnsureIndexes[Group, BSONObjectID] with GroupsRepository with DeleteAll[Group] {

  import ImplicitBSONHandlers._

  private final val UNIQUE_KEYS = "_uniqueKeys"
  private final val KEYS = "_keys"
  final val UPDATED = "_last_updated_at"
  private final val GROUP_ID = "groupId"
  private final val KEY_GROUP_ID = "keyGroupId"
  private final val PLANET_ID = "planetId"
  private final val KEY_PLANET_ID = "keyPlanetId"

  private def keyOf(key: String, planetId: String): String = s"${key.replace(" ", "")}@$planetId"
  override def indexes: Seq[Index] =
    Seq(
      Index(Seq(KEYS -> Ascending), Some("Keys")),
      Index(Seq(UNIQUE_KEYS -> Ascending), Some("UniqueKeys"), unique = true, sparse = true),
      Index(Seq(GROUP_ID -> Ascending), Some(KEY_GROUP_ID)),
      Index(Seq(PLANET_ID -> Ascending), Some(KEY_PLANET_ID))
    )

  def groupIdIndexKey(groupId: String): String = s"gid:$groupId"
  def principalEnrolmentIndexKey(key: String): String = s"penr:${key.toLowerCase}"
  def delegatedEnrolmentIndexKey(key: String): String = s"denr:${key.toLowerCase}"

  override def findByPlanetId(planetId: String, affinityGroup: Option[String])(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[Group]] =
    cursor(
      Seq(
        KEYS -> affinityGroup
          .map(ag => keyOf(Group.affinityGroupKey(ag), planetId))
          .orElse(Some(planetIdKey(planetId)))
      )
    )(Group.format)
      .collect[Seq](maxDocs = limit, err = Cursor.ContOnError[Seq[Group]]())

  override def findByGroupId(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[Group]] =
    one[Group](Seq("planetId" -> Some(planetId), "groupId" -> Some(groupId)))(Group.format)

  override def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[Group]] =
    one[Group](
      Seq(UNIQUE_KEYS -> Some(keyOf(principalEnrolmentIndexKey(enrolmentKey.toString), planetId)))
    )(Group.format)

  override def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[Group]] =
    cursor(
      Seq(KEYS -> Option(keyOf(delegatedEnrolmentIndexKey(enrolmentKey.toString), planetId)))
    )(Group.format)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[Group]]())

  override def updateFriendlyNameForEnrolment(
    group: Group,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    friendlyName: String
  )(implicit
    ec: ExecutionContext
  ): Future[Option[Group]] = {

    def update(enrolmentType: String, identifier: Identifier): Future[Option[Group]] = {
      val selector = BSONDocument(
        PLANET_ID                                       -> planetId,
        GROUP_ID                                        -> group.groupId,
        s"${enrolmentType}Enrolments.identifiers.key"   -> identifier.key,
        s"${enrolmentType}Enrolments.identifiers.value" -> identifier.value
      )
      val modifier = BSONDocument(
        "$set" -> BSONDocument(s"${enrolmentType}Enrolments.$$.friendlyName" -> friendlyName)
      )

      collection
        .findAndUpdate(selector, modifier, fetchNewObject = true, upsert = false)
        .map(_.result[Group])
    }

    enrolmentKey.identifiers.headOption match {
      case None =>
        Future successful None
      case Some(identifier) =>
        update("principal", identifier) flatMap {
          case None =>
            update("delegated", identifier)
          case Some(updatedGroup) =>
            Future successful Option(updatedGroup)
        }
    }
  }

  override def findByAgentCode(agentCode: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[Group]] =
    one[Group](Seq("planetId" -> Some(planetId), "agentCode" -> Some(agentCode)))(Group.format)

  private val toJsWrapper: PartialFunction[(String, Option[String]), (String, Json.JsValueWrapper)] = {
    case (name, Some(value)) => name -> toJsFieldJsValueWrapper(value)
  }

  private def one[T](query: Seq[(String, Option[String])], projection: Seq[(String, Int)] = Seq.empty)(
    reader: collection.pack.Reader[T]
  )(implicit ec: ExecutionContext): Future[Option[T]] =
    collection
      .find(
        Json.obj(query.collect(toJsWrapper): _*),
        if (projection.isEmpty) None
        else Some(Json.obj(projection.map(option => option._1 -> toJsFieldJsValueWrapper(option._2)): _*))
      )
      .one[T](readPreference = ReadPreference.primary)(reader, ec)

  private def cursor[T](query: Seq[(String, Option[String])], projection: Seq[(String, Int)] = Seq.empty)(
    reader: collection.pack.Reader[T]
  ): Cursor[T] =
    collection
      .find(
        Json.obj(query.collect(toJsWrapper): _*),
        if (projection.isEmpty) None
        else Some(Json.obj(projection.map(option => option._1 -> toJsFieldJsValueWrapper(option._2)): _*))
      )
      .cursor[T](readPreference = ReadPreference.primary)(reader, implicitly[CursorProducer[T]])

  private def planetIdKey(planetId: String): String = s"planet:$planetId"

  private def serializeGroup(group: Group, planetId: String): JsObject =
    Json
      .toJson[Group](group.copy(planetId = planetId))
      .as[JsObject]
      .+(
        KEYS -> JsArray(group.lookupKeys.map(key => JsString(keyOf(key, planetId))) :+ JsString(planetIdKey(planetId)))
      )
      .+(UNIQUE_KEYS -> JsArray(group.uniqueKeys.map(key => JsString(keyOf(key, planetId)))))

  override def create(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .insert(ordered = false)
      .one(serializeGroup(group, planetId).+(UPDATED -> JsNumber(System.currentTimeMillis())))
      .map(_ => ())
      .recoverWith {
        case e: DatabaseException if e.code.contains(11000) =>
          throwDuplicatedException(e, group, planetId)
      }

  override def update(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .update(ordered = false)
      .one(
        Json.obj(UNIQUE_KEYS                      -> keyOf(groupIdIndexKey(group.groupId), planetId)),
        serializeGroup(group, planetId).+(UPDATED -> JsNumber(System.currentTimeMillis())),
        upsert = true
      )
      .map(wr => logger.info(s"Group update was OK?: ${wr.ok}"))
      .recoverWith {
        case e: DatabaseException if e.code.contains(11000) => throwDuplicatedException(e, group, planetId)
      }

  override def delete(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove(UNIQUE_KEYS -> keyOf(groupIdIndexKey(groupId), planetId))

  private final val keyValueRegex = """\skey\:\s\{\s\w*\:\s\"(.*?)\"\s\}""".r

  private def extractKey(msg: String): Option[String] =
    if (msg.contains("11000"))
      keyValueRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
    else None

  private def throwDuplicatedException(e: DatabaseException, group: Group, planetId: String): Future[Unit] =
    throw extractKey(e.getMessage()) match {
      case Some(key) =>
        val prefix = key.takeWhile(_ != ':')
        val value = key.dropWhile(_ != ':').drop(1).takeWhile(_ != '@')
        DuplicateGroupException(
          duplicatedGroupMessageByKeyPrefix
            .get(prefix)
            .map(_(value, planetId))
            .getOrElse(s"Duplicated key $key already exists on $planetId"),
          Some(key)
        )
      case None =>
        DuplicateGroupException(e.getMessage())
    }

  private val duplicatedGroupMessageByKeyPrefix: Map[String, (String, String) => String] = Map(
    "gid" -> ((k: String, p: String) => s"Duplicated group $k on $p"),
    "ac" -> ((k: String, p: String) =>
      s"Existing group already has this agentCode $k. Two groups cannot share the same agentCode on the same $p planet."
    ),
    "penr" -> ((k: String, p: String) =>
      s"Existing group already has similar principal enrolment ${k.toUpperCase()}. Two groups cannot have the same principal enrolment on the same $p planet."
    )
  )

  def syncRecordId(groupId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    if (recordId.nonEmpty) {
      if (recordId.startsWith("--")) {
        collection
          .update(ordered = false)
          .one(
            Json.obj(UNIQUE_KEYS -> keyOf(groupIdIndexKey(groupId), planetId)),
            Json.obj("$pull"     -> Json.obj("recordIds" -> recordId.substring(2)))
          )
          .flatMap(MongoHelper.interpretWriteResultUnit)
      } else {
        collection
          .update(ordered = false)
          .one(
            Json.obj(UNIQUE_KEYS -> keyOf(groupIdIndexKey(groupId), planetId)),
            Json.obj("$push"     -> Json.obj("recordIds" -> recordId))
          )
          .flatMap(MongoHelper.interpretWriteResultUnit)
      }
    } else Future.failed(new Exception("Empty recordId"))

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove(KEYS -> Option(planetIdKey(planetId))).map(_ => ())

  private val idReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = JsSuccess((json \ "_id" \ "$oid").as[String])
  }

  override def reindexAllGroups(implicit ec: ExecutionContext): Future[String] = {
    val logger = Logger("uk.gov.hmrc.agentsexternalstubs.re-indexing")
    cursor(Seq(), Seq("_id" -> 1))(idReads)
      .collect[Seq](maxDocs = -1, err = Cursor.FailOnError())
      .flatMap(ids =>
        collection.indexesManager
          .dropAll()
          .flatMap { ic =>
            logger.info(s"Existing $ic indexes has been dropped.")
            ensureIndexes.flatMap(_ =>
              Future
                .sequence(ids.map { id =>
                  findById(BSONObjectID.parse(id).get).flatMap {
                    case Some(group) =>
                      collection
                        .update(ordered = false)
                        .one(
                          Json.obj("_id" -> Json.obj("$oid" -> JsString(id))),
                          serializeGroup(group, group.planetId),
                          upsert = false
                        )
                        .map(_ => 1)
                        .recover { case NonFatal(e) =>
                          logger
                            .warn(s"Group ${group.groupId}@${group.planetId} re-indexing failed: ${e.getMessage}")
                          0
                        }
                    case None => Future.successful(0)
                  }

                })
            )
          }
          .map { l =>
            val msg = s"${l.sum} out of ${ids.size} groups has been re-indexed"
            logger.info(msg)
            msg
          }
      )
  }

}
