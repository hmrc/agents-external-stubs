package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.{NinoClStoreEntry, User}
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationController @Inject()(
  cc: ControllerComponents,
  usersService: UsersService,
  val authenticationService: AuthenticationService)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  private def addNinoToUser(nino: Nino)(user: User): User = user.copy(nino = Some(nino))

  def storeNino(credId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[NinoClStoreEntry] { entry =>
      if (entry.credId != credId)
        Future.successful(BadRequest)
      else {
        withCurrentSession { session =>
          usersService
            .updateUser(session.userId, session.planetId, addNinoToUser(entry.nino))
            .map(theUser => Created(s"Current user ${theUser.userId} has been updated"))
            .recover {
              case DuplicateUserException(msg, _) => Conflict(msg)
              case e: NotFoundException           => notFound("USER_NOT_FOUND", e.getMessage)
            }
        }(SessionRecordNotFound)
      }
    }
  }

}
