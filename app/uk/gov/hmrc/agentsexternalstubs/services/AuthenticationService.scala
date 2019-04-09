package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticateRequest, AuthenticatedSession, Planet}
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AuthenticationService @Inject()(
  authSessionRepository: AuthenticatedSessionsRepository,
  externalAuthorisationService: ExternalAuthorisationService,
  userService: UsersService) {

  private val authenticatedSessionCache =
    new AsyncCache[String, AuthenticatedSession](
      maximumSize = 1000000,
      expireAfterWrite = Some(5.minutes),
      keys = as => Seq(as.authToken, as.sessionId))

  def findByAuthTokenOrLookupExternal(authToken: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier,
    request: Request[_]): Future[Option[AuthenticatedSession]] =
    authenticatedSessionCache.getOption(
      authToken,
      authSessionRepository.findByAuthToken(authToken).flatMap {
        case Some(session) => Future.successful(Some(session))
        case None =>
          val planetId = Planet.DEFAULT
          externalAuthorisationService.maybeExternalSession(planetId, this.authenticate)
      }
    )

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authenticatedSessionCache.getOption(authToken, authSessionRepository.findByAuthToken(authToken))

  def findBySessionId(sessionId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authenticatedSessionCache.getOption(sessionId, authSessionRepository.findBySessionId(sessionId))

  def findByPlanetId(planetId: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByPlanetId(planetId)

  def authenticate(request: AuthenticateRequest)(
    implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] = {
    val authToken = request.authTokenOpt.getOrElse(UUID.randomUUID().toString)
    val authenticatedSession =
      AuthenticatedSession(request.sessionId, request.userId, authToken, request.providerType, request.planetId)
    (for {
      _ <- authSessionRepository.create(authenticatedSession)
      _ <- authenticatedSessionCache.put(authenticatedSession)
    } yield Some(authenticatedSession)).recover {
      case NonFatal(e) =>
        Logger(getClass).warn(s"Could not create new authorised session ${e.getMessage}")
        None
    }
  }

  def removeAuthentication(authToken: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      _ <- authenticatedSessionCache.invalidate(authToken)
      _ <- authSessionRepository.delete(authToken)
    } yield ()

}
