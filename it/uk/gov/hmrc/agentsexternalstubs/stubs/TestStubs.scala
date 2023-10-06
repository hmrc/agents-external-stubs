package uk.gov.hmrc.agentsexternalstubs.stubs

import org.scalatest.Suite
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.admin.{AG, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GroupsService, UsersService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait TestStubs {
  this: Suite =>

  def app: Application

  lazy val authenticationService: AuthenticationService = app.injector.instanceOf[AuthenticationService]
  lazy val userService: UsersService = app.injector.instanceOf[UsersService]
  lazy val groupsService: GroupsService = app.injector.instanceOf[GroupsService]

  def givenAnAuthenticatedUser(
    user: User,
    providerType: String = "GovernmentGateway",
    planetId: String = UUID.randomUUID().toString,
    affinityGroup: Option[String],
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None
  )(implicit ec: ExecutionContext): String =
    await(for {
      authSession <-
        authenticationService
          .authenticate(AuthenticateRequest(UUID.randomUUID().toString, user.userId, "any", providerType, planetId))
      mUser <- userService.findByUserId(user.userId, planetId)
      user <- mUser match {
                case Some(user) => Future.successful(user)
                case None       => userService.createUser(user, planetId, affinityGroup)
              }
      _ <- if (agentCode.isDefined || agentFriendlyName.isDefined)
             groupsService.updateGroup(
               user.groupId.get,
               planetId,
               _.copy(agentCode = agentCode, agentFriendlyName = agentFriendlyName)
             )
           else Future.successful(())
    } yield authSession)
      .getOrElse(throw new Exception("Could not sign in user"))
      .authToken

  def givenUserEnrolledFor(
    userId: String,
    planetId: String,
    service: String,
    identifierKey: String,
    identifierValue: String
  )(implicit ec: ExecutionContext): Unit = await {
    userService.findByUserId(userId, planetId).flatMap {
      case None =>
        userService.createUser(
          UserGenerator
            .individual(userId)
            .copy(assignedPrincipalEnrolments =
              Seq(EnrolmentKey(service, Seq(Identifier(identifierKey, identifierValue))))
            ),
          planetId,
          Some(AG.Individual)
        )
      case Some(existingUser) =>
        userService.updateUser(
          userId,
          planetId,
          _.copy(assignedPrincipalEnrolments =
            existingUser.assignedPrincipalEnrolments :+ EnrolmentKey(
              service,
              Seq(Identifier(identifierKey, identifierValue))
            )
          )
        )
    }
  }

  def givenUserWithStrideRole(userId: String, planetId: String, role: String)(implicit
    ec: ExecutionContext
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
