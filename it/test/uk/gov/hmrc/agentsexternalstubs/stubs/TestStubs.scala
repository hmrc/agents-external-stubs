/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.stubs

import org.scalatest.Suite
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.User.CR
import uk.gov.hmrc.agentsexternalstubs.models._
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
    identifierValue: String,
    credRole: String = CR.User
  )(implicit ec: ExecutionContext): Unit = await {
    userService.findByUserId(userId, planetId).flatMap {
      case None =>
        userService.createUser(
          UserGenerator
            .individual(userId, credentialRole = credRole)
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
          _.copy(
            assignedPrincipalEnrolments = existingUser.assignedPrincipalEnrolments :+ EnrolmentKey(
              service,
              Seq(Identifier(identifierKey, identifierValue))
            ),
            credentialRole = Some(credRole)
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
