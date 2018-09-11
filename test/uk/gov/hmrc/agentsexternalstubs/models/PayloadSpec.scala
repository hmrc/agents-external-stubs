package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json

import uk.gov.hmrc.play.test.UnitSpec

class PayloadSpec extends UnitSpec {

  "SetKnownFactsRequest" should {
    "parse payload" in {
      import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest
      import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest._
      Json.parse("""{
                   |  "verifiers": [
                   |    {
                   |      "key": "Postcode",
                   |      "value": "TF2 6NU"
                   |    },
                   |    {
                   |      "key": "NINO",
                   |      "value": "AB123456X"
                   |    }
                   |  ],
                   |  "legacy": {
                   |    "previousVerifiers": [
                   |      {
                   |        "key": "Postcode",
                   |        "value": "TF2 6NU"
                   |      },
                   |      {
                   |        "key": "NINO",
                   |        "value": "AB123456X"
                   |      }
                   |    ]
                   |  }
                   |}
                 """.stripMargin).as[SetKnownFactsRequest] shouldBe SetKnownFactsRequest(
        List(KnownFact("Postcode", "TF2 6NU"), KnownFact("NINO", "AB123456X")),
        Some(Legacy(List(KnownFact("Postcode", "TF2 6NU"), KnownFact("NINO", "AB123456X")))))
    }
  }

}
