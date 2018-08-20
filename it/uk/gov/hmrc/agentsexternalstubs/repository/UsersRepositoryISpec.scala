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

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.MongoDbPerTest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class UsersRepositoryISpec extends UnitSpec with OneAppPerSuite with MongoDbPerTest {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri,
        "proxies.start" -> "false"
      )

  override implicit lazy val app: Application = appBuilder.build()

  def repo: UsersRepository = app.injector.instanceOf[UsersRepository]

  "create" should {
    "create a simple user" in {
      await(repo.create(User("foo"), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "foo"
    }

    "respect provided planetId" in {
      await(repo.create(User("foo", planetId = Some("saturn")), "juniper"))
      await(repo.find()).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo","juniper"))
      userFoo.map(_.userId) shouldBe Some("foo")

      await(repo.findByUserId("foo","saturn")) shouldBe None
    }

    "create a user with simple principal enrolment" in {
      await(repo.create(User("889foo", principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "889foo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar"))
    }

    "do not allow users with the same principal enrolment on a same planet" in {
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), "juniper"))
      val e = intercept[DuplicateUserException] {
        await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("something", Some(Seq(Identifier("B","2")))), Enrolment("foobar", Some(Seq(Identifier("A","1")))))), "juniper"))
      }

      e.getMessage should include("Duplicated principal enrolment")
    }

    "allow users with the same principal enrolment on a different planets" in {
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), "juniper"))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))))), "saturn"))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("A","1")))), Enrolment("barfoo", Some(Seq(Identifier("B","2")))))), "mars"))

      val result = await(repo.find())

      result.size shouldBe 3
    }

    "create a user with single principal enrolment" in {
      await(repo.create(User("abcfoo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "create a user with multiple principal enrolments" in {
      await(
        repo.create(
          User(
            "foo888",
            principalEnrolments = Seq(
              Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
              Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345"))))
            )), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "foo888"
      result.head.principalEnrolments shouldBe
        Seq(
          Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
          Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345")))))
    }

    "create a user with single delegated enrolment" in {
      await(repo.create(User("abcfoo", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "allow different users with same delegated enrolment" in {
      await(repo.create(User("abcfoo1", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), "juniper"))
      await(repo.create(User("abcfoo2", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 2
      result.head.userId shouldBe "abcfoo1"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "allow duplicate usersId to be created for different planetIds" in {
      await(repo.create(User("foo"), "juniper"))
      await(repo.create(User("foo"), "saturn"))

      val result = await(repo.find())
      result.size shouldBe 2
    }

    "not allow duplicate users to be created for the same userId and planetId" in {
      await(repo.create(User("foo"), "juniper"))

      val e = intercept[DuplicateUserException] {
        await(repo.create(User("foo"), "juniper"))
      }

      e.getMessage should include("Duplicated userId foo on juniper")
    }

    "allow duplicate users with same nino to be created for different planetIds" in {
      await(repo.create(User("foo", nino = Some(Nino("HW827856C"))), "juniper"))
      await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), "saturn"))

      val result = await(repo.find())
      result.size shouldBe 2

      val userFoo = await(repo.findByNino("HW827856C","juniper"))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW827856C","saturn"))
      userBoo.map(_.userId) shouldBe Some("boo")
    }

    "allow users with different userId and nino to be created for the same planetId" in {
      await(repo.create(User("foo", nino = Some(Nino("YL 97 02 21 C"))), "juniper"))
      await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), "juniper"))

      val result = await(repo.find())
      result.size shouldBe 2

      val userFoo = await(repo.findByNino("YL970221C","juniper"))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW 82 78 56 C","juniper"))
      userBoo.map(_.userId) shouldBe Some("boo")
    }

    "not allow duplicate users to be created for the same nino and planetId" in {
      await(repo.create(User("foo", nino = Some(Nino("HW 82 78 56 C"))), "juniper"))

      val e = intercept[DuplicateUserException] {
        await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), "juniper"))
      }

      e.getMessage should include("Duplicated NINO HW827856C on juniper")
    }
  }

  "update" should {
    "update an existing user" in {
      await(repo.create(User("foo"), "juniper"))
      await(repo.find()).size shouldBe 1

      await(repo.update(User("foo", principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))
      await(repo.find()).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo","juniper"))
      userFoo.map(_.principalEnrolments) shouldBe Some(Seq(Enrolment("foobar")))
    }

    "respect provided planetId" in {
      await(repo.create(User("foo"), "juniper"))
      await(repo.find()).size shouldBe 1

      await(repo.update(User("foo", planetId = Some("saturn"), principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))
      await(repo.find()).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo","juniper"))
      userFoo.map(_.principalEnrolments) shouldBe Some(Seq(Enrolment("foobar")))

      await(repo.findByUserId("foo","saturn")) shouldBe None
    }
  }

  "delete" should {
    "remove a user identified by userId and planetId" in {
      await(repo.create(User("boo"), "juniper"))
      await(repo.create(User("foo"), "juniper"))
      await(repo.create(User("foo"), "saturn"))
      await(repo.find()).size shouldBe 3

      await(repo.delete("foo", "juniper"))
      await(repo.find()).size shouldBe 2

      await(repo.delete("foo", "saturn"))
      await(repo.find()).size shouldBe 1
    }
  }

  "findByPlanetId" should {
    "return id and affinity of users having provided planetId" in {
      await(repo.create(User("boo", groupId= Some("g1"), affinityGroup = Some("Individual"), credentialRole = Some("User")), "juniper"))
      await(repo.create(User("foo", groupId= Some("g2"), affinityGroup = Some("Agent"), credentialRole = Some("Admin")), "juniper"))
      await(repo.create(User("foo", groupId= Some("g3"), affinityGroup = Some("Individual"), credentialRole = Some("Assistant")), "saturn"))
      await(repo.find()).size shouldBe 3

      val result1 = await(repo.findByPlanetId("juniper", None)(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain.only("foo", "boo")
      result1.flatMap(_.affinityGroup) should contain.only("Individual", "Agent")
      result1.flatMap(_.groupId) should contain.only("g1", "g2")
      result1.flatMap(_.credentialRole) should contain.only("Admin", "User")

      val result2 = await(repo.findByPlanetId("juniper", Some("Agent"))(10))
      result2.size shouldBe 1
      result2.map(_.userId) should contain.only("foo")
      result2.flatMap(_.affinityGroup) should contain.only("Agent")
      result2.flatMap(_.groupId) should contain.only("g2")
      result2.flatMap(_.credentialRole) should contain.only("Admin")

      val result3 = await(repo.findByPlanetId("juniper", Some("foo"))(10))
      result3.size shouldBe 0

      val result4 = await(repo.findByPlanetId("saturn", Some("Agent"))(10))
      result4.size shouldBe 0
    }
  }

  "findByGroupId" should {
    "return users having provided groupId and planetId" in {
      await(repo.create(User("boo", affinityGroup = Some("Individual"), groupId = Some("ABC")), planetId = "juniper"))
      await(repo.create(User("foo", affinityGroup = Some("Agent"), groupId = Some("ABC")), planetId = "juniper"))
      await(repo.create(User("zoo", affinityGroup = Some("Individual"), groupId = Some("ABC")), planetId = "saturn"))
      await(repo.find()).size shouldBe 3

      val result1 = await(repo.findByGroupId(groupId = "ABC", planetId = "juniper")(10))
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
      await(repo.create(User("foo1", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = "juniper"))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = "juniper"))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = "saturn"))
      await(repo.find()).size shouldBe 3

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = "juniper")(10))
      result1.size shouldBe 2
      result1.flatMap(_.agentCode) should contain("ABC")
      result1.map(_.userId) should contain("foo1")
      result1.map(_.userId) should contain("foo2")
      result1.flatMap(_.affinityGroup) should contain("Agent")
    }

    "return empty list if none user with the agentCode and planetId exist" in {
      await(repo.create(User("foo1", affinityGroup = Some("Agent")), planetId = "juniper"))
      await(repo.create(User("foo2", affinityGroup = Some("Individual")), planetId = "juniper"))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), agentCode = Some("ABC")), planetId = "saturn"))
      await(repo.find()).size shouldBe 3

      val result1 = await(repo.findByAgentCode(agentCode = "ABC", planetId = "juniper")(10))
      result1.size shouldBe 0
    }
  }

  "findByPrincipalEnrolmentKey" should {
    "return user having provided principal enrolment key" in {
      await(repo.create(User("foo1", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","222")))))), planetId = "juniper"))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "saturn"))

      val result1 = await(repo.findByPrincipalEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = "juniper"))
      result1.isDefined shouldBe true
      result1.get.userId shouldBe "foo1"
      result1.get.principalEnrolments should contain.only(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))
    }
  }

  "findByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      await(repo.create(User("foo1", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "saturn"))

      val result1 = await(repo.findByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = "juniper")(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain.only("foo1","foo2")
      result1.flatMap(_.delegatedEnrolments).distinct should contain.only(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))
    }
  }

  "findUserIdsByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      await(repo.create(User("foo1", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo3", affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo2", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo4", affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "saturn"))

      val result1 = await(repo.findUserIdsByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = "juniper")(10))
      result1.size shouldBe 2
      result1 should contain.only("foo1","foo2")
    }
  }

  "findGroupIdsByDelegatedEnrolmentKey" should {
    "return users having provided delegated enrolment key" in {
      await(repo.create(User("foo1", groupId = Some("group1"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo3", groupId = Some("group2"), affinityGroup = Some("Agent"), principalEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo2", groupId = Some("group3"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("BAR", Some(Seq(Identifier("AAA","111")))))), planetId = "juniper"))
      await(repo.create(User("foo4", groupId = Some("group4"), affinityGroup = Some("Agent"), delegatedEnrolments = Seq(Enrolment("FOO", Some(Seq(Identifier("AAA","111")))))), planetId = "saturn"))

      val result1 = await(repo.findGroupIdsByDelegatedEnrolmentKey(EnrolmentKey.from("FOO", "AAA" -> "111"), planetId = "juniper")(10))
      result1.size shouldBe 1
      result1 should contain.only(Some("group1"))
    }
  }

  "findAdminByGroupId" should {
    "return Admin user having provided groupId" in {
      await(repo.create(UserGenerator.individual("foo1", groupId = "group1", credentialRole = User.CR.Admin), planetId = "juniper"))
      await(repo.create(UserGenerator.individual("foo2", groupId = "group1", credentialRole = User.CR.User), planetId = "juniper"))
      await(repo.create(UserGenerator.individual("foo3", groupId = "group1", credentialRole = User.CR.Assistant), planetId = "juniper"))
      await(repo.create(UserGenerator.individual("foo4", groupId = "group2", credentialRole = User.CR.Admin), planetId = "juniper"))

      val result1 = await(repo.findAdminByGroupId("group1", planetId = "juniper"))

      result1.size shouldBe 1
      result1.map(_.userId) shouldBe Some("foo1")

      val result2 = await(repo.findAdminByGroupId("group2", planetId = "juniper"))

      result2.size shouldBe 1
      result2.map(_.userId) shouldBe Some("foo4")

      val result3 = await(repo.findAdminByGroupId("group3", planetId = "juniper"))

      result3.size shouldBe 0
    }
  }

  "findAdminByAgentCode" should {
    "return Admin user having provided groupId" in {
      await(repo.create(UserGenerator.agent("foo1", agentCode = "ABC123", credentialRole = User.CR.Admin), planetId = "juniper"))
      await(repo.create(UserGenerator.agent("foo2", agentCode = "ABC123", credentialRole = User.CR.User), planetId = "juniper"))
      await(repo.create(UserGenerator.agent("foo3", agentCode = "ABC123", credentialRole = User.CR.Assistant), planetId = "juniper"))
      await(repo.create(UserGenerator.agent("foo4", groupId="group2", agentCode = "ABC456", credentialRole = User.CR.Admin), planetId = "juniper"))

      val result1 = await(repo.findAdminByAgentCode("ABC123", planetId = "juniper"))

      result1.size shouldBe 1
      result1.map(_.userId) shouldBe Some("foo1")

      val result2 = await(repo.findAdminByAgentCode("ABC456", planetId = "juniper"))

      result2.size shouldBe 1
      result2.map(_.userId) shouldBe Some("foo4")

      val result3 = await(repo.findAdminByAgentCode("FOOBAR", planetId = "juniper"))

      result3.size shouldBe 0
    }
  }
}
