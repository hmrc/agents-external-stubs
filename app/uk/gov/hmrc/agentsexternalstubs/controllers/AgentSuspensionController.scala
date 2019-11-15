package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.Inject
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Identifier}
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
      usersService
        .findByPrincipalEnrolmentKey(
          EnrolmentKey("HMRC-AS-AGENT", Seq(Identifier("AgentReferenceNumber", arn.value))),
          session.planetId)
        .flatMap {
          case None => Future successful notFound("USER_NOT_FOUND")
          case Some(user) =>
            user.suspendedServices match {
              case None     => Future successful Ok(Json.toJson(SuspendedServices(Set.empty)))
              case Some(ss) => Future successful Ok(Json.toJson(SuspendedServices(ss)))
            }
        }
    }(SessionRecordNotFound)
  }
}

case class SuspendedServices(services: Set[String])

object SuspendedServices {
  implicit val formats: OFormat[SuspendedServices] = Json.format
}
