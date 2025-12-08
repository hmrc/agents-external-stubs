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

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.agentsexternalstubs.models.{Record, RecordMetaData, RecordUtils, TakesKey}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.TaxIdentifier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class RecordsService @Inject() (recordsRepository: RecordsRepository, externalUserService: ExternalUserService) {

  def findByKeys[T](keys: Seq[String], planetId: String)(implicit
    metadata: RecordMetaData[T],
    reads: Reads[T]
  ): Future[Seq[T]] =
    recordsRepository.findByKeys[T](keys, planetId, limit = Some(1000))

  def store[A <: Record](record: A, autoFill: Boolean, planetId: String)(implicit
    recordUtils: RecordUtils[A],
    writes: Writes[A]
  ): Future[String] = {
    val entity =
      if (autoFill)
        recordUtils.sanitize(record.uniqueKey.getOrElse(throw new RuntimeException("no unique key!")))(record)
      else record
    recordUtils
      .validate(entity)
      .fold(
        errors => Future.failed(new RuntimeException(errors.mkString(", "))),
        _ => recordsRepository.store(entity, planetId)
      )
  }

  /** Retrieve a record of the given type and by using the given identifier.
    * There must be an implicit instance of TakesKey to instruct how the identifier is to be turned into a key for
    * the lookup in the records repository. These instances are usually defined in the record type definition.
    */
  def getRecord[A, K](identifier: K, planetId: String)(implicit
    ec: ExecutionContext,
    metadata: RecordMetaData[A],
    ev: TakesKey[A, K],
    reads: Reads[A]
  ): Future[Option[A]] = {
    val keys = ev.toKeys(identifier)
    findByKeys[A](keys, planetId).map(_.headOption)
  }

  /** Same as getRecord, but if not found it will try to find an external user (from api-platform) with the given
    * identifier, sync it (if found) and try again.
    */
  def getRecordMaybeExt[A, K <: TaxIdentifier](identifier: K, planetId: String)(implicit
    ec: ExecutionContext,
    metadata: RecordMetaData[A],
    ev: TakesKey[A, K],
    reads: Reads[A]
  ): Future[Option[A]] =
    externalUserService
      .syncAndRetry(identifier, planetId) { () =>
        getRecord[A, K](identifier, planetId)
      }

  def deleteRecord[A, K](identifier: K, planetId: String)(implicit
    ec: ExecutionContext,
    metadata: RecordMetaData[A],
    ev: TakesKey[A, K]
  ): Future[Unit] = {
    val key = ev.toKey(identifier)
    recordsRepository.removeByKey(key, planetId).map(_ => ())
  }

}
