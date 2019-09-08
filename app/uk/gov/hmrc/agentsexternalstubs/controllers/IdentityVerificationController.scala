package uk.gov.hmrc.agentsexternalstubs.controllers
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Request, Result}
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.JourneyCreation
import uk.gov.hmrc.agentsexternalstubs.repository.JourneyIvRepository
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class IdentityVerificationController @Inject()(journeyIvRepository: JourneyIvRepository, cc: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def createJourney = Action.async(parse.json) { implicit request =>
    withJsonBody[JourneyCreation] { journeyCreation =>
      journeyIvRepository.createJourneyId(journeyCreation).map { journeyId =>
        Created(Json.obj("journeyId" -> journeyId))
      }
    }
  }

  def getJourney(journeyId: String) = Action.async { implicit request =>
    journeyIvRepository.getJourneyInfo(journeyId).map {
      case Some(jd) => Ok(Json.toJson(jd))
      case _        => NotFound
    }
  }

  def deleteJourney(journeyId: String) = Action.async {
    journeyIvRepository.deleteJourneyRecord(journeyId).map(_ => NoContent)
  }
}
