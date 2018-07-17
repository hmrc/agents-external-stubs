package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User}
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationService @Inject()(
  authSessionRepository: AuthenticatedSessionsRepository,
  userService: UsersService) {

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByAuthToken(authToken)

  def createNewAuthentication(userId: String, password: String)(
    implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] = {
    val authToken = UUID.randomUUID().toString
    for {
      _ <- userService.findByUserId(userId).flatMap {
            case None       => userService.createUser(User(userId))
            case Some(user) => Future.successful(user)
          }
      _            <- authSessionRepository.create(userId, authToken)
      maybeSession <- authSessionRepository.findByAuthToken(authToken)
    } yield maybeSession
  }
}
