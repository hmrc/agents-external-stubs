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

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Urn, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessPartnerRecordsService @Inject() (
  val recordsRepository: RecordsRepository,
  externalUserService: ExternalUserService
) extends RecordsService {

  def store(record: BusinessPartnerRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) BusinessPartnerRecord.sanitize(record.safeId)(record) else record
    BusinessPartnerRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getBusinessPartnerRecord(arn: Arn, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.agentReferenceNumberKey(arn.value), planetId)
      .map(_.headOption)

  def getBusinessPartnerRecord(utr: Utr, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    // if there is no record found, try to sync from api platform and try again. THIS SHOULD NOT BE IN THIS CLASS
    externalUserService.syncAndRetry(utr, planetId) { () =>
      findByKey[BusinessPartnerRecord](BusinessPartnerRecord.utrKey(utr.value), planetId).map(_.headOption)
    }

  def getBusinessPartnerRecord(urn: Urn, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.urnKey(urn.value), planetId).map(_.headOption)

  def getBusinessPartnerRecord(nino: Nino, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    // if there is no record found, try to sync from api platform and try again. THIS SHOULD NOT BE IN THIS CLASS
    externalUserService.syncAndRetry(nino, planetId) { () =>
      findByKey[BusinessPartnerRecord](BusinessPartnerRecord.ninoKey(nino.value), planetId).map(_.headOption)
    }

  def getBusinessPartnerRecordByEori(eori: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.eoriKey(eori), planetId).map(_.headOption)

  def getBusinessPartnerRecordByCrn(crn: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.crnKey(crn), planetId).map(_.headOption)

  def getBusinessPartnerRecordBySafeId(safeId: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.uniqueKey(safeId), planetId).map(_.headOption)

}
