/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.{Address, IndividualDetails, OrganisationDetails, TestUser}
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

import java.time.LocalDate

class ApiPlatformSpec extends BaseUnitSpec {

  private val address = Address("1 Test Street", "Test Town", "AA1 1AA")
  private val individualDetails = IndividualDetails("Test", "User", LocalDate.of(1990, 1, 1), address)

  "ApiPlatform.TestUser" should {
    "retain Individual affinity when only individual details are present" in {
      val testUser = TestUser(
        userId = "individual-user-id",
        userFullName = "Test User",
        affinityGroup = AG.Individual,
        individualDetails = Some(individualDetails)
      )

      val (_, group) = TestUser.asUserAndGroup(testUser)

      testUser.affinityGroup shouldBe AG.Individual
      group.affinityGroup shouldBe AG.Individual
    }

    "retain Organisation affinity when the APTU Organisation contains individual details" in {
      val testUser = TestUser(
        userId = "organisation-user-id",
        userFullName = "Test Organisation",
        affinityGroup = AG.Organisation,
        individualDetails = Some(individualDetails),
        organisationDetails = Some(OrganisationDetails("Test Organisation", address)),
        pillar2Id = Some("XAPLR1234567890")
      )

      val (_, group) = TestUser.asUserAndGroup(testUser)

      testUser.affinityGroup shouldBe AG.Organisation
      group.affinityGroup shouldBe AG.Organisation
      group.principalEnrolments should contain(
        Enrolment("HMRC-PILLAR2-ORG", "PLRID", "XAPLR1234567890")
      )
    }

    "reject an unsupported explicit affinity" in {
      val result = Json
        .obj(
          "userId"        -> "test-user-id",
          "userFullName"  -> "Test User",
          "affinityGroup" -> "Unknown"
        )
        .validate[TestUser]

      result.isError shouldBe true
    }
  }
}
