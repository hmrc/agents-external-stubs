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
import play.api.Logger
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest.Legacy
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.*
import uk.gov.hmrc.agentsexternalstubs.models.Validator.{Validator, check, checkProperty}
import uk.gov.hmrc.agentsexternalstubs.models.*
import uk.gov.hmrc.agentsexternalstubs.models.identifiers.*
import uk.gov.hmrc.agentsexternalstubs.repository.{DuplicateUserException, KnownFactsRepository}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, EnrolmentAlreadyExists, GroupsService, RecordsService, UsersService}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import uk.gov.hmrc.http.{BadRequestException, ForbiddenException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Instant, LocalDate, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EnrolmentStoreProxyStubController @Inject() (
  val authenticationService: AuthenticationService,
  knownFactsRepository: KnownFactsRepository,
  usersService: UsersService,
  groupsService: GroupsService,
  recordsService: RecordsService,
  cc: ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  /** No session resolvable (e.g. a machine-to-machine caller with no live session on any planet) - fall back to
    * finding which planet owns the BusinessPartnerRecord for the ARN embedded in this enrolment key, the same
    * way HipStubController does for its BPR-keyed endpoints. Only safe because ARNs are minted globally unique
    * (as of the safeId fix - see .claude/findings/validation-and-records.md). Logs a warning (doesn't fail) if
    * more than one planet has a matching record - see RecordsRepository.findFirstByKeysAnyPlanet.
    */
  private def planetIdFromArnGlobally(enrolmentKey: EnrolmentKey): Future[Option[String]] =
    enrolmentKey.identifiers.find(_.key == "AgentReferenceNumber").map(_.value) match {
      case Some(arn) => recordsService.getRecordAnyPlanet[BusinessPartnerRecord, Arn](Arn(arn)).map(_.map(_._1))
      case None =>
        Logger(getClass).warn(
          s"Cannot fall back to a global lookup for enrolmentKey $enrolmentKey - no AgentReferenceNumber identifier"
        )
        Future.successful(None)
    }

  def getUserIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      (for {
        principal <- if `type` == "all" || `type` == "principal" then
                       usersService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if `type` == "all" || `type` == "delegated" then {
                       usersService.findUserIdsByAssignedDelegatedEnrolmentKey(
                         enrolmentKey,
                         session.planetId,
                         limit = Some(1000)
                       )
                     } else Future.successful(Seq.empty)
      } yield GetUserIdsResponse.from(principal, delegated)).map {
        case GetUserIdsResponse(None, None) => NoContent
        case response                       => Ok(RestfulResponse(response))
      }

    }(SessionRecordNotFound)
  }

  def getGroupIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      (for {
        principal <- if `type` == "all" || `type` == "principal" then
                       groupsService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if `type` == "all" || `type` == "delegated" then
                       groupsService.findByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                     else Future.successful(Seq.empty)
      } yield GetGroupIdsResponse.from(principal, delegated)).map {
        case GetGroupIdsResponse(None, None) => NoContent
        case response                        => Ok(RestfulResponse(response))
      }

    }(SessionRecordNotFound)
  }

  def setKnownFacts(enrolmentKey: EnrolmentKey): Action[JsValue] = Action.async(parse.tolerantJson) {
    request =>
      given Request[JsValue] = request
      withCurrentSession(session => handleSetKnownFacts(enrolmentKey, session.planetId)) {
        planetIdFromArnGlobally(enrolmentKey).flatMap {
          case Some(planetId) => handleSetKnownFacts(enrolmentKey, planetId)
          case None           => SessionRecordNotFound
        }
      }
  }

  private def handleSetKnownFacts(enrolmentKey: EnrolmentKey, planetId: String)(using request: Request[JsValue]): Future[Result] =
    withPayload[SetKnownFactsRequest] { payload =>
      knownFactsRepository
        .upsert(KnownFacts(enrolmentKey, enrolmentKey.identifiers, payload.verifiers), planetId)
        .map(_ => NoContent)
    }

  def removeKnownFacts(enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      knownFactsRepository
        .delete(enrolmentKey, session.planetId)
        .map(_ => NoContent)
        .recover { case NonFatal(_) =>
          NoContent
        }
    }(SessionRecordNotFound)
  }

  def assignUser(userId: String, enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      usersService
        .assignEnrolmentToUser(userId, enrolmentKey, session.planetId)
        .map(_ => Created)
        .recover {
          case e: NotFoundException =>
            notFound(e.getMessage)
          case e: BadRequestException =>
            badRequest(e.getMessage)
          case e: ForbiddenException =>
            forbidden(e.getMessage)
        }
    }(SessionRecordNotFound)
  }

  def deassignUser(userId: String, enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      usersService
        .deassignEnrolmentFromUser(userId, enrolmentKey, session.planetId)
        .map(_ => NoContent)
        .recover {
          case e: NotFoundException =>
            notFound(e.getMessage)
          case e: BadRequestException =>
            badRequest(e.getMessage)
        }
    }(SessionRecordNotFound)
  }

  def getGroupAllocatedEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey
  ): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService.findByGroupId(groupId, session.planetId).map {
        case None => NotFound
        case Some(group) =>
          val matched =
            group.principalEnrolments.find { e =>
              val matches = e.toEnrolmentKey.exists(_.tag == enrolmentKey.tag)
              matches
            }
          matched.fold(NotFound: Result) { e =>
            Ok(
              Json.toJson(
                Es5GroupAllocatedEnrolment(
                  service = e.key,
                  status = Some(e.state),
                  enrolmentDate = Option(randomDateTimeInTheLastFiveYears)
                )
              )
            )
          }
      }
    }(SessionRecordNotFound)
  }

  // ES8
  def allocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]
  ): Action[JsValue] = Action.async(parse.tolerantJson) { request =>
    given Request[JsValue] = request
    withCurrentSession(session =>
      handleAllocateGroupEnrolment(groupId, enrolmentKey, `legacy-agentCode`, session.planetId)
    ) {
      planetIdFromArnGlobally(enrolmentKey).flatMap {
        case Some(planetId) => handleAllocateGroupEnrolment(groupId, enrolmentKey, `legacy-agentCode`, planetId)
        case None           => SessionRecordNotFound
      }
    }
  }

  private def handleAllocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String],
    planetId: String
  )(using request: Request[JsValue]): Future[Result] =
    withPayload[AllocateGroupEnrolmentRequest] { payload =>
      AllocateGroupEnrolmentRequest
        .validate(payload)
        .fold(
          error => badRequestF("INVALID_JSON_BODY", error.mkString(", ")),
          _ =>
            (for {
              maybeUser <- usersService.findByUserId(payload.userId, planetId)
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
                       planetId
                     )
              // Assign the new enrolment to the user specified in the payload (as per EACD behaviour spec)
              _ <- usersService
                     .assignEnrolmentToUser(
                       userId = payload.userId,
                       enrolmentKey = enrolmentKey,
                       planetId = planetId
                     )
            } yield Created)
              .recover {
                case _: EnrolmentAlreadyExists                          => Conflict
                case _: DuplicateUserException                          => Conflict
                case UnsupportedCredentialRole("INVALID_CREDENTIAL_ID") => Forbidden
              }
        )
    }

  def deallocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]
  ): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      for {
        // First unassign the enrolment from any users belonging to this group
        groupUsers <- usersService.findByGroupId(groupId, session.planetId)(limit = None)
        usersWithAssignment = groupUsers.filter(user =>
                                user.assignedPrincipalEnrolments.contains(enrolmentKey)
                                  || user.assignedDelegatedEnrolments.contains(enrolmentKey)
                              )
        _ <- Future.traverse(usersWithAssignment)(user =>
               usersService.updateUser(
                 user.userId,
                 session.planetId,
                 u =>
                   u.copy(
                     assignedPrincipalEnrolments = u.assignedPrincipalEnrolments.filterNot(_.tag == enrolmentKey.tag),
                     assignedDelegatedEnrolments = u.assignedDelegatedEnrolments.filterNot(_.tag == enrolmentKey.tag)
                   )
               )
             )

        // Now remove the allocated enrolment from the group
        _ <- groupsService.deallocateEnrolmentFromGroup(
               groupId,
               enrolmentKey,
               `legacy-agentCode`,
               session.planetId
             )
      } yield NoContent
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
    if `type` != "principal" && `type` != "delegated" then badRequestF("INVALID_ENROLMENT_TYPE")
    else if service.isDefined && !Services.servicesByKey.contains(service.get) then badRequestF("INVALID_SERVICE")
    else if `start-record`.isDefined && `start-record`.get < 1 then badRequestF("INVALID_START_RECORD")
    else if `max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000) then
      badRequestF("INVALID_MAX_RECORDS")
    else {
      groupsService.findByGroupId(groupId, planetId).flatMap {
        case None =>
          notFoundF("INVALID_GROUP_ID")
        case Some(group) =>
          val principal = `type` == "principal"
          val startRecord = `start-record`.getOrElse(1)
          def assignedEnrolments(user: User) = if principal then user.assignedPrincipalEnrolments
          else user.assignedDelegatedEnrolments
          val enrolments = (if principal then group.principalEnrolments else group.delegatedEnrolments)
            .filter(e => service.forall(_ == e.key))
            .filter(e => assignedToUser.forall(user => e.toEnrolmentKey.exists(assignedEnrolments(user).contains(_))))
            .slice(startRecord - 1, startRecord - 1 + `max-records`.getOrElse(1000))

          val response = GetUserEnrolmentsResponse.from(startRecord, enrolments)
          Future.successful {
            if response.totalRecords == 0 then NoContent
            else Ok(Json.toJson(response))
          }
      }
    }

  def getUserEnrolments(
    userId: String,
    `type`: String,
    service: Option[String],
    `start-record`: Option[Int],
    `max-records`: Option[Int]
  ): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      if `type` != "principal" && `type` != "delegated" then badRequestF("INVALID_ENROLMENT_TYPE")
      else if service.isDefined && !Services.servicesByKey.contains(service.get) then badRequestF("INVALID_SERVICE")
      else if `start-record`.isDefined && `start-record`.get < 1 then badRequestF("INVALID_START_RECORD")
      else if `max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000) then
        badRequestF("INVALID_MAX_RECORDS")
      else {
        usersService.findByUserId(userId, session.planetId).flatMap {
          case None =>
            notFoundF("INVALID_CREDENTIAL_ID")
          case Some(user) if user.groupId.isEmpty =>
            notFoundF("INVALID_GROUP_ID")
          case Some(user) =>
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

  //ES3
  def getGroupEnrolments(
                          groupId: String,
                          `type`: String,
                          service: Option[String],
                          `start-record`: Option[Int],
                          `max-records`: Option[Int],
                          @unused _userId: Option[String],
                          @unused _unassignedClients: Option[Boolean]
  ): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      if `type` != "principal" && `type` != "delegated" then badRequestF("INVALID_ENROLMENT_TYPE")
      else if service.isDefined && !Services.servicesByKey.contains(service.get) then badRequestF("INVALID_SERVICE")
      else if `start-record`.isDefined && `start-record`.get < 1 then badRequestF("INVALID_START_RECORD")
      else if `max-records`.isDefined && (`max-records`.get < 10 || `max-records`.get > 1000) then
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

  def getDelegatedEnrolments(groupId: String): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      for {
        maybeGroup <- groupsService.findByGroupId(groupId, session.planetId)
        users      <- usersService.findByGroupId(groupId, session.planetId)(limit = Some(100))
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
            if
              accumulatedMap.keySet.exists(assignedEnrolment =>
                assignedEnrolment.key == delegatedEnrolment.key &&
                  assignedEnrolment.identifiers == delegatedEnrolment.identifiers
              )
            then {
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
              if enrolmentToUserIds._2.size == 1 then enrolmentToUserIds._2.head
              else enrolmentToUserIds._2.size.toString
            )
          )
          .toSeq
        Ok(Json.toJson(GroupDelegatedEnrolments(assignedClients)))
      }
    }(SessionRecordNotFound)
  }

  def setEnrolmentFriendlyName(groupId: String, enrolmentKey: EnrolmentKey): Action[JsValue] =
    Action.async(parse.tolerantJson) { request =>
    given Request[JsValue] = request
      withCurrentSession { session =>
        withPayload[SetFriendlyNameRequest] { payload =>
          SetFriendlyNameRequest
            .validate(payload)
            .fold(
              error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
              _ =>
                groupsService
                  .setEnrolmentFriendlyName(groupId, session.planetId, enrolmentKey, payload.friendlyName)
                  .map(_ => NoContent)
            )
        }
      }(SessionRecordNotFound)
    }

  /** ES20 - ES allows for a list of known facts (identifiers and verifiers)
    *
    *  Assume it is 1 identifier (cbcId) to get multiple identifiers (UTR and cbcId),
    *  to allow us to construct the full enrolment key for HMRC-CBC-ORG
    */
  def queryEnrolmentsFromKnownFacts: Action[JsValue] =
    Action.async(parse.tolerantJson) { request =>
    given Request[JsValue] = request
      withCurrentSession { session =>
        withPayload[EnrolmentsFromKnownFactsRequest] { payload =>
          EnrolmentsFromKnownFactsRequest
            .validate(payload)
            .fold(
              error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
              _ =>
                knownFactsRepository
                  .findAllByIdentifier(payload.knownFacts.head, session.planetId)
                  .map(knownFacts =>
                    knownFacts
                      .find(_.enrolmentKey.service == payload.service)
                      .map(kf =>
                        Ok(
                          Json.toJson(
                            EnrolmentsFromKnownFactsResponse.fromKnownFacts(kf)
                          )
                        )
                      )
                      .getOrElse(NoContent)
                  )
            )
        }
      }(SessionRecordNotFound)
    }
}

object EnrolmentStoreProxyStubController {

  //matches response from ES20
  case class IdentifiersAndVerifiers(identifiers: Seq[Identifier], verifiers: Seq[KnownFact])

  object IdentifiersAndVerifiers {
    given formats: OFormat[IdentifiersAndVerifiers] = Json.format[IdentifiersAndVerifiers]
  }

  case class EnrolmentsFromKnownFactsResponse(service: String, enrolments: Seq[IdentifiersAndVerifiers])

  object EnrolmentsFromKnownFactsResponse {
    given formats: OFormat[EnrolmentsFromKnownFactsResponse] = Json.format[EnrolmentsFromKnownFactsResponse]

    def fromKnownFacts(knownFacts: KnownFacts): EnrolmentsFromKnownFactsResponse =
      EnrolmentsFromKnownFactsResponse(
        knownFacts.enrolmentKey.service,
        Seq(
          IdentifiersAndVerifiers(
            knownFacts.enrolmentKey.identifiers,
            knownFacts.verifiers
          )
        )
      )
  }

  // Note: knownFacts could be identifiers or verifiers in ES20
  case class EnrolmentsFromKnownFactsRequest(service: String, knownFacts: Seq[Identifier])

  object EnrolmentsFromKnownFactsRequest {
    given formats: OFormat[EnrolmentsFromKnownFactsRequest] = Json.format[EnrolmentsFromKnownFactsRequest]

    private val serviceValidator: Validator[String] =
      check(Services(_).isDefined, s"INVALID_SERVICE")

    val validate: Validator[EnrolmentsFromKnownFactsRequest] = Validator(
      checkProperty(_.service, serviceValidator),
      check(_.knownFacts.nonEmpty, "INVALID_JSON")
    )
  }

  case class GetUserIdsResponse(principalUserIds: Option[Seq[String]], delegatedUserIds: Option[Seq[String]])

  object GetUserIdsResponse {
    given writes: Writes[GetUserIdsResponse] = Json.writes[GetUserIdsResponse]

    def from(principal: Option[User], delegated: Seq[String]): GetUserIdsResponse =
      GetUserIdsResponse(principal.map(u => Seq(u.userId)), if delegated.isEmpty then None else Some(delegated.distinct))
  }

  case class GetGroupIdsResponse(principalGroupIds: Option[Seq[String]], delegatedGroupIds: Option[Seq[String]])

  object GetGroupIdsResponse {
    given writes: Writes[GetGroupIdsResponse] = Json.writes[GetGroupIdsResponse]

    def from(principal: Option[Group], delegated: Seq[Group]): GetGroupIdsResponse =
      GetGroupIdsResponse(
        principal.map(u => Seq(u.groupId)),
        if delegated.isEmpty then None else Some(delegated.map(_.groupId).distinct)
      )
  }

  case class AllocateGroupEnrolmentRequest(userId: String, `type`: String)

  object AllocateGroupEnrolmentRequest {
    given reads: Reads[AllocateGroupEnrolmentRequest] = Json.reads[AllocateGroupEnrolmentRequest]

    val validate: AllocateGroupEnrolmentRequest => Validated[List[String], Unit] =
      Validator[AllocateGroupEnrolmentRequest](
        Validator.check(_.`type`.matches("principal|delegated"), "Unsupported `type` param value")
      )
  }

  case class SetKnownFactsRequest(verifiers: Seq[KnownFact], legacy: Option[Legacy] = None)

  object SetKnownFactsRequest {

    case class Legacy(previousVerifiers: Seq[KnownFact])

    object Legacy {
      given formats: Format[Legacy] = Json.format[Legacy]
    }

    given formats: Format[SetKnownFactsRequest] = Json.format[SetKnownFactsRequest]

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

      def from(e: uk.gov.hmrc.agentsexternalstubs.models.Enrolment): Enrolment = Enrolment(
        service = e.key,
        state = e.state,
        friendlyName = e.friendlyName.getOrElse(""),
        failedActivationCount = 0,
        activationDate = Option(randomDateTimeInTheLastFiveYears),
        enrolmentDate = Option(randomDateTimeInTheLastFiveYears),
        enrolmentTokenExpiryDate = None,
        identifiers = e.identifiers
          .getOrElse(Seq.empty)
      )
    }

    def from(
      startRecord: Int,
      enrolments: Seq[uk.gov.hmrc.agentsexternalstubs.models.Enrolment]
    ): GetUserEnrolmentsResponse = {
      val responseEnrolment =
        enrolments
          .map(e => Enrolment.from(e))
      GetUserEnrolmentsResponse(
        startRecord = startRecord,
        totalRecords = responseEnrolment.size,
        enrolments = responseEnrolment
      )
    }

    private def randomDateTimeInTheLastFiveYears: Instant = {
      val start = LocalDate.now().minusYears(5)
      val end = LocalDate.now()
      Generator.date(start, end).sample.get.atStartOfDay(ZoneId.systemDefault).toInstant
    }

    given writes1: Writes[Enrolment] = Json.writes[Enrolment]
    given writes2: Writes[GetUserEnrolmentsResponse] = Json.writes[GetUserEnrolmentsResponse]
  }

  case class SetFriendlyNameRequest(friendlyName: String)

  object SetFriendlyNameRequest {

    given format: Format[SetFriendlyNameRequest] = Json.format[SetFriendlyNameRequest]

    private val friendlyNamePattern = "^[!%*^()_+\\-={}:;@~#,.?\\[\\]/A-Za-z0-9 ]{0,80}$"
    private val es19FriendlyNameValidator: Validator[String] =
      check(_.matches(friendlyNamePattern), s"""Invalid friendlyName, does not matches regex $friendlyNamePattern""")

    val validate: Validator[SetFriendlyNameRequest] = Validator(
      checkProperty(_.friendlyName, es19FriendlyNameValidator)
    )
  }

  case class Es5GroupAllocatedEnrolment(
    service: String,
    status: Option[String],
    enrolmentDate: Option[Instant]
  )

  object Es5GroupAllocatedEnrolment {
    implicit val formats: OFormat[Es5GroupAllocatedEnrolment] = Json.format[Es5GroupAllocatedEnrolment]
  }

  private def randomDateTimeInTheLastFiveYears: Instant = {
    val start = LocalDate.now().minusYears(5)
    val end = LocalDate.now()
    Generator.date(start, end).sample.get.atStartOfDay(ZoneId.systemDefault).toInstant
  }
}
