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

package uk.gov.hmrc.agentsexternalstubs.controllers.datagen

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.CurrentSession
import uk.gov.hmrc.agentsexternalstubs.models.{AG, GranPermsGenRequest, GranPermsGenResponse}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GranPermsService, UsersService}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GranPermsController @Inject() (
  usersService: UsersService,
  granPermsService: GranPermsService,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def massGenerateAgentsAndClients: Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withCurrentSession { session =>
        withPayload[GranPermsGenRequest] { genRequest =>
          usersService.findUserAndGroup(session.userId, session.planetId).flatMap {
            case (None, _)       => Future.successful(Unauthorized("No logged-in user."))
            case (Some(_), None) => Future.successful(Unauthorized("Current logged-in user is not part of a group."))
            case _ if genRequest.numberOfAgents > appConfig.granPermsTestGenMaxAgents =>
              Future.successful(BadRequest("Too many agents requested."))
            case _ if genRequest.numberOfClients > appConfig.granPermsTestGenMaxClients =>
              Future.successful(BadRequest("Too many clients requested."))
            case (Some(currentUser), mGroup) if !(mGroup.exists(_.affinityGroup == AG.Agent)) =>
              Future.successful(Unauthorized("Currently logged-in user is not an Agent."))
            case (Some(currentUser), _) if !currentUser.isAdmin =>
              Future.successful(Unauthorized("Currently logged-in user is not a group Admin."))
            case (Some(currentUser), _) if currentUser.groupId.isEmpty =>
              Future.successful(BadRequest("Currently logged-in user has no group id."))
            case (Some(currentUser), Some(group)) =>
              granPermsService
                .massGenerateAgentsAndClients(
                  planetId = session.planetId,
                  currentUser = currentUser,
                  genRequest = genRequest,
                  usersGroup = group
                )
                .map { case (createdAgents, createdClients) =>
                  Created(Json.toJson(GranPermsGenResponse(createdAgents.size, createdClients.size)))
                }
          }
        }
      }(SessionRecordNotFound)
    }

}
