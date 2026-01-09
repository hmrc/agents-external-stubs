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

package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{ApiPlatform, RegexPatterns, User, UserIdGenerator, Users}
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GroupsService, UsersService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersController @Inject() (
  usersService: UsersService,
  groupsService: GroupsService,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  val userIdFromPool = "userIdFromPool"

  def getUsers(
    affinityGroup: Option[String],
    limit: Option[Int],
    groupId: Option[String],
    agentCode: Option[String],
    principalEnrolmentService: Option[String],
    userId: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { session =>
        require(
          !(agentCode.isDefined && groupId.isDefined),
          "You cannot query users by both groupId and agentCode at the same time."
        )
        val effectiveLimit: Int =
          if (
            (userId.isDefined || principalEnrolmentService.isDefined) && (agentCode.isDefined || groupId.isDefined || affinityGroup.isDefined)
          ) {
            //        TODO: If userId/principalEnrolmentService AND either of agentCode, groupId, affinityGroup is defined, use effective/modified limit as will do filtering in futureUsers.map
            500
          } else {
            limit.getOrElse(100)
          }
        val futureUsers: Future[Seq[User]] = (userId, groupId, agentCode, affinityGroup) match {
          case (Some(uId), None, None, None) =>
            usersService.findByUserIdContains(
              partialUserId = uId,
              planetId = session.planetId
            )(effectiveLimit)
          case (_, Some(gId), _, _) =>
            usersService.findByGroupId(gId, session.planetId)(Some(effectiveLimit))
          case (_, _, Some(aCode), _) =>
            groupsService.findByAgentCode(aCode, session.planetId).flatMap {
              case Some(group) => usersService.findByGroupId(group.groupId, session.planetId)(Some(effectiveLimit))
              case None        => Future.successful(Seq.empty[User])
            }
          case (_, _, _, Some(_)) =>
            for { // TODO note that this will probably be slow. Consider whether we really want to search users by affinity group (a property that no longer pertains to User)
              groups <- groupsService.findByPlanetId(session.planetId, affinityGroup)(effectiveLimit)
              users <- Future.traverse(groups)(group =>
                         usersService.findByGroupId(group.groupId, session.planetId)(Some(effectiveLimit))
                       )
            } yield users.flatten.take(effectiveLimit)
          case _ => usersService.findByPlanetId(session.planetId)(effectiveLimit)
        }
        futureUsers.map { users =>
//          TODO: Is this place to add additive searching?? For userId (probably)
          //            TODO: Filter by userId and principal enrolment service here (when using modifiedLimit)
          val modifiedUsers = users.take(limit.getOrElse(100))
          Ok(RestfulResponse(Users(modifiedUsers)))
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
              Link("store", routes.UsersController.createUser(None).url),
              Link("list", routes.UsersController.getUsers(None, None).url)
            )(User.writes)
          )
        case None => notFound("USER_NOT_FOUND", s"Could not found user $userId")
      }
    }(SessionRecordNotFound)
  }

  def getUserForNino(nino: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns.validNinoNoSpaces(nino) match {
        case Left(_) => badRequestF("INVALID_NINO", s"Provided NINO $nino is not valid")
        case Right(_) =>
          usersService.findByNino(nino, session.planetId).map {
            case Some(user) => Ok(RestfulResponse(user)(User.writes))
            case None       => notFound("USER_NOT_FOUND", s"Could not found user for nino $nino")
          }
      }
    }(SessionRecordNotFound)
  }

  def updateCurrentUser: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      implicit val userReads =
        User.tolerantReads // tolerant Reads needed to allow enrolments without identifiers (to be populated by the sanitizer)
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
      implicit val userReads =
        User.tolerantReads // tolerant Reads needed to allow enrolments without identifiers (to be populated by the sanitizer)
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

  /** In order to create a user, the stubs platform needs to know the planet id.
    * This is either assumed from the session of the logged-in user, or supplied explicitly.
    * The value supplied explicitly, if any, takes precedence.
    */
  def createUser(affinityGroup: Option[String], planetId: Option[String] = None): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      def doCreateUser(planetId: String): Future[Result] = {
        implicit val userReads =
          User.tolerantReads // tolerant Reads needed to allow enrolments without identifiers (to be populated by the sanitizer)
        withPayload[User] { newUser =>
          usersService
            .createUser(
              newUser.copy(
                userId =
                  if (newUser.userId == null)
                    UserIdGenerator.nextUserIdFor(planetId, request.getQueryString(userIdFromPool).isDefined)
                  else newUser.userId
              ),
              planetId,
              affinityGroup
            )
            .map(theUser =>
              Created(s"User ${theUser.userId} has been created.")
                .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url)
            )
            .recover { case DuplicateUserException(msg, _) =>
              Conflict(msg)
            }
        }
      }

      withCurrentSession { session =>
        doCreateUser(planetId.getOrElse(session.planetId))
      }(ifSessionNotFound = planetId match {
        case Some(planetId) => doCreateUser(planetId)
        case None =>
          Future.successful(BadRequest("Cannot create user without either an active session or a specified planetId"))
      })
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
      withPayload[ApiPlatform.TestUser] { testUser =>
        val (user, group) = ApiPlatform.TestUser.asUserAndGroup(testUser)
        (for {
          createdGroup <- groupsService.createGroup(group, planetId)
          createdUser  <- usersService.createUser(user, planetId, Some(testUser.affinityGroup))
        } yield Created(s"API Platform test user ${createdUser.userId} has been created on the planet $planetId")
          .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(createdUser.userId).url)).recover {
          case DuplicateUserException(msg, _) =>
            Conflict(msg)
        }
      }
    }
  }

  def reindexAllUsers(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { _ =>
      usersService.reindexAllUsers.map(result => Ok(result.toString))
    }(SessionRecordNotFound)
  }
}
