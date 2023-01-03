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

package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

class RegexPatternsSpec extends UnitSpec {

  "RegexPatterns" should {
    "validate nino" in {
      RegexPatterns.validNinoNoSpaces("HW827856C").isRight shouldBe true
      RegexPatterns.validNinoNoSpaces("").isLeft shouldBe true
      RegexPatterns.validNinoNoSpaces("HW827856Z").isLeft shouldBe true
      RegexPatterns.validNinoNoSpaces("HWW27856D").isLeft shouldBe true
      RegexPatterns.validNinoNoSpaces("HW827856C1").isLeft shouldBe true
      RegexPatterns.validNinoNoSpaces("HW8278561C").isLeft shouldBe true
    }

    "validate utr" in {
      RegexPatterns.validUtr("9999999999").isRight shouldBe true
      RegexPatterns.validUtr("").isLeft shouldBe true
      RegexPatterns.validUtr("99999999999").isLeft shouldBe true
    }

    "validate urn" in {
      RegexPatterns.validUrn("XXTRUST80000001").isRight shouldBe true
      RegexPatterns.validUrn("").isLeft shouldBe true
      RegexPatterns.validUrn("XXABCDE12345678").isLeft shouldBe true
      RegexPatterns.validUrn("XXTRUST800000101").isLeft shouldBe true
    }

    "validate mtdbsa" in {
      RegexPatterns.validMtdbsa("999999999999999").isRight shouldBe true
      RegexPatterns.validMtdbsa("").isLeft shouldBe true
      RegexPatterns.validMtdbsa("9999999999999999").isLeft shouldBe true
    }

    "validate arn" in {
      RegexPatterns.validArn("TARN0000001").isRight shouldBe true
      RegexPatterns.validArn("").isLeft shouldBe true
      RegexPatterns.validArn("TARN00000010").isLeft shouldBe true
      RegexPatterns.validArn("TARN000000").isLeft shouldBe true
      RegexPatterns.validArn("1ARN0000001").isLeft shouldBe true
      RegexPatterns.validArn("TTTT0000001").isLeft shouldBe true
    }

    "validate date" in {
      RegexPatterns.validDate("2018-12-31").isRight shouldBe true
      RegexPatterns.validDate("2018-01-01").isRight shouldBe true
      RegexPatterns.validDate("2018-06-15").isRight shouldBe true
      RegexPatterns.validDate("").isLeft shouldBe true
      RegexPatterns.validDate("2018").isLeft shouldBe true
      RegexPatterns.validDate("2018-02").isLeft shouldBe true
      RegexPatterns.validDate("2018-02-1").isLeft shouldBe true
      RegexPatterns.validDate("18-02-12").isLeft shouldBe true
      RegexPatterns.validDate("18/02/12").isLeft shouldBe true
      RegexPatterns.validDate("2018/02/12").isLeft shouldBe true
    }

    "validate postcode" in {
      RegexPatterns.validPostcode("BN14 7BU").isRight shouldBe true
      RegexPatterns.validPostcode("BN147BU").isRight shouldBe true
      RegexPatterns.validPostcode("").isLeft shouldBe true
      RegexPatterns.validPostcode("BN14").isLeft shouldBe true
      RegexPatterns.validPostcode("BN147B").isLeft shouldBe true
    }

    "validate PptRef" in {
      RegexPatterns.validPptRef("XAPPT0001234567").isRight shouldBe true
      RegexPatterns.validPptRef("XZPPT0000000000").isRight shouldBe true
      RegexPatterns.validPptRef("").isLeft shouldBe true
      RegexPatterns.validPptRef("1234567890").isLeft shouldBe true
      RegexPatterns.validPptRef("XAPPT123456789A").isLeft shouldBe true
    }
  }

}
