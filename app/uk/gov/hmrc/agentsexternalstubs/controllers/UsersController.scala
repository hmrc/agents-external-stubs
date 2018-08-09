package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{User, Users}
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class UsersController @Inject()(usersService: UsersService, val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

  def getUsers(affinityGroup: Option[String], limit: Option[Int]): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        usersService.findByPlanetId(session.planetId, affinityGroup)(limit.getOrElse(100)).map { users =>
          Ok(Json.toJson(Users(users)))
        }
      }(SessionRecordNotFound)
  }

  def getUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).map {
        case Some(user) => Ok(Json.toJson(user))
        case None       => NotFound(s"Could not found user $userId")
      }
    }(SessionRecordNotFound)
  }

  def updateUser(userId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withJsonBody[User](
        updatedUser =>
          usersService
            .updateUser(userId, session.planetId, _ => updatedUser)
            .map(theUser =>
              Accepted(s"User ${theUser.userId} has been updated")
                .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url))
            .recover {
              case DuplicateUserException(msg) => Conflict(msg)
              case e: NotFoundException        => NotFound(e.getMessage)
          })
    }(SessionRecordNotFound)
  }

  def createUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withJsonBody[User](
        newUser =>
          usersService
            .createUser(newUser, session.planetId)
            .map(theUser =>
              Created(s"User ${theUser.userId} has been created.")
                .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url))
            .recover {
              case DuplicateUserException(msg) => Conflict(msg)
          })
    }(SessionRecordNotFound)
  }

  def deleteUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).flatMap {
        case Some(_) => usersService.deleteUser(userId, session.planetId).map(_ => NoContent)
        case None    => Future.successful(NotFound(s"Could not found user $userId"))
      }
    }(SessionRecordNotFound)
  }

}
