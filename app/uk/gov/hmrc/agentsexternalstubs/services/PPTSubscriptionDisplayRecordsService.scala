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

package uk.gov.hmrc.agentsexternalstubs.services

import uk.gov.hmrc.agentmtdidentifiers.model.PptRef
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PPTSubscriptionDisplayRecordsService @Inject() (
  val recordsRepository: RecordsRepository
) extends RecordsService {
  def store(record: PPTSubscriptionDisplayRecord, autoFill: Boolean, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[String] = {
    val entity = if (autoFill) PPTSubscriptionDisplayRecord.sanitize(record.pptReference)(record) else record
    PPTSubscriptionDisplayRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getPPTSubscriptionDisplayRecord(pptReference: PptRef, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[PPTSubscriptionDisplayRecord]] =
    findByKey[PPTSubscriptionDisplayRecord](PPTSubscriptionDisplayRecord.pptReferenceKey(pptReference.value), planetId)
      .map(_.headOption)

}
