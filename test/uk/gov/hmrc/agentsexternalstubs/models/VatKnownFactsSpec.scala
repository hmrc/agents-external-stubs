/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentsexternalstubs.support.ExampleDesPayload
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

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
