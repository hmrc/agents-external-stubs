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
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, User, UserBrief}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

case class DuplicateUserException(msg: String) extends IllegalStateException(msg)

@ImplementedBy(classOf[UsersRepositoryMongo])
trait UsersRepository {

  def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]]
  def findByGroupId(groupId: String, planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[User]]
  def findAdminByGroupId(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByAgentCode(agentCode: String, planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[User]]
  def findAdminByAgentCode(agentCode: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[User]]
  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]]
  def findUserIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[String]]
  def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[Option[String]]]
  def create(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def update(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit]
  def delete(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult]
  def addRecordId(userId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit]

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit]
}

@Singleton
class UsersRepositoryMongo @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[User, BSONObjectID](
      "users",
      mongoComponent.mongoConnector.db,
      User.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[User, BSONObjectID] with UsersRepository {

  import ImplicitBSONHandlers._

  private final val PLANET_ID = "planetId"

  private final val UsersIndexName = "Users"
  private final val NinosIndexName = "Ninos"
  private final val PrincipalEnrolmentKeysIndexName = "PrincipalEnrolmentKeys"
  private final val AgentCodesIndexName = "UniqueAgentCodes"
  private final val GroupIdsIndexName = "UniqueGroupIds"

  override def indexes = Seq(
    // Unique indexes
    Index(Seq(User.user_index_key       -> Ascending), Some(UsersIndexName), unique = true),
    Index(Seq(User.nino_index_key       -> Ascending), Some(NinosIndexName), unique = true, sparse = true),
    Index(Seq(User.agent_code_index_key -> Ascending), Some(AgentCodesIndexName), unique = true, sparse = true),
    Index(Seq(User.group_id_index_key   -> Ascending), Some(GroupIdsIndexName), unique = true, sparse = true),
    Index(
      Seq(User.principal_enrolment_keys -> Ascending),
      Some(PrincipalEnrolmentKeysIndexName),
      unique = true,
      sparse = true),
    // Lookup indexes
    Index(Seq(User.delegated_enrolment_keys -> Ascending), Some("DelegatedEnrolmentKeys"), sparse = true),
    Index(Seq(PLANET_ID                     -> Ascending), Some("Planets")),
    Index(Seq(PLANET_ID                     -> Ascending, "affinityGroup" -> Ascending), Some("PlanetsWithAffinityGroup")),
    Index(Seq("groupId"                     -> Ascending, PLANET_ID -> Ascending), Some("Groups"), sparse = true),
    Index(
      Seq("groupId" -> Ascending, PLANET_ID -> Ascending, "credentialRole" -> Ascending),
      Some("GroupsWithCredentialRole"),
      sparse = true),
    Index(
      Seq("agentCode" -> Ascending, PLANET_ID -> Ascending, "credentialRole" -> Ascending),
      Some("AgentCodesWithCredentialRole"),
      sparse = true),
    Index(Seq("agentCode" -> Ascending, PLANET_ID -> Ascending), Some("AgentCodes"), sparse = true),
    // TTL indexes
    Index(
      Seq(User.ttl_index_key -> Ascending),
      Some("TTL"),
      sparse = true,
      options = BSONDocument("expireAfterSeconds" -> 43200)
    )
  )

  override def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    find(Seq(User.user_index_key -> Option(User.userIndexKey(userId, planetId))).map(option =>
      option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case _ :: _   => throw DuplicateUserException(s"Duplicated userId $userId for $planetId")
    }

  override def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    find(Seq(User.nino_index_key -> Option(User.ninoIndexKey(nino, planetId))).map(option =>
      option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case _ :: _   => throw DuplicateUserException(s"Duplicated nino $nino for $planetId")
    }

  override def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(Seq(PLANET_ID -> Option(planetId), "affinityGroup" -> affinityGroup))(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.ContOnError[Seq[User]]())

  override def findByGroupId(groupId: String, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq("groupId" -> Option(groupId), PLANET_ID -> Option(planetId))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  override def findAdminByGroupId(groupId: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[User]] =
    cursor(
      Seq("groupId" -> Option(groupId), "credentialRole" -> Option(User.CR.Admin), PLANET_ID -> Option(planetId))
    )(User.formats)
      .collect[Seq](maxDocs = 1, err = Cursor.FailOnError[Seq[User]]())
      .map(_.headOption)

  override def findByAgentCode(agentCode: String, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq("agentCode" -> Option(agentCode), PLANET_ID -> Option(planetId))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  override def findAdminByAgentCode(agentCode: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[User]] =
    cursor(
      Seq("agentCode" -> Option(agentCode), "credentialRole" -> Option(User.CR.Admin), PLANET_ID -> Option(planetId))
    )(User.formats)
      .collect[Seq](maxDocs = 1, err = Cursor.FailOnError[Seq[User]]())
      .map(_.headOption)

  override def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[User]] =
    find(
      Seq(User.principal_enrolment_keys -> Option(User.enrolmentIndexKey(enrolmentKey.toString, planetId)))
        .map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case _ :: _   => throw DuplicateUserException(s"Duplicated enrolment key $enrolmentKey for $planetId")
    }

  override def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    cursor(
      Seq(User.delegated_enrolment_keys -> Option(User.enrolmentIndexKey(enrolmentKey.toString, planetId)))
    )(User.formats)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[User]]())

  private val userIdReads = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = JsSuccess((json \ "userId").as[String])
  }

  override def findUserIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[String]] =
    cursor(
      Seq(User.delegated_enrolment_keys -> Option(User.enrolmentIndexKey(enrolmentKey.toString, planetId))),
      Seq("userId"                      -> 1)
    )(userIdReads).collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[String]]())

  private val groupIdReads = new Reads[Option[String]] {
    override def reads(json: JsValue): JsResult[Option[String]] = JsSuccess((json \ "groupId").asOpt[String])
  }

  override def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[Option[String]]] =
    cursor(
      Seq(User.delegated_enrolment_keys -> Option(User.enrolmentIndexKey(enrolmentKey.toString, planetId))),
      Seq("groupId"                     -> 1)
    )(groupIdReads)
      .collect[Seq](maxDocs = limit, err = Cursor.FailOnError[Seq[Option[String]]]())

  private val toJsWrapper: PartialFunction[(String, Option[String]), (String, Json.JsValueWrapper)] = {
    case (name, Some(value)) => name -> toJsFieldJsValueWrapper(value)
  }

  private def cursor[T](query: Seq[(String, Option[String])], projection: Seq[(String, Int)] = Seq.empty)(
    reader: collection.pack.Reader[T])(implicit ec: ExecutionContext): Cursor[T] =
    collection
      .find(
        Json.obj(query.collect(toJsWrapper): _*),
        Json.obj(projection.map(option => option._1 -> toJsFieldJsValueWrapper(option._2)): _*)
      )
      .cursor[T](ReadPreference.primaryPreferred)(reader, ec, implicitly[CursorProducer[T]])

  override def create(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    insert(user.copy(planetId = Some(planetId), isPermanent = explicitFlag(user.isPermanent))).map(_ => ()).recover {
      case e: DatabaseException if e.code.contains(11000) =>
        throw DuplicateUserException(transformMessage(e.getMessage(), user, planetId))
    }

  override def update(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    (User.formats.writes(user.copy(planetId = Some(planetId), isPermanent = explicitFlag(user.isPermanent))) match {
      case u @ JsObject(_) =>
        collection.update(Json.obj(User.user_index_key -> User.userIndexKey(user.userId, planetId)), u, upsert = true)
      case _ =>
        Future.failed[WriteResult](new Exception("Cannot update User"))
    }).map(_ => ()).recover {
      case e: DatabaseException if e.code.contains(11000) =>
        throw DuplicateUserException(transformMessage(e.getMessage(), user, planetId))
    }

  override def delete(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove(User.user_index_key -> Option(User.userIndexKey(userId, planetId)))

  private val indexNameRegex = """\sindex\:\s(\w*?)\s""".r

  private def transformMessage(msg: String, user: User, planetId: String): String =
    if (msg.contains("11000")) {
      indexNameRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
        .flatMap(i => duplicatedUserMessageByIndex.get(i).map(_(user)(planetId)))
        .getOrElse(s"Duplicated user setup on $planetId")
    } else msg

  private val duplicatedUserMessageByIndex: Map[String, User => String => String] = Map(
    UsersIndexName -> (u => p => s"Duplicated userId ${u.userId} on $p"),
    NinosIndexName -> (u => p => s"Duplicated NINO ${u.nino.get} on $p"),
    GroupIdsIndexName -> (u =>
      p => s"Duplicated groupId ${u.groupId.get} on $p. Two Admin users cannot share the same groupId."),
    AgentCodesIndexName -> (u =>
      p => s"Duplicated agentCode ${u.agentCode.get} on $p. Two Admin agents cannot share the same agentCode."),
    PrincipalEnrolmentKeysIndexName -> (u =>
      p =>
        s"Duplicated principal${if (u.principalEnrolments.size > 1) " enrolment, one of" else ""} ${u.principalEnrolments
          .map(_.description)
          .mkString(", ")} on $p")
  )

  private def explicitFlag(flag: Option[Boolean]): Option[Boolean] = flag match {
    case Some(true) => Some(true)
    case _          => Some(false)
  }

  def addRecordId(userId: String, recordId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .update(
        Json.obj(User.user_index_key -> User.userIndexKey(userId, planetId)),
        Json.obj("$push"             -> Json.obj("recordIds" -> recordId)))
      .flatMap(MongoHelper.interpretWriteResultUnit)

  def destroyPlanet(planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove(PLANET_ID -> Option(planetId)).map(_ => ())

}
