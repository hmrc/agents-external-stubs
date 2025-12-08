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

case class Urn(value: String) extends TaxIdentifier with TrustTaxIdentifier

object Urn {

  private val urnPattern = "^((?i)[a-z]{2}trust[0-9]{8})$".r

  def isValid(urn: String): Boolean =
    urn match {
      case urnPattern(_*) => true
      case _              => false
    }

  implicit val urnReads: SimpleObjectReads[Urn] = new SimpleObjectReads[Urn]("value", Urn.apply)
  implicit val urnWrites: SimpleObjectWrites[Urn] = new SimpleObjectWrites[Urn](_.value)

}
