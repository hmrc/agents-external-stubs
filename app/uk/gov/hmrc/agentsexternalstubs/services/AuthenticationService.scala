package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticateRequest, AuthenticatedSession}
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationService @Inject()(
  authSessionRepository: AuthenticatedSessionsRepository,
  externalAuthorisationService: ExternalAuthorisationService,
  userService: UsersService) {

  def findByAuthTokenOrLookupExternal(authToken: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier,
    request: Request[_]): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByAuthToken(authToken).flatMap {
      case Some(session) => Future.successful(Some(session))
      case None =>
        val planetId =
          request.session.get("X-Client-ID").orElse(hc.deviceID).getOrElse("hmrc")
        externalAuthorisationService.maybeExternalSession(planetId, this.authenticate)
    }

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByAuthToken(authToken)

  def findBySessionId(sessionId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findBySessionId(sessionId)

  def findByPlanetId(planetId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByPlanetId(planetId)

  def authenticate(request: AuthenticateRequest)(
    implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] = {
    val authToken = request.authTokenOpt.getOrElse(UUID.randomUUID().toString)
    for {
      _ <- authSessionRepository
            .create(request.sessionId, request.userId, authToken, request.providerType, request.planetId)
      maybeSession <- authSessionRepository.findByAuthToken(authToken)
    } yield maybeSession
  }

  def removeAuthentication(authToken: String)(implicit ec: ExecutionContext): Future[Unit] =
    authSessionRepository.delete(authToken).map(_ => ())

}
