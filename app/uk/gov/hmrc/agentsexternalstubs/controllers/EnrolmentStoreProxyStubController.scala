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

package uk.gov.hmrc.agentsexternalstubs.controllers

import cats.data.Validated
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest.Legacy
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.http.NotFoundException

@Singleton
class EnrolmentStoreProxyStubController @Inject() (
  val authenticationService: AuthenticationService,
  knownFactsRepository: KnownFactsRepository,
  cc: ControllerComponents
)(implicit usersService: UsersService, executionContext: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getUserIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      (for {
        principal <- if (`type` == "all" || `type` == "principal")
                       usersService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if (`type` == "all" || `type` == "delegated")
                       usersService.findUserIdsByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                     else Future.successful(Seq.empty)
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
                       usersService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                     else Future.successful(None)
        delegated <- if (`type` == "all" || `type` == "delegated")
                       usersService.findGroupIdsByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                     else Future.successful(Seq.empty)
      } yield GetGroupIdsResponse.from(principal, delegated.collect { case Some(x) => x })).map {
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
        .updateUser(userId, session.planetId, _.withPrincipalEnrolment(Enrolment.from(enrolmentKey)))
        .map { case _ =>
          Created
        }
        .recover { case e: NotFoundException =>
          notFound("INVALID_CREDENTIAL_ID")
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
              usersService
                .allocateEnrolmentToGroup(
                  payload.userId,
                  groupId,
                  enrolmentKey,
                  payload.`type`,
                  `legacy-agentCode`,
                  session.planetId
                )
                .map(_ => Created)
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
      usersService
        .deallocateEnrolmentFromGroup(groupId, enrolmentKey, `legacy-agentCode`, keepAgentAllocations, session.planetId)
        .map(_ => NoContent)
    }(SessionRecordNotFound)
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
          case Some(user) =>
            val principal = `type` == "principal"
            val getKnownFacts: EnrolmentKey => Future[Option[KnownFacts]] =
              if (principal) knownFactsRepository.findByEnrolmentKey(_, session.planetId)
              else _ => Future.successful(None)
            val startRecord = `start-record`.getOrElse(1)
            val enrolments = (if (principal) user.principalEnrolments else user.delegatedEnrolments)
              .filter(e => service.forall(_ == e.key))
              .slice(startRecord - 1, startRecord - 1 + `max-records`.getOrElse(1000))
            Future
              .sequence(enrolments.map(_.toEnrolmentKey).collect { case Some(x) => x }.map(getKnownFacts))
              .map(_.collect { case Some(x) => x })
              .map { knownFacts =>
                val response =
                  GetUserEnrolmentsResponse.from(user, startRecord, enrolments, knownFacts)
                if (response.totalRecords == 0) NoContent else Ok(Json.toJson(response))
              }
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
        usersService.findAdminByGroupId(groupId, session.planetId).flatMap {
          case None =>
            notFoundF("INVALID_GROUP_ID")
          case Some(user) =>
            val principal = `type` == "principal"
            val getKnownFacts: EnrolmentKey => Future[Option[KnownFacts]] =
              if (principal) knownFactsRepository.findByEnrolmentKey(_, session.planetId)
              else _ => Future.successful(None)
            val startRecord = `start-record`.getOrElse(1)
            val enrolments = (if (principal) user.principalEnrolments else user.delegatedEnrolments)
              .filter(e => service.forall(_ == e.key))
              .slice(startRecord - 1, startRecord - 1 + `max-records`.getOrElse(1000))
            Future
              .sequence(enrolments.map(_.toEnrolmentKey).collect { case Some(x) => x }.map(getKnownFacts))
              .map(_.collect { case Some(x) => x })
              .map { knownFacts =>
                val response =
                  GetUserEnrolmentsResponse.from(user, startRecord, enrolments, knownFacts)
                if (response.totalRecords == 0) NoContent else Ok(Json.toJson(response))
              }
        }
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

    def from(principal: Option[User], delegated: Seq[String]): GetGroupIdsResponse =
      GetGroupIdsResponse(
        principal.map(u => Seq(u.groupId).collect { case Some(x) => x }),
        if (delegated.isEmpty) None else Some(delegated.distinct)
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
      enrolmentDate: Option[DateTime],
      failedActivationCount: Int,
      activationDate: Option[DateTime],
      enrolmentTokenExpiryDate: Option[DateTime],
      identifiers: Seq[Identifier]
    )

    object Enrolment {

      def from(e: uk.gov.hmrc.agentsexternalstubs.models.Enrolment, kf: Option[KnownFacts]): Enrolment = Enrolment(
        service = e.key,
        state = e.state,
        friendlyName = Services.servicesByKey.get(e.key).map(_.description).getOrElse(""),
        failedActivationCount = 0,
        activationDate = None,
        enrolmentDate = None,
        enrolmentTokenExpiryDate = None,
        identifiers = e.identifiers
          .getOrElse(Seq.empty) ++ kf.map(_.verifiers.map(v => Identifier(v.key, v.value))).getOrElse(Seq.empty)
      )
    }

    def from(
      user: User,
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

    import play.api.libs.json.JodaWrites._
    import play.api.libs.json.JodaReads._
    implicit val writes1: Writes[Enrolment] = Json.writes[Enrolment]
    implicit val writes2: Writes[GetUserEnrolmentsResponse] = Json.writes[GetUserEnrolmentsResponse]
  }

}
