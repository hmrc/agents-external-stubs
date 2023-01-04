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
import play.api.libs.json.Reads
import uk.gov.hmrc.agentsexternalstubs.models.{Record, RecordMetaData}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository

import scala.concurrent.Future

trait RecordsService {

  def recordsRepository: RecordsRepository

  protected def findByKey[T <: Record](key: String, planetId: String)(implicit
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]] =
    recordsRepository.findByKey[T](key, planetId, limit = Some(1000))

  protected def findByKeys[T <: Record](keys: Seq[String], planetId: String)(implicit
    recordType: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]] =
    recordsRepository.findByKeys[T](keys, planetId, limit = Some(1000))
}
