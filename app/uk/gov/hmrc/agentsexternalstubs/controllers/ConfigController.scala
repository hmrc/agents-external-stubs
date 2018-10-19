package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfigController @Inject()(val authenticationService: AuthenticationService, ecp: ExecutionContextProvider)
    extends BaseController with CurrentSession {

  implicit val ec: ExecutionContext = ecp.get()

  private lazy val servicesResponse = RestfulResponse(Services(services = Services.services))

  def getServices: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(servicesResponse))
  }

}
