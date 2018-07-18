package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class User(
  userId: String,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  affinityGroup: Option[String] = None,
  confidenceLevel: Int = 50,
  credentialStrength: Option[String] = None
)

object User {
  implicit val formats: Format[User] = Json.format[User]
}
