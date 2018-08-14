package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.{AllocateGroupEnrolmentRequest, GetGroupIdsResponse, GetUserIdsResponse}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, EnrolmentKey, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class EnrolmentStoreProxyStubController @Inject()(val authenticationService: AuthenticationService)(
  implicit usersService: UsersService)
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

  def allocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentUser { (user, session) =>
      withJsonBody[AllocateGroupEnrolmentRequest] { payload =>
        validate(payload).fold(
          error => badRequestF(error),
          _ =>
            if (user.groupId.contains(groupId)) {
              if (user.userId == payload.userId) {
                if (!`legacy-agentCode`.forall(ac => user.agentCode.contains(ac))) badRequestF("INVALID_AGENT_FORMAT")
                else
                  (if (payload.`type` == "principal")
                     usersService
                       .updateUser(
                         user.userId,
                         session.planetId,
                         u => u.copy(principalEnrolments = u.principalEnrolments :+ Enrolment.from(enrolmentKey)))
                   else
                     usersService
                       .updateUser(
                         user.userId,
                         session.planetId,
                         u => u.copy(delegatedEnrolments = u.delegatedEnrolments :+ Enrolment.from(enrolmentKey))))
                    .map(_ => Created(""))
              } else badRequestF("INVALID_JSON_BODY")
            } else badRequestF("INVALID_GROUP_ID")
        )
      }
    }(SessionRecordNotFound)
  }

  private def validate(payload: AllocateGroupEnrolmentRequest): Either[String, AllocateGroupEnrolmentRequest] =
    if (payload.`type` == "principal" || payload.`type` == "delegated") Right(payload) else Left("INVALID_JSON_BODY")

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
  }

}
