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

import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class EnrolmentFormatSpec extends BaseUnitSpec {

  val testEnrolment: Enrolment = Enrolment(
    key = "HMRC-MTD-VAT",
    identifiers = Some(Seq(Identifier("VRN", "123456789"))),
    state = Enrolment.ACTIVATED,
    friendlyName = Some("Frank Martens")
  )

  "Enrolment tinyFormat" should {
    "round-trip an enrolment correctly" in {
      val json = Enrolment.tinyFormat.writes(testEnrolment)
      Enrolment.tinyFormat.reads(json).get shouldBe testEnrolment
    }
    "handle an enrolment with an unusual state " in {
      val enrolment = testEnrolment.copy(state = "Pending")
      val json = Enrolment.tinyFormat.writes(enrolment)
      Enrolment.tinyFormat.reads(json).get shouldBe enrolment
    }
    "handle an enrolment without friendly name" in {
      val enrolment = testEnrolment.copy(friendlyName = None)
      val json = Enrolment.tinyFormat.writes(enrolment)
      Enrolment.tinyFormat.reads(json).get shouldBe enrolment
    }
  }
}
