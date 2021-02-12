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
import play.api.Logger
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, User}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class DuplicateUserException(msg: String, key: Option[String] = None) extends IllegalStateException(msg)

@ImplementedBy(classOf[UsersRepositoryMongo])
trait UsersRepository {

  def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[User]]
  def findByGroupId(groupId: String, planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[User]]
  def findAdminByGroupId(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByAgentCode(agentCode: String, planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[User]]
  def findAdminByAgentCode(agentCode: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[User]]
  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[User]]
  def findUserIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[String]]
  def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[Option[String]]]
  def create(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def update(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def delete(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult]
  def syncRecordId(userId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def reindexAllUsers(implicit ec: ExecutionContext): Future[String]
  def deleteAll(createdBefore: Long)(implicit ec: ExecutionContext): Future[Int]
}

@Singleton
class UsersRepositoryMongo @Inject() (mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[User, BSONObjectID](
      "users",
      mongoComponent.mongoConnector.db,
      User.formats,
      ReactiveMongoFormats.objectIdFormats
    ) with StrictlyEnsureIndexes[User, BSONObjectID] with UsersRepository with DeleteAll[User] {

  import ImplicitBSONHandlers._

  private final val UNIQUE_KEYS = "_uniqueKeys"
  private final val KEYS = "_keys"
  final val UPDATED = "_last_updated_at"

  private def keyOf(key: String, planetId: String): String = s"${key.replace(" ", "")}@$planetId"

  override def indexes = Seq(
    Index(Seq(KEYS -> Ascending), Some("Keys")),
    Index(Seq(UNIQUE_KEYS -> Ascending), Some("UniqueKeys"), unique = true, sparse = true)
  )

  override def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    one[User](Seq(UNIQUE_KEYS -> Option(keyOf(User.userIdKey(userId), planetId))))(User.formats)

  override def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    one[User](Seq(UNIQUE_KEYS -> Option(keyOf(User.ninoIndexKey(nino), planetId))))(User.formats)

  override def findByPlanetId(planetId: String, affinityGroup: Option[String])(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq(
        KEYS -> affinityGroup
          .map(ag => keyOf(User.affinityGroupKey(ag), planetId))
          .orElse(Some(planetIdKey(planetId)))
      )
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.ContOnError[Seq[User]]())

  override def findByGroupId(groupId: String, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq(KEYS -> Option(keyOf(User.groupIdIndexKey(groupId), planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  override def findAdminByGroupId(groupId: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[User]] =
    cursor(
      Seq(KEYS -> Option(keyOf(User.groupIdWithCredentialRoleKey(groupId, User.CR.Admin), planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = 1, err = Cursor.FailOnError[Seq[User]]())
      .map(_.headOption)

  override def findByAgentCode(agentCode: String, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq(KEYS -> Option(keyOf(User.agentCodeIndexKey(agentCode), planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  override def findAdminByAgentCode(agentCode: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[User]] =
    cursor(
      Seq(KEYS -> Option(keyOf(User.agentCodeWithCredentialRoleKey(agentCode, User.CR.Admin), planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = 1, err = Cursor.FailOnError[Seq[User]]())
      .map(_.headOption)

  override def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[User]] =
    one[User](Seq(UNIQUE_KEYS -> Option(keyOf(User.enrolmentIndexKey(enrolmentKey.toString), planetId))))(User.formats)

  override def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq(KEYS -> Option(keyOf(User.enrolmentIndexKey(enrolmentKey.toString), planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  private val userIdReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = JsSuccess((json \ "userId").as[String])
  }

  override def findUserIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[String]] =
    cursor(
      Seq(KEYS     -> Option(keyOf(User.enrolmentIndexKey(enrolmentKey.toString), planetId))),
      Seq("userId" -> 1)
    )(userIdReads).collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[String]]())

  private val groupIdReads = new Reads[Option[String]] {
    override def reads(json: JsValue): JsResult[Option[String]] = JsSuccess((json \ "groupId").asOpt[String])
  }

  override def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[Option[String]]] =
    cursor(
      Seq(KEYS      -> Option(keyOf(User.enrolmentIndexKey(enrolmentKey.toString), planetId))),
      Seq("groupId" -> 1)
    )(groupIdReads)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[Option[String]]]())

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

  private def serializeUser(user: User, planetId: String): JsObject = {
    val json = Json
      .toJson[User](user.copy(planetId = Some(planetId)))
      .as[JsObject]
      .+(KEYS -> JsArray(user.lookupKeys.map(key => JsString(keyOf(key, planetId))) :+ JsString(planetIdKey(planetId))))
      .+(UNIQUE_KEYS -> JsArray(user.uniqueKeys.map(key => JsString(keyOf(key, planetId)))))
    json
  }

  override def create(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .insert(serializeUser(user, planetId).+(UPDATED -> JsNumber(System.currentTimeMillis())))
      .map(_ => ())
      .recoverWith {
        case e: DatabaseException if e.code.contains(11000) =>
          throwDuplicatedException(e, user, planetId)
      }

  override def update(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .update(
        Json.obj(UNIQUE_KEYS                    -> keyOf(User.userIdKey(user.userId), planetId)),
        serializeUser(user, planetId).+(UPDATED -> JsNumber(System.currentTimeMillis())),
        upsert = true
      )
      .map(_ => ())
      .recoverWith {
        case e: DatabaseException if e.code.contains(11000) =>
          throwDuplicatedException(e, user, planetId)
      }

  override def delete(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove(UNIQUE_KEYS -> keyOf(User.userIdKey(userId), planetId))

  private final val keyValueRegex = """\skey\:\s\{\s\:\s\"(.*?)\"\s\}""".r

  private def extractKey(msg: String): Option[String] =
    if (msg.contains("11000"))
      keyValueRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
    else None

  private def throwDuplicatedException(e: DatabaseException, user: User, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    throw extractKey(e.getMessage()) match {
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
        DuplicateUserException(e.getMessage())
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

  def syncRecordId(userId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    if (recordId.nonEmpty) {
      if (recordId.startsWith("--")) {
        collection
          .update(
            Json.obj(UNIQUE_KEYS -> keyOf(User.userIdKey(userId), planetId)),
            Json.obj("$pull"     -> Json.obj("recordIds" -> recordId.substring(2)))
          )
          .flatMap(MongoHelper.interpretWriteResultUnit)
      } else {
        collection
          .update(
            Json.obj(UNIQUE_KEYS -> keyOf(User.userIdKey(userId), planetId)),
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

  override def reindexAllUsers(implicit ec: ExecutionContext): Future[String] = {
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
                    case Some(user) =>
                      collection
                        .update(
                          Json.obj("_id" -> Json.obj("$oid" -> JsString(id))),
                          serializeUser(user, user.planetId.get),
                          upsert = false
                        )
                        .map(_ => 1)
                        .recover { case NonFatal(e) =>
                          logger.warn(s"User ${user.userId}@${user.planetId.get} re-indexing failed: ${e.getMessage}")
                          0
                        }
                    case None => Future.successful(0)
                  }

                })
            )
          }
          .map { l =>
            val msg = s"${l.sum} out of ${ids.size} users has been re-indexed"
            logger.info(msg)
            msg
          }
      )
  }

}
