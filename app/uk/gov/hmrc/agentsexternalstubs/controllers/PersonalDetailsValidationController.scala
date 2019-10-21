package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.{Generator, PersonalDetailsValidation, PersonalDetailsWithNino}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.collection.mutable
import scala.concurrent.ExecutionContext

@Singleton
class PersonalDetailsValidationController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  import uk.gov.hmrc.agentsexternalstubs.models.PersonalDetailsValidationFormat._

  private val validationIds = mutable.Map[String, Boolean]()

  def get(id: String): Action[AnyContent] = Action { implicit request =>
    Ok(toJson(personalDetailsValidationRepository.get(id)))
  }

  // associate a result (success/failure) with a validation id
  def pdvResult(id: String, success: Boolean): Action[AnyContent] = Action {
    validationIds(id) = success
    Ok
  }

  object personalDetailsValidationRepository {
    def get(id: String): PersonalDetailsValidation =
      if (validationIds.getOrElse(id, false))
        PersonalDetailsValidation
          .successful(id, PersonalDetailsWithNino("Fred", "Bloggs", LocalDate.now(), Generator.ninoNoSpaces(id)))
      else
        PersonalDetailsValidation.failed(id)
  }

}
