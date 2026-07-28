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

import java.net.URLDecoder
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Reads, Writes}
import play.api.libs.streams.Accumulator
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Id, SpecialCase}
import uk.gov.hmrc.agentsexternalstubs.repository.SpecialCasesRepository
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class SpecialCasesController @Inject() (
  specialCasesRepository: SpecialCasesRepository,
  val authenticationService: AuthenticationService,
  appConfig: AppConfig,
  cc: ControllerComponents
)(using materializer: Materializer, ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import SpecialCasesController.given

  val getAllSpecialCases: Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      specialCasesRepository.findByPlanetId(session.planetId)(1000).map {
        case sc if sc.nonEmpty => ok(sc)
        case _                 => NoContent
      }
    }(SessionRecordNotFound)
  }

  def getSpecialCase(id: String): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      specialCasesRepository.findById(id, session.planetId).map {
        case Some(specialCase) => ok(specialCase)
        case None              => notFound("NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def createSpecialCase: Action[JsValue] = Action.async(parse.tolerantJson) { request =>
    given Request[JsValue] = request
    withCurrentSession { session =>
      withPayload[SpecialCase](specialCase =>
        specialCasesRepository
          .upsert(specialCase.copy(planetId = None), session.planetId)
          .map(id =>
            Created(s"Special case $id has been created.")
              .withHeaders(HeaderNames.LOCATION -> routes.SpecialCasesController.getSpecialCase(id).url)
          )
      )
    }(SessionRecordNotFound)
  }

  def updateSpecialCase(id: String): Action[JsValue] = Action.async(parse.tolerantJson) { request =>
    given Request[JsValue] = request
    withCurrentSession { session =>
      withPayload[SpecialCase](specialCase =>
        specialCasesRepository.findById(id, session.planetId).flatMap {
          case None    => notFoundF("NOT_FOUND")
          case Some(_) =>
            specialCasesRepository
              .upsert(specialCase.copy(id = Some(Id(id)), planetId = None), session.planetId)
              .map(id =>
                Accepted(s"Special case $id has been updated.")
                  .withHeaders(HeaderNames.LOCATION -> routes.SpecialCasesController.getSpecialCase(id).url)
              )

        }
      )
    }(SessionRecordNotFound)
  }

  def deleteSpecialCase(id: String): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    withCurrentSession { session =>
      specialCasesRepository.delete(id, session.planetId).map(_ => NoContent)
    }(SessionRecordNotFound)
  }

  final def maybeSpecialCase(action: EssentialAction): EssentialAction = new EssentialAction {

    override def apply(
      rh: RequestHeader
    ): Accumulator[ByteString, Result] =
      Accumulator.flatten(withMaybeCurrentSessionInCache { maybeSession =>
        val planetId = CurrentPlanetId(maybeSession)
        if appConfig.specialCasesUseTruncatedRequestUriMatch then {
          specialCasesRepository.findByPlanetId(planetId)(25).map {
            _.flatMap { specialCase =>
              val lengthOfSpecialCase = specialCase.requestMatch.path.length
              val requestUriKeyToMatch =
                SpecialCase.matchKey(
                  rh.method,
                  URLDecoder
                    .decode(rh.uri, "utf-8")
                    .take(lengthOfSpecialCase)
                )
              if specialCase.requestMatch.toKey == requestUriKeyToMatch then
                Some(Accumulator.done(specialCase.response.asResult))
              else None
            }.headOption
              .getOrElse(action(AuthenticatedSession.tagRequest(rh, maybeSession)))
          }
        } else {
          val key = SpecialCase.matchKey(rh.method, URLDecoder.decode(rh.uri, "utf-8"))
          specialCasesRepository.findByMatchKey(key, planetId).map {
            case None              => action(AuthenticatedSession.tagRequest(rh, maybeSession))
            case Some(specialCase) =>
              Accumulator.done(specialCase.response.asResult)
          }
        }
      }(using rh, ec))
  }
}

object SpecialCasesController {

  given reads: Reads[SpecialCase] = SpecialCase.external.reads
  given writes: Writes[SpecialCase] = SpecialCase.external.writes
}
