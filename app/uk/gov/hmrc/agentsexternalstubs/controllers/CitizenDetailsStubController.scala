package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsStubController @Inject()(usersService: UsersService) extends BaseController {

  def getCitizen(idName: String, taxId: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotFound(""))
  }

}
