package uk.gov.hmrc.agentsexternalstubs.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

@Singleton
class SignInController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  ecp: ExecutionContextProvider)
    extends BaseController with CurrentSession {

  implicit val ec: ExecutionContext = ecp.get()

  def signIn(): Action[AnyContent] = Action.async { implicit request =>
    withPayloadOrDefault[SignInRequest](SignInRequest(None, None, None, None)) { signInRequest =>
      withCurrentOrExternalSession { session =>
        if (signInRequest.userId.contains(session.userId))
          Future.successful(
            Ok.withHeaders(HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url))
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
      maybeSession <- authenticationService.authenticate(
                       AuthenticateRequest(
                         sessionId = UUID.randomUUID().toString,
                         userId = signInRequest.userId.getOrElse(Generator.userID(Random.nextString(8))),
                         password = signInRequest.plainTextPassword.getOrElse("p@ssw0rd"),
                         providerType = signInRequest.providerType.getOrElse("GovernmentGateway"),
                         planetId = signInRequest.planetId.getOrElse(Generator.planetID(Random.nextString(8)))
                       ))
      result <- maybeSession match {
                 case Some(session) =>
                   usersService
                     .tryCreateUser(User(session.userId), session.planetId)
                     .map {
                       case Success(_) =>
                         Created.withHeaders(
                           HeaderNames.LOCATION -> routes.SignInController.session(session.authToken).url)
                       case Failure(_) =>
                         Accepted.withHeaders(
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

  def currentSession: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      okF(session, Link("delete", routes.SignInController.signOut().url))
    }(notFoundF("MISSING_AUTH_SESSION"))
  }

}
