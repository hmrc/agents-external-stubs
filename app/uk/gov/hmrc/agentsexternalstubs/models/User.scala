package uk.gov.hmrc.agentsexternalstubs.models

import cats.data.{NonEmptyList, Validated}
import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  planetId: Option[String] = None,
  isNonStandardUser: Option[Boolean] = None
)

object User {

  object AG {
    final val Individual = "Individual"
    final val Organisation = "Organisation"
    final val Agent = "Agent"
  }

  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: Writes[User] = Json
    .writes[User]
    .transform(addNormalizedUserIndexKey _)
    .transform(addNormalizedNinoIndexKey _)

  val formats = Format(reads, writes)

  val user_index_key = "user_index_key"
  val nino_index_key = "nino_index_key"

  def userIndexKey(userId: String, planetId: String): String = s"$userId@$planetId"
  def ninoIndexKey(nino: String, planetId: String): String = s"${nino.replace(" ", "")}@$planetId"

  private def addNormalizedUserIndexKey(json: JsObject): JsObject = {
    val userId = (json \ "userId").as[String]
    val planetId = (json \ "planetId").asOpt[String].getOrElse("hmrc")
    json + (user_index_key, JsString(userIndexKey(userId, planetId)))
  }

  private def addNormalizedNinoIndexKey(json: JsObject): JsObject =
    (json \ "nino")
      .asOpt[String]
      .map(nino => {
        val planetId = (json \ "planetId").asOpt[String].getOrElse("hmrc")
        json + (nino_index_key, JsString(ninoIndexKey(nino, planetId)))
      })
      .getOrElse(json)

  def validate(user: User): Validated[NonEmptyList[String], Unit] = UserValidator.validate(user)

}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None)

object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class Identifier(key: String, value: String)

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}
