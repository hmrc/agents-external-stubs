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

import uk.gov.hmrc.agentmtdidentifiers.model.PlrId
import uk.gov.hmrc.agentsexternalstubs.models.Pillar2Record
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Pillar2RecordsService @Inject() (
  val recordsRepository: RecordsRepository
) extends RecordsService {
  def store(record: Pillar2Record, autoFill: Boolean, planetId: String): Future[String] = {
    val entity = if (autoFill) Pillar2Record.sanitize(record.plrReference)(record) else record
    Pillar2Record
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getPillar2Record(plrId: PlrId, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[Pillar2Record]] =
    findByKey[Pillar2Record](Pillar2Record.plrReferenceKey(plrId.value), planetId)
      .map(_.headOption)

}
