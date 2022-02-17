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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec
import org.scalatest.Inspectors._
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers

class UserValidatorSpec extends UnitSpec with ValidatedMatchers {

  "UserValidator" should {
    "validate only when affinityGroup is none or one of [Individual, Organisation, Agent]" in {
      UserValidator.validate(User("foo", affinityGroup = Some(User.AG.Individual))).isValid shouldBe true
      UserValidator
        .validate(User("foo", affinityGroup = Some(User.AG.Organisation), credentialRole = Some(User.CR.Admin)))
        .isValid shouldBe true
      UserValidator.validate(UserGenerator.agent("foo")).isValid shouldBe true
      UserValidator.validate(User("foo", affinityGroup = None)).isValid shouldBe true

      UserValidator.validate(User("foo", affinityGroup = Some("Foo"))).isValid shouldBe false
      UserValidator.validate(User("foo", affinityGroup = Some(""))).isValid shouldBe false
    }

    "validate only when confidenceLevel is none, or one of [50,100,200,300] and user is Individual and NINO is not empty" in {
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(50),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(100),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(300),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true

      UserValidator
        .validate(
          User("foo", confidenceLevel = Some(200), affinityGroup = Some(User.AG.Agent), nino = Some(Nino("HW827856C")))
        )
        .isValid shouldBe false
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Organisation),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", confidenceLevel = Some(200), affinityGroup = Some(User.AG.Individual), nino = None))
        .isValid shouldBe false
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(55),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
      UserValidator
        .validate(
          User(
            "foo",
            confidenceLevel = Some(0),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
    }

    "validate only when credentialStrength is none, or one of [weak, strong]" in {
      UserValidator.validate(User("foo", credentialStrength = Some("weak"))).isValid shouldBe true
      UserValidator.validate(User("foo", credentialStrength = Some("strong"))).isValid shouldBe true
      UserValidator.validate(User("foo", credentialStrength = None)).isValid shouldBe true

      UserValidator.validate(User("foo", credentialStrength = Some("very strong"))).isValid shouldBe false
      UserValidator.validate(User("foo", credentialStrength = Some("little weak"))).isValid shouldBe false
      UserValidator.validate(User("foo", credentialStrength = Some(""))).isValid shouldBe false
    }

    "validate only when credentialRole is none, or one of [Admin, User, Assistant] for Individual or Agent" in {
      UserValidator
        .validate(User("foo", credentialRole = Some(User.CR.User), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true
      UserValidator.validate(UserGenerator.agent("foo", credentialRole = User.CR.User)).isValid shouldBe true
      UserValidator
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true
      UserValidator.validate(UserGenerator.agent("foo", credentialRole = "Assistant")).isValid shouldBe true
      UserValidator.validate(UserGenerator.agent("foo")).isValid shouldBe true
      UserValidator
        .validate(User("foo", credentialRole = None, affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true

      UserValidator
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when credentialRole is Admin or User for Organisation" in {
      UserValidator
        .validate(User("foo", credentialRole = Some("Admin"), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe true

      UserValidator
        .validate(User("foo", credentialRole = Some("User"), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe true
      UserValidator
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when nino is none or set for an Individual" in {
      UserValidator
        .validate(
          User(
            "foo",
            nino = Some(Nino("HW827856C")),
            affinityGroup = Some(User.AG.Individual),
            confidenceLevel = Some(200)
          )
        )
        .isValid shouldBe true
      UserValidator.validate(User("foo", nino = None, affinityGroup = Some(User.AG.Individual))).isValid shouldBe true

      UserValidator
        .validate(
          User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Individual), confidenceLevel = None)
        )
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Agent)))
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when dateOfBirth is none or set for an Individual" in {
      val now = LocalDate.now()
      UserValidator.validate(UserGenerator.individual(userId = "foo", dateOfBirth = "1975-03-29")).isValid shouldBe true

      UserValidator
        .validate(User("foo", affinityGroup = Some(User.AG.Organisation), dateOfBirth = Some(now)))
        .isValid shouldBe false
    }

    "validate only when agentCode is none or set for an Agent" in {
      UserValidator.validate(UserGenerator.agent(userId = "foo", agentCode = "LMNOPQ234568")).isValid shouldBe true

      UserValidator.validate(User("foo", affinityGroup = Some(User.AG.Agent))).isValid shouldBe false
    }

    "validate only when delegatedEnrolments are empty or user is an Agent" in {
      UserValidator.validate(User("foo", delegatedEnrolments = Seq.empty)).isValid shouldBe true
      UserValidator
        .validate(UserGenerator.agent("foo", delegatedEnrolments = Seq(Enrolment("A"))))
        .isValid shouldBe true

      UserValidator
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when principal enrolments are valid" in {
      UserValidator.validate(User("foo", principalEnrolments = Seq(Enrolment("A")))).isValid shouldBe true
      UserValidator.validate(User("foo", principalEnrolments = Seq(Enrolment("A", "A", null)))).isInvalid shouldBe true
      UserValidator.validate(User("foo", principalEnrolments = Seq(Enrolment("A", "A", "A")))).isInvalid shouldBe true

      forAll(Services.services) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(User("foo", principalEnrolments = Seq(enrolment)))
          .isValid shouldBe true
      }
    }

    "validate only when principal enrolments are distinct" in {
      UserValidator
        .validate(User("foo", principalEnrolments = Seq(Enrolment("A", "A", "A"), Enrolment("A", "A", "A"))))
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", principalEnrolments = Seq(Enrolment("A"), Enrolment("A"))))
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", principalEnrolments = Seq(Enrolment("A", "A", "A"), Enrolment("A", "B", "B"))))
        .isValid shouldBe false
      UserValidator
        .validate(User("foo", principalEnrolments = Seq(Enrolment("A", "B", "C1"), Enrolment("A", "B", "C2"))))
        .isValid shouldBe false
    }

    "validate only when principal enrolments are valid for an individual" in {
      val user = UserGenerator.individual("foo")
      forAll(Services.individualServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(User.AG.Individual))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe false
      }
    }

    "validate only when principal enrolments are valid for an organisation" in {
      val user = UserGenerator.organisation("foo")
      forAll(Services.organisationServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(User.AG.Organisation))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe false
      }
    }

    "validate only when principal enrolments are valid for an agent" in {
      val user = UserGenerator.agent("foo")
      forAll(Services.agentServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe true
      }
      forAll(Services.services.filter(!_.affinityGroups.contains(User.AG.Agent))) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withPrincipalEnrolment(enrolment))
          .isValid shouldBe false
      }
    }

    "validate only when delegated enrolments have distinct values" in {
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
        )
        .isValid shouldBe true
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
        )
        .isValid shouldBe false
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "429754517")
        )
        .isValid shouldBe true
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "429754517")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
        )
        .isValid shouldBe false
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
            .withDelegatedEnrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
        )
        .isValid shouldBe false
      UserValidator
        .validate(
          UserGenerator
            .agent("foo")
            .withDelegatedEnrolment("HMRC-MTD-VAT", "VRN", "410392784")
            .withDelegatedEnrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368")
            .withDelegatedEnrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368")
        )
        .isValid shouldBe false
    }

    "validate only when delegated enrolment is of Individual or Organisation affinity" in {
      val user = UserGenerator.agent("foo")
      forAll(Services.individualServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withDelegatedEnrolment(enrolment))
          .isValid shouldBe true
      }
      forAll(Services.organisationServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withDelegatedEnrolment(enrolment))
          .isValid shouldBe true
      }
      forAll(Services.agentServices) { service =>
        val enrolment = Generator.get(service.generator)("foo").get
        UserValidator
          .validate(user.withDelegatedEnrolment(enrolment))
          .isValid shouldBe false
      }
    }

    "validate only when address is none or valid" in {
      val user = UserGenerator.agent()
      UserValidator.validate(user.copy(address = None)).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address()))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(line1 = Some("f"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(line1 = Some("f" * 35))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(line2 = Some("f"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(line3 = Some("f"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(line4 = Some("f"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(postcode = Some("CX12 6BU"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(countryCode = Some("GB"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(countryCode = Some("GB"))))).isValid shouldBe true
      UserValidator.validate(user.copy(address = Some(User.Address(countryCode = None)))).isValid shouldBe true

      UserValidator.validate(user.copy(address = Some(User.Address(postcode = Some("CX12"))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(postcode = Some(""))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line1 = Some(""))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line2 = Some(""))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line3 = Some(""))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line4 = Some(""))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line1 = Some("a" * 36))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line2 = Some("a" * 36))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line3 = Some("a" * 36))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(line4 = Some("a" * 36))))).isValid shouldBe false
      UserValidator.validate(user.copy(address = Some(User.Address(countryCode = Some(""))))).isValid shouldBe false
    }

    "validate only when suspended regimes are all valid regimes or empty" in {
      val user = UserGenerator.agent()
      UserValidator.validate(user.copy(suspendedRegimes = None)).isValid shouldBe true
      UserValidator
        .validate(user.copy(suspendedRegimes = Some(Set("ITSA", "VATC"))))
        .isValid shouldBe true

      UserValidator.validate(user.copy(suspendedRegimes = Some(Set("foo")))).isValid shouldBe false
      UserValidator
        .validate(user.copy(suspendedRegimes = Some(Set("ITSA", "VATC", "foo"))))
        .isValid shouldBe false
    }
  }

}
