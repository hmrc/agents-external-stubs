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

import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

class GroupSanitizerSpec extends UnitSpec {
  private val planetId = "myPlanetId"
  private val groupId = "myGroupId"

  "GroupSanitizer" should {
    "add missing GroupIdentifier" in {
      GroupSanitizer.sanitize(Group(planetId, groupId, affinityGroup = AG.Individual)).groupId.nonEmpty shouldBe true
      GroupSanitizer.sanitize(Group(planetId, groupId, affinityGroup = AG.Organisation)).groupId.nonEmpty shouldBe true
      GroupSanitizer.sanitize(Group(planetId, groupId, affinityGroup = AG.Agent)).groupId.nonEmpty shouldBe true
    }

    "add missing AgentCode to Agent" in {
      GroupSanitizer
        .sanitize(Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = None))
        .agentCode
        .isDefined shouldBe true
    }

    "remove AgentCode from Individual or Organisation" in {
      GroupSanitizer
        .sanitize(Group(planetId, groupId, affinityGroup = AG.Individual, agentCode = Some("foo")))
        .agentCode
        .isDefined shouldBe false
      GroupSanitizer
        .sanitize(Group(planetId, groupId, affinityGroup = AG.Organisation, agentCode = Some("foo")))
        .agentCode
        .isDefined shouldBe false
    }

    "add missing friendly name to Agent" in {
      GroupSanitizer
        .sanitize(Group(planetId, groupId, affinityGroup = AG.Agent, agentFriendlyName = None))
        .agentFriendlyName
        .isDefined shouldBe true
    }

    "add missing identifiers to principal enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(planetId, groupId, affinityGroup = AG.Individual, principalEnrolments = Seq(Enrolment("HMRC-MTD-IT")))
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("MTDITID")
      GroupSanitizer
        .sanitize(
          Group(planetId, groupId, affinityGroup = AG.Agent, principalEnrolments = Seq(Enrolment("HMRC-AS-AGENT")))
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("AgentReferenceNumber")
    }

    "remove redundant dangling principal enrolment keys" in {
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT"), Enrolment("HMRC-MTD-IT"))
          )
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) shouldBe Seq("MTDITID")
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT"), Enrolment("HMRC-MTD-VAT"))
          )
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain theSameElementsAs Seq("MTDITID", "VRN")
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(
              Enrolment("HMRC-MTD-IT"),
              Enrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368"),
              Enrolment("HMRC-MTD-IT")
            )
          )
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.value)) shouldBe Seq("CNOB96766112368")
    }

    "add missing identifier names to principal enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT", "", "123456789"))
          )
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key))
        .filter(_.nonEmpty) should contain.only("MTDITID")
    }

    "add missing identifier values to principal enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT", "MTDITID", ""))
          )
        )
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.value))
        .filter(_.nonEmpty) should not be empty
    }

    "add missing identifiers to delegated enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(planetId, groupId, affinityGroup = AG.Agent, delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT")))
        )
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("MTDITID")
      GroupSanitizer
        .sanitize(
          Group(planetId, groupId, affinityGroup = AG.Agent, delegatedEnrolments = Seq(Enrolment("IR-SA")))
        )
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("UTR")
    }

    "add missing identifier names to delegated enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT", "", "123456789"))
          )
        )
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key))
        .filter(_.nonEmpty) should contain.only("MTDITID")
    }

    "add missing identifier values to delegated enrolments" in {
      GroupSanitizer
        .sanitize(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT", "MTDITID", ""))
          )
        )
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.value))
        .filter(_.nonEmpty) should not be empty
    }
  }

}
