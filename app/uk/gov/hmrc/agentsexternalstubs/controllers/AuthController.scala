package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.agentsexternalstubs.models.AuthoriseRequest
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AuthController @Inject()() extends BaseController {

  def authorise(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case None => unauthorized("MissingBearerToken")
      case Some(token) =>
        for {
          authSession <- validateAuthToken(token)
          response <- withJsonBody[AuthoriseRequest] { authoriseRequest =>
                       unauthorized("InvalidBearerToken")
                     }
        } yield response
    }
  }

  def validateAuthToken(token: String): Future[Unit] = Future.successful(())

  def unauthorized(reason: String) =
    Future.successful(
      Unauthorized("")
        .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason""""))

}
