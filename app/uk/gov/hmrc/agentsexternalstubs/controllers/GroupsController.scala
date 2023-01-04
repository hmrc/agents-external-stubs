/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{Group, GroupGenerator, Groups}
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateGroupException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GroupsService, UsersService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class GroupsController @Inject() (
  groupsService: GroupsService,
  usersService: UsersService,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  val groupIdFromPool = "groupIdFromPool"

  def getGroups(affinityGroup: Option[String], limit: Option[Int], agentCode: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { session =>
        (if (agentCode.isDefined)
           groupsService.findByAgentCode(agentCode.get, session.planetId).map(_.toSeq)
         else
           groupsService.findByPlanetId(session.planetId, affinityGroup)(limit.getOrElse(100))).map(groups =>
          Ok(RestfulResponse(Groups(groups)))
        )
      }(SessionRecordNotFound)
    }

  def getGroup(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService.findByGroupId(groupId, session.planetId).map {
        case Some(group) =>
          Ok(
            RestfulResponse(
              group,
              Link("update", routes.GroupsController.updateGroup(groupId).url),
              Link("delete", routes.GroupsController.deleteGroup(groupId).url),
              Link("store", routes.GroupsController.createGroup.url),
              Link("list", routes.GroupsController.getGroups(None, None).url)
            )(Group.format)
          )
        case None => notFound("GROUP_NOT_FOUND", s"Could not find group $groupId")
      }
    }(SessionRecordNotFound)
  }

  def updateCurrentGroup: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[Group](updatedGroup =>
        for {
          maybeCurrentUser <- usersService.findByUserId(session.userId, session.planetId)
          maybeCurrentGroupId = maybeCurrentUser.flatMap(_.groupId)
          result <- maybeCurrentGroupId match {
                      case Some(groupId) =>
                        groupsService
                          .updateGroup(groupId, session.planetId, _ => updatedGroup)
                          .map(theGroup =>
                            Accepted(s"Current group ${theGroup.groupId} has been updated")
                              .withHeaders(
                                HeaderNames.LOCATION -> routes.GroupsController.getGroup(theGroup.groupId).url
                              )
                          )
                          .recover {
                            case DuplicateGroupException(msg, _) => Conflict(msg)
                            case e: NotFoundException            => notFound("GROUP_NOT_FOUND", e.getMessage)
                          }
                      case None => Future.successful(notFound("GROUP_NOT_FOUND"))
                    }
        } yield result
      )
    }(SessionRecordNotFound)
  }

  def updateGroup(groupId: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[Group] { updatedGroup =>
        groupsService
          .updateGroup(groupId, session.planetId, _ => updatedGroup)
          .map(theGroup =>
            Accepted(s"Group ${theGroup.groupId} has been updated")
              .withHeaders(HeaderNames.LOCATION -> routes.GroupsController.getGroup(theGroup.groupId).url)
          )
          .recover {
            case DuplicateGroupException(msg, _) => Conflict(msg)
            case e: NotFoundException            => notFound("GROUP_NOT_FOUND", e.getMessage)
          }
      }
    }(SessionRecordNotFound)
  }

  def createGroup: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[Group] { group =>
        groupsService
          .createGroup(
            group.copy(
              groupId = if (group.groupId == null) {
                val randomSeed = session.sessionId + Instant.now().hashCode() + Random.nextString(length = 20)
                GroupGenerator.groupId(seed = randomSeed)
              } else group.groupId
            ),
            session.planetId
          )
          .map { case theGroup =>
            Created(
              s"Group ${theGroup.groupId} has been created."
            )
              .withHeaders(HeaderNames.LOCATION -> routes.GroupsController.getGroup(theGroup.groupId).url)
          }
          .recover { case DuplicateGroupException(msg, _) =>
            Conflict(msg)
          }
      }
    }(SessionRecordNotFound)
  }

  def deleteGroup(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      groupsService.findByGroupId(groupId, session.planetId).flatMap {
        case Some(_) => groupsService.deleteGroup(groupId, session.planetId).map(_ => NoContent)
        case None    => notFoundF("GROUP_NOT_FOUND", s"Could not find group $groupId")
      }
    }(SessionRecordNotFound)
  }

  def reindexAllGroups(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { _ =>
      groupsService.reindexAllGroups.map(result => Ok(result.toString))
    }(SessionRecordNotFound)
  }
}
