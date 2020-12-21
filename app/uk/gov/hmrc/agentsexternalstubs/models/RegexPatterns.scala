/*
 * Copyright 2020 HM Revenue & Customs
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
import java.util.regex.Pattern

import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, Urn, Utr}
import uk.gov.hmrc.domain.TaxIdentifier

import scala.util.matching.Regex

object RegexPatterns {

  type Matcher = String => Either[String, String]

  val validNinoNoSpaces: Matcher = validate(
    "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r)
  val validNinoWithSpaces: Matcher = validate(
    "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-D]?$".r)
  val validArn: Matcher = validate("^[A-Z]ARN[0-9]{7}$".r)
  val validUtr: Matcher = validate("^[0-9]{10}$".r)
  val validUrn: Matcher = validate("^([A-Z0-9]{1,15})$".r)
  val validUtrOrUrn = validateTrustTaxIdentifier("^[0-9]{10}$".r, "^([A-Z0-9]{1,15})$".r)
  val validMtdbsa: Matcher = validate("^[A-Z0-9]{1,15}$".r)
  val validVrn: Matcher = validate("^[0-9]{1,9}$".r)
  val validEori: Matcher = validate("^[A-Z]{2}[0-9]{12}$".r)
  val validCrn: Matcher = validate("^([A-Za-z0-9]{0,2})?([0-9]{1,6})$".r)
  val validCgtRef: Matcher = validate(CgtRef.cgtRegex.r)
  val validSafeId: Matcher = validate("^[A-Za-z0-9 \\-,.&'\\/]{1,15}$".r)
  val validAgentCode: Matcher = validate("^[A-Z0-9]{1,12}$".r)
  val validTaxOfficeNumber: Matcher = validate("^\\d{1,3}$".r)
  val validTaxOfficeReference: Matcher = validate("^[A-Za-z0-9 ]{1,10}$".r)

  val validDate: Matcher =
    validate(
      "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r)

  val validPostcode: Matcher = validate("^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,10}$".r)

  def validate(regex: Regex): Matcher =
    value =>
      if (regex.pattern.matcher(value).matches()) Right(value)
      else Left(s"Supplied value $value does not match pattern ${regex.pattern.toString}")

  def validateTrustTaxIdentifier(utrR: Regex, urnR: Regex): String => Either[String, TaxIdentifier] =
    value =>
      if (utrR.pattern.matcher(value).matches()) Right(Utr(value))
      else if (urnR.pattern.matcher(value).matches()) Right(Urn(value))
      else Left(s"Supplied value $value does not match either ${utrR.pattern.toString} or ${urnR.pattern.toString}")

  def validate(pattern: String): Matcher =
    value => {
      val regex = Pattern.compile(pattern)
      if (regex.matcher(value).matches()) Right(value)
      else Left(s"Supplied value $value does not match pattern ${regex.pattern}")
    }

}
