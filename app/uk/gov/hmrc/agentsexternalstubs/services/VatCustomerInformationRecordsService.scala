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

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{VatCustomerInformationRecord, VatKnownFacts}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatCustomerInformationRecordsService @Inject() (
  val recordsRepository: RecordsRepository,
  externalUserService: ExternalUserService,
  usersServiceProvider: Provider[UsersService]
) extends RecordsService {

  def store(record: VatCustomerInformationRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) VatCustomerInformationRecord.sanitize(record.vrn)(record) else record
    VatCustomerInformationRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getCustomerInformation(vrn: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[VatCustomerInformationRecord]] =
    // if there is no record found, try to sync from api platform and try again. THIS SHOULD NOT BE IN THIS CLASS
    externalUserService.syncAndRetry(Vrn(vrn), planetId, usersServiceProvider.get.createUser(_, _, _)) { () =>
      findByKey[VatCustomerInformationRecord](VatCustomerInformationRecord.uniqueKey(vrn), planetId)
        .map(_.headOption)
    }

  def getVatKnownFacts(vrn: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[VatKnownFacts]] =
    getCustomerInformation(vrn, planetId).map(VatKnownFacts.fromVatCustomerInformationRecord(vrn, _))

  def customerIsInsolvent(vrn: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[Boolean]] =
    getCustomerInformation(vrn, planetId).map(_.flatMap(_.approvedInformation.flatMap(_.customerDetails.isInsolvent)))
}
