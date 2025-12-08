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

package uk.gov.hmrc.agentsexternalstubs.models.identifiers

import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

case class PptRef(value: String) extends TaxIdentifier

object PptRef {

  private val pattern = "X[A-Z]PPT000[0-9]{7}".r

  def isValid(ppt: String): Boolean =
    ppt match {
      case pattern(_*) => true
      case _           => false
    }

  implicit val reads: SimpleObjectReads[PptRef] = new SimpleObjectReads[PptRef]("value", PptRef.apply)
  implicit val writes: SimpleObjectWrites[PptRef] = new SimpleObjectWrites[PptRef](_.value)

}
