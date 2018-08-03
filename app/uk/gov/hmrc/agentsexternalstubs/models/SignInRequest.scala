package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class SignInRequest(userId: String, plainTextPassword: String, providerType: String, planetId: String)

object SignInRequest {
  implicit val formats: Format[SignInRequest] = Json.format[SignInRequest]
}
