/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Reads, Writes}
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Id, SpecialCase}
import uk.gov.hmrc.agentsexternalstubs.repository.SpecialCasesRepository
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class SpecialCasesController @Inject() (
  specialCasesRepository: SpecialCasesRepository,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents
)(implicit materializer: Materializer, ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import SpecialCasesController._

  val getAllSpecialCases: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.findByPlanetId(session.planetId)(1000).map {
        case sc if sc.nonEmpty => ok(sc)
        case _                 => NoContent
      }
    }(SessionRecordNotFound)
  }

  def getSpecialCase(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.findById(id, session.planetId).map {
        case Some(specialCase) => ok(specialCase)
        case None              => notFound("NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def createSpecialCase: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
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

  def updateSpecialCase(id: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[SpecialCase](specialCase =>
        specialCasesRepository.findById(id, session.planetId).flatMap {
          case None => notFoundF("NOT_FOUND")
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

  def deleteSpecialCase(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.delete(id, session.planetId).map(_ => NoContent)
    }(SessionRecordNotFound)
  }

  final def maybeSpecialCase(action: EssentialAction): EssentialAction = new EssentialAction {

    override def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      Accumulator.flatten(withMaybeCurrentSession { maybeSession =>
        val planetId = CurrentPlanetId(maybeSession, rh)
        val key = SpecialCase.matchKey(rh.method, URLDecoder.decode(rh.uri, "utf-8"))
        specialCasesRepository.findByMatchKey(key, planetId).map {
          case None => action(AuthenticatedSession.tagRequest(rh, maybeSession))
          case Some(specialCase) =>
            Accumulator.done(specialCase.response.asResult)
        }
      }(Request(rh, ()), ec, hc(rh)))
  }

}

object SpecialCasesController {

  implicit val reads: Reads[SpecialCase] = SpecialCase.external.reads
  implicit val writes: Writes[SpecialCase] = SpecialCase.external.writes
}
