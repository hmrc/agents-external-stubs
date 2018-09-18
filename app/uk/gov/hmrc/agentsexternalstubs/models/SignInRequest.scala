package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class SignInRequest(
  userId: Option[String],
  plainTextPassword: Option[String],
  providerType: Option[String],
  planetId: Option[String])

object SignInRequest {
  implicit val formats: Format[SignInRequest] = Json.format[SignInRequest]
}
