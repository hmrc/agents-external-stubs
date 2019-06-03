package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.support.ExampleDesPayload
import uk.gov.hmrc.play.test.UnitSpec

class VatKnownFactsSpec extends UnitSpec with ExampleDesPayload {

  "VatKnownFacts" should {
    "create an instance of VatKnownFacts given an instance of vatCustomerInformation" in {

      val vatCustomerInformation = Json.parse(validVatCustomerInformationPayload).as[VatCustomerInformationRecord]

      val result = VatKnownFacts.fromVatCustomerInformationRecord("123456789", Some(vatCustomerInformation))

      result.isInstanceOf[Option[VatKnownFacts]] shouldBe true
      result.get.vrn shouldBe "123456789"
      result.get.dateOfReg shouldBe "1967-08-13"
    }
  }

}
