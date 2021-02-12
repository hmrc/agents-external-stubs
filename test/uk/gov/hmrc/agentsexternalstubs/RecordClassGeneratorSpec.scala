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

package uk.gov.hmrc.agentsexternalstubs

import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers
import uk.gov.hmrc.play.test.UnitSpec

class RecordClassGeneratorSpec extends UnitSpec with PropertyChecks with ValidatedMatchers with KnownFieldGenerators {

  "KnownFieldGenerators" should {
    "find a generator for a common field name" in {
      knownFieldGenerators("safeId") shouldBe Some("Generator.safeIdGen")
      knownFieldGenerators("agentReferenceNumber") shouldBe Some("Generator.arnGen")
      knownFieldGenerators("nino") shouldBe Some("Generator.ninoNoSpacesGen")
      knownFieldGenerators("mtdbsa") shouldBe Some("Generator.mtdbsaGen")
      knownFieldGenerators("vrn") shouldBe Some("Generator.vrnGen")
      knownFieldGenerators("dateString") shouldBe Some("Generator.dateYYYYMMDDGen")
      knownFieldGenerators("date") shouldBe Some("Generator.dateYYYYMMDDGen")
      knownFieldGenerators("dateOfBirth") shouldBe Some("Generator.dateYYYYMMDDGen.variant(\"ofbirth\")")
      knownFieldGenerators("tradingName") shouldBe Some("Generator.tradingNameGen")
      knownFieldGenerators("phoneNumber") shouldBe Some("Generator.ukPhoneNumber")
      knownFieldGenerators("primaryPhoneNumber") shouldBe Some("Generator.ukPhoneNumber.variant(\"primary\")")
      knownFieldGenerators("phoneNumber1") shouldBe Some("Generator.ukPhoneNumber.variant(\"1\")")
      knownFieldGenerators("myPhoneNumber1") shouldBe Some("Generator.ukPhoneNumber.variant(\"my-1\")")
      knownFieldGenerators("yourMobileNumber") shouldBe Some("Generator.ukPhoneNumber.variant(\"your\")")
      knownFieldGenerators("faxNumber") shouldBe Some("Generator.ukPhoneNumber")
      knownFieldGenerators("emailAddress") shouldBe Some("Generator.emailGen")
      knownFieldGenerators("mainEmailAddress") shouldBe Some("Generator.emailGen.variant(\"main\")")
      knownFieldGenerators("line1") shouldBe Some("Generator.address4Lines35Gen.map(_.line1)")
      knownFieldGenerators("line2") shouldBe Some("Generator.address4Lines35Gen.map(_.line2)")
      knownFieldGenerators("line3") shouldBe Some("Generator.address4Lines35Gen.map(_.line3)")
      knownFieldGenerators("line4") shouldBe Some("Generator.address4Lines35Gen.map(_.line4)")
      knownFieldGenerators("addressLine1") shouldBe Some("Generator.address4Lines35Gen.map(_.line1)")
      knownFieldGenerators("addressLine2") shouldBe Some("Generator.address4Lines35Gen.map(_.line2)")
      knownFieldGenerators("addressLine3") shouldBe Some("Generator.address4Lines35Gen.map(_.line3)")
      knownFieldGenerators("addressLine4") shouldBe Some("Generator.address4Lines35Gen.map(_.line4)")
      knownFieldGenerators("secondaryAddressLine4") shouldBe Some(
        "Generator.address4Lines35Gen.map(_.line4).variant(\"secondary\")"
      )
      knownFieldGenerators("lastName") shouldBe Some("Generator.surname")
      knownFieldGenerators("firstName") shouldBe Some("Generator.forename()")
      knownFieldGenerators("middleName") shouldBe Some("Generator.forename().variant(\"middle\")")
    }
  }

}
