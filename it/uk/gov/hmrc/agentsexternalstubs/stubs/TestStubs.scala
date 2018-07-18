package uk.gov.hmrc.agentsexternalstubs.stubs

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.Application
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait TestStubs {
  this: Suite with ServerProvider =>

  def app: Application
  def await[A](future: Future[A])(implicit timeout: Duration): A

  lazy val authenticationService: AuthenticationService = app.injector.instanceOf[AuthenticationService]
  lazy val userService: UsersService = app.injector.instanceOf[UsersService]

  def givenAnAuthenticatedUser(user: User, providerType: String = "GovernmentGateway")(
    implicit ec: ExecutionContext,
    timeout: Duration): String =
    await(for {
      authSession <- authenticationService.createNewAuthentication(user.userId, "any", providerType)
      _           <- userService.tryCreateUser(user)
    } yield authSession)
      .getOrElse(throw new Exception("Could not sign in user"))
      .authToken

  def givenUserEnrolledFor(userId: String, service: String, identifierKey: String, identifierValue: String)(
    implicit ec: ExecutionContext,
    timeout: Duration): Unit =
    await(userService.addEnrolment(userId, service, identifierKey, identifierValue))

}
