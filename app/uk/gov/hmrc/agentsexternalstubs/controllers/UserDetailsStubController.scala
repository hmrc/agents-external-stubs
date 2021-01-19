/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OFormat, Writes}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Generator, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext
@Singleton
class UserDetailsStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import UserDetailsStubController._

  def getUser(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(id, session.planetId).map {
        case None       => notFound("NOT_FOUND", s"User $id details are not found")
        case Some(user) => Ok(RestfulResponse(GetUserResponse.from(user, session)))
      }
    }(SessionRecordNotFound)
  }

}

object UserDetailsStubController {

  /**
  {
    "name":"test",
    "email":"test@test.com",
    "affinityGroup" : "affinityGroup",
    "description" : "description",
    "lastName":"test",
    "dateOfBirth":"1980-06-30",
    "postCode":"NW94HD",
    "authProviderId": "12345-PID",
    "authProviderType": "Verify"
  }

    or for a gateway user that's an agent

  {
    "authProviderId" : "12345-credId",
    "authProviderType" : "GovernmentGateway",
    "name" : "test",
    "email" : "test@test.com",
    "affinityGroup" : "Agent",
    "agentCode" : "TZRXXV",
    "agentFriendlyName" : "Bodgitt & Legget LLP",
    "agentId": "BDGL",
    "credentialRole" : "admin",
    "description" : "blah"
  }
 **/
  case class GetUserResponse(
    authProviderId: String,
    authProviderType: String,
    name: String,
    email: String,
    affinityGroup: String,
    credentialRole: String,
    description: String,
    lastName: Option[String] = None,
    postCode: Option[String] = None,
    dateOfBirth: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None)

  object GetUserResponse {
    implicit val writes: Writes[GetUserResponse] = Json.writes[GetUserResponse]

    def from(user: User, session: AuthenticatedSession): GetUserResponse = GetUserResponse(
      authProviderId = user.userId,
      authProviderType = session.providerType,
      name = (if (user.affinityGroup.contains(User.AG.Individual)) user.firstName else user.name).getOrElse("John Doe"),
      lastName = if (user.affinityGroup.contains(User.AG.Individual)) user.lastName else None,
      email = Generator.email(user.userId),
      affinityGroup = user.affinityGroup.getOrElse("none"),
      agentCode = user.agentCode,
      agentFriendlyName = user.agentFriendlyName,
      agentId = user.agentId,
      credentialRole = user.credentialRole.getOrElse("User"),
      description = s"Agent Stubs test user on the planet ${user.planetId.getOrElse("?")}",
      postCode = user.address.flatMap(_.postcode),
      dateOfBirth = user.dateOfBirth.map(_.toString("yyyy-MM-dd"))
    )
  }

}
