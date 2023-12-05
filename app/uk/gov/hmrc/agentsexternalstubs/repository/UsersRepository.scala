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
import com.mongodb.client.model.Updates
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Projections.{excludeId, fields, include}
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json._
import play.api.{Logger, Logging}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, User}
import uk.gov.hmrc.agentsexternalstubs.repository.UsersRepositoryMongo._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class DuplicateUserException(msg: String, key: Option[String] = None) extends IllegalStateException(msg)

@ImplementedBy(classOf[UsersRepositoryMongo])
trait UsersRepository {

  def findByUserId(userId: String, planetId: String): Future[Option[User]]
  def findByNino(nino: String, planetId: String): Future[Option[User]]
  def findByPlanetId(planetId: String)(limit: Int): Future[Seq[User]]
  def findByGroupId(groupId: String, planetId: String)(limit: Option[Int]): Future[Seq[User]]
  def findAdminByGroupId(groupId: String, planetId: String): Future[Option[User]]
  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[User]]
  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int): Future[Seq[User]]
  def findUserIdsByAssignedDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Option[Int]
  ): Future[Seq[String]]
  def findUserIdsByAssignedPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Option[Int]
  ): Future[Seq[String]]
  def assignEnrolment(
    userId: String,
    groupId: String,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    isPrincipal: Boolean
  ): Future[Unit]
  def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  ): Future[Seq[Option[String]]]
  def create(user: User, planetId: String): Future[Unit]
  def update(user: User, planetId: String): Future[Unit]
  def delete(userId: String, planetId: String): Future[DeleteResult]
  def syncRecordId(userId: String, recordId: String, planetId: String): Future[Unit]

  def destroyPlanet(planetId: String): Future[Unit]
  def reindexAllUsers: Future[Boolean]
}

object UsersRepositoryMongo {
  private final val UNIQUE_KEYS = "_uniqueKeys"
  private final val KEYS = "_keys"
  private final val USER_ID = "userId"
  private final val KEY_USER_ID = "keyUserId"
  private final val PLANET_ID = "planetId"
  private final val KEY_PLANET_ID = "keyPlanetId"
}

@Singleton
class UsersRepositoryMongo @Inject() (mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[JsonAbuse[User]](
      mongoComponent = mongo,
      collectionName = "users",
      domainFormat = JsonAbuse.format[User](false),
      indexes = Seq(
        IndexModel(Indexes.ascending(KEYS), IndexOptions().name("Keys")),
        IndexModel(Indexes.ascending(UNIQUE_KEYS), IndexOptions().name("UniqueKeys").unique(true).sparse(true)),
        IndexModel(Indexes.ascending(USER_ID), IndexOptions().name(KEY_USER_ID)),
        IndexModel(Indexes.ascending(PLANET_ID), IndexOptions().name(KEY_PLANET_ID))
      ),
      replaceIndexes = true
    ) with UsersRepository with Logging {

  final val UPDATED = "_last_updated_at"

  private def keyOf(key: String, planetId: String): String = s"${key.replace(" ", "")}@$planetId"

  override def findByUserId(userId: String, planetId: String): Future[Option[User]] =
    collection
      .find(Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(userId), planetId)))
      .headOption()
      .map(_.map(_.value))

  override def findByNino(nino: String, planetId: String): Future[Option[User]] =
    collection
      .find(Filters.equal(UNIQUE_KEYS, keyOf(User.ninoIndexKey(nino), planetId)))
      .headOption()
      .map(_.map(_.value))

  override def findByPlanetId(planetId: String)(
    limit: Int
  ): Future[Seq[User]] =
    collection.find(Filters.equal(KEYS, planetIdKey(planetId))).limit(limit).toFuture().map(_.map(_.value))

  override def findByGroupId(groupId: String, planetId: String)(
    limit: Option[Int]
  ): Future[Seq[User]] =
    collection
      .find(Filters.equal(KEYS, keyOf(User.groupIdIndexKey(groupId), planetId)))
      .limit(limit.getOrElse(0))
      .toFuture()
      .map(_.map(_.value))

  override def findAdminByGroupId(groupId: String, planetId: String): Future[Option[User]] =
    collection
      .find(Filters.equal(KEYS, keyOf(User.groupIdWithCredentialRoleKey(groupId, User.CR.Admin), planetId)))
      .headOption()
      .map(_.map(_.value))

  override def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[User]] =
    collection
      .find(Filters.equal(KEYS, keyOf(User.assignedPrincipalEnrolmentIndexKey(enrolmentKey.toString), planetId)))
      .headOption()
      .map(_.map(_.value))

  override def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  ): Future[Seq[User]] =
    collection
      .find(Filters.equal(KEYS, keyOf(User.assignedDelegatedEnrolmentIndexKey(enrolmentKey.toString), planetId)))
      .limit(limit)
      .toFuture()
      .map(_.map(_.value))

  override def findUserIdsByAssignedDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Option[Int]
  ): Future[Seq[String]] =
    mongo.database
      .getCollection("users")
      .find(
        equal(KEYS, keyOf(User.assignedDelegatedEnrolmentIndexKey(enrolmentKey.toString), planetId))
      )
      .limit(limit.getOrElse(0))
      .projection(fields(include("userId"), excludeId()))
      .toFuture()
      .map(_.map(_.getString("userId")))

  override def findUserIdsByAssignedPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Option[Int]
  ): Future[Seq[String]] =
    mongo.database
      .getCollection("users")
      .find(
        equal(KEYS, keyOf(User.assignedPrincipalEnrolmentIndexKey(enrolmentKey.toString), planetId))
      )
      .limit(limit.getOrElse(0))
      .projection(fields(include("userId"), excludeId()))
      .toFuture()
      .map(_.map(_.getString("userId")))

  override def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  ): Future[Seq[Option[String]]] =
    collection
      .find(Filters.equal(KEYS, keyOf(User.assignedDelegatedEnrolmentIndexKey(enrolmentKey.toString), planetId)))
      .limit(limit)
      .toFuture()
      .map(_.map(_.value.groupId))

  override def assignEnrolment(
    userId: String,
    groupId: String,
    planetId: String,
    enrolmentKey: EnrolmentKey,
    isPrincipal: Boolean
  ): Future[Unit] = {
    val (fieldName, keyToAdd) =
      if (isPrincipal)
        ("assignedPrincipalEnrolments", keyOf(User.assignedPrincipalEnrolmentIndexKey(enrolmentKey.tag), planetId))
      else ("assignedDelegatedEnrolments", keyOf(User.assignedDelegatedEnrolmentIndexKey(enrolmentKey.tag), planetId))

    collection
      .updateOne(
        Filters.and(
          Filters.equal(KEYS, keyOf(User.groupIdIndexKey(groupId), planetId)),
          Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(userId), planetId))
        ),
        Updates.combine(Updates.addToSet(fieldName, enrolmentKey.tag), Updates.addToSet(KEYS, keyToAdd))
      )
      .toFuture()
      .map(_ => ())
  }

  private def planetIdKey(planetId: String): String = s"planet:$planetId"

  private def serializeUser(user: User, planetId: String): JsonAbuse[User] =
    JsonAbuse(user.copy(planetId = Some(planetId)))
      .addField(
        KEYS,
        JsArray(user.lookupKeys.map(key => JsString(keyOf(key, planetId))) :+ JsString(planetIdKey(planetId)))
      )
      .addField(UNIQUE_KEYS, JsArray(user.uniqueKeys.map(key => JsString(keyOf(key, planetId)))))

  override def create(user: User, planetId: String): Future[Unit] =
    collection
      .insertOne(serializeUser(user, planetId).addField(UPDATED, JsNumber(System.currentTimeMillis())))
      .toFuture()
      .map(_ => ())
      .recoverWith {
        case e: MongoWriteException if e.getMessage.contains("11000") =>
          throwDuplicatedException(e, user, planetId)
      }

  override def update(user: User, planetId: String): Future[Unit] =
    collection
      .replaceOne(
        filter = Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(user.userId), planetId)),
        replacement = serializeUser(user, planetId).addField(UPDATED, JsNumber(System.currentTimeMillis())),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(wr => logger.info(s"User update was OK?: ${wr.getModifiedCount}"))
      .recoverWith {
        case e: MongoWriteException if e.getMessage.contains("11000") =>
          throwDuplicatedException(e, user, planetId)
      }

  override def delete(userId: String, planetId: String): Future[DeleteResult] =
    collection.deleteOne(Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(userId), planetId))).toFuture()

  private final val keyValueRegex = """\skey\:\s\{\s\w*\:\s\"(.*?)\"\s\}""".r

  private def extractKey(msg: String): Option[String] =
    if (msg.contains("11000"))
      keyValueRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
    else None

  private def throwDuplicatedException(e: MongoWriteException, user: User, planetId: String): Future[Unit] =
    throw extractKey(e.getMessage) match {
      case Some(key) =>
        val prefix = key.takeWhile(_ != ':')
        val value = key.dropWhile(_ != ':').drop(1).takeWhile(_ != '@')
        DuplicateUserException(
          duplicatedUserMessageByKeyPrefix
            .get(prefix)
            .map(_(value, planetId))
            .getOrElse(s"Duplicated key $key already exists on $planetId"),
          Some(key)
        )
      case None =>
        DuplicateUserException(e.getMessage)
    }

  private val duplicatedUserMessageByKeyPrefix: Map[String, (String, String) => String] = Map(
    "uid" -> ((k: String, p: String) => s"Duplicated user $k on $p"),
    "nino" -> ((k: String, p: String) =>
      s"Existing user already has this NINO ${k.toUpperCase}. Two individuals cannot have the same NINO on the same $p planet."
    ),
    "gid" -> ((k: String, p: String) =>
      s"Existing admin user already has this groupId $k. Two Admin users cannot share the same groupId on the same $p planet."
    ),
    "ac" -> ((k: String, p: String) =>
      s"Existing agent user already has this agentCode $k. Two Admin agents cannot share the same agentCode on the same $p planet."
    ),
    "enr" -> ((k: String, p: String) =>
      s"Existing user already has similar enrolment ${k.toUpperCase()}. Two users cannot have the same principal identifier on the same $p planet."
    )
  )

  def syncRecordId(userId: String, recordId: String, planetId: String): Future[Unit] =
    if (recordId.nonEmpty) {
      if (recordId.startsWith("--")) {
        collection
          .updateOne(
            filter = Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(userId), planetId)),
            update = Updates.pull("recordIds", recordId.substring(2))
          )
          .toFuture()
          .flatMap(MongoHelper.interpretUpdateResultUnit)
      } else {
        collection
          .updateOne(
            filter = Filters.equal(UNIQUE_KEYS, keyOf(User.userIdKey(userId), planetId)),
            update = Updates.push("recordIds", recordId)
          )
          .toFuture()
          .flatMap(MongoHelper.interpretUpdateResultUnit)
      }
    } else Future.failed(new Exception("Empty recordId"))

  def destroyPlanet(planetId: String): Future[Unit] =
    collection.deleteMany(Filters.equal(KEYS, planetIdKey(planetId))).toFuture().map(_ => ())

  override def reindexAllUsers: Future[Boolean] = {
    val logger = Logger("uk.gov.hmrc.agentsexternalstubs.re-indexing")
    (for {
      _ <- collection.dropIndexes().toFuture()
      _ <- collection.createIndexes(this.indexes).toFuture()
    } yield {
      logger.info(s"Users have been re-indexed")
      true
    }).recover { case NonFatal(e) =>
      logger
        .warn(s"Users re-indexing failed: ${e.getMessage}")
      false
    }
  }
}
