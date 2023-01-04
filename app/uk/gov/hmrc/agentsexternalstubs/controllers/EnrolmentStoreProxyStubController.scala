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

package uk.gov.hmrc.agentsexternalstubs.controllers

import cats.data.Validated
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{AssignedClient, GroupDelegatedEnrolments}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest.Legacy
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.{DuplicateUserException, KnownFactsRepository}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, EnrolmentAlreadyExists, GroupsService, UsersService}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import uk.gov.hmrc.http.{BadRequestException, ForbiddenException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Instant, LocalDate, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EnrolmentStoreProxyStubController @Inject() (
  val authenticationService: AuthenticationService,
  knownFactsRepository: KnownFactsRepository,
  usersService: UsersService,
  groupsService: GroupsService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getUserIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      (for {
        principal <- if (`type` == "all" || `type` == "principal")
                       usersService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if (`type` == "all" || `type` == "delegated") {
                       enrolmentKey.service match {
                         case "IR-PAYE" | "IR-SA" =>
                           usersService.findUserIdsByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                         case _ => usersService.findUserIdsByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                       }
                     } else Future.successful(Seq.empty)
      } yield GetUserIdsResponse.from(principal, delegated)).map {
        case GetUserIdsResponse(None, None) => NoContent
        case response                       => Ok(RestfulResponse(response))
      }

    }(SessionRecordNotFound)
  }

  def getGroupIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      (for {
        principal <- if (`type` == "all" || `type` == "principal")
                       groupsService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if (`type` == "all" || `type` == "delegated")
                       groupsService.findByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                     else Future.successful(Seq.empty)
      } yield GetGroupIdsResponse.from(principal, delegated)).map {
        case GetGroupIdsResponse(None, None) => NoContent
        case response                        => Ok(RestfulResponse(response))
      }

    }(SessionRecordNotFound)
  }

  def setKnownFacts(enrolmentKey: EnrolmentKey): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[SetKnownFactsRequest] { payload =>
          knownFactsRepository
            .upsert(KnownFacts(enrolmentKey, payload.verifiers), session.planetId)
            .map(_ => NoContent)
        }
      }(SessionRecordNotFound)
  }

  def removeKnownFacts(enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      knownFactsRepository
        .delete(enrolmentKey, session.planetId)
        .map(_ => NoContent)
        .recover { case NonFatal(_) =>
          NoContent
        }
    }(SessionRecordNotFound)
  }

  def assignUser(userId: String, enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .assignEnrolmentToUser(userId, enrolmentKey, session.planetId)
        .map(_ => Created)
        .recover {
          case e: NotFoundException =>
            notFound(e.getMessage())
          case e: BadRequestException =>
            badRequest(e.getMessage())
          case e: ForbiddenException =>
            forbidden(e.getMessage())
        }
    }(SessionRecordNotFound)
  }

  def deassignUser(userId: String, enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .deassignEnrolmentFromUser(userId, enrolmentKey, session.planetId)
        .map(_ => NoContent)
        .recover {
          case e: NotFoundException =>
            notFound(e.getMessage())
          case e: BadRequestException =>
            badRequest(e.getMessage())
        }
    }(SessionRecordNotFound)
  }

  def allocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]
  ): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[AllocateGroupEnrolmentRequest] { payload =>
        AllocateGroupEnrolmentRequest
          .validate(payload)
          .fold(
            error => badRequestF("INVALID_JSON_BODY", error.mkString(", ")),
            _ =>
              (for {
                maybeUser <- usersService.findByUserId(payload.userId, session.planetId)
                user = maybeUser match {
                         case Some(usr)
                             if usr.credentialRole.exists(cr => Seq(User.CR.User, User.CR.Admin).contains(cr)) =>
                           usr
                         case _ => throw UnsupportedCredentialRole("INVALID_CREDENTIAL_ID")
                       }
                _ <- groupsService
                       .allocateEnrolmentToGroup(
                         user,
                         groupId,
                         enrolmentKey,
                         payload.`type`,
                         `legacy-agentCode`,
                         session.planetId
                       )
              } yield Created)
                .recover {
                  case _: EnrolmentAlreadyExists                          => Conflict
                  case _: DuplicateUserException                          => Conflict
                  case UnsupportedCredentialRole("INVALID_CREDENTIAL_ID") => Forbidden
                }
          )
      }
    }(SessionRecordNotFound)
  }

  def deallocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String],
    keepAgentAllocations: Option[String]
  ): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService
        .deallocateEnrolmentFromGroup(groupId, enrolmentKey, `legacy-agentCode`, keepAgentAllocations, session.planetId)
        .map(_ => NoContent)
    }(SessionRecordNotFound)
  }

  def doGetGroupEnrolments(
    planetId: String,
    groupId: String,
    `type`: String,
    service: Option[String],
    `start-record`: Option[Int],
    `max-records`: Option[Int],
    assignedToUser: Option[User] // if non-empty, only return the enrolments assigned to the given user.
  ): Future[Result] =
    if (`type` != "principal" && `type` != "delegated") badRequestF("INVALID_ENROLMENT_TYPE")
    else if (service.isDefined && !Services.servicesByKey.contains(service.get)) badRequestF("INVALID_SERVICE")
    else if (`start-record`.isDefined && `start-record`.get < 1) badRequestF("INVALID_START_RECORD")
    else if (`max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000))
      badRequestF("INVALID_MAX_RECORDS")
    else {
      groupsService.findByGroupId(groupId, planetId).flatMap {
        case None =>
          notFoundF("INVALID_GROUP_ID")
        case Some(group) =>
          val principal = `type` == "principal"
          val getKnownFacts: EnrolmentKey => Future[Option[KnownFacts]] =
            if (principal) knownFactsRepository.findByEnrolmentKey(_, planetId)
            else _ => Future.successful(None)
          val startRecord = `start-record`.getOrElse(1)
          def assignedEnrolments(user: User) = if (principal) user.assignedPrincipalEnrolments
          else user.assignedDelegatedEnrolments
          val enrolments = (if (principal) group.principalEnrolments else group.delegatedEnrolments)
            .filter(e => service.forall(_ == e.key))
            .filter(e => assignedToUser.forall(user => e.toEnrolmentKey.exists(assignedEnrolments(user).contains(_))))
            .slice(startRecord - 1, startRecord - 1 + `max-records`.getOrElse(1000))
          Future
            .sequence(enrolments.map(_.toEnrolmentKey).collect { case Some(x) => x }.map(getKnownFacts))
            .map(_.collect { case Some(x) => x })
            .map(knownFacts => GetUserEnrolmentsResponse.from(startRecord, enrolments, knownFacts))
            .map { response =>
              if (response.totalRecords == 0) NoContent else Ok(Json.toJson(response))
            }
      }
    }

  def getUserEnrolments(
    userId: String,
    `type`: String,
    service: Option[String],
    `start-record`: Option[Int],
    `max-records`: Option[Int]
  ): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      if (`type` != "principal" && `type` != "delegated") badRequestF("INVALID_ENROLMENT_TYPE")
      else if (service.isDefined && !Services.servicesByKey.contains(service.get)) badRequestF("INVALID_SERVICE")
      else if (`start-record`.isDefined && `start-record`.get < 1) badRequestF("INVALID_START_RECORD")
      else if (`max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000))
        badRequestF("INVALID_MAX_RECORDS")
      else {
        usersService.findByUserId(userId, session.planetId).flatMap {
          case None =>
            notFoundF("INVALID_CREDENTIAL_ID")
          case Some(user) if user.groupId.isEmpty =>
            notFoundF("INVALID_GROUP_ID")
          case Some(user) if user.groupId.nonEmpty =>
            doGetGroupEnrolments(
              session.planetId,
              user.groupId.get,
              `type`,
              service,
              `start-record`,
              `max-records`,
              assignedToUser = Some(user)
            )
        }
      }
    }(SessionRecordNotFound)
  }

  def getGroupEnrolments(
    groupId: String,
    `type`: String,
    service: Option[String],
    `start-record`: Option[Int],
    `max-records`: Option[Int],
    userId: Option[String],
    `unassigned-clients`: Option[Boolean]
  ): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      if (`type` != "principal" && `type` != "delegated") badRequestF("INVALID_ENROLMENT_TYPE")
      else if (service.isDefined && !Services.servicesByKey.contains(service.get)) badRequestF("INVALID_SERVICE")
      else if (`start-record`.isDefined && `start-record`.get < 1) badRequestF("INVALID_START_RECORD")
      else if (`max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000))
        badRequestF("INVALID_MAX_RECORDS")
      else {
        doGetGroupEnrolments(
          session.planetId,
          groupId,
          `type`,
          service,
          `start-record`,
          `max-records`,
          assignedToUser = None
        )
      }
    }(SessionRecordNotFound)
  }

  def getDelegatedEnrolments(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      for {
        maybeGroup <- groupsService.findByGroupId(groupId, session.planetId)
        users      <- usersService.findByGroupId(groupId, session.planetId)(100)
      } yield {
        val setOfDelegatedEnrolments: Set[Enrolment] =
          maybeGroup.fold(Set.empty[Enrolment])(_.delegatedEnrolments.toSet)
        val mapOfEnrolmentsToAssignedUsers: Map[Enrolment, Seq[String]] =
          users
            .flatMap(user =>
              user.assignedDelegatedEnrolments
                .map(enrolmentKey => (Enrolment.from(enrolmentKey), user.userId))
            )
            .groupBy(_._1)
            .map(groupedByEnrolment => groupedByEnrolment._1 -> groupedByEnrolment._2.map(_._2))
        val enrolmentsToAssignedUsersMergedWithDelegatedEnrolments: Map[Enrolment, Seq[String]] =
          setOfDelegatedEnrolments.foldLeft(mapOfEnrolmentsToAssignedUsers) { (accumulatedMap, delegatedEnrolment) =>
            if (
              accumulatedMap.keySet.exists(assignedEnrolment =>
                assignedEnrolment.key == delegatedEnrolment.key &&
                  assignedEnrolment.identifiers == delegatedEnrolment.identifiers
              )
            ) {
              accumulatedMap
            } else {
              accumulatedMap + (delegatedEnrolment -> Seq("0"))
            }
          }

        val assignedClients = enrolmentsToAssignedUsersMergedWithDelegatedEnrolments
          .map(enrolmentToUserIds =>
            AssignedClient(
              enrolmentToUserIds._1.toEnrolmentKey.get.toString,
              None,
              if (enrolmentToUserIds._2.size == 1) enrolmentToUserIds._2.head
              else enrolmentToUserIds._2.size.toString
            )
          )
          .toSeq
        Ok(Json.toJson(GroupDelegatedEnrolments(assignedClients)))
      }
    }(SessionRecordNotFound)
  }

  def setEnrolmentFriendlyName(groupId: String, enrolmentKey: EnrolmentKey): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withCurrentSession { session =>
        withPayload[SetFriendlyNameRequest] { payload =>
          SetFriendlyNameRequest
            .validate(payload)
            .fold(
              error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
              _ =>
                groupsService.findByGroupId(groupId, session.planetId).flatMap {
                  case None => notFoundF("INVALID_GROUP_ID")
                  case Some(group) =>
                    groupsService
                      .setEnrolmentFriendlyName(group, session.planetId, enrolmentKey, payload.friendlyName)
                      .map(_ => NoContent)
                }
            )
        }
      }(SessionRecordNotFound)
    }
}

object EnrolmentStoreProxyStubController {

  case class GetUserIdsResponse(principalUserIds: Option[Seq[String]], delegatedUserIds: Option[Seq[String]])

  object GetUserIdsResponse {
    implicit val writes: Writes[GetUserIdsResponse] = Json.writes[GetUserIdsResponse]

    def from(principal: Option[User], delegated: Seq[String]): GetUserIdsResponse =
      GetUserIdsResponse(principal.map(u => Seq(u.userId)), if (delegated.isEmpty) None else Some(delegated.distinct))
  }

  case class GetGroupIdsResponse(principalGroupIds: Option[Seq[String]], delegatedGroupIds: Option[Seq[String]])

  object GetGroupIdsResponse {
    implicit val writes: Writes[GetGroupIdsResponse] = Json.writes[GetGroupIdsResponse]

    def from(principal: Option[Group], delegated: Seq[Group]): GetGroupIdsResponse =
      GetGroupIdsResponse(
        principal.map(u => Seq(u.groupId)),
        if (delegated.isEmpty) None else Some(delegated.map(_.groupId).distinct)
      )
  }

  case class AllocateGroupEnrolmentRequest(userId: String, `type`: String)

  object AllocateGroupEnrolmentRequest {
    implicit val reads: Reads[AllocateGroupEnrolmentRequest] = Json.reads[AllocateGroupEnrolmentRequest]

    val validate: AllocateGroupEnrolmentRequest => Validated[List[String], Unit] =
      Validator[AllocateGroupEnrolmentRequest](
        Validator.check(_.`type`.matches("principal|delegated"), "Unsupported `type` param value")
      )
  }

  case class SetKnownFactsRequest(verifiers: Seq[KnownFact], legacy: Option[Legacy] = None)

  object SetKnownFactsRequest {

    case class Legacy(previousVerifiers: Seq[KnownFact])

    object Legacy {
      implicit val formats: Format[Legacy] = Json.format[Legacy]
    }

    implicit val formats: Format[SetKnownFactsRequest] = Json.format[SetKnownFactsRequest]

    def generate(enrolmentKey: String, alreadyKnownFacts: String => Option[String]): Option[SetKnownFactsRequest] =
      KnownFacts
        .generate(EnrolmentKey(enrolmentKey), enrolmentKey, alreadyKnownFacts)
        .map(kf => SetKnownFactsRequest(kf.verifiers, Some(Legacy(kf.verifiers))))
  }

  case class GetUserEnrolmentsResponse(
    startRecord: Int,
    totalRecords: Int,
    enrolments: Seq[GetUserEnrolmentsResponse.Enrolment]
  )

  object GetUserEnrolmentsResponse {

    case class Enrolment(
      service: String,
      state: String,
      friendlyName: String,
      enrolmentDate: Option[Instant],
      failedActivationCount: Int,
      activationDate: Option[Instant],
      enrolmentTokenExpiryDate: Option[Instant],
      identifiers: Seq[Identifier]
    )

    object Enrolment {

      def from(e: uk.gov.hmrc.agentsexternalstubs.models.Enrolment, kf: Option[KnownFacts]): Enrolment = Enrolment(
        service = e.key,
        state = e.state,
        friendlyName = e.friendlyName.getOrElse(""),
        failedActivationCount = 0,
        activationDate = Option(randomDateTimeInTheLastFiveYears),
        enrolmentDate = Option(randomDateTimeInTheLastFiveYears),
        enrolmentTokenExpiryDate = None,
        identifiers = e.identifiers
          .getOrElse(Seq.empty) ++ kf.map(_.verifiers.map(v => Identifier(v.key, v.value))).getOrElse(Seq.empty)
      )
    }

    def from(
      startRecord: Int,
      enrolments: Seq[uk.gov.hmrc.agentsexternalstubs.models.Enrolment],
      knownFacts: Seq[KnownFacts]
    ): GetUserEnrolmentsResponse = {
      val ee =
        enrolments
          .map(e => (e, knownFacts.find(kf => e.toEnrolmentKeyTag.contains(kf.enrolmentKey.tag))))
          .map { case (e, kf) => Enrolment.from(e, kf) }
      GetUserEnrolmentsResponse(
        startRecord = startRecord,
        totalRecords = ee.size,
        enrolments = ee
      )
    }

    private def randomDateTimeInTheLastFiveYears: Instant = {
      val start = LocalDate.now().minusYears(5)
      val end = LocalDate.now()
      Generator.date(start, end).sample.get.atStartOfDay(ZoneId.systemDefault).toInstant
    }

    implicit val writes1: Writes[Enrolment] = Json.writes[Enrolment]
    implicit val writes2: Writes[GetUserEnrolmentsResponse] = Json.writes[GetUserEnrolmentsResponse]
  }

  case class SetFriendlyNameRequest(friendlyName: String)

  object SetFriendlyNameRequest {

    implicit val format: Format[SetFriendlyNameRequest] = Json.format[SetFriendlyNameRequest]

    import Validator._

    private val friendlyNamePattern = "^[!%*^()_+\\-={}:;@~#,.?\\[\\]/A-Za-z0-9 ]{0,80}$"
    private val es19FriendlyNameValidator: Validator[String] =
      check(_.matches(friendlyNamePattern), s"""Invalid friendlyName, does not matches regex $friendlyNamePattern""")

    val validate: Validator[SetFriendlyNameRequest] = Validator(
      checkProperty(_.friendlyName, es19FriendlyNameValidator)
    )
  }
}
