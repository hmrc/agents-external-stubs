package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.play.test.UnitSpec

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

    "validate mtdbsa" in {
      RegexPatterns.validMtdbsa("9999999999999999").isRight shouldBe true
      RegexPatterns.validMtdbsa("").isLeft shouldBe true
      RegexPatterns.validMtdbsa("99999999999999999").isLeft shouldBe true
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
  }

}
