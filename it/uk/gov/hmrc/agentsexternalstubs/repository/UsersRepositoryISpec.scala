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
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec
import uk.gov.hmrc.domain.Nino
import play.api.test.Helpers._

class UsersRepositoryISpec extends AppBaseISpec {

  lazy val repo = app.injector.instanceOf[UsersRepositoryMongo]

  "store" should {
    "store a simple user" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo"), planetId))

      val result = await(repo.findByPlanetId(planetId)(100))

      result.size shouldBe 1
      result.head.userId shouldBe "foo"
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo", planetId = Some(planetId)), planetId))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo", planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      await(repo.findByUserId("foo", planetId2)) shouldBe None
    }

    "allow duplicate usersId to be created for different planetIds" in {
      val planetId1 = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo"), planetId1))
      await(repo.create(User("foo"), planetId2))

      val result1 = await(repo.findByPlanetId(planetId1)(100))
      val result2 = await(repo.findByPlanetId(planetId2)(100))

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

      val result1 = await(repo.findByPlanetId(planetId)(100))
      result1.size shouldBe 1

      val result2 = await(repo.findByPlanetId(planetId2)(100))
      result2.size shouldBe 1

      val userFoo = await(repo.findByNino("HW827856C", planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW827856C", planetId2))
      userBoo.map(_.userId) shouldBe Some("boo")
    }

    "allow users with different userId and nino to be created for the same planetId" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(User("foo", nino = Some(Nino("YL 97 02 21 C"))), planetId))
      await(repo.create(User("boo", nino = Some(Nino("HW827856C"))), planetId))

      val result = await(repo.findByPlanetId(planetId)(100))
      result.size shouldBe 2

      val userFoo = await(repo.findByNino("YL970221C", planetId))
      userFoo.map(_.userId) shouldBe Some("foo")

      val userBoo = await(repo.findByNino("HW 82 78 56 C", planetId))
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
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1

      await(repo.update(User("foo", name = Some("New Name")), planetId))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo", planetId))
      userFoo.flatMap(_.name) shouldBe Some("New Name")
    }

    "respect provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("foo"), planetId))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1

      await(
        repo.update(
          User("foo", planetId = Some(planetId), name = Some("New Name")),
          planetId
        )
      )
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1

      val userFoo = await(repo.findByUserId("foo", planetId))
      userFoo.flatMap(_.name) shouldBe Some("New Name")

      await(repo.findByUserId("foo", planetId2)) shouldBe None
    }
  }

  "delete" should {
    "remove a user identified by userId and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo"), planetId))
      await(repo.create(User("foo"), planetId))
      await(repo.create(User("foo"), planetId2))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 2
      await(repo.findByPlanetId(planetId2)(100)).size shouldBe 1

      await(repo.delete("foo", planetId))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1
      await(repo.findByPlanetId(planetId2)(100)).size shouldBe 1

      await(repo.delete("foo", planetId2))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 1
      await(repo.findByPlanetId(planetId2)(100)).size shouldBe 0
    }
  }

  "findByPlanetId" should {
    "return id of users having provided planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(
        repo.create(
          User("boo", groupId = Some("g1"), credentialRole = Some("User")),
          planetId
        )
      )
      await(
        repo.create(
          User("foo", groupId = Some("g2"), credentialRole = Some("Admin")),
          planetId
        )
      )
      await(
        repo.create(
          User("foo", groupId = Some("g3"), credentialRole = Some("Assistant")),
          planetId2
        )
      )
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 2

      val result1 = await(repo.findByPlanetId(planetId)(10))
      result1.size shouldBe 2
      result1.map(_.userId) should contain.only("foo", "boo")
      result1.flatMap(_.groupId) should contain.only("g1", "g2")
      result1.flatMap(_.credentialRole) should contain.only("Admin", "User")
    }
  }

  "findByGroupId" should {
    "return users having provided groupId and planetId" in {
      val planetId = UUID.randomUUID().toString
      val planetId2 = UUID.randomUUID().toString

      await(repo.create(User("boo", groupId = Some("ABC")), planetId = planetId))
      await(repo.create(User("foo", groupId = Some("ABC")), planetId = planetId))
      await(repo.create(User("zoo", groupId = Some("ABC")), planetId = planetId2))
      await(repo.findByPlanetId(planetId)(100)).size shouldBe 2

      val result1 = await(repo.findByGroupId(groupId = "ABC", planetId = planetId)(limit = Some(10)))
      result1.size shouldBe 2
      result1.flatMap(_.groupId) should contain("ABC")
      result1.map(_.userId) should contain("foo")
      result1.map(_.userId) should contain("boo")
    }
  }

  "findAdminByGroupId" should {
    "return Admin user having provided groupId" in {
      val planetId = UUID.randomUUID().toString
      await(
        repo.create(
          UserGenerator.individual("foo1", groupId = "group1", credentialRole = User.CR.Admin),
          planetId = planetId
        )
      )
      await(
        repo.create(
          UserGenerator.individual("foo2", groupId = "group1", credentialRole = User.CR.User),
          planetId = planetId
        )
      )
      await(
        repo.create(
          UserGenerator.individual("foo3", groupId = "group1", credentialRole = User.CR.Assistant),
          planetId = planetId
        )
      )
      await(
        repo.create(
          UserGenerator.individual("foo4", groupId = "group2", credentialRole = User.CR.Admin),
          planetId = planetId
        )
      )

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

  "syncRecordId" should {
    "add record id to the array" in {
      val planetId = UUID.randomUUID().toString
      await(
        repo.create(
          UserGenerator.agent("foo1", credentialRole = User.CR.Admin),
          planetId = planetId
        )
      )

      await(repo.syncRecordId("foo1", "123456789", planetId))

      val userOpt = await(repo.findByUserId("foo1", planetId))
      userOpt.map(_.recordIds).get should contain.only("123456789")
    }
  }
}
