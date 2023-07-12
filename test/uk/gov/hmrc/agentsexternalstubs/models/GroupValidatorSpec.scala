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

import org.scalatest.Inspectors._
import uk.gov.hmrc.agentsexternalstubs.support.{BaseUnitSpec, ValidatedMatchers}

class GroupValidatorSpec extends BaseUnitSpec with ValidatedMatchers {

  val planetId = "myPlanet"
  val groupId = "myGroupId"

  "GroupValidator" should {
    "validate only when affinityGroup is none or one of [Individual, Organisation, Agent]" in {
      GroupValidator.validate(Group(planetId, groupId, affinityGroup = AG.Individual)).isValid shouldBe true
      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Organisation))
        .isValid shouldBe true
      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = Some("LMNOPQ234568")))
        .isValid shouldBe true

      GroupValidator.validate(Group(planetId, groupId, affinityGroup = "Foo")).isValid shouldBe false
      GroupValidator.validate(Group(planetId, groupId, affinityGroup = "")).isValid shouldBe false
    }

    "validate only when agentCode is none or set for an Agent" in {
      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = Some("LMNOPQ234568")))
        .isValid shouldBe true

      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = None))
        .isValid shouldBe false
    }

    "validate only when delegatedEnrolments are empty or user is an Agent" in {
      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Individual, delegatedEnrolments = Seq.empty))
        .isValid shouldBe true

      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(Enrolment("A"))
          )
        )
        .isValid shouldBe true

      GroupValidator
        .validate(
          Group(planetId, groupId, affinityGroup = AG.Individual, delegatedEnrolments = Seq(Enrolment("A")))
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(planetId, groupId, affinityGroup = AG.Organisation, delegatedEnrolments = Seq(Enrolment("A")))
        )
        .isValid shouldBe false
    }

    "validate only when principal enrolments are valid" in {
      GroupValidator
        .validate(Group(planetId, groupId, affinityGroup = AG.Individual, principalEnrolments = Seq(Enrolment("A"))))
        .isValid shouldBe true
      GroupValidator
        .validate(
          Group(planetId, groupId, affinityGroup = AG.Individual, principalEnrolments = Seq(Enrolment("A", "A", null)))
        )
        .isInvalid shouldBe true
      GroupValidator
        .validate(
          Group(planetId, groupId, affinityGroup = AG.Individual, principalEnrolments = Seq(Enrolment("A", "A", "A")))
        )
        .isInvalid shouldBe true

      forAll(Services.services) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(
            Group(planetId, groupId, affinityGroup = AG.Individual, principalEnrolments = Seq(enrolment))
          )
          .isValid shouldBe service.affinityGroups.contains(AG.Individual)
      }
    }

    "validate only when principal enrolments are distinct" in {
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("A", "A", "A"), Enrolment("A", "A", "A"))
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("A"), Enrolment("A"))
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("A", "A", "A"), Enrolment("A", "B", "B"))
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("A", "B", "C1"), Enrolment("A", "B", "C2"))
          )
        )
        .isValid shouldBe false
    }

    "validate only when principal enrolments are valid for an individual" in {
      val group = Group(planetId, groupId, affinityGroup = AG.Individual)
      forAll(Services.individualServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(AG.Individual))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe false
      }
    }

    "validate only when principal enrolments are valid for an organisation" in {
      val group = Group(planetId, groupId, affinityGroup = AG.Organisation)
      forAll(Services.organisationServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(AG.Organisation))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe false
      }
    }

    "validate only when principal enrolments are valid for an agent" in {
      val group = Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = Some("LMNOPQ234568"))
      forAll(Services.agentServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(AG.Agent))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(principalEnrolments = Seq(enrolment)))
          .isValid shouldBe false
      }
    }

    "validate only when delegated enrolments have distinct values" in {
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784")
            )
          )
        )
        .isValid shouldBe true
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784"),
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784")
            )
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784"),
              Enrolment("HMRC-MTD-VAT", "VRN", "429754517")
            )
          )
        )
        .isValid shouldBe true
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784"),
              Enrolment("HMRC-MTD-VAT", "VRN", "429754517"),
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784")
            )
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784"),
              Enrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368"),
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784")
            )
          )
        )
        .isValid shouldBe false
      GroupValidator
        .validate(
          Group(
            planetId,
            groupId,
            affinityGroup = AG.Agent,
            agentCode = Some("LMNOPQ234568"),
            delegatedEnrolments = Seq(
              Enrolment("HMRC-MTD-VAT", "VRN", "410392784"),
              Enrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368"),
              Enrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368")
            )
          )
        )
        .isValid shouldBe false
    }

    "validate only when delegated enrolment is of Individual or Organisation affinity" in {
      val group = Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = Some("LMNOPQ234568"))
      forAll(Services.individualServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(delegatedEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
      forAll(Services.organisationServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(delegatedEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
      forAll(Services.agentServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        GroupValidator
          .validate(group.copy(delegatedEnrolments = Seq(enrolment)))
          .isValid shouldBe false
      }
    }

    "validate only when suspended regimes are all valid regimes or empty" in {
      val group = Group(planetId, groupId, affinityGroup = AG.Agent, agentCode = Some("LMNOPQ234568"))
      GroupValidator.validate(group.copy(suspendedRegimes = Set.empty)).isValid shouldBe true
      GroupValidator
        .validate(group.copy(suspendedRegimes = Set("ITSA", "VATC")))
        .isValid shouldBe true

      GroupValidator.validate(group.copy(suspendedRegimes = Set("foo"))).isValid shouldBe false
      GroupValidator
        .validate(group.copy(suspendedRegimes = Set("ITSA", "VATC", "foo")))
        .isValid shouldBe false
    }
  }

}
