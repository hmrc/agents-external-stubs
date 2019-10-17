package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.{Generator, PersonalDetailsValidation, PersonalDetailsWithNino}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class PersonalDetailsValidationController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  import uk.gov.hmrc.agentsexternalstubs.models.PersonalDetailsValidationFormat._

  def get(id: String): Action[AnyContent] = Action { implicit request =>
    Ok(toJson(personalDetailsValidationRepository.get(id)))
  }

  object personalDetailsValidationRepository {
    def get(id: String): PersonalDetailsValidation =
      PersonalDetailsValidation
        .successful(id, PersonalDetailsWithNino("Fred", "Bloggs", LocalDate.now(), Generator.ninoNoSpaces(id)))
  }

}
