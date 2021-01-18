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

import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers
import uk.gov.hmrc.play.test.UnitSpec

class KnownFactsSpec extends UnitSpec with ValidatedMatchers {

  "KnownFacts" should {
    "run sanitize and return same entity if no issue found" in {
      val knownFacts =
        KnownFacts(
          EnrolmentKey.parse("HMRC-MTD-IT~MTDITID~X12345678909876").right.get,
          Seq(KnownFact("NINO", "AB087054B"), KnownFact("businesspostcode", "BN14 7BU")),
          Some("")
        )
      KnownFacts.sanitize("foo")(knownFacts) shouldBe knownFacts
    }
    "generate missing verifier value" in {
      val knownFacts =
        KnownFacts(
          EnrolmentKey.parse("HMRC-MTD-IT~MTDITID~X12345678909876").right.get,
          Seq(KnownFact("NINO", ""), KnownFact("businesspostcode", "")),
          Some("")
        )
      val sanitized = KnownFacts.sanitize("foo")(knownFacts)
      sanitized.getVerifierValue("NINO").get should not be empty
      sanitized.getVerifierValue("businesspostcode").get should not be empty
    }
    "add missing verifier" in {
      val knownFacts =
        KnownFacts(
          EnrolmentKey.parse("HMRC-MTD-IT~MTDITID~X12345678909876").right.get,
          Seq(KnownFact("NINO", "AB087054B")),
          Some("")
        )
      val sanitized = KnownFacts.sanitize("foo")(knownFacts)
      sanitized.getVerifierValue("NINO").get shouldBe "AB087054B"
      sanitized.getVerifierValue("businesspostcode").get should not be empty
    }
    "add missing verifiers" in {
      val knownFacts =
        KnownFacts(
          EnrolmentKey.parse("HMRC-MTD-VAT~VRN~750296137").right.get,
          Seq.empty,
          Some("")
        )
      val sanitized = KnownFacts.sanitize("foo")(knownFacts)
      sanitized.getVerifierValue("Postcode").get should not be empty
      sanitized.getVerifierValue("VATRegistrationDate").get should not be empty
    }
  }
}
