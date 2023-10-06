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

package uk.gov.hmrc.agentsexternalstubs.services

import uk.gov.hmrc.agentsexternalstubs.models.admin.UserGenerator

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{Generator, LegacyAgentRecord, LegacyRelationshipRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LegacyRelationshipRecordsService @Inject() (recordsRepository: RecordsRepository) {

  def store(record: LegacyRelationshipRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) LegacyRelationshipRecord.sanitize(record.agentId)(record) else record
    LegacyRelationshipRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def store(record: LegacyAgentRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) LegacyAgentRecord.sanitize(record.agentId)(record) else record
    LegacyAgentRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getLegacyRelationshipsByNino(nino: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[(String, LegacyAgentRecord)]] =
    findRelationshipsByKey(LegacyRelationshipRecord.ninoKey(nino), planetId)
      .flatMap(rr => getNinosWithAgents(rr.distinct, planetId))

  def getLegacyRelationshipsByUtr(utr: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[(String, LegacyAgentRecord)]] =
    findRelationshipsByKey(LegacyRelationshipRecord.utrKey(utr), planetId)
      .flatMap(rr => getNinosWithAgents(rr.distinct, planetId))

  def getLegacyRelationshipByAgentIdAndUtr(agentId: String, utr: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[LegacyRelationshipRecord]] =
    findRelationshipsByKey(LegacyRelationshipRecord.agentIdAndUtrKey(agentId, utr), planetId).map(_.headOption)

  def getLegacyRelationship(id: String, planetId: String): Future[Option[LegacyRelationshipRecord]] =
    recordsRepository.findById[LegacyRelationshipRecord](id, planetId)

  def getLegacyAgent(id: String, planetId: String): Future[Option[LegacyAgentRecord]] =
    recordsRepository.findById[LegacyAgentRecord](id, planetId)

  def getLegacyAgentByAgentId(saAgentRef: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[LegacyAgentRecord]] =
    findAgentByKey(LegacyAgentRecord.agentIdKey(saAgentRef), planetId)

  private def getNinosWithAgents(relationships: Seq[LegacyRelationshipRecord], planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[(String, LegacyAgentRecord)]] =
    Future
      .sequence(
        relationships
          .map(_.agentId)
          .distinct
          .map(aid =>
            findAgentByKey(LegacyAgentRecord.agentIdKey(aid), planetId)
              .map(agentOpt => agentOpt.map(agent => (aid, agent)))
          )
      )
      .map(_.collect { case Some(x) => x }.toMap)
      .map(agentsMap =>
        relationships.map(r =>
          (
            r.nino.getOrElse(Generator.ninoNoSpaces(r.agentId).value),
            agentsMap.getOrElse(
              r.agentId, {
                val address = Generator.address(r.agentId)
                val agent = LegacyAgentRecord(
                  agentId = r.agentId,
                  agentName = UserGenerator.nameForAgent(r.agentId),
                  address1 = address.street,
                  address2 = address.town
                )
                LegacyAgentRecord.sanitize(agent.agentId)(agent)
              }
            )
          )
        )
      )

  private def findAgentByKey(key: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[LegacyAgentRecord]] =
    recordsRepository
      .findByKey[LegacyAgentRecord](key, planetId, limit = Some(1))
      .map(_.headOption.collect { case x: LegacyAgentRecord => x })

  private def findRelationshipsByKey(key: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[LegacyRelationshipRecord]] =
    recordsRepository
      .findByKey[LegacyRelationshipRecord](key, planetId, limit = Some(1000))
      .map(_.collect { case x: LegacyRelationshipRecord => x })

}
