package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService)
    extends BaseController with CurrentSession {

  def getCitizen(idName: String, taxId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      Future.successful(NotFound(""))
    }(SessionRecordNotFound)
  }

}
