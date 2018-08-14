package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Format
import play.api.libs.json.Json.format
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class AuthenticatedSession(
  sessionId: String,
  userId: String,
  authToken: String,
  providerType: String,
  planetId: String)

object AuthenticatedSession extends ReactiveMongoFormats {
  implicit val formats: Format[AuthenticatedSession] = format[AuthenticatedSession]
}
