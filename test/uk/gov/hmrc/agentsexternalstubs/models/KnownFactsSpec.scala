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
