package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Int = 50,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty
)

object User {
  implicit val formats: Format[User] = Json.format[User]
}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None)

object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class Identifier(key: String, value: String)

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}
