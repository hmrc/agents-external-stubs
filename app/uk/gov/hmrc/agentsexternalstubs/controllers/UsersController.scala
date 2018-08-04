package uk.gov.hmrc.agentsexternalstubs.controllers

import cats.data.Validated.{Invalid, Valid}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import play.mvc.Http.HeaderNames
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class UsersController @Inject()(usersService: UsersService, val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

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
          (for {
            _ <- validateUser(updatedUser)
            _ <- usersService.updateUser(userId, session.planetId, _ => updatedUser)
          } yield
            Accepted(s"User $userId has been updated").withHeaders(
              HeaderNames.LOCATION -> routes.UsersController.getUser(userId).url))
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
          (for {
            _       <- validateUser(newUser)
            theUser <- usersService.createUser(newUser, session.planetId)
          } yield
            Created(s"User ${theUser.userId} has been created.")
              .withHeaders(HeaderNames.LOCATION -> routes.UsersController.getUser(theUser.userId).url))
            .recover {
              case DuplicateUserException(msg) => Conflict(msg)
          })
    }(SessionRecordNotFound)

  }

  protected def validateUser(user: User): Future[Unit] =
    User.validate(user) match {
      case Valid(_)        => Future.successful(())
      case Invalid(errors) => Future.failed(new BadRequestException(errors.toList.mkString(", ")))
    }

}
