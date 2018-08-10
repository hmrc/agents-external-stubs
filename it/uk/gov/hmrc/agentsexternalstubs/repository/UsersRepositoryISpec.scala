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
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, Identifier, User}
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

    "create multiple users with the same principal enrolment" in {
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))
      await(repo.create(User("3oo", principalEnrolments = Seq(Enrolment("foobar"))), "juniper"))

      val result = await(repo.find())

      result.size shouldBe 3
      result.flatMap(_.principalEnrolments).toSet shouldBe Set(Enrolment("foobar"))
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
    "return id and affinity of users having given planetId" in {
      await(repo.create(User("boo", affinityGroup = Some("Individual")), "juniper"))
      await(repo.create(User("foo", affinityGroup = Some("Agent")), "juniper"))
      await(repo.create(User("foo", affinityGroup = Some("Individual")), "saturn"))
      await(repo.find()).size shouldBe 3

      val result1 = await(repo.findByPlanetId("juniper", None)(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain("foo")
      result1.map(_.userId) should contain("boo")
      result1.flatMap(_.affinityGroup) should contain("Individual")
      result1.flatMap(_.affinityGroup) should contain("Agent")

      val result2 = await(repo.findByPlanetId("juniper", Some("Agent"))(10))
      result2.size shouldBe 1
      result2.map(_.userId) should contain("foo")
      result2.map(_.userId) should not contain "boo"
      result2.flatMap(_.affinityGroup) should not contain "Individual"
      result2.flatMap(_.affinityGroup) should contain("Agent")

      val result3 = await(repo.findByPlanetId("juniper", Some("foo"))(10))
      result3.size shouldBe 0

      val result4 = await(repo.findByPlanetId("saturn", Some("Agent"))(10))
      result4.size shouldBe 0
    }
  }

  "findByGroupId" should {
    "return users having given groupId and planetId" in {
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
    "return users having given agentCode and planetId" in {
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
}
