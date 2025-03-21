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

import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipRecordsService @Inject() (recordsRepository: RecordsRepository) {

  private val MAX_DOCS = 1000

  def store(record: RelationshipRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) RelationshipRecord.sanitize(record.arn)(record) else record
    RelationshipRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def authorise(relationship: RelationshipRecord, planetId: String, isExclusiveAgent: Boolean = true)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    for {
      existing <-
        if (isExclusiveAgent)
          findByKey(
            RelationshipRecord
              .clientWithAuthProfileKey(
                relationship.regime,
                relationship.idType,
                relationship.refNumber,
                relationship.authProfile.getOrElse("ALL00001")
              ),
            planetId
          )
        else Future.successful(Seq.empty)

      _ <- deActivate(existing, planetId)

      _ <- recordsRepository
             .store[RelationshipRecord](relationship.copy(active = true, startDate = Some(LocalDate.now())), planetId)
    } yield ()

  def deAuthorise(relationship: RelationshipRecord, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[String]] =
    for {
      existing <- findByKey(
                    RelationshipRecord
                      .fullKey(relationship.regime, relationship.arn, relationship.idType, relationship.refNumber),
                    planetId
                  )
      result <- deActivate(existing, planetId)

    } yield result

  private def deActivate(relationships: Seq[RelationshipRecord], planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[String]] =
    Future.sequence(
      relationships
        .filter(_.active)
        .map(r => r.copy(active = false, endDate = Some(LocalDate.now)))
        .map(r => recordsRepository.store[RelationshipRecord](r, planetId))
    )

  def findByKey(key: String, planetId: String): Future[Seq[RelationshipRecord]] =
    recordsRepository.findByKey[RelationshipRecord](key, planetId, limit = Some(MAX_DOCS))

  def findByKeys(keys: Seq[String], planetId: String): Future[Seq[RelationshipRecord]] =
    recordsRepository.findByKeys[RelationshipRecord](keys, planetId, limit = Some(MAX_DOCS))

  def findByQuery(query: RelationshipRecordQuery, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Seq[RelationshipRecord]] = {

    val maybeActiveOnly: RelationshipRecord => Boolean = r => if (query.activeOnly) r.active else true

    val maybeFromDate: RelationshipRecord => Boolean = r =>
      if (query.activeOnly) true
      else query.from.forall(qf => r.startDate.forall(rf => !rf.isBefore(qf)))

    val maybeToDate: RelationshipRecord => Boolean = r =>
      if (query.activeOnly) true else query.to.forall(qt => r.startDate.forall(rt => !rt.isAfter(qt)))

    val keys =
      if (query.agent) {
        if (!query.activeOnly && query.regime == "AGSV") { //AGSV to retrieve all types of inactive relationships
          RelationshipRecord.agentKeys(query.arn.getOrElse(throw new Exception("Missing arn parameter")))
        } else {
          Seq(
            RelationshipRecord
              .agentKey(query.regime, query.arn.getOrElse(throw new Exception("Missing arn parameter")))
          )
        }
      } else {
        val refNum = query.getRefNumber.getOrElse(throw new Exception("Missing refNumber parameter"))
        if (query.authProfile.isEmpty) {
          Seq(
            RelationshipRecord.clientKey(
              query.regime,
              idType(query.idType, refNum),
              refNum
            )
          )
        } else {
          Seq(
            RelationshipRecord.clientWithAuthProfileKey(
              query.regime,
              idType(query.idType, refNum),
              refNum,
              query.authProfile.get
            )
          )
        }

      }

    findByKeys(keys, planetId)
      .map(
        _.filter(maybeActiveOnly)
          .filter(maybeFromDate)
          .filter(maybeToDate)
      )
  }

  /*
   * Resolves queries from API#1168 with stubs records.
   * For example, for ITSA idType param is not required in the request, but when the record was created
   * (via API#1167) idType was supplied and therefore included in the stub record.
   * */
  private def idType(suppliedIdType: String, refNumber: String): String = if (suppliedIdType == "none")
    refNumber match {
      case value if MtdItId.isValid(value) => "MTDBSA"
      case value =>
        throw new RuntimeException(
          s"idType was not supplied in the query and refNumber $value has not been implemented as a idType lookup"
        )
    }
  else suppliedIdType
}

case class RelationshipRecordQuery(
  regime: String,
  arn: Option[String] = None,
  idType: String,
  private val refNumber: Option[String] = None, // Deprecated, DES service
  private val referenceNumber: Option[String] = None, // IF Service
  activeOnly: Boolean = true,
  agent: Boolean,
  from: Option[LocalDate] = None,
  to: Option[LocalDate] = None,
  relationship: Option[String] = None,
  authProfile: Option[String] = None
) {
  def getRefNumber: Option[String] = Seq(refNumber, referenceNumber).flatten.headOption
}
