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
class SignInController @Inject()(val authenticationService: AuthenticationService, usersService: UsersService)
    extends BaseController with CurrentSession {

  def signIn(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withPayload[SignInRequest] { signInRequest =>
      withCurrentSession { session =>
        if (session.userId == signInRequest.userId)
          Future.successful(
            Ok("").withHeaders(HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url))
        else createNewAuthentication(signInRequest)
      }(createNewAuthentication(signInRequest))
    }
  }

  def signOut(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      authenticationService.removeAuthentication(session.authToken).map(_ => NoContent)
    }(Future.successful(NoContent))
  }

  private def createNewAuthentication(signInRequest: SignInRequest)(implicit ec: ExecutionContext): Future[Result] =
    for {
      maybeSession <- authenticationService
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
      maybeSession <- authenticationService.findByAuthToken(authToken)
    } yield
      maybeSession match {
        case Some(session) => Ok(RestfulResponse(session, Link("delete", routes.SignInController.signOut().url)))
        case None          => notFound("AUTH_SESSION_NOT_FOUND")
      }
  }

}
