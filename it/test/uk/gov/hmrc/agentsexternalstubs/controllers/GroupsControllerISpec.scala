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

package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.GroupsService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class GroupsControllerISpec extends ServerBaseISpec with TestRequests with TestStubs with BeforeAndAfterEach {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  override lazy val groupsService = app.injector.instanceOf[GroupsService]

  private val aValidEnrolment = Enrolment("HMRC-MTD-VAT", "VRN", "123456789")

  "GroupsController" when {

    "GET /agents-external-stubs/groups/:groupId" should {
      "return 404 NotFound for non existent group id" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Groups.get("1261a762761")
        result should haveStatus(404)
      }

      "return 404 NotFound if group exists but on a different planet" in {
        SignIn.signInAndGetSession()
        implicit val authSession2: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        val result = Groups.get("foo")
        result should haveStatus(404)
      }

      "return an existing group" in {
        userService
          .createUser(UserGenerator.individual("testUser", groupId = "testGroup"), "testPlanet", Some(AG.Individual))
          .futureValue
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("testUser", planetId = "testPlanet")
        val result = Groups.get("testGroup")
        result should haveStatus(200)
        val group = result.json.as[Group]
        group.groupId shouldBe "testGroup"
        group.planetId shouldBe "testPlanet"
      }
    }

    "POST /agents-external-stubs/groups/" should {
      "store a new group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Groups.create(
          GroupGenerator.generate(authSession.planetId, AG.Individual, groupId = Some("yuwyquhh"))
        )
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/groups/yuwyquhh")
      }

      "fail if trying to store group with duplicated groupId on the same planet" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result1 = Groups.create(
          GroupGenerator.generate(authSession.planetId, AG.Individual, groupId = Some("yuwyquhh"))
        )
        result1 should haveStatus(201)
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/groups/yuwyquhh")
        val result2 = Groups.create(
          GroupGenerator.generate(authSession.planetId, AG.Individual, groupId = Some("yuwyquhh"))
        )
        result2.status shouldBe Status.CONFLICT
      }

      "sanitize invalid group and succeed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Groups.create(
          GroupGenerator.generate(authSession.planetId, AG.Individual, groupId = Some("yuwyquhh"))
        )
        result should haveStatus(201)
      }
    }

    "PUT /agents-external-stubs/groups" should {
      "update current group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val group = GroupGenerator.generate(authSession.planetId, AG.Individual, groupId = Some("someGroupId"))
        Groups.create(group)
        val result = Groups.updateCurrent(group.copy(principalEnrolments = Seq(aValidEnrolment)))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/groups/someGroupId")
        val result2 = Groups.get("someGroupId")
        result2.json.as[Group].principalEnrolments should contain(aValidEnrolment)
      }
    }

    "PUT /agents-external-stubs/groups/:groupId" should {
      "return 404 if groupId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Groups.update(
          Group(
            authSession.planetId,
            "7728378273",
            affinityGroup = AG.Individual,
            principalEnrolments = Seq(aValidEnrolment)
          )
        )
        result should haveStatus(404)
      }

      "update an existing group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val group = Group(authSession.planetId, "7728378273", AG.Individual)
        Groups.create(group)
        val result = Groups.update(group.copy(principalEnrolments = Seq(aValidEnrolment)))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/groups/7728378273")
        val result2 = Groups.get("7728378273")
        result2.json.as[Group].principalEnrolments should contain(aValidEnrolment)
      }
    }

    "DELETE /agents-external-stubs/groups/:groupId" should {
      "return 204 if group can be removed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val groupId = Users.get(authSession.userId).json.as[User].groupId.get
        val result = Groups.delete(groupId)
        result should haveStatus(204)
      }

      "return 404 if groupId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Groups.delete("ABC123")
        result should haveStatus(404)
      }
    }

    "GET /agents-external-stubs/groups" should {
      "return 200 with the list of all groups on the current planet only" in {
        userService
          .createUser(
            UserGenerator.individual("foo1", groupId = "foo1group"),
            planetId = "thisPlanet",
            Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.organisation("foo2", groupId = "foo2group"),
            planetId = "thisPlanet",
            Some(AG.Organisation)
          )
          .futureValue
        userService
          .createUser(UserGenerator.agent("foo3", groupId = "foo3group"), planetId = "thisPlanet", Some(AG.Agent))
          .futureValue

        userService
          .createUser(
            UserGenerator.individual("boo1", groupId = "boo1group"),
            planetId = "otherPlanet",
            Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.individual("boo2", groupId = "boo2group"),
            planetId = "otherPlanet",
            Some(AG.Organisation)
          )
          .futureValue

        implicit val currentAuthSession: AuthenticatedSession =
          SignIn.signInAndGetSession("foo1", planetId = "thisPlanet")
        val otherPlanetAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("boo1", planetId = "otherPlanet")

        val result1 = Groups.getAll()
        result1 should haveStatus(200)
        val groups1 = result1.json.as[Groups].groups
        groups1.size shouldBe 3
        groups1.map(_.groupId) should contain.only("foo1group", "foo2group", "foo3group")
        groups1.map(_.affinityGroup) should contain.only("Individual", "Agent", "Organisation")

        val result2 = Groups.getAll()(otherPlanetAuthSession)
        result2 should haveStatus(200)
        val groups2 = result2.json.as[Groups].groups
        groups2.size shouldBe 2
        groups2.map(_.groupId) should contain.only("boo1group", "boo2group")
        groups2.map(_.affinityGroup) should contain.only("Individual", "Organisation")
      }

      "return 200 with the list of groups having given affinity" in {
        userService
          .createUser(
            UserGenerator.individual("foo1", groupId = "foo1group"),
            planetId = "testPlanet",
            Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.organisation("foo2", groupId = "foo2group"),
            planetId = "testPlanet",
            Some(AG.Organisation)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.agent("foo3", groupId = "foo3group"),
            planetId = "testPlanet",
            Some(AG.Agent)
          )
          .futureValue

        implicit val currentAuthSession: AuthenticatedSession =
          SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

        val result1 = Groups.getAll(affinityGroup = Some("Agent"))
        result1 should haveStatus(200)
        val groups1 = result1.json.as[Groups].groups
        groups1.size shouldBe 1
        groups1.map(_.groupId) should contain.only("foo3group")
        groups1.map(_.affinityGroup) should contain.only("Agent")

        val result2 = Groups.getAll(affinityGroup = Some("Individual"))
        result2 should haveStatus(200)
        val groups2 = result2.json.as[Groups].groups
        groups2.size shouldBe 1
        groups2.map(_.groupId) should contain.only("foo1group")
        groups2.map(_.affinityGroup) should contain.only("Individual")
      }
    }
  }

  "POST /agents-external-stubs/groups/re-index" should {
    "re-index all existing groups" in {
      implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
      val result = Groups.reindexAllGroups
      result.status shouldBe 200
      result.body should include("true")
    }
  }
}
