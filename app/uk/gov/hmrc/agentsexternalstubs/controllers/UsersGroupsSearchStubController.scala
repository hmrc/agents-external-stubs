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
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.UsersGroupsSearchStubController.{GetGroupResponse, GetUserResponse}
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class UsersGroupsSearchStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).map {
        case Some(user) => NonAuthoritativeInformation(RestfulResponse(GetUserResponse.from(user)))
        case None       => notFound("USER_NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def getGroup(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByGroupId(groupId, session.planetId)(100)
        .map(s =>
          s.find(_.isAgent).orElse(s.find(_.isOrganisation)).orElse(s.headOption) match {
            case Some(user) =>
              NonAuthoritativeInformation(
                RestfulResponse(
                  GetGroupResponse.from(groupId, user),
                  Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(groupId).url)))
            case None => notFound("GROUP_NOT_FOUND")
        })
    }(SessionRecordNotFound)
  }

  def getGroupUsers(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByGroupId(groupId, session.planetId)(100)
        .map {
          case users if users.isEmpty =>
            notFound("GROUP_NOT_FOUND")
          case users =>
            NonAuthoritativeInformation(RestfulResponse(users))
        }
    }(SessionRecordNotFound)
  }

  def getGroupByAgentCode(agentCode: String, agentId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByAgentCode(agentCode, session.planetId)(100)
        .map(s =>
          s.find(_.isAgent).orElse(s.find(_.isOrganisation)).orElse(s.headOption) match {
            case Some(user) =>
              NonAuthoritativeInformation(RestfulResponse(
                GetGroupResponse.from(user.groupId.getOrElse(""), user),
                Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(user.groupId.getOrElse("")).url)
              ))
            case None => notFound("GROUP_NOT_FOUND")
        })
    }(SessionRecordNotFound)
  }

}

object UsersGroupsSearchStubController {

  /**
    * {
    *     "userId": ":userId",
    *     "name": "Subscribed MTD Agent",
    *     "email": "default@email.com",
    *     "affinityGroup": "Agent",
    *     "agentCode": "LMNOPQ234568",
    *     "agentFriendlyName": "MTD Agency",
    *     "agentId": "?",
    *     "credentialRole": "User",
    *     "description": "ManualUserCreation",
    *     "groupId": "04389535-78F7-4213-9169-FD0DD3553731"
    * }
    */
  case class GetUserResponse(
    name: String,
    userId: Option[String] = None,
    email: Option[String] = None,
    affinityGroup: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None,
    credentialRole: Option[String] = None,
    description: Option[String] = None,
    groupId: Option[String] = None,
    owningUserId: Option[String] = None)

  object GetUserResponse {
    implicit val writes: Writes[GetUserResponse] = Json.writes[GetUserResponse]

    def from(user: User): GetUserResponse =
      GetUserResponse(
        name = user.name.getOrElse("John Doe"),
        userId = Some(user.userId),
        affinityGroup = user.affinityGroup,
        agentCode = user.agentCode,
        agentFriendlyName = user.agentFriendlyName,
        agentId = user.agentId,
        credentialRole = user.credentialRole,
        groupId = user.groupId
      )
  }

  /**
    * {
    *   "_links": [
    *     { "rel": "users", "href": "/groups/:groupdId/users" }
    *   ],
    *   "groupId": ":groupId",
    *   "affinityGroup": "Agent",
    *   "agentCode": "NQJUEJCWT14",
    *   "agentFriendlyName": "JoeBloggs",
    *   "agentId": "?" //missing in GsoAdminGetUserDetailsByGroupId
    * }
    */
  case class GetGroupResponse(
    groupId: Option[String] = None,
    affinityGroup: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None)

  object GetGroupResponse {
    implicit val writes: Writes[GetGroupResponse] = Json.writes[GetGroupResponse]

    def from(groupId: String, user: User): GetGroupResponse =
      GetGroupResponse(
        groupId = Some(groupId),
        affinityGroup = user.affinityGroup,
        agentCode = user.agentCode,
        agentFriendlyName = user.agentFriendlyName,
        agentId = user.agentId)
  }

}
