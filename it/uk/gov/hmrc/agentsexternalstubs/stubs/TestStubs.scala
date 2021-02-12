package uk.gov.hmrc.agentsexternalstubs.stubs

import java.util.UUID

import org.scalatest.Suite
import play.api.Application
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait TestStubs {
  this: Suite =>

  def app: Application
  def await[A](future: Future[A])(implicit timeout: Duration): A

  lazy val authenticationService: AuthenticationService = app.injector.instanceOf[AuthenticationService]
  lazy val userService: UsersService = app.injector.instanceOf[UsersService]

  def givenAnAuthenticatedUser(
    user: User,
    providerType: String = "GovernmentGateway",
    planetId: String = UUID.randomUUID().toString
  )(implicit ec: ExecutionContext, timeout: Duration): String =
    await(for {
      authSession <-
        authenticationService
          .authenticate(AuthenticateRequest(UUID.randomUUID().toString, user.userId, "any", providerType, planetId))
      _ <- userService.tryCreateUser(user, planetId)
    } yield authSession)
      .getOrElse(throw new Exception("Could not sign in user"))
      .authToken

  def givenUserEnrolledFor(
    userId: String,
    planetId: String,
    service: String,
    identifierKey: String,
    identifierValue: String
  )(implicit ec: ExecutionContext, timeout: Duration): Unit =
    await(addPrincipalEnrolment(userId, planetId, service, identifierKey, identifierValue))

  private def addPrincipalEnrolment(
    userId: String,
    planetId: String,
    service: String,
    identifierKey: String,
    identifierValue: String
  )(implicit ec: ExecutionContext): Future[User] =
    userService.updateUser(
      userId,
      planetId,
      user =>
        user.copy(
          principalEnrolments =
            user.principalEnrolments :+ Enrolment(service, Some(Seq(Identifier(identifierKey, identifierValue))))
        )
    )

  def givenUserWithStrideRole(userId: String, planetId: String, role: String)(implicit
    ec: ExecutionContext,
    timeout: Duration
  ): Unit =
    await(addStrideRole(userId, planetId, role))

  private def addStrideRole(userId: String, planetId: String, role: String)(implicit
    ec: ExecutionContext
  ): Future[User] =
    userService.updateUser(
      userId,
      planetId,
      user => user.copy(strideRoles = user.strideRoles :+ role)
    )

}
