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

package uk.gov.hmrc.agentsexternalstubs.controllers.datagen

import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.CurrentSession
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class PerfDataRequest(numAgents: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)

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
  agencyDataAssembler: AgencyDataAssembler,
  agencyCreator: AgencyCreator
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession with Logging {

  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      Future {
        dropCollections()
        reapplyIndexes()
        generateData(perfDataRequest)
        logger.info(s"Done with data generation")
      }(scala.concurrent.ExecutionContext.global)

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent + perfDataRequest.teamMembersPerAgent)} 'user(s)', " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent)} 'record(s)', and " +
          s"${perfDataRequest.numAgents} 'group(s)'"
      )
    }
  }

  private def dropCollections(): Unit = {

    def dropCollectionOf[R <: PlayMongoRepository[_]](repository: R): Unit = {
      repository.collection.drop
      logger.info(s"Dropped '${repository.collectionName}' collection if it existed")
    }

    dropCollectionOf(usersRepository)
    dropCollectionOf(recordsRepository)
    dropCollectionOf(authenticatedSessionsRepository)
    dropCollectionOf(knownFactsRepository)
    dropCollectionOf(specialCasesRepository)
  }

  private def reapplyIndexes(): Unit = {
    usersRepository.ensureIndexes
    recordsRepository.ensureIndexes
    authenticatedSessionsRepository.ensureIndexes
    knownFactsRepository.ensureIndexes
    specialCasesRepository.ensureIndexes

    logger.info(s"Re-applied indexes")
  }

  private def generateData(perfDataRequest: PerfDataRequest): Unit =
    for (indexAgency <- (1 to perfDataRequest.numAgents).toList) {
      val agencyCreationPayload =
        agencyDataAssembler.build(
          indexAgency,
          perfDataRequest.clientsPerAgent,
          perfDataRequest.teamMembersPerAgent
        )

      agencyCreator.create(agencyCreationPayload)
    }

}
