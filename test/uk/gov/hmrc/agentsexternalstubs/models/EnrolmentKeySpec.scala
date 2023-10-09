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

import uk.gov.hmrc.agentmtdidentifiers.model.Identifier
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class EnrolmentKeySpec extends BaseUnitSpec {

  "EnrolmentKey.parse" should {
    "return EnrolmentKey if string format and content are valid" in {
      EnrolmentKey.parse("IR-SA~UTR~12345678") shouldBe Right(EnrolmentKey("IR-SA", Seq(Identifier("UTR", "12345678"))))
      EnrolmentKey.parse("IR-PAYE~TaxOfficeNumber~754~TaxOfficeReference~KG12512") shouldBe Right(
        EnrolmentKey("IR-PAYE", Seq(Identifier("TaxOfficeNumber", "754"), Identifier("TaxOfficeReference", "KG12512")))
      )
    }
    "return error if enrolment key is empty" in {
      EnrolmentKey.parse("") shouldBe Left("INVALID_ENROLMENT_KEY")
    }
    "return error if service is empty" in {
      EnrolmentKey.parse("~FOO~11223344") shouldBe Left("INVALID_SERVICE")
    }
    "return error if service is unknown" in {
      EnrolmentKey.parse("IR-AS~FOO~11223344") shouldBe Left("INVALID_SERVICE")
    }
    "return error if there are no identifiers" in {
      EnrolmentKey.parse("IR-SA") shouldBe Left("INVALID_ENROLMENT_KEY")
      EnrolmentKey.parse("IR-SA~") shouldBe Left("INVALID_ENROLMENT_KEY")
    }
    "return error if identifier is not a key~value pair" in {
      EnrolmentKey.parse("IR-SA~UTR") shouldBe Left("INVALID_ENROLMENT_KEY")
      EnrolmentKey.parse("IR-SA~UTR~") shouldBe Left("INVALID_ENROLMENT_KEY")
      EnrolmentKey.parse("IR-SA~FOO~11223344~UTR") shouldBe Left("INVALID_ENROLMENT_KEY")
      EnrolmentKey.parse("IR-SA~FOO~11223344~UTR~") shouldBe Left("INVALID_ENROLMENT_KEY")
    }
    "return error if identifier key is empty" in {
      EnrolmentKey.parse("IR-SA~~11223344") shouldBe Left("INVALID_IDENTIFIERS")
      EnrolmentKey.parse("IR-SA~FOO~11223344~~12345678") shouldBe Left("INVALID_IDENTIFIERS")
    }
    "return error if identifier key is too long" in {
      EnrolmentKey.parse(s"IR-SA~${"A" * 41}~11223344") shouldBe Left("INVALID_IDENTIFIERS")
      EnrolmentKey.parse(s"IR-SA~FOO~11223344~${"AB" * 21}~5362563562") shouldBe Left("INVALID_IDENTIFIERS")
    }
    "return error if identifier value is empty" in {
      EnrolmentKey.parse("IR-SA~UTR~") shouldBe Left("INVALID_ENROLMENT_KEY")
      EnrolmentKey.parse("IR-SA~FOO~11223344~UTR~") shouldBe Left("INVALID_ENROLMENT_KEY")
    }
    "return error if identifier value is too long" in {
      EnrolmentKey.parse(s"IR-SA~UTR~${"9" * 51}") shouldBe Left("INVALID_IDENTIFIERS")
      EnrolmentKey.parse(s"IR-SA~FOO~11223344~UTR~${"09" * 25}1") shouldBe Left("INVALID_IDENTIFIERS")
    }
    "return error if enrolment keys are not sorted lexicographically" in {
      EnrolmentKey.parse("IR-SA~UTR~12345678~FOO~11223344") shouldBe Left("INVALID_IDENTIFIERS")
    }
  }
  "EnrolmentKey.toString" should {
    "return valid enrolment key" in {
      EnrolmentKey(
        "IR-SA",
        Seq(Identifier("FOO", "11223344"), Identifier("UTR", "12345678"))
      ).toString shouldBe "IR-SA~FOO~11223344~UTR~12345678"
    }
    "return valid enrolment key with sorted identifiers" in {
      EnrolmentKey(
        "IR-SA",
        Seq(Identifier("UTR", "12345678"), Identifier("FOO", "11223344"))
      ).toString shouldBe "IR-SA~FOO~11223344~UTR~12345678"
    }
  }

}
