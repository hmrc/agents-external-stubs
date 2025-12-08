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

import uk.gov.hmrc.domain.{Modulus11Check, SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

case class Utr(value: String) extends TaxIdentifier with TrustTaxIdentifier

object Utr {

  private val utrPattern = "^\\d{10}$".r

  def isValid(utr: String): Boolean =
    utr match {
      case utrPattern(_*) => UtrCheck.isValid(utr)
      case _              => false
    }

  implicit val utrReads: SimpleObjectReads[Utr] = new SimpleObjectReads[Utr]("value", Utr.apply)
  implicit val utrWrites: SimpleObjectWrites[Utr] = new SimpleObjectWrites[Utr](_.value)

}

object UtrCheck extends Modulus11Check {

  def isValid(utr: String): Boolean = {
    val suffix: String = utr.substring(1)
    calculateCheckCharacter(suffix) == utr.charAt(0)
  }
}
