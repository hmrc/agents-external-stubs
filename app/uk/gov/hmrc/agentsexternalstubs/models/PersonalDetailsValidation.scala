package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait PersonalDetailsValidation {
  val id: String
}

case class SuccessfulPersonalDetailsValidation(id: String, personalDetails: PersonalDetails)
    extends PersonalDetailsValidation

case class FailedPersonalDetailsValidation(id: String) extends PersonalDetailsValidation

object PersonalDetailsValidation {

  def successful(id: String, personalDetails: PersonalDetails) =
    SuccessfulPersonalDetailsValidation(id, personalDetails)

  def failed(id: String) = FailedPersonalDetailsValidation(id)
}

object PersonalDetailsValidationFormat {

  implicit val personalDetailsValidationFormats: Format[PersonalDetailsValidation] = {

    implicit class JsonOps(json: JsValue) {

      lazy val toSuccessfulPersonalDetailsValidation: JsResult[SuccessfulPersonalDetailsValidation] = (
        (json \ "id").validate[String] and
          (json \ "personalDetails").validate[PersonalDetailsWithNino]
      )((id, pd) => SuccessfulPersonalDetailsValidation(id, pd))

      lazy val toFailedPersonalDetailsValidation: JsResult[FailedPersonalDetailsValidation] =
        (json \ "id")
          .validate[String]
          .map(id => FailedPersonalDetailsValidation(id))
    }

    val reads: Reads[PersonalDetailsValidation] = Reads[PersonalDetailsValidation] { json =>
      (json \ "validationStatus").validate[String] flatMap {
        case "success" => json.toSuccessfulPersonalDetailsValidation
        case "failure" => json.toFailedPersonalDetailsValidation
      }
    }

    val writes: Writes[PersonalDetailsValidation] = Writes[PersonalDetailsValidation] {
      case SuccessfulPersonalDetailsValidation(id, personalDetails: PersonalDetails) =>
        Json.obj(
          "id"               -> id,
          "validationStatus" -> "success",
          "personalDetails"  -> personalDetails
        )
      case FailedPersonalDetailsValidation(id) =>
        Json.obj(
          "id"               -> id,
          "validationStatus" -> "failure"
        )
    }

    Format(reads, writes)
  }
}
