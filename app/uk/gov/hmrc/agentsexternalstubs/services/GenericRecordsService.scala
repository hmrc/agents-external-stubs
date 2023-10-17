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
import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GenericRecordsService @Inject() (
  val recordsRepository: RecordsRepository,
  externalUserService: ExternalUserService,
  usersServiceProvider: Provider[UsersService]
) extends RecordsService {

  def store(record: BusinessDetailsRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) BusinessDetailsRecord.sanitize(record.safeId)(record) else record
    BusinessDetailsRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getBusinessDetails(nino: Nino, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessDetailsRecord]] =
    externalUserService
      .tryLookupExternalUserIfMissingForIdentifier(nino, planetId, usersServiceProvider.get.createUser(_, _, _))(id =>
        findByKey[BusinessDetailsRecord](BusinessDetailsRecord.ninoKey(id.value), planetId).map(_.headOption)
      )

  def getBusinessDetails(mtdbsa: MtdItId, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessDetailsRecord]] =
    findByKey[BusinessDetailsRecord](BusinessDetailsRecord.mtdbsaKey(mtdbsa.value), planetId).map(_.headOption)

  def getBusinessDetails(cgtPdRef: CgtRef, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessDetailsRecord]] =
    findByKey[BusinessDetailsRecord](BusinessDetailsRecord.cgtPdRefKey(cgtPdRef.value), planetId).map(_.headOption)

}
