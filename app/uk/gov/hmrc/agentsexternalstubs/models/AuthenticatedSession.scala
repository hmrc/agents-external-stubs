package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Format
import play.api.libs.json.Json.format
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class AuthenticatedSession(
  sessionId: String,
  userId: String,
  authToken: String,
  providerType: String,
  planetId: String,
  createdAt: Long = System.currentTimeMillis())

object AuthenticatedSession extends ReactiveMongoFormats {

  val TAG_SESSION_ID = "AuthenticatedSession-Planet-ID"
  val TAG_USER_ID = "AuthenticatedSession-User-ID"
  val TAG_AUTH_TOKEN = "AuthenticatedSession-Auth-Token"
  val TAG_PROVIDER_TYPE = "AuthenticatedSession-Provider-Type"
  val TAG_PLANET_ID = "AuthenticatedSession-Planet-ID"

  def fromRequest[T](request: Request[T]): Option[AuthenticatedSession] =
    for {
      sessionId    <- request.headers.get(TAG_SESSION_ID)
      userId       <- request.headers.get(TAG_USER_ID)
      authToken    <- request.headers.get(TAG_AUTH_TOKEN)
      providerType <- request.headers.get(TAG_PROVIDER_TYPE)
      planetId     <- request.headers.get(TAG_PLANET_ID)
    } yield AuthenticatedSession(sessionId, userId, authToken, providerType, planetId)

  def tagRequest(rh: RequestHeader, maybeSession: Option[AuthenticatedSession]): RequestHeader = maybeSession match {
    case None => rh
    case Some(session) =>
      rh.withTag(TAG_SESSION_ID, session.sessionId)
        .withTag(TAG_USER_ID, session.userId)
        .withTag(TAG_AUTH_TOKEN, session.authToken)
        .withTag(TAG_PROVIDER_TYPE, session.providerType)
        .withTag(TAG_PLANET_ID, session.planetId)
  }

  implicit val formats: Format[AuthenticatedSession] = format[AuthenticatedSession]
}
