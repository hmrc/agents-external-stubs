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

package uk.gov.hmrc.agentsexternalstubs.models
import com.github.blemale.scaffeine.Scaffeine
import org.scalacheck.Gen
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

trait RecordUtils[T] {

  type Update = String => T => T

  val gen: Gen[T]

  val validate: Validator[T]

  val sanitizers: Seq[Update]

  private val sanitizedRecordCache = Scaffeine().maximumSize(1000).build[String, T]()
  private val seededRecordCache = Scaffeine().maximumSize(1000).build[String, T]()

  final def seed(s: String): T =
    seededRecordCache
      .get(s, s => Generator.get(gen)(s).getOrElse(seed(Generator.shake(s))))

  final def sanitize(s: String)(entity: T): T = sanitizers.foldLeft(entity)((u, fx) => fx(s)(u))

  final def generate(s: String): T = sanitizedRecordCache.get(s, s => sanitize(s)(seed(s)))

}
