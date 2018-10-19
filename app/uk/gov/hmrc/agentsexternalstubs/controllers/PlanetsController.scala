package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, RecordsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PlanetsController @Inject()(
  knownFactsRepository: KnownFactsRepository,
  usersRepository: UsersRepository,
  recordsRepository: RecordsRepository,
  val authenticationService: AuthenticationService,
  ecp: ExecutionContextProvider)
    extends BaseController with CurrentSession {

  implicit val ec: ExecutionContext = ecp.get()

  def destroy(planetId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      if (planetId == session.planetId) {
        Logger(getClass).info(s"About to start destroying test planet ${session.planetId} ..")
        (for {
          _ <- authenticationService.removeAuthentication(session.authToken)
          _ <- Future.sequence(Seq(
                usersRepository.destroyPlanet(session.planetId),
                recordsRepository.destroyPlanet(session.planetId),
                knownFactsRepository.destroyPlanet(session.planetId)
              ))
        } yield NoContent).recover {
          case NonFatal(e) =>
            Logger(getClass).warn(s"Attempted test planet destroy failed with $e")
            internalServerError(e.getMessage)
        }
      } else forbiddenF("PLANET_ID_NOT_MATCHED")
    }(SessionRecordNotFound)
  }

}
