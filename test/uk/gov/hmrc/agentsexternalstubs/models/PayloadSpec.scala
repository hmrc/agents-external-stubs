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
