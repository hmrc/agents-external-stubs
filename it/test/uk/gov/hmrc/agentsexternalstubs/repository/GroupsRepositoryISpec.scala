/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.repository

import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec

import java.util.UUID

class GroupsRepositoryISpec extends AppBaseISpec {

  lazy val repo = app.injector.instanceOf[GroupsRepositoryMongo]

  private val aValidEnrolment = Enrolment("HMRC-MTD-VAT", "VRN", "123456789")
  private val anotherValidEnrolment = Enrolment("HMRC-PPT-ORG", "EtmpRegistrationNumber", "XAPPT0000012345")

  "store" should {
    "store a simple group" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(Group(planetId, "foo", AG.Individual), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.groupId shouldBe "foo"
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(Group(planetId = planetId, groupId = "foo", affinityGroup = AG.Individual), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val groupFoo = await(repo.findByGroupId("foo", planetId))
      groupFoo.map(_.groupId) shouldBe Some("foo")

      await(repo.findByGroupId("foo", planetId2)) shouldBe None
    }

    "store a group with simple principal enrolment" in {
      val planetId = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId = planetId,
            groupId = "889foo",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId
        )
      )

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.groupId shouldBe "889foo"
      result.head.principalEnrolments shouldBe Seq(aValidEnrolment)
    }

    "not allow groups with the same principal enrolment on a same planet" in {
      val planetId = UUID.randomUUID().toString
      await(
        repo.create(
          Group(
            planetId,
            "1foo",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A", "1")))))
          ),
          planetId
        )
      )
      val e = repo
        .create(
          Group(
            planetId,
            "foo2",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(
              Enrolment("something", Some(Seq(Identifier("B", "2")))),
              Enrolment("foobar", Some(Seq(Identifier("A", "1"))))
            )
          ),
          planetId
        )
        .failed
        .futureValue

      e shouldBe a[DuplicateGroupException]
      e.getMessage should include("Existing group already has similar principal enrolment FOOBAR~A~1.")
    }

    "allow groups with the same principal enrolment on a different planets" in {
      val planetId1 = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString
      val planetId3 = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId1,
            "1foo",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId1
        )
      )
      await(
        repo.create(
          Group(
            planetId2,
            "foo2",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId2
        )
      )
      await(
        repo.create(
          Group(
            planetId3,
            "foo2",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment, anotherValidEnrolment)
          ),
          planetId3
        )
      )

      val result1 = await(repo.findByPlanetId(planetId1, None)(100))
      val result2 = await(repo.findByPlanetId(planetId2, None)(100))
      val result3 = await(repo.findByPlanetId(planetId3, None)(100))

      result1.size shouldBe 1
      result2.size shouldBe 1
      result3.size shouldBe 1
    }

    "store a group with single principal enrolment" in {
      val planetId = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "abcfoo",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId
        )
      )

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.groupId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq(aValidEnrolment)
    }

    "store a group with multiple principal enrolments" in {
      val planetId = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "foo888",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment, anotherValidEnrolment)
          ),
          planetId
        )
      )

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.groupId shouldBe "foo888"
      result.head.principalEnrolments shouldBe Seq(aValidEnrolment, anotherValidEnrolment)
    }

    "store a group with single delegated enrolment" in {
      val planetId = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "abcfoo",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId
        )
      )

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.groupId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(aValidEnrolment)
    }

    "allow different groups with same delegated enrolment" in {
      val planetId = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "abcfoo1",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId,
            "abcfoo2",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId
        )
      )

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 2
      result.head.groupId shouldBe "abcfoo1"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(aValidEnrolment)
    }

    "allow duplicate groupsId to be created for different planetIds" in {
      val planetId1 = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(Group(planetId1, "foo", AG.Individual), planetId1))
      await(repo.create(Group(planetId2, "foo", AG.Individual), planetId2))

      val result1 = await(repo.findByPlanetId(planetId1, None)(100))
      val result2 = await(repo.findByPlanetId(planetId2, None)(100))

      result1.size shouldBe 1
      result2.size shouldBe 1
    }

    "not allow duplicate groups to be created for the same groupId and planetId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(Group(planetId, "foo", AG.Individual), planetId))

      val e = intercept[DuplicateGroupException] {
        await(repo.create(Group(planetId, "foo", AG.Individual), planetId))
      }

      e.getMessage should include(s"Duplicated group foo")
    }
  }

  "update" should {
    "update an existing group" in {
      val planetId = UUID.randomUUID().toString

      await(repo.create(Group(planetId, "foo", AG.Individual), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      await(repo.update(Group(planetId, "foo", AG.Individual, principalEnrolments = Seq(aValidEnrolment)), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val groupFoo = await(repo.findByGroupId("foo", planetId))
      groupFoo.map(_.principalEnrolments) shouldBe Some(Seq(aValidEnrolment))
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(Group(planetId, "foo", AG.Individual), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      await(
        repo.update(
          Group(planetId, "foo", AG.Individual, principalEnrolments = Seq(aValidEnrolment)),
          planetId
        )
      )
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val groupFoo = await(repo.findByGroupId("foo", planetId))
      groupFoo.map(_.principalEnrolments) shouldBe Some(Seq(aValidEnrolment))

      await(repo.findByGroupId("foo", planetId2)) shouldBe None
    }
  }

  "delete" should {
    "remove a group identified by groupId and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(Group(planetId, "boo", AG.Individual), planetId))
      await(repo.create(Group(planetId, "foo", AG.Individual), planetId))
      await(repo.create(Group(planetId2, "foo", AG.Individual), planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 1

      await(repo.delete("foo", planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 1

      await(repo.delete("foo", planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 0
    }
  }

  "findByPlanetId" should {
    "return id and affinity of groups having provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(
        repo.create(
          Group(planetId, groupId = "g1", affinityGroup = AG.Individual),
          planetId
        )
      )
      await(
        repo.create(
          Group(planetId, groupId = "g2", affinityGroup = AG.Agent),
          planetId
        )
      )
      await(
        repo.create(
          Group(planetId2, groupId = "g3", affinityGroup = AG.Individual),
          planetId2
        )
      )
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val result1 = await(repo.findByPlanetId(planetId, None)(10))
      result1.size shouldBe 2
      result1.map(_.affinityGroup) should contain.only("Individual", "Agent")
      result1.map(_.groupId) should contain.only("g1", "g2")

      val result2 = await(repo.findByPlanetId(planetId, Some("Agent"))(10))
      result2.size shouldBe 1
      result2.map(_.affinityGroup) should contain.only("Agent")
      result2.map(_.groupId) should contain.only("g2")

      val result3 = await(repo.findByPlanetId(planetId, Some("foo"))(10))
      result3.size shouldBe 0

      val result4 = await(repo.findByPlanetId(planetId2, Some("Agent"))(10))
      result4.size shouldBe 0
    }
  }

  "findByGroupId" should {
    "return group having provided groupId and planetId" in {
      val planetId = UUID.randomUUID().toString

      await(repo.create(Group(planetId, "foo", affinityGroup = AG.Individual), planetId = planetId))
      await(repo.create(Group(planetId, "bar", affinityGroup = AG.Individual), planetId = planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val maybeResult1 = await(repo.findByGroupId(groupId = "foo", planetId = planetId))
      maybeResult1 should not be empty
      val result1 = maybeResult1.get
      result1.groupId shouldBe "foo"
      result1.affinityGroup shouldBe AG.Individual
    }
  }

  "findByAgentCode" should {
    "return group having provided agentCode and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(
        repo.create(Group(planetId, "foo1", affinityGroup = AG.Agent, agentCode = Some("ABC")), planetId = planetId)
      )
      await(
        repo.create(Group(planetId2, "foo3", affinityGroup = AG.Agent, agentCode = Some("ABC")), planetId = planetId2)
      )
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = planetId))
      result1 should not be empty
      result1.flatMap(_.agentCode) should contain("ABC")
      result1.map(_.groupId) should contain("foo1")
      result1.map(_.affinityGroup) should contain(AG.Agent)
    }

    "return nothing if no group with the agentCode and planetId exist" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(Group(planetId, "foo1", affinityGroup = AG.Agent), planetId = planetId))
      await(
        repo.create(Group(planetId2, "foo3", affinityGroup = AG.Agent, agentCode = Some("ABC")), planetId = planetId2)
      )
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = planetId))
      result1 should not be defined
    }
  }

  "findByPrincipalEnrolmentKey" should {
    "return group having provided principal enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "foo1",
            affinityGroup = AG.Agent,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId,
            "foo2",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId,
            "foo3",
            affinityGroup = AG.Agent,
            principalEnrolments = Seq(anotherValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId2,
            "foo4",
            affinityGroup = AG.Agent,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId2
        )
      )

      val result1 =
        await(repo.findByPrincipalEnrolmentKey(aValidEnrolment.toEnrolmentKey.get, planetId = planetId))
      result1.isDefined shouldBe true
      result1.get.groupId shouldBe "foo1"
      result1.get.principalEnrolments should contain.only(aValidEnrolment)
    }
  }

  "findByDelegatedEnrolmentKey" should {
    "return groups having provided delegated enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(
        repo.create(
          Group(
            planetId,
            "foo1",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId,
            "foo3",
            affinityGroup = AG.Agent,
            principalEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId,
            "foo2",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId
        )
      )
      await(
        repo.create(
          Group(
            planetId2,
            "foo4",
            affinityGroup = AG.Agent,
            delegatedEnrolments = Seq(aValidEnrolment)
          ),
          planetId = planetId2
        )
      )

      val result1 =
        await(repo.findByDelegatedEnrolmentKey(aValidEnrolment.toEnrolmentKey.get, planetId = planetId)(10))
      result1.size shouldBe 2
      result1.map(_.groupId) should contain.only("foo1", "foo2")
      result1.flatMap(_.delegatedEnrolments).distinct should contain.only(aValidEnrolment)
    }
  }
}
