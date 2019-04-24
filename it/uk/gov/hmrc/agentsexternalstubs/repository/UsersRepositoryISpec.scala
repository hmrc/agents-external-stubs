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

import java.util.UUID

import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.{AppBaseISpec, MongoDB}
import uk.gov.hmrc.domain.Nino

class UsersRepositoryISpec extends AppBaseISpec with MongoDB {

  lazy val repo = app.injector.instanceOf[UsersRepository]

  "store" should {
    "store a simple user" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo"), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "foo"
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo", planetId = Some(planetId)), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo",planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      await(repo.findByUserId("foo",planetId2)) shouldBe None
    }

    "store a user with simple principal enrolment" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("889foo", principalEnrolments = Seq(Enrolment("foobar"))), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "889foo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar"))
    }

    "do not allow users with the same principal enrolment on a same planet" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), planetId))
      val e = intercept[DuplicateUserException] {
        await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("something", Some(Seq(Identifier("B","2")))), Enrolment("foobar", Some(Seq(Identifier("A","1")))))), planetId))
      }

      e.getMessage should include("Existing user already has similar enrolment FOOBAR~A~1.")
    }

    "allow users with the same principal enrolment on a different planets" in {
      val planetId1 = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString
      val planetId3 = UUID.randomUUID().toString
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), planetId1))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), planetId2))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))), Enrolment("barfoo", Some(Seq(Identifier("B","2")))))), planetId3))

      val result1 = await(repo.findByPlanetId(planetId1, None)(100))
      val result2 = await(repo.findByPlanetId(planetId2, None)(100))
      val result3 = await(repo.findByPlanetId(planetId3, None)(100))

      result1.size shouldBe 1
      result2.size shouldBe 1
      result3.size shouldBe 1
    }

    "store a user with single principal enrolment" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("abcfoo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "store a user with multiple principal enrolments" in {
      val planetId = UUID.randomUUID().toString
      await(
        repo.create(
          User(
            "foo888",
            principalEnrolments = Seq(
              Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
              Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345"))))
            )), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "foo888"
      result.head.principalEnrolments shouldBe
        Seq(
          Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
          Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345")))))
    }

    "store a user with single delegated enrolment" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("abcfoo", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "allow different users with same delegated enrolment" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("abcfoo1", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), planetId))
      await(repo.create(User("abcfoo2", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))

      result.size shouldBe 2
      result.head.userId shouldBe "abcfoo1"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "allow duplicate usersId to be created for different planetIds" in {
      val planetId1 = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo"), planetId1))
      await(repo.create(User("foo"), planetId2))

      val result1 = await(repo.findByPlanetId(planetId1, None)(100))
      val result2 = await(repo.findByPlanetId(planetId2, None)(100))

      result1.size shouldBe 1
      result2.size shouldBe 1
    }

    "not allow duplicate users to be created for the same userId and planetId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo"), planetId))

      val e = intercept[DuplicateUserException] {
        await(repo.create(User("foo"), planetId))
      }

      e.getMessage should include(s"Duplicated user foo")
    }

    "allow duplicate users with same nino to be created for different planetIds" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo", nino = Some(Nino("HW827856C"))), planetId))
      await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), planetId2))

      val result1 = await(repo.findByPlanetId(planetId, None)(100))
      result1.size shouldBe 1

      val result2 = await(repo.findByPlanetId(planetId2, None)(100))
      result2.size shouldBe 1

      val userFoo = await(repo.findByNino("HW827856C",planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW827856C",planetId2))
      userBoo.map(_.userId) shouldBe Some("boo")
    }

    "allow users with different userId and nino to be created for the same planetId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo", nino = Some(Nino("YL 97 02 21 C"))), planetId))
      await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), planetId))

      val result = await(repo.findByPlanetId(planetId, None)(100))
      result.size shouldBe 2

      val userFoo = await(repo.findByNino("YL970221C",planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW 82 78 56 C",planetId))
      userBoo.map(_.userId) shouldBe Some("boo")
    }

    "not allow duplicate users to be created for the same nino and planetId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo", nino = Some(Nino("HW 82 78 56 C"))), planetId))

      val e = intercept[DuplicateUserException] {
        await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), planetId))
      }

      e.getMessage should include("Existing user already has this NINO HW827856C")
    }
  }

  "update" should {
    "update an existing user" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo"), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      await(repo.update(User("foo", principalEnrolments = Seq(Enrolment("foobar"))), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo",planetId))
      userFoo.map(_.principalEnrolments) shouldBe Some(Seq(Enrolment("foobar")))
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo"), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      await(repo.update(User("foo", planetId = Some(planetId), principalEnrolments = Seq(Enrolment("foobar"))), planetId))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo",planetId))
      userFoo.map(_.principalEnrolments) shouldBe Some(Seq(Enrolment("foobar")))

      await(repo.findByUserId("foo",planetId2)) shouldBe None
    }
  }

  "delete" should {
    "remove a user identified by userId and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo"), planetId))
      await(repo.create(User("foo"), planetId))
      await(repo.create(User("foo"), planetId2))
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

  "deleteAll" should {
    "remove all users from the database" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo"), planetId))
      await(repo.create(User("foo"), planetId))
      await(repo.create(User("foo"), planetId2))
      await(repo.create(User("zoo"), planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 2

      await(repo.deleteAll(System.currentTimeMillis()))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 0
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 0
    }

    "remove all users created more than some timestamp" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo"), planetId))
      await(repo.create(User("foo"), planetId))
      await(repo.create(User("foo"), planetId2))
      val t0 = System.currentTimeMillis()
      Thread.sleep(100)
      await(repo.create(User("zoo"), planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 2

      await(repo.deleteAll(t0)) should be >= 3

      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 0
      await(repo.findByPlanetId(planetId2, None)(100)).size shouldBe 1
    }
  }

  "findByPlanetId" should {
    "return id and affinity of users having provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo", groupId= Some("g1"), affinityGroup = Some("Individual"), credentialRole = Some("User")), planetId))
      await(repo.create(User("foo", groupId= Some("g2"), affinityGroup = Some("Agent"), credentialRole = Some("Admin")), planetId))
      await(repo.create(User("foo", groupId= Some("g3"), affinityGroup = Some("Individual"), credentialRole = Some("Assistant")), planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val result1 = await(repo.findByPlanetId(planetId, None)(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain.only("foo", "boo")
      result1.flatMap(_.affinityGroup) should contain.only("Individual", "Agent")
      result1.flatMap(_.groupId) should contain.only("g1", "g2")
      result1.flatMap(_.credentialRole) should contain.only("Admin", "User")

      val result2 = await(repo.findByPlanetId(planetId, Some("Agent"))(10))
      result2.size shouldBe 1
      result2.map(_.userId) should contain.only("foo")
      result2.flatMap(_.affinityGroup) should contain.only("Agent")
      result2.flatMap(_.groupId) should contain.only("g2")
      result2.flatMap(_.credentialRole) should contain.only("Admin")

      val result3 = await(repo.findByPlanetId(planetId, Some("foo"))(10))
      result3.size shouldBe 0

      val result4 = await(repo.findByPlanetId(planetId2, Some("Agent"))(10))
      result4.size shouldBe 0
    }
  }

  "findByGroupId" should {
    "return users having provided groupId and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo", affinityGroup = Some("Individual"), groupId = Some("ABC")), planetId = planetId))
      await(repo.create(User("foo", affinityGroup = Some("Agent"), groupId = Some("ABC")), planetId = planetId))
      await(repo.create(User("zoo", affinityGroup = Some("Individual"), groupId = Some("ABC")), planetId = planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val result1 = await(repo.findByGroupId(groupId = "ABC", planetId = planetId)(10))
      result1.size shouldBe 2
      result1.flatMap(_.groupId) should contain("ABC")
      result1.map(_.userId) should contain("foo")
      result1.map(_.userId) should contain("boo")
      result1.flatMap(_.affinityGroup) should contain("Individual")
      result1.flatMap(_.affinityGroup) should contain("Agent")
    }
  }

  "findByAgentCode" should {
    "return users having provided agentCode and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = planetId))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = planetId))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = planetId)(10))
      result1.size shouldBe 2
      result1.flatMap(_.agentCode) should contain("ABC")
      result1.map(_.userId) should contain("foo1")
      result1.map(_.userId) should contain("foo2")
      result1.flatMap(_.affinityGroup) should contain("Agent")
    }

    "return empty list if none user with the agentCode and planetId exist" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", affinityGroup = Some("Agent")), planetId = planetId))
      await(repo.create(User("foo2", affinityGroup = Some("Individual")), planetId = planetId))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = planetId2))
      await(repo.findByPlanetId(planetId, None)(100)).size shouldBe 2

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = planetId)(10))
      result1.size shouldBe 0
    }
  }

  "findByPrincipalEnrolmentKey" should {
    "return user having provided principal enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","222")))))), planetId = planetId))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId2))

      val result1 = await(repo.findByPrincipalEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = planetId))
      result1.isDefined shouldBe true
      result1.get.userId shouldBe "foo1"
      result1.get.principalEnrolments should contain.only(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))
    }
  }

  "findByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId2))

      val result1 = await(repo.findByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = planetId)(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain.only("foo1","foo2")
      result1.flatMap(_.delegatedEnrolments).distinct should contain.only(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))
    }
  }

  "findUserIdsByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId2))

      val result1 = await(repo.findUserIdsByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = planetId)(10))
      result1.size shouldBe 2
      result1 should contain.only("foo1","foo2")
    }
  }

  "findGroupIdsByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo1", groupId = Some("group1"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo3", groupId = Some("group2"), affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo2", groupId = Some("group3"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("BAR", Some(Seq(Identifier("AAA","111")))))), planetId = planetId))
      await(repo.create(User("foo4", groupId = Some("group4"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = planetId2))

      val result1 = await(repo.findGroupIdsByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = planetId)(10))
      result1.size shouldBe 1
      result1 should contain.only(Some("group1"))
    }
  }

  "findAdminByGroupId" should {
    "return Admin user having provided groupId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(UserGenerator.individual("foo1", groupId = "group1", credentialRole = User.CR.Admin), planetId = planetId))
      await(repo.create(UserGenerator.individual("foo2", groupId = "group1", credentialRole = User.CR.User), planetId = planetId))
      await(repo.create(UserGenerator.individual("foo3", groupId = "group1", credentialRole = User.CR.Assistant), planetId = planetId))
      await(repo.create(UserGenerator.individual("foo4", groupId = "group2", credentialRole = User.CR.Admin), planetId = planetId))

      val result1 = await(repo.findAdminByGroupId("group1", planetId = planetId))

      result1.size shouldBe 1
      result1.map(_.userId) shouldBe Some("foo1")

      val result2 = await(repo.findAdminByGroupId("group2", planetId = planetId))

      result2.size shouldBe 1
      result2.map(_.userId) shouldBe Some("foo4")

      val result3 = await(repo.findAdminByGroupId("group3", planetId = planetId))

      result3.size shouldBe 0
    }
  }

  "findAdminByAgentCode" should {
    "return Admin user having provided groupId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(UserGenerator.agent("foo1", agentCode = "ABC123", credentialRole = User.CR.Admin), planetId = planetId))
      await(repo.create(UserGenerator.agent("foo2", agentCode = "ABC123", credentialRole = User.CR.User), planetId = planetId))
      await(repo.create(UserGenerator.agent("foo3", agentCode = "ABC123", credentialRole = User.CR.Assistant), planetId = planetId))
      await(repo.create(UserGenerator.agent("foo4", groupId="group2", agentCode = "ABC456", credentialRole = User.CR.Admin), planetId = planetId))

      val result1 = await(repo.findAdminByAgentCode("ABC123", planetId = planetId))

      result1.size shouldBe 1
      result1.map(_.userId) shouldBe Some("foo1")

      val result2 = await(repo.findAdminByAgentCode("ABC456", planetId = planetId))

      result2.size shouldBe 1
      result2.map(_.userId) shouldBe Some("foo4")

      val result3 = await(repo.findAdminByAgentCode("FOOBAR", planetId = planetId))

      result3.size shouldBe 0
    }
  }

  "syncRecordId" should {
    "add record id to the array" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(UserGenerator.agent("foo1", agentCode = "ABC123", credentialRole = User.CR.Admin), planetId = planetId))

      await(repo.syncRecordId("foo1","123456789", planetId))

      val userOpt = await(repo.findByUserId("foo1", planetId))
      userOpt.map(_.recordIds).get should contain.only("123456789")
    }
  }
}
