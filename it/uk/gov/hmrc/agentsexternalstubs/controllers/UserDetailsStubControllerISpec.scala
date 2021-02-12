package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, NotAuthorized, ServerBaseISpec, TestRequests}

class UserDetailsStubControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "UserDetailsStubController" when {

    "GET /user-details/id/:id" should {
      "respond 200 with individual user data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(UserGenerator.individual(userId = session.userId))
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
            be("Admin")
          ) and haveProperty[String]("description") and haveProperty[String](
            "postCode",
            be(user.address.get.postcode.get)
          ) and haveProperty[String]("dateOfBirth") and notHaveProperty("agentCode") and notHaveProperty(
            "agentFriendlyName"
          ) and notHaveProperty("agentId")
        )
      }

      "respond 200 with organisation user data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(UserGenerator.organisation(userId = session.userId))
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
            be("Admin")
          ) and haveProperty[String]("description") and haveProperty[String](
            "postCode",
            be(user.address.get.postcode.get)
          ) and notHaveProperty("agentCode") and notHaveProperty("agentFriendlyName") and notHaveProperty(
            "agentId"
          ) and notHaveProperty("dateOfBirth") and notHaveProperty("lastName")
        )
      }

      "respond 200 with agent user data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(UserGenerator.agent(userId = session.userId))
        val user = Users.get(session.userId).json.as[User]

        val result = UserDetailsStub.getUser(session.userId)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("authProviderId", be(user.userId)) and
            haveProperty[String]("authProviderType", be(session.providerType)) and
            haveProperty[String]("name", be(user.name.get)) and
            haveProperty[String]("email") and
            haveProperty[String]("affinityGroup", be("Agent")) and
            haveProperty[String]("credentialRole", be("Admin")) and
            haveProperty[String]("description") and
            haveProperty[String]("postCode", be(user.address.get.postcode.get)) and
            haveProperty[String]("agentCode", be(user.agentCode.get)) and
            haveProperty[String]("agentFriendlyName", be(user.agentFriendlyName.get)) and
            haveProperty[String]("agentId", be(user.agentId.get)) and
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
