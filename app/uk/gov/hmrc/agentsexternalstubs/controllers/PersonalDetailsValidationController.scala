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

import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.{Generator, PersonalDetailsValidation, PersonalDetailsWithNino}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.collection.mutable

@Singleton
class PersonalDetailsValidationController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  import uk.gov.hmrc.agentsexternalstubs.models.PersonalDetailsValidationFormat._

  private val validationIds = mutable.Map[String, Boolean]()

  def get(id: String): Action[AnyContent] = Action {
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
