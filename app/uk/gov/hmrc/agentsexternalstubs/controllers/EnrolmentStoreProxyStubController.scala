package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.{AllocateGroupEnrolmentRequest, GetGroupIdsResponse, GetUserIdsResponse}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, EnrolmentKey, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import uk.gov.hmrc.http.BadRequestException
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

  /* Group enrolment is assigned to the unique Admin user of the group */
  def allocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String]): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withJsonBody[AllocateGroupEnrolmentRequest] { payload =>
        validate(payload).fold(
          error => badRequestF("INVALID_JSON_BODY", error),
          _ =>
            usersService
              .findByUserId(payload.userId, session.planetId)
              .flatMap {
                case Some(user) =>
                  if (user.groupId.contains(groupId)) {
                    (if (user.credentialRole.contains(User.CR.Admin)) Future.successful(user)
                     else if (user.credentialRole.contains(User.CR.Assistant))
                       Future.failed(UnsupportedCredentialRole("INVALID_CREDENTIAL_TYPE"))
                     else {
                       `legacy-agentCode` match {
                         case None =>
                           usersService
                             .findAdminByGroupId(groupId, session.planetId)
                             .map(_.getOrElse(throw new BadRequestException("INVALID_GROUP_ID")))
                         case Some(agentCode) =>
                           usersService
                             .findAdminByAgentCode(agentCode, session.planetId)
                             .map(_.getOrElse(throw new BadRequestException("INVALID_AGENT_FORMAT")))
                       }
                     }).flatMap { admin =>
                      (if (payload.`type` == "principal")
                         usersService
                           .updateUser(
                             admin.userId,
                             session.planetId,
                             u => u.copy(principalEnrolments = u.principalEnrolments :+ Enrolment.from(enrolmentKey)))
                       else
                         usersService
                           .updateUser(
                             admin.userId,
                             session.planetId,
                             u => u.copy(delegatedEnrolments = u.delegatedEnrolments :+ Enrolment.from(enrolmentKey))))
                        .map(_ => Created(""))
                    }
                  } else
                    badRequestF(
                      "INVALID_GROUP_ID",
                      s"User's groupId value ${user.groupId} does not match groupId path param $groupId")
                case None => badRequestF("INVALID_JSON_BODY", s"Could not find user ${payload.userId}")
            }
        )
      }
    }(SessionRecordNotFound)
  }

  private def validate(payload: AllocateGroupEnrolmentRequest): Either[String, AllocateGroupEnrolmentRequest] =
    if (payload.`type` == "principal" || payload.`type` == "delegated") Right(payload)
    else Left(s"Unsupported `type` param value ${payload.`type`}")

  /* Group enrolment is de-assigned from the unique Admin user of the group */
  def deallocateGroupEnrolment(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    `legacy-agentCode`: Option[String],
    keepAgentAllocations: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      `legacy-agentCode` match {
        case None => {
          usersService
            .findAdminByGroupId(groupId, session.planetId)
            .flatMap {
              case Some(admin) if admin.credentialRole.contains(User.CR.Admin) => {
                usersService
                  .updateUser(
                    admin.userId,
                    session.planetId,
                    u => u.copy(principalEnrolments = removeEnrolment(u.principalEnrolments, enrolmentKey)))
                  .map(_ => NoContent)
              }
              case _ => badRequestF("INVALID_GROUP_ID")
            }
        }
        case Some(agentCode) =>
          usersService
            .findAdminByAgentCode(agentCode, session.planetId)
            .flatMap {
              case Some(admin) if admin.credentialRole.contains(User.CR.Admin) =>
                usersService
                  .updateUser(
                    admin.userId,
                    session.planetId,
                    u => u.copy(delegatedEnrolments = removeEnrolment(u.delegatedEnrolments, enrolmentKey)))
                  .map(_ => NoContent)
              case _ => badRequestF("INVALID_AGENT_FORMAT")
            }
      }
    }(SessionRecordNotFound)
  }

  private def removeEnrolment(enrolments: Seq[Enrolment], key: EnrolmentKey): Seq[Enrolment] =
    enrolments.filterNot(_.matches(key))

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
