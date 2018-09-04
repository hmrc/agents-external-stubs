package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}

class UsersGroupsSearchStubControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "UsersGroupsSearchStubController" when {

    "GET /users-groups-search/users/:userId" should {
      "respond 200 with individual user details if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", name = "Alan Brian Foo-Foe", groupId = "foo-group-1"))

        val result = UsersGroupSearchStub.getUser("foo1")

        result should haveStatus(203)
        val json = result.json
        (json \ "userId").as[String] shouldBe "foo1"
        (json \ "name").as[String] shouldBe "Alan Brian Foo-Foe"
        (json \ "email").asOpt[String] shouldBe None
        (json \ "affinityGroup").as[String] shouldBe "Individual"
        (json \ "agentCode").asOpt[String] shouldBe None
        (json \ "agentFriendlyName").asOpt[String] shouldBe None
        (json \ "agentId").asOpt[String] shouldBe None
        (json \ "credentialRole").as[String] shouldBe "Admin"
        (json \ "description").asOpt[String] shouldBe None
        (json \ "groupId").as[String] shouldBe "foo-group-1"
      }

      "respond 200 with agent user details if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo2")
        Users.update(
          UserGenerator
            .agent(
              userId = "foo2",
              name = "Alan Brian Foo-Foe",
              groupId = "foo-group-2",
              agentCode = "AAABBBCCCDDD",
              agentFriendlyName = "Foo-Foe Accountants",
              agentId = "1234567"))

        val result = UsersGroupSearchStub.getUser("foo2")

        result should haveStatus(203)
        val json = result.json
        (json \ "userId").as[String] shouldBe "foo2"
        (json \ "name").as[String] shouldBe "Alan Brian Foo-Foe"
        (json \ "email").asOpt[String] shouldBe None
        (json \ "affinityGroup").as[String] shouldBe "Agent"
        (json \ "agentCode").as[String] shouldBe "AAABBBCCCDDD"
        (json \ "agentFriendlyName").as[String] shouldBe "Foo-Foe Accountants"
        (json \ "agentId").as[String] shouldBe "1234567"
        (json \ "credentialRole").as[String] shouldBe "Admin"
        (json \ "description").asOpt[String] shouldBe None
        (json \ "groupId").as[String] shouldBe "foo-group-2"
      }

      "respond 404 if user not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo2")

        val result = UsersGroupSearchStub.getUser("foo2-1")

        result should haveStatus(404)
      }
    }

    "GET /users-groups-search/groups/:groupId" should {
      "respond 200 with ordinary group details if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo3")
        Users.update(
          UserGenerator
            .individual(userId = "foo3", name = "Alan Brian Foo-Foe", groupId = "foo-group-3"))

        val result = UsersGroupSearchStub.getGroup("foo-group-3")

        result should haveStatus(203)
        val json = result.json
        (json \ "groupId").as[String] shouldBe "foo-group-3"
        (json \ "agentCode").asOpt[String] shouldBe None
        (json \ "agentFriendlyName").asOpt[String] shouldBe None
        (json \ "agentId").asOpt[String] shouldBe None
        ((json \ "_links")(0) \ "rel").as[String] shouldBe "users"
        ((json \ "_links")(0) \ "link").as[String] shouldBe "/users-groups-search/groups/foo-group-3/users"
      }

      "respond 200 with agent group details if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo4")
        Users.update(
          UserGenerator
            .agent(
              userId = "foo4",
              name = "Alan Brian Foo-Foe",
              groupId = "foo-group-4",
              agentCode = "AAABBBCCCDDD",
              agentFriendlyName = "Foo-Foe Accountants",
              agentId = "1234567"))

        val result = UsersGroupSearchStub.getGroup("foo-group-4")

        result should haveStatus(203)
        val json = result.json
        (json \ "groupId").as[String] shouldBe "foo-group-4"
        (json \ "agentCode").as[String] shouldBe "AAABBBCCCDDD"
        (json \ "agentFriendlyName").as[String] shouldBe "Foo-Foe Accountants"
        (json \ "agentId").as[String] shouldBe "1234567"
        ((json \ "_links")(0) \ "rel").as[String] shouldBe "users"
        ((json \ "_links")(0) \ "link").as[String] shouldBe "/users-groups-search/groups/foo-group-4/users"
      }

      "respond 404 if group not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo2")

        val result = UsersGroupSearchStub.getGroup("foo-group-4-1")

        result should haveStatus(404)
      }
    }

    "GET /users-groups-search/groups?agentCode=:agentCode&agentId=:agentId" should {
      "respond 200 with agent group details if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo5")
        Users.update(
          UserGenerator
            .agent(
              userId = "foo5",
              name = "Alan Brian Foo-Foe",
              groupId = "foo-group-5",
              agentCode = "ABC123",
              agentFriendlyName = "Foo-Foe Accountants",
              agentId = "1234567"))

        val result = UsersGroupSearchStub.getGroupByAgentCode("ABC123", "any")

        result should haveStatus(203)
        val json = result.json
        (json \ "groupId").as[String] shouldBe "foo-group-5"
        (json \ "agentCode").as[String] shouldBe "ABC123"
        (json \ "agentFriendlyName").as[String] shouldBe "Foo-Foe Accountants"
        (json \ "agentId").as[String] shouldBe "1234567"
        ((json \ "_links")(0) \ "rel").as[String] shouldBe "users"
        ((json \ "_links")(0) \ "link").as[String] shouldBe "/users-groups-search/groups/foo-group-5/users"
      }

      "respond 404 if group not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo2")

        val result = UsersGroupSearchStub.getGroupByAgentCode("dumb", "any")

        result should haveStatus(404)
      }
    }
  }

  "GET /users-groups-search/groups/:groupId/users" should {
    "respond 200 with the list of users in the group" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo6-1", planetId = "juniper")
      Users.update(
        UserGenerator
          .individual(userId = "foo6-1", name = "A", groupId = "foo-group-6"))
      Users.create(
        UserGenerator
          .individual(userId = "foo6-2", name = "B", groupId = "foo-group-6"))

      val session2: AuthenticatedSession = SignIn.signInAndGetSession("foo6-1", planetId = "saturn")
      Users.create(
        UserGenerator
          .individual(userId = "foo6-3", name = "C", groupId = "foo-group-6"))(session2)

      val result = UsersGroupSearchStub.getGroupUsers("foo-group-6")

      result should haveStatus(203)
      val users = result.json.as[Seq[User]]
      users.map(_.userId) should contain.only("foo6-1", "foo6-2")
    }

    "respond 404 if group is empty" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo7-1", planetId = "juniper")
      Users.update(
        UserGenerator
          .individual(userId = "foo7-1", name = "A", groupId = "foo-group-7"))
      Users.create(
        UserGenerator
          .individual(userId = "foo7-2", name = "B", groupId = "foo-group-7"))

      val result = UsersGroupSearchStub.getGroupUsers("foo-group-7-x")

      result should haveStatus(404)
    }
  }
}
