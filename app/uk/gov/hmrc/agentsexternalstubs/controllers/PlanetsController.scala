/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.UserIdGenerator
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthorisationCache
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PlanetsController @Inject() (
  knownFactsRepository: KnownFactsRepository,
  usersRepository: UsersRepository,
  recordsRepository: RecordsRepository,
  specialCasesRepository: SpecialCasesRepository,
  authSessionRepository: AuthenticatedSessionsRepository,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def destroy(planetId: String): Action[AnyContent] = Action.async {
    Logger(getClass).info(s"About to start destroying test planet $planetId ..")
    UserIdGenerator.destroyPlanetId(planetId)
    AuthorisationCache.destroyPlanet(planetId)
    Future
      .sequence(
        Seq(
          authSessionRepository.destroyPlanet(planetId),
          usersRepository.destroyPlanet(planetId),
          recordsRepository.destroyPlanet(planetId),
          knownFactsRepository.destroyPlanet(planetId),
          specialCasesRepository.destroyPlanet(planetId)
        )
      )
      .map { _ =>
        Logger(getClass).info(s"Test planet $planetId destroyed.")
      }
      .recover { case NonFatal(e) =>
        Logger(getClass).warn(s"Attempted test planet $planetId destroy failed with $e")
      }
    Future.successful(NoContent)
  }

}
