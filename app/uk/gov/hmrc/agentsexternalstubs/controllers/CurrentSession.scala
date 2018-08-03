package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.mvc.{Request, Result, Results}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService

import scala.concurrent.{ExecutionContext, Future}

trait CurrentSession {

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

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Results
      .Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

  val SessionRecordNotFound = unauthorizedF("SessionRecordNotFound")
}
