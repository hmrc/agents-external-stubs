/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.UsersGroupsSearchStubController.{GetGroupResponse, GetUserResponse}
import uk.gov.hmrc.agentsexternalstubs.models.{AG, Generator, Group, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GroupsService, UsersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersGroupsSearchStubController @Inject() (
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  groupsService: GroupsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      for {
        maybeUser <- usersService.findByUserId(userId, session.planetId)
        maybeGroup <- maybeUser.fold(Future.successful(Option.empty[Group]))(user =>
                        groupsService.findByGroupId(user.groupId.getOrElse(""), session.planetId)
                      )
      } yield (maybeUser, maybeGroup) match {
        case (Some(user), Some(group)) =>
          NonAuthoritativeInformation(RestfulResponse(GetUserResponse.from(user, group)))
        case _ => notFound("USER_NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def getGroup(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService
        .findByGroupId(groupId, session.planetId)
        .map(s =>
          s.find(_.affinityGroup == AG.Agent)
            .orElse(s.find(_.affinityGroup == AG.Organisation)) match { // TODO! If the group is individual, return 404! Why this behaviour?
            case Some(group) =>
              NonAuthoritativeInformation(
                RestfulResponse(
                  GetGroupResponse.from(group),
                  Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(groupId).url)
                )
              )
            case None => notFound("GROUP_NOT_FOUND")
          }
        )
    }(SessionRecordNotFound)
  }

  def getGroupUsers(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    def addEmailFieldInUserJson(user: User): JsObject =
      Json.toJson(user).as[JsObject] + ("email" -> Json.toJson(Generator.email(user.userId)))

    withCurrentSession { session =>
      if (groupId == "wrongGroupId") {
        Future.successful(notFound("GROUP_NOT_FOUND"))
      } else {
        usersService
          .findByGroupId(groupId, session.planetId)(100)
          .map {
            case users if users.isEmpty =>
              notFound("GROUP_NOT_FOUND")
            case users =>
              NonAuthoritativeInformation(JsArray(users.map(addEmailFieldInUserJson)))
          }
      }
    }(SessionRecordNotFound)
  }

  def getGroupByAgentCode(agentCode: String, agentId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService
        .findByAgentCode(agentCode, session.planetId)
        .map(s =>
          s.find(_.affinityGroup == AG.Agent).orElse(s.find(_.affinityGroup == AG.Organisation)) match {
            case Some(group) =>
              NonAuthoritativeInformation(
                RestfulResponse(
                  GetGroupResponse.from(group),
                  Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(group.groupId).url)
                )
              )
            case None => notFound("GROUP_NOT_FOUND")
          }
        )
    }(SessionRecordNotFound)
  }

}

object UsersGroupsSearchStubController {

  /** {
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
    affinityGroup: String = AG.Individual,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None,
    credentialRole: Option[String] = None,
    description: Option[String] = None,
    groupId: Option[String] = None,
    owningUserId: Option[String] = None
  )

  object GetUserResponse {
    implicit val writes: Writes[GetUserResponse] = Json.writes[GetUserResponse]

    def from(user: User, group: Group): GetUserResponse =
      GetUserResponse(
        name = user.name.getOrElse("John Doe"),
        userId = Some(user.userId),
        affinityGroup = group.affinityGroup,
        agentCode = group.agentCode,
        agentFriendlyName = group.agentFriendlyName,
        agentId = group.agentId,
        credentialRole = user.credentialRole,
        groupId = user.groupId
      )
  }

  /** {
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
    groupId: String,
    affinityGroup: String,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None
  )

  object GetGroupResponse {
    implicit val writes: Writes[GetGroupResponse] = Json.writes[GetGroupResponse]

    def from(group: Group): GetGroupResponse =
      GetGroupResponse(
        groupId = group.groupId,
        affinityGroup = group.affinityGroup,
        agentCode = group.agentCode,
        agentFriendlyName = group.agentFriendlyName,
        agentId = group.agentId
      )
  }

}
