package uk.gov.hmrc.agentsexternalstubs.controllers

import cats.data.Validated
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest.Legacy
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.{AllocateGroupEnrolmentRequest, GetGroupIdsResponse, GetUserIdsResponse, SetKnownFactsRequest}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class EnrolmentStoreProxyStubController @Inject()(
  val authenticationService: AuthenticationService,
  knownFactsRepository: KnownFactsRepository)(implicit usersService: UsersService)
    extends BaseController with CurrentSession {

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
        .recover {
          case NonFatal(_) => NoContent
        }
    }(SessionRecordNotFound)
  }

  def allocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
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
                  session.planetId)
                .map(_ => Created)
          )
      }
    }(SessionRecordNotFound)
  }

  def deallocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String],
    keepAgentAllocations: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .deallocateEnrolmentFromGroup(groupId, enrolmentKey, `legacy-agentCode`, keepAgentAllocations, session.planetId)
        .map(_ => NoContent)
    }(SessionRecordNotFound)
  }

}

object EnrolmentStoreProxyStubController {

  /**
    *{
    *     "principalUserIds": [
    *        "ABCEDEFGI1234567",
    *        "ABCEDEFGI1234568"
    *     ],
    *     "delegatedUserIds": [
    *        "ABCEDEFGI1234567",
    *        "ABCEDEFGI1234568"
    *     ]
    * }
    */
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
        if (delegated.isEmpty) None else Some(delegated.distinct))
  }

  case class AllocateGroupEnrolmentRequest(userId: String, `type`: String)

  object AllocateGroupEnrolmentRequest {
    implicit val reads: Reads[AllocateGroupEnrolmentRequest] = Json.reads[AllocateGroupEnrolmentRequest]

    val validate: AllocateGroupEnrolmentRequest => Validated[List[String], Unit] =
      Validator[AllocateGroupEnrolmentRequest](
        Validator.check(_.`type`.matches("principal|delegated"), "Unsupported `type` param value"))
  }

  case class SetKnownFactsRequest(verifiers: Seq[KnownFact], legacy: Option[Legacy] = None)

  object SetKnownFactsRequest {

    case class Legacy(previousVerifiers: Seq[KnownFact])

    object Legacy {
      implicit val formats: Format[Legacy] = Json.format[Legacy]
    }

    implicit val formats: Format[SetKnownFactsRequest] = Json.format[SetKnownFactsRequest]

    def generate(enrolmentKey: String): Option[SetKnownFactsRequest] =
      KnownFacts
        .generate(EnrolmentKey(enrolmentKey), enrolmentKey)
        .map(kf => SetKnownFactsRequest(kf.verifiers, Some(Legacy(kf.verifiers))))
  }

}
