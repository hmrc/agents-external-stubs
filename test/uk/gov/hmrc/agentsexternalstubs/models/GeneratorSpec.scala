/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.Inspectors.*
import uk.gov.hmrc.agentsexternalstubs.models.Generator.GenOps.*
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class GeneratorSpec extends BaseUnitSpec {

  private val seeds = Seq("foo", "bar", "baz", "zig")

  "Generator" should {
    "generate stable and valid identifiers for the same seed" in
      forAll(seeds) { seed =>
        val nino = Generator.get(Generator.ninoNoSpacesGen)(seed).value
        val arn = Generator.get(Generator.arnGen)(seed).value
        val utr = Generator.get(Generator.utrGen)(seed).value
        val vrn = Generator.get(Generator.vrnGen)(seed).value
        val date = Generator.get(Generator.dateYYYYMMDDGen)(seed).value
        val email = Generator.get(Generator.emailGen)(seed).value
        val postcode = Generator.get(Generator.postcode)(seed).value

        Generator.get(Generator.ninoNoSpacesGen)(seed).value shouldBe nino
        Generator.get(Generator.arnGen)(seed).value shouldBe arn
        Generator.get(Generator.utrGen)(seed).value shouldBe utr
        Generator.get(Generator.vrnGen)(seed).value shouldBe vrn
        Generator.get(Generator.dateYYYYMMDDGen)(seed).value shouldBe date
        Generator.get(Generator.emailGen)(seed).value shouldBe email
        Generator.get(Generator.postcode)(seed).value shouldBe postcode

        RegexPatterns.validNinoNoSpaces(nino).isRight shouldBe true
        RegexPatterns.validArn(arn).isRight shouldBe true
        RegexPatterns.validUtr(utr).isRight shouldBe true
        RegexPatterns.validVrn(vrn).isRight shouldBe true
        RegexPatterns.validDate(date).isRight shouldBe true
        email should include("@")
        RegexPatterns.validPostcode(postcode).isRight shouldBe true
      }

    "generate stable address records and perturb variants" in {
      forAll(seeds) { seed =>
        val address = Generator.get(Generator.address4Lines35Gen)(seed).value

        Generator.get(Generator.address4Lines35Gen)(seed).value shouldBe address
        address.line1 should not be empty
        address.line2 should not be empty
        address.line3 should not be empty
        address.line4 should not be empty
        address.line1.length should be <= 35
        address.line2.length should be <= 35
        address.line3.length should be <= 35
        address.line4.length should be <= 35
        RegexPatterns.validPostcode(address.line4).isRight shouldBe true
      }

      val base = Generator.get(Generator.dateYYYYMMDDGen)("foo").value
      val variant = Generator.get(Generator.dateYYYYMMDDGen.variant("secondary"))("foo").value

      Generator.get(Generator.dateYYYYMMDDGen.variant("secondary"))("foo").value shouldBe variant
      variant should not be base
      RegexPatterns.validDate(variant).isRight shouldBe true
    }
  }

  "UserGenerator" should {
    "produce stable individual users with the expected default shape" in
      forAll(seeds) { seed =>
        val user = UserGenerator.individual(seed)

        UserGenerator.individual(seed) shouldBe user
        user.userId shouldBe seed
        user.confidenceLevel shouldBe Some(50)
        user.credentialRole shouldBe None
        user.name shouldBe defined
        user.dateOfBirth shouldBe defined
        user.groupId shouldBe defined
        user.nino shouldBe defined
      }

    "produce stable agent and organisation users" in
      forAll(seeds) { seed =>
        val agent = UserGenerator.agent(seed)
        val organisation = UserGenerator.organisation(seed)

        UserGenerator.agent(seed) shouldBe agent
        UserGenerator.organisation(seed) shouldBe organisation

        agent.userId shouldBe seed
        agent.groupId shouldBe defined
        agent.name shouldBe defined
        agent.nino shouldBe defined
        agent.assignedPrincipalEnrolments shouldBe empty

        organisation.userId shouldBe seed
        organisation.groupId shouldBe defined
        organisation.name shouldBe defined
        organisation.credentialRole shouldBe Some(User.CR.User)
      }
  }

  "GroupGenerator" should {
    "dispatch generate to the expected affinity-group factory" in {
      val planetId = "planet-1"

      GroupGenerator.generate(planetId, AG.Individual).affinityGroup shouldBe AG.Individual
      GroupGenerator.generate(planetId, AG.Organisation).affinityGroup shouldBe AG.Organisation
      GroupGenerator.generate(planetId, AG.Agent).affinityGroup shouldBe AG.Agent
    }

    "produce stable helper ids and names for the same seed" in
      forAll(seeds) { seed =>
        GroupGenerator.groupId(seed) shouldBe GroupGenerator.groupId(seed)
        GroupGenerator.agentCode(seed) shouldBe GroupGenerator.agentCode(seed)
        GroupGenerator.agentId(seed) shouldBe GroupGenerator.agentId(seed)
        GroupGenerator.nameForIndividual(seed) shouldBe GroupGenerator.nameForIndividual(seed)
        GroupGenerator.nameForAgent(seed) shouldBe GroupGenerator.nameForAgent(seed)
        GroupGenerator.nameForOrganisation(seed) shouldBe GroupGenerator.nameForOrganisation(seed)
      }

    "produce groups with the expected shape" in
      forAll(seeds) { seed =>
        val individual = GroupGenerator.individual(seed, None)
        val agent = GroupGenerator.agent(seed, None)
        val organisation = GroupGenerator.organisation(seed, None)

        individual.planetId shouldBe seed
        individual.affinityGroup shouldBe AG.Individual
        individual.groupId should not be empty

        agent.planetId shouldBe seed
        agent.affinityGroup shouldBe AG.Agent
        agent.groupId should not be empty
        agent.agentCode shouldBe defined
        agent.agentFriendlyName shouldBe defined
        agent.agentId shouldBe defined

        organisation.planetId shouldBe seed
        organisation.affinityGroup shouldBe AG.Organisation
        organisation.groupId should not be empty
      }
  }
}
