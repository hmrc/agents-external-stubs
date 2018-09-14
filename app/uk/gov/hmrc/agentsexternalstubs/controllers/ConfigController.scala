package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class ConfigController @Inject()(val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

  private lazy val servicesResponse = RestfulResponse(Services(services = Services.services))

  def getServices: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(servicesResponse))
  }

}
