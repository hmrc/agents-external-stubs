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

package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Planet}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.agentsexternalstubs.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait CurrentSession extends HttpHelpers with RequestAwareLogging {

  def authenticationService: AuthenticationService

  final val errorHandler: PartialFunction[Throwable, Result] = {
    case e: NotFoundException      => notFound("NOT_FOUND", e.getMessage)
    case e: BadRequestException    => badRequest("BAD_REQUEST", e.getMessage)
    case e: HttpException          => Results.Status(e.responseCode)(errorMessage("SERVER_ERROR", Some(e.getMessage)))
    case e: AuthorisationException => forbidden(e.getMessage)
    case NonFatal(e)               =>
      e.printStackTrace()
      internalServerError("SERVER_ERROR", e.getMessage)
  }

  final def withMaybeCurrentSession[T, R](
    body: Option[AuthenticatedSession] => Future[R]
  )(using request: Request[T], ec: ExecutionContext, hc: HeaderCarrier): Future[R] =
    AuthenticatedSession.fromRequest(request) match {
      case s @ Some(_) => body(s)
      case None        =>
        for {
          maybeSession1 <- request.headers.get(HeaderNames.AUTHORIZATION) match {
                             case Some(BearerToken(authToken)) =>
                               authenticationService.findByAuthTokenOrLookupExternal(authToken)
                             case _ =>
                               Future.successful(None)
                           }
          maybeSession2 <- maybeSession1 match {
                             case None =>
                               request.headers.get(uk.gov.hmrc.http.HeaderNames.xSessionId) match {
                                 case Some(sessionId) =>
                                   authenticationService.findBySessionId(sessionId)
                                 case None =>
                                   Future.successful(None)
                               }
                             case some => Future.successful(some)
                           }
          result <- body(maybeSession2)
        } yield result
    }

  final def withMaybeCurrentSessionInCache[R](
    body: Option[AuthenticatedSession] => Future[R]
  )(using request: RequestHeader, ec: ExecutionContext): Future[R] =
    request.headers.get(uk.gov.hmrc.http.HeaderNames.xSessionId) match {
      case Some(sessionId) =>
        authenticationService.findBySessionId(sessionId).flatMap(body)
      case None =>
        body(None)
    }

  def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result]
  )(using request: Request[T], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = withMaybeCurrentSession {
    case Some(session) => body(session)
    case None          => ifSessionNotFound
  }

}

/*
     The main purpose of this is not for auth, but to link requests to the relevant planetId.
     We do not actually deal with auth for external APIs.

     When stubbing DES request we can't just rely on the `Authorization` header
     because it is not the same token value issued by MTDP Auth,
     instead we have to exploit fact that `HeaderCarrierConverter` copy over sessionId
     from HTTP session as `X-Session-ID` header and lookup session by its ID.
 */
trait ExternalCurrentSession extends DesHttpHelpers {

  def authenticationService: AuthenticationService

  case class DesErrorResponse(code: String, reason: Option[String])
  object DesErrorResponse {
    given writes: Writes[DesErrorResponse] = Json.writes[DesErrorResponse]
  }

  override def errorMessage(code: String, reason: Option[String]): JsValue =
    Json.toJson(DesErrorResponse(code, reason))

  val errorHandler: PartialFunction[Throwable, Result] = {
    case e: NotFoundException      => notFound("NOT_FOUND", e.getMessage)
    case e: BadRequestException    => badRequest("BAD_REQUEST", e.getMessage)
    case e: HttpException          => Results.Status(e.responseCode)(errorMessage("SERVER_ERROR", Some(e.getMessage)))
    case e: AuthorisationException => forbidden(e.getMessage)
    case NonFatal(e)               => internalServerError("SERVER_ERROR", e.getMessage)
  }

  final def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result]
  )(using request: Request[T], ec: ExecutionContext): Future[Result] = {
    // When DES request originates from an API gateway (no X-Session-ID at all) - fall back
    // to whatever session currently exists on the default planet before finally giving up
    // via ifSessionNotFound (some controllers use that slot to attempt a further, more
    // precise global-by-identifier lookup instead of failing outright - see
    // RecordsService.getRecordAnyPlanet, used by HipStubController and
    // EnrolmentStoreProxyStubController). Deliberately tried in this order, default planet
    // first: it's backward compatible with callers that already worked this way, and it
    // avoids the global lookup landing on some unrelated caller's planet by coincidence
    // when the default planet would have resolved things correctly anyway.
    def fallBackToDefaultPlanet(): Future[Result] = {
      val planetId = CurrentPlanetId(None)
      authenticationService.findByPlanetId(planetId).flatMap {
        case Some(session) =>
          body(session)
        case _ =>
          Logger(getClass).warn(
            s"AuthenticatedSession for planetId=$planetId not found, cannot continue to DES stubs"
          )
          ifSessionNotFound
      }
    }

    (request.headers.get(uk.gov.hmrc.http.HeaderNames.xSessionId) match {
      case Some(sessionId) =>
        // When a DES request comes from an authenticated UI session, this code looks up
        // the session using the provided session ID. If the session ID is invalid, expired,
        // or not found, it does NOT fall back to the default planet. This is intentional:
        // an invalid session ID means something went wrong (user logged out, session expired,
        // or wrong ID), which is different from having no session ID at all (which typically
        // happens when a backend microservice makes a call from a scheduler, where there's
        // no user request and thus no session ID is possible). Using the default planet's
        // session when given an invalid ID could return data belonging to a completely
        // different user, which is dangerous. Instead, it goes to ifSessionNotFound, which
        // either tries other lookup methods (like finding by safeId/arn) or fails safely.
        // This approach prevents accidentally serving the wrong user's data.
        authenticationService.findBySessionId(sessionId).flatMap {
          case Some(session) =>
            body(session)
          case _ =>
            Logger(getClass).warn(
              s"AuthenticatedSession for sessionId=$sessionId not found, cannot continue to DES stubs"
            )
            ifSessionNotFound
        }
      case None =>
        fallBackToDefaultPlanet()
    }).recover(errorHandler)
  }

}

object CurrentPlanetId {

  def apply(maybeSession: Option[AuthenticatedSession]): String =
    maybeSession match {
      case Some(session) => session.planetId
      case None          => Planet.DEFAULT

    }

}
