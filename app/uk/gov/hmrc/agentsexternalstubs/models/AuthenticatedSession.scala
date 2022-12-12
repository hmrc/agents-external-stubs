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

import play.api.libs.json.Format
import play.api.libs.json.Json.format
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Request, RequestHeader}

case class AuthenticatedSession(
  sessionId: String,
  userId: String,
  authToken: String,
  providerType: String,
  planetId: String,
  createdAt: Long = System.currentTimeMillis()
)

object AuthenticatedSession {

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
      rh.addAttr(TypedKey(TAG_SESSION_ID), session.sessionId)
        .addAttr(TypedKey(TAG_USER_ID), session.userId)
        .addAttr(TypedKey(TAG_AUTH_TOKEN), session.authToken)
        .addAttr(TypedKey(TAG_PROVIDER_TYPE), session.providerType)
        .addAttr(TypedKey(TAG_PLANET_ID), session.planetId)
  }

  implicit val formats: Format[AuthenticatedSession] = format[AuthenticatedSession]
}
