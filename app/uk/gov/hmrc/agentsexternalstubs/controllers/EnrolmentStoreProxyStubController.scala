package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.GetUserIdsResponse
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class EnrolmentStoreProxyStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService)
    extends BaseController with CurrentSession {

  def getUserIds(enrolmentKey: EnrolmentKey, `type`: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      (for {
        principal <- if (`type` == "all" || `type` == "principal")
                      usersService.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                    else Future.successful(None)
        delegated <- if (`type` == "all" || `type` == "delegated")
                      usersService.findByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
                    else Future.successful(Seq.empty)
      } yield GetUserIdsResponse.from(principal, delegated)).map {
        case GetUserIdsResponse(None, None) => NoContent
        case response                       => Ok(RestfulResponse(response))
      }

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
    implicit val writes: OWrites[GetUserIdsResponse] = Json.writes[GetUserIdsResponse]

    def from(principal: Option[User], delegated: Seq[User]): GetUserIdsResponse =
      GetUserIdsResponse(
        principal.map(u => Seq(u.userId)),
        if (delegated.isEmpty) None else Some(delegated.map(_.userId)))
  }

}
