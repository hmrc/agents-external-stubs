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

import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentsexternalstubs.models.CbcSubscriptionRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CbCSubscriptionRecordsService @Inject() (
  val recordsRepository: RecordsRepository
) extends RecordsService {
  def store(record: CbcSubscriptionRecord, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) CbcSubscriptionRecord.sanitize(record.cbcId)(record) else record
    CbcSubscriptionRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getCbcSubscriptionRecord(cbcId: CbcId, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[CbcSubscriptionRecord]] =
    findByKey[CbcSubscriptionRecord](CbcSubscriptionRecord.cbcIdKey(cbcId.value), planetId)
      .map(_.headOption)

}
