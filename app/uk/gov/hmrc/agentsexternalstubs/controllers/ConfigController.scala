package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfigController @Inject()(val authenticationService: AuthenticationService, cc: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  private lazy val servicesResponse = RestfulResponse(Services(services = Services.services))

  def getServices: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(servicesResponse))
  }

}
