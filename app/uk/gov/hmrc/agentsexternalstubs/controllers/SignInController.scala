package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.agentsexternalstubs.models.{SignInRequest, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignInController @Inject()(signInService: AuthenticationService, usersService: UsersService)
    extends BaseController {

  def signIn(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SignInRequest] { signInRequest =>
      request.headers.get(HeaderNames.AUTHORIZATION) match {
        case None => createNewAuthentication(signInRequest, "GovernmentGateway")
        case Some(BearerToken(authToken)) =>
          for {
            maybeSession <- signInService.findByAuthToken(authToken)
            result <- maybeSession match {
                       case Some(session) if session.userId == signInRequest.userId =>
                         Future.successful(
                           Ok("").withHeaders(
                             HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url))
                       case _ =>
                         createNewAuthentication(signInRequest, "GovernmentGateway")
                     }
          } yield result
      }
    }
  }

  private def createNewAuthentication(signInRequest: SignInRequest, providerType: String)(
    implicit ec: ExecutionContext): Future[Result] =
    for {
      maybeSession <- signInService
                       .createNewAuthentication(signInRequest.userId, signInRequest.plainTextPassword, providerType)
      result <- maybeSession match {
                 case Some(session) =>
                   usersService
                     .tryCreateUser(User(session.userId))
                     .map(_ =>
                       Created("").withHeaders(
                         HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url))
                 case None => Future.successful(Unauthorized("SESSION_CREATE_FAILED"))
               }
    } yield result

  def session(authToken: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      maybeSession <- signInService.findByAuthToken(authToken)
    } yield
      maybeSession match {
        case Some(session) => Ok(Json.toJson(session))
        case None          => NotFound("AUTH_SESSION_NOT_FOUND")
      }
  }

}
