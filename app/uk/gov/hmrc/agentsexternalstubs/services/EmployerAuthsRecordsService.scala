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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.EmployerAuths
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmployerAuthsRecordsService @Inject() (val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: EmployerAuths, autoFill: Boolean, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[String] = {
    val entity = if (autoFill) EmployerAuths.sanitize(record.agentCode)(record) else record
    EmployerAuths
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  def getEmployerAuthsByAgentCode(agentCode: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[EmployerAuths]] =
    findByKey[EmployerAuths](EmployerAuths.uniqueKey(agentCode), planetId).map(_.headOption)

  def delete(agentCode: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeRecord <- getEmployerAuthsByAgentCode(agentCode, planetId)
      result <- maybeRecord.flatMap(_.id) match {
                  case Some(recordId) => recordsRepository.remove(recordId, planetId)
                  case None           => Future.successful(())
                }
    } yield result
}
