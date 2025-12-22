/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain._

// THIS MODEL IS DIFFERENT FROM THE ONE IN agent-client-relationships REPO
// This is because while the suffix does not matter on ACR, within the stubs we need to replicate API behaviour
// APIs typically match with the provided suffix or match every possible suffix if none is provided
case class NinoWithoutSuffix(nino: String) extends TaxIdentifier with SimpleName {

  require(NinoWithoutSuffix.isValid(nino), s"$nino is not a valid nino.")

  override def value: String = nino.replace(" ", "")
  def suffixlessValue: String = value.take(suffixlessNinoLength)
  override def toString: String = value
  override val name: String = "nino-without-suffix"
  private val suffixlessNinoLength = 8
  def variations: Seq[String] =
    if (value.length > suffixlessNinoLength) {
      Seq(value)
    } else {
      Nino.validSuffixes.map(suffix => value + suffix) ++ Seq(value)
    }

  def allVariations: Seq[String] = {
    val suffixless = value.take(suffixlessNinoLength)
    Nino.validSuffixes.map(suffix => suffixless + suffix) ++ Seq(suffixless)
  }

}

object NinoWithoutSuffix extends (String => NinoWithoutSuffix) {

  implicit val ninoWrite: Writes[NinoWithoutSuffix] = new SimpleObjectWrites[NinoWithoutSuffix](_.value)
  implicit val ninoRead: Reads[NinoWithoutSuffix] =
    new SimpleObjectReads[NinoWithoutSuffix]("nino-without-suffix", NinoWithoutSuffix.apply)

  def isValid(nino: String): Boolean = nino != null && (Nino.isValid(nino + "A") || Nino.isValid(nino))

}
