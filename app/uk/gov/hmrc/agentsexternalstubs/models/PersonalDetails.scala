/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

trait PersonalDetails {
  def toJson: JsObject
}

trait PersonalDetailsNino {
  def nino: Nino
}

trait PersonalDetailsPostCode {
  def postCode: String
}

object PersonalDetails {
  implicit val implicitPersonalDetailsWrite: Writes[PersonalDetails] = new Writes[PersonalDetails] {
    override def writes(details: PersonalDetails): JsValue =
      details.toJson
  }
  implicit val withNinoFormats: Format[PersonalDetailsWithNino] = Json.format
}

case class PersonalDetailsWithNino(firstName: String, lastName: String, dateOfBirth: LocalDate, nino: Nino)
    extends PersonalDetails with PersonalDetailsNino {
  lazy val toJson: JsObject = Json.obj(
    "firstName"   -> firstName,
    "lastName"    -> lastName,
    "dateOfBirth" -> dateOfBirth,
    "nino"        -> nino
  )
}
