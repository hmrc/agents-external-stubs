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
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json._
import play.api.{Logger, Logging}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, EnrolmentKey, Group, Identifier}
import uk.gov.hmrc.agentsexternalstubs.repository.GroupsRepositoryMongo._
import uk.gov.hmrc.agentsexternalstubs.syntax.|>
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class DuplicateGroupException(msg: String, key: Option[String] = None) extends IllegalStateException(msg)

@ImplementedBy(classOf[GroupsRepositoryMongo])
trait GroupsRepository {
  def findByGroupId(groupId: String, planetId: String): Future[Option[Group]]
  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int): Future[Seq[Group]]
  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[Group]]
  def findByAgentCode(agentCode: String, planetId: String): Future[Option[Group]]
  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int): Future[Seq[Group]]
  def updateFriendlyNameForEnrolment(
    group: Group,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    friendlyName: String
  ): Future[Option[Group]]
  def create(group: Group, planetId: String): Future[Unit]
  def update(group: Group, planetId: String): Future[Unit]
  def delete(groupId: String, planetId: String): Future[DeleteResult]
//  def syncRecordId(groupId: String, recordId: String, planetId: String): Future[Unit]
  def reindexAllGroups: Future[Boolean]
}

object GroupsRepositoryMongo {
  private final val UNIQUE_KEYS = "_uniqueKeys"
  private final val KEYS = "_keys"
  private final val GROUP_ID = "groupId"
  private final val PLANET_ID = "planetId"
}

@Singleton
class GroupsRepositoryMongo @Inject() (mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[JsonAbuse[Group]](
      mongoComponent = mongo,
      collectionName = "groups",
      domainFormat =
        JsonAbuse.format[Group](false)(Group.compressedFormat /* use space-saving Enrolment Json representation */ ),
      indexes = Seq(
        IndexModel(Indexes.ascending(KEYS), IndexOptions().name("Keys")),
        IndexModel(Indexes.ascending(UNIQUE_KEYS), IndexOptions().name("UniqueKeys").unique(true).sparse(true)),
        IndexModel(Indexes.ascending(GROUP_ID), IndexOptions().name("keyGroupId")),
        IndexModel(Indexes.ascending(PLANET_ID), IndexOptions().name("keyPlanetId"))
      ),
      replaceIndexes = true
    ) with GroupsRepository with Logging {

  final val UPDATED = "_last_updated_at"

  private def keyOf(key: String, planetId: String): String = s"${key.replace(" ", "")}@$planetId"

  private def groupIdIndexKey(groupId: String): String = s"gid:$groupId"
  private def principalEnrolmentIndexKey(key: String): String = s"penr:${key.toLowerCase}"
  private def delegatedEnrolmentIndexKey(key: String): String = s"denr:${key.toLowerCase}"

  override def findByPlanetId(planetId: String, affinityGroup: Option[String])(
    limit: Int
  ): Future[Seq[Group]] = {
    val filter = affinityGroup match {
      case Some(ag) => Filters.equal(KEYS, keyOf(Group.affinityGroupKey(ag), planetId))
      case None     => Filters.equal(KEYS, planetIdKey(planetId))
    }
    collection
      .find(filter)
      .|>(o => if (limit >= 0) o.limit(limit) else o)
      .toFuture
      .map(_.map(_.value))
  }

  override def findByGroupId(groupId: String, planetId: String): Future[Option[Group]] =
    collection
      .find(
        Filters.and(
          Filters.equal("planetId", planetId),
          Filters.equal("groupId", groupId)
        )
      )
      .toFuture
      .map(_.headOption.map(_.value))

  override def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[Group]] =
    collection
      .find(Filters.equal(UNIQUE_KEYS, keyOf(principalEnrolmentIndexKey(enrolmentKey.toString), planetId)))
      .toFuture
      .map(_.headOption.map(_.value))

  override def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  ): Future[Seq[Group]] =
    collection
      .find(Filters.equal(KEYS, keyOf(delegatedEnrolmentIndexKey(enrolmentKey.toString), planetId)))
      .|>(o => if (limit >= 0) o.limit(limit) else o)
      .toFuture
      .map(_.map(_.value))

  override def updateFriendlyNameForEnrolment(
    group: Group,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    friendlyName: String
  ): Future[Option[Group]] = {

    def update(enrolmentType: String, identifier: Identifier): Future[Option[Group]] = {
      val filter = enrolmentType match {
        case "principal" =>
          Filters.equal(UNIQUE_KEYS, keyOf(principalEnrolmentIndexKey(enrolmentKey.toString), planetId))
        case "delegated" =>
          Filters.and(
            Filters.equal(UNIQUE_KEYS, keyOf(groupIdIndexKey(group.groupId), planetId)),
            Filters.equal(KEYS, keyOf(delegatedEnrolmentIndexKey(enrolmentKey.toString), planetId))
          )
      }

      // Should use a lens but don't want to introduce a library dependency just for this
      def getEnrolments(group: Group): Seq[Enrolment] = enrolmentType match {
        case "principal" => group.principalEnrolments
        case "delegated" => group.delegatedEnrolments
      }
      def setEnrolments(group: Group, enrolments: Seq[Enrolment]): Group = enrolmentType match {
        case "principal" => group.copy(principalEnrolments = enrolments)
        case "delegated" => group.copy(delegatedEnrolments = enrolments)
      }

      // N.B. The double mongo query below was originally written as a single 'update' query using positional operator
      // e.g. Updates.set(s"${enrolmentType}Enrolments.$$.friendlyName", friendlyName)
      // which should be faster, but I just could NOT get positional operators to work with the new Mongo driver :(
      // Feel free to try again to make that work if better performance is required.
      collection.find(filter).map(_.value).headOption.flatMap {
        case None        => Future.successful(Option.empty[Group])
        case Some(group) =>
          // Find the relevant enrolment and set its friendly name
          val enrolments = getEnrolments(group)
          val updatedEnrolments = enrolments
            .map(e => if (e.toEnrolmentKey.contains(enrolmentKey)) e.copy(friendlyName = Some(friendlyName)) else e)
          val updatedGroup = setEnrolments(group, updatedEnrolments)
          collection
            .findOneAndReplace(
              filter = Filters.equal(UNIQUE_KEYS, keyOf(groupIdIndexKey(group.value.groupId), planetId)),
              replacement = serializeGroup(updatedGroup, planetId)
            )
            .toFutureOption
            .map(_.map(_.value))
      }
    }

    enrolmentKey.identifiers.headOption match {
      case None =>
        Future.successful(None)
      case Some(identifier) =>
        update("principal", identifier) flatMap {
          case None =>
            update("delegated", identifier)
          case Some(updatedGroup) =>
            Future.successful(Some(updatedGroup))
        }
    }
  }

  override def findByAgentCode(agentCode: String, planetId: String): Future[Option[Group]] =
    collection
      .find(
        Filters.and(
          Filters.equal("planetId", planetId),
          Filters.equal("agentCode", agentCode)
        )
      )
      .toFuture()
      .map(_.headOption.map(_.value))

  private def planetIdKey(planetId: String): String = s"planet:$planetId"

  private def serializeGroup(group: Group, planetId: String): JsonAbuse[Group] =
    JsonAbuse(group.copy(planetId = planetId))
      .addField(UNIQUE_KEYS, JsArray(group.uniqueKeys.map(key => JsString(keyOf(key, planetId)))))
      .addField(
        KEYS,
        JsArray((group.lookupKeys.map(key => keyOf(key, planetId)) :+ planetIdKey(planetId)).map(JsString))
      )

  override def create(group: Group, planetId: String): Future[Unit] =
    collection
      .insertOne(serializeGroup(group, planetId).addField(UPDATED, JsNumber(System.currentTimeMillis())))
      .toFuture
      .recoverWith {
        case e: MongoWriteException if e.getMessage.contains("11000") =>
          throwDuplicatedException(e, group, planetId)
      }
      .map(_ => ())

  override def update(group: Group, planetId: String): Future[Unit] =
    collection
      .findOneAndReplace(
        filter = Filters.equal(UNIQUE_KEYS, keyOf(groupIdIndexKey(group.groupId), planetId)),
        replacement = serializeGroup(group, planetId).addField(UPDATED, JsNumber(System.currentTimeMillis())),
        options = FindOneAndReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
      .recoverWith {
        case e: MongoWriteException if e.getMessage.contains("11000") => throwDuplicatedException(e, group, planetId)
      }

  override def delete(groupId: String, planetId: String): Future[DeleteResult] =
    collection
      .deleteOne(
        filter = Filters.equal(UNIQUE_KEYS, keyOf(groupIdIndexKey(groupId), planetId))
      )
      .toFuture

  private final val keyValueRegex = """\skey\:\s\{\s\w*\:\s\"(.*?)\"\s\}""".r

  private def extractKey(msg: String): Option[String] =
    if (msg.contains("11000"))
      keyValueRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
    else None

  private def throwDuplicatedException(e: MongoWriteException, group: Group, planetId: String): Future[Unit] =
    throw extractKey(e.getMessage) match {
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
        DuplicateGroupException(e.getMessage)
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

  def destroyPlanet(planetId: String): Future[Unit] =
    collection
      .deleteMany(filter = Filters.equal(KEYS, planetIdKey(planetId)))
      .toFuture()
      .map(_ => ())

  override def reindexAllGroups: Future[Boolean] = {
    val logger = Logger("uk.gov.hmrc.agentsexternalstubs.re-indexing")
    (for {
      _ <- collection.dropIndexes().toFuture()
      _ <- collection.createIndexes(this.indexes).toFuture()
    } yield {
      logger.info(s"Groups have been re-indexed")
      true
    }).recover { case NonFatal(e) =>
      logger
        .warn(s"Groups re-indexing failed: ${e.getMessage}")
      false
    }
  }

}
