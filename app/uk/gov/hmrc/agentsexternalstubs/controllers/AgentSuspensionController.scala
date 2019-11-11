package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.Inject
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class AgentSuspensionController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getSuspensionStatus(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(session.userId, session.planetId).flatMap {
        case None => Future successful notFound("USER_NOT_FOUND")
        case Some(user)
            if user.principalEnrolments.exists(e => e.identifiers.getOrElse(Seq()).exists(_.value == arn.value)) =>
          user.suspendedServices match {
            case None     => Future successful Ok(Json.toJson(AgentSuspensionResponse("NotSuspended")))
            case Some(ss) => Future successful Ok(Json.toJson(AgentSuspensionResponse("Suspended", Some(ss))))
          }
        case Some(user) if user.affinityGroup.contains("Agent") =>
          Future successful Unauthorized("arn does not match that of logged in user")
        case Some(_) => Future successful Unauthorized("user is not an agent")
      }
    }(SessionRecordNotFound)
  }
}

case class AgentSuspensionResponse(status: String, suspendedServices: Option[Set[String]] = None)

object AgentSuspensionResponse {
  implicit val formats: OFormat[AgentSuspensionResponse] = Json.format
}
