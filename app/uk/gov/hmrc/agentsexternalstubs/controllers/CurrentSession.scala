package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.mvc.{Request, Result, Results}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}

import scala.concurrent.{ExecutionContext, Future}

trait CurrentSession extends HttpHelpers {

  def authenticationService: AuthenticationService

  def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result])(implicit request: Request[T], ec: ExecutionContext): Future[Result] =
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case None => ifSessionNotFound
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authenticationService.findByAuthToken(authToken)
          result <- maybeSession match {
                     case Some(session) => body(session)
                     case _             => ifSessionNotFound
                   }
        } yield result
    }

  def withCurrentUser[T](body: (User, AuthenticatedSession) => Future[Result])(ifSessionNotFound: => Future[Result])(
    implicit request: Request[T],
    ec: ExecutionContext,
    usersService: UsersService): Future[Result] =
    withCurrentSession { session =>
      usersService.findByUserId(session.userId, session.planetId).flatMap {
        case None       => Future.successful(Results.NotFound("CURRENT_USER_NOT_FOUND"))
        case Some(user) => body(user, session)
      }
    }(SessionRecordNotFound)
}

/*
 When stubbing DES request we can't just rely on the `Authorization` header
 because it is not the same token value issued by MDTP Auth,
 instead we have to exploit fact that `HeaderCarrierConverter` copy over sessionId
 from HTTP session as `X-Session-ID` header and lookup session by its ID.
 */
trait DesCurrentSession extends DesHttpHelpers {

  def authenticationService: AuthenticationService

  def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result])(implicit request: Request[T], ec: ExecutionContext): Future[Result] =
    request.headers.get(uk.gov.hmrc.http.HeaderNames.xSessionId) match {
      case None => ifSessionNotFound
      case Some(sessionId) =>
        for {
          maybeSession <- authenticationService.findBySessionId(sessionId)
          result <- maybeSession match {
                     case Some(session) => body(session)
                     case _             => ifSessionNotFound
                   }
        } yield result
    }

}
