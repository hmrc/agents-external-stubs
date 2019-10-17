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
