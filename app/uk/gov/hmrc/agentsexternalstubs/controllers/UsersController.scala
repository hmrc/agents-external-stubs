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
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{ApiPlatform, User, UserIdGenerator, Users}
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class UsersController @Inject() (
  usersService: UsersService,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  val userIdFromPool = "userIdFromPool"

  def getUsers(affinityGroup: Option[String], limit: Option[Int], agentCode: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { session =>
        (if (agentCode.isDefined)
           usersService
             .findByAgentCode(agentCode.get, session.planetId)(limit.getOrElse(100))
         else
           usersService
             .findByPlanetId(session.planetId, affinityGroup)(limit.getOrElse(100))).map { users =>
          Ok(RestfulResponse(Users(users)))
        }
      }(SessionRecordNotFound)
    }

  def getUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).map {
        case Some(user) =>
          Ok(
            RestfulResponse(
              user,
              Link("update", routes.UsersController.updateUser(userId).url),
              Link("delete", routes.UsersController.deleteUser(userId).url),
              Link("store", routes.UsersController.createUser().url),
              Link("list", routes.UsersController.getUsers(None, None).url)
            )(User.writes)
          )
        case None => notFound("USER_NOT_FOUND", s"Could not found user $userId")
      }
    }(SessionRecordNotFound)
  }

  def updateCurrentUser: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[User](updatedUser =>
        usersService
          .updateUser(session.userId, session.planetId, _ => updatedUser)
          .map(theUser =>
            Accepted(s"Current user ${theUser.userId} has been updated")
              .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url)
          )
          .recover {
            case DuplicateUserException(msg, _) => Conflict(msg)
            case e: NotFoundException           => notFound("USER_NOT_FOUND", e.getMessage)
          }
      )
    }(SessionRecordNotFound)
  }

  def updateUser(userId: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[User] { updatedUser =>
        usersService
          .updateUser(userId, session.planetId, _ => updatedUser)
          .map(theUser =>
            Accepted(s"User ${theUser.userId} has been updated")
              .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url)
          )
          .recover {
            case DuplicateUserException(msg, _) => Conflict(msg)
            case e: NotFoundException           => notFound("USER_NOT_FOUND", e.getMessage)
          }
      }
    }(SessionRecordNotFound)
  }

  def createUser(): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[User](newUser =>
        usersService
          .createUser(
            newUser.copy(
              userId =
                if (newUser.userId == null)
                  UserIdGenerator.nextUserIdFor(session.planetId, request.getQueryString(userIdFromPool).isDefined)
                else newUser.userId
            ),
            session.planetId
          )
          .map(theUser =>
            Created(s"User ${theUser.userId} has been created.")
              .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url)
          )
          .recover { case DuplicateUserException(msg, _) =>
            Conflict(msg)
          }
      )
    }(SessionRecordNotFound)
  }

  def deleteUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).flatMap {
        case Some(_) => usersService.deleteUser(userId, session.planetId).map(_ => NoContent)
        case None    => notFoundF("USER_NOT_FOUND", s"Could not found user $userId")
      }
    }(SessionRecordNotFound)
  }

  def createApiPlatformTestUser(): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withMaybeCurrentSession { maybeSession =>
      val planetId = CurrentPlanetId(maybeSession, request)
      withPayload[ApiPlatform.TestUser](testUser =>
        usersService
          .createUser(ApiPlatform.TestUser.asUser(testUser), planetId)
          .map(theUser =>
            Created(s"API Platform test user ${theUser.userId} has been created on the planet $planetId")
              .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url)
          )
          .recover { case DuplicateUserException(msg, _) =>
            Conflict(msg)
          }
      )
    }
  }

  def reindexAllUsers(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { _ =>
      usersService.reindexAllUsers.map(msg => Ok(msg))
    }(SessionRecordNotFound)
  }

}
