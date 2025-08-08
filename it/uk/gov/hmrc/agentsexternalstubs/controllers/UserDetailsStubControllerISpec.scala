package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{NotAuthorized, ServerBaseISpec, TestRequests}

class UserDetailsStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val testPlanet = "testPlanet"

  "UserDetailsStubController" when {

    "GET /user-details/id/:id" should {
      "respond 200 with individual user data if found" in {
        userService
          .createUser(
            UserGenerator.individual(userId = "foo"),
            planetId = testPlanet,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = testPlanet)

        val user = Users.get(session.userId).json.as[User]

        val result = UserDetailsStub.getUser(session.userId)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("authProviderId", be(user.userId)) and haveProperty[String](
            "authProviderType",
            be(session.providerType)
          ) and haveProperty[String]("name") and haveProperty[String]("lastName") and haveProperty[String](
            "email"
          ) and haveProperty[String]("affinityGroup", be("Individual")) and haveProperty[String](
            "credentialRole",
            be("User")
          ) and haveProperty[String]("description") and haveProperty[String](
            "postCode",
            be(user.address.get.postcode.get)
          ) and haveProperty[String]("dateOfBirth") and notHaveProperty("agentCode") and notHaveProperty(
            "agentFriendlyName"
          ) and notHaveProperty("agentId")
        )
      }

      "respond 200 with organisation user data if found" in {
        userService
          .createUser(
            UserGenerator.organisation(userId = "foo"),
            planetId = testPlanet,
            affinityGroup = Some(AG.Organisation)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = testPlanet)

        val user = Users.get(session.userId).json.as[User]

        val result = UserDetailsStub.getUser(session.userId)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("authProviderId", be(user.userId)) and haveProperty[String](
            "authProviderType",
            be(session.providerType)
          ) and haveProperty[String]("name", be(user.name.get)) and haveProperty[String]("email") and haveProperty[
            String
          ]("affinityGroup", be("Organisation")) and haveProperty[String](
            "credentialRole",
            be("User")
          ) and haveProperty[String]("description") and haveProperty[String](
            "postCode",
            be(user.address.get.postcode.get)
          ) and notHaveProperty("agentCode") and notHaveProperty("agentFriendlyName") and notHaveProperty(
            "agentId"
          ) and notHaveProperty("dateOfBirth") and notHaveProperty("lastName")
        )
      }

      "respond 200 with agent user data if found" in {
        val newUser = userService
          .createUser(
            UserGenerator.agent(userId = "foo"),
            planetId = testPlanet,
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        groupsService.updateGroup(newUser.groupId.get, testPlanet, _.copy(agentFriendlyName = Some("Foo Ltd")))

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = testPlanet)

        val user = Users.get(session.userId).json.as[User]
        val group = Groups.get(user.groupId.get).json.as[Group]

        val result = UserDetailsStub.getUser(session.userId)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("authProviderId", be(user.userId)) and
            haveProperty[String]("authProviderType", be(session.providerType)) and
            haveProperty[String]("name", be(user.name.get)) and
            haveProperty[String]("email") and
            haveProperty[String]("affinityGroup", be("Agent")) and
            haveProperty[String]("credentialRole", be("User")) and
            haveProperty[String]("description") and
            haveProperty[String]("postCode", be(user.address.get.postcode.get)) and
            haveProperty[String]("agentCode", be(group.agentCode.get)) and
            haveProperty[String]("agentFriendlyName", be(group.agentFriendlyName.get)) and
            haveProperty[String]("agentId", be(group.agentId.get)) and
            haveProperty[String]("dateOfBirth", be(user.dateOfBirth.get.toString)) and
            notHaveProperty("lastName")
        )
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = UserDetailsStub.getUser("bar")
        result should haveStatus(404)
      }

      "respond 401 if not authenticated" in {
        val result = UserDetailsStub.getUser("foo")(NotAuthorized)
        result should haveStatus(401)
      }
    }
  }
}
