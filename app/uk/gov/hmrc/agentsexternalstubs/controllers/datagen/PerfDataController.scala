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

package uk.gov.hmrc.agentsexternalstubs.controllers.datagen

import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.CurrentSession
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

case class Agency(clients: Int, teamMembers: Int)

object Agency {
  implicit val format: OFormat[Agency] = Json.format[Agency]
}

case class PerfDataRequest(agencies: Seq[Agency], populateFriendlyNames: Boolean)

object PerfDataRequest {
  implicit val format: OFormat[PerfDataRequest] = Json.format[PerfDataRequest]
}

class PerfDataController @Inject() (
  val authenticationService: AuthenticationService,
  cc: ControllerComponents,
  usersRepository: UsersRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  authenticatedSessionsRepository: AuthenticatedSessionsRepository,
  knownFactsRepository: KnownFactsRepositoryMongo,
  specialCasesRepository: SpecialCasesRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo,
  agencyDataAssembler: AgencyDataAssembler,
  agencyCreator: AgencyCreator
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession with Logging {

  /** Accepts a JSON payload like:
    * <pre>
    * {
    *  "agencies": [
    *   {
    *    "clients": 10,
    *    "teamMembers": 3
    *   },
    *   {
    *    "clients": 460,
    *    "teamMembers": 21
    *   }
    *  ],
    *  "populateFriendlyNames": false
    * }
    * </pre>
    */
  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      Future {
        generateData(perfDataRequest)
        reapplyIndexes()
        logger.info(s"Done with data generation")
      }

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of ${perfDataRequest.agencies.size} agent(s)."
      )
    }
  }

  private def generateData(perfDataRequest: PerfDataRequest): Unit =
    perfDataRequest.agencies.zipWithIndex foreach { case (agency, indexAgency) =>
      val agencyCreationPayload =
        agencyDataAssembler.build(
          indexAgency + 1,
          agency.clients,
          agency.teamMembers,
          perfDataRequest.populateFriendlyNames
        )

      Await.result(agencyCreator.create(agencyCreationPayload), 15.minutes)
    }

  private def reapplyIndexes(): Unit = {
    usersRepository.ensureIndexes
    recordsRepository.ensureIndexes
    authenticatedSessionsRepository.ensureIndexes
    knownFactsRepository.ensureIndexes
    specialCasesRepository.ensureIndexes
    groupsRepository.ensureIndexes

    logger.info(s"Re-applied indexes")
  }

}
