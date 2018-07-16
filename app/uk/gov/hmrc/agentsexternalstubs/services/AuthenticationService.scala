package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationService @Inject()(authSessionRepository: AuthenticatedSessionRepository) {

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByAuthToken(authToken)

  def createNewAuthentication(userId: String, password: String)(
    implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] = {
    val authToken = UUID.randomUUID().toString
    for {
      _            <- authSessionRepository.create(userId, authToken)
      maybeSession <- authSessionRepository.findByAuthToken(authToken)
    } yield maybeSession
  }
}
