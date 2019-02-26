package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PlanetsController @Inject()(
  knownFactsRepository: KnownFactsRepository,
  usersRepository: UsersRepository,
  recordsRepository: RecordsRepository,
  specialCasesRepository: SpecialCasesRepository,
  authSessionRepository: AuthenticatedSessionsRepository)(implicit ec: ExecutionContext)
    extends BaseController {

  def destroy(planetId: String): Action[AnyContent] = Action.async { implicit request =>
    Logger(getClass).info(s"About to start destroying test planet $planetId ..")
    Future
      .sequence(Seq(
        authSessionRepository.destroyPlanet(planetId),
        usersRepository.destroyPlanet(planetId),
        recordsRepository.destroyPlanet(planetId),
        knownFactsRepository.destroyPlanet(planetId),
        specialCasesRepository.destroyPlanet(planetId)
      ))
      .map(
        _ => NoContent
      )
      .recover {
        case NonFatal(e) =>
          Logger(getClass).warn(s"Attempted test planet destroy failed with $e")
          InternalServerError(e.getMessage)
      }
  }

}
