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
import scala.util.{Failure, Success}

@Singleton
class SignInController @Inject()(signInService: AuthenticationService, usersService: UsersService)
    extends BaseController {

  def signIn(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SignInRequest] { signInRequest =>
      request.headers.get(HeaderNames.AUTHORIZATION) match {
        case None => createNewAuthentication(signInRequest)
        case Some(BearerToken(authToken)) =>
          for {
            maybeSession <- signInService.findByAuthToken(authToken)
            result <- maybeSession match {
                       case Some(session) if session.userId == signInRequest.userId =>
                         Future.successful(
                           Ok("").withHeaders(
                             HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url))
                       case _ =>
                         createNewAuthentication(signInRequest)
                     }
          } yield result
      }
    }
  }

  def signOut(): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case None => Future.successful(NoContent)
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- signInService.findByAuthToken(authToken)
          result <- maybeSession match {
                     case Some(session) =>
                       signInService.removeAuthentication(session.authToken).map(_ => NoContent)
                     case _ =>
                       Future.successful(NoContent)
                   }
        } yield result
    }
  }

  private def createNewAuthentication(signInRequest: SignInRequest)(implicit ec: ExecutionContext): Future[Result] =
    for {
      maybeSession <- signInService
                       .createNewAuthentication(
                         signInRequest.userId,
                         signInRequest.plainTextPassword,
                         signInRequest.providerType,
                         signInRequest.planetId)
      result <- maybeSession match {
                 case Some(session) =>
                   usersService
                     .tryCreateUser(User(session.userId), session.planetId)
                     .map {
                       case Success(_) =>
                         Created("").withHeaders(
                           HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url)
                       case Failure(_) =>
                         Accepted("").withHeaders(
                           HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url)

                     }
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
