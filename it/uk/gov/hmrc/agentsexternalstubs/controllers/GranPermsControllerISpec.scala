package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, GranPermsGenRequest, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}

class GranPermsControllerISpec extends ServerBaseISpec with MongoDB with TestRequests {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val usersService = app.injector.instanceOf[UsersService]

  "massGenerateAgentsAndClients" should {
    "return 201 Created with the request number of agent users and clients" in {

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
      Users.update(
        UserGenerator
          .agent(userId = session.userId)
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382")
      )

      val payload = GranPermsGenRequest("test", 3, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(201)

      val json = result.json
      val createdAgents = (json \ "createdAgentsCount").as[Int]
      val createdClients = (json \ "createdClientsCount").as[Int]

      createdAgents shouldBe 3
      createdClients shouldBe 10
    }

    "return 400 BadRequest when specified number of agents is too large" in {

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
      Users.update(
        UserGenerator
          .agent(userId = session.userId)
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382")
      )

      val payload = GranPermsGenRequest("test", 6, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(400)
      result.body.contains("Too many agents requested.") shouldBe true
    }

    "return 400 BadRequest when specified number of clients is too large" in {

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
      Users.update(
        UserGenerator
          .agent(userId = session.userId)
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382")
      )

      val payload = GranPermsGenRequest("test", 5, 11, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(400)
      result.body.contains("Too many clients requested.") shouldBe true
    }

    "return 401 Unauthorized when user is not an agent" in {

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
      Users.update(
        UserGenerator
          .individual(userId = session.userId)
      )

      val payload = GranPermsGenRequest("test", 5, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(401)
      result.body.contains("Currently logged-in user is not an Agent.") shouldBe true
    }
  }

  "return 401 Unauthorized when user is not an Admin" in {

    implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

    val adminUser = usersService
      .createUser(
        UserGenerator
          .agent(userId = "foo1")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382"),
        session.planetId
      )
      .futureValue

    Users.update(
      UserGenerator.agent(session.userId, credentialRole = "Assistant", groupId = adminUser.groupId.get)
    )

    val payload = GranPermsGenRequest("test", 5, 10, false, None, None, None, None)

    val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

    result should haveStatus(401)
    result.body.contains("Currently logged-in user is not a group Admin.") shouldBe true
  }

  "allow for correctly adding additional clients if the logged-in user has already some" in {

    implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
    Users.update(
      UserGenerator
        .agent(userId = session.userId)
        .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382")
    )

    // Create some clients
    val payload1 = GranPermsGenRequest("test1", 0, 5, false, None, None, None, None)
    val result1 = GranPermsStubs.massGenerateAgentsAndClients(payload1)
    result1 should haveStatus(201)

    // Create some more clients for the same agent
    val payload2 = GranPermsGenRequest("test2", 0, 3, false, None, None, None, None)
    val result2 = GranPermsStubs.massGenerateAgentsAndClients(payload2)
    result2 should haveStatus(201)

    val Some(currentUser) = usersService.findByUserId(session.userId, session.planetId).futureValue
    currentUser.enrolments.delegated.length shouldBe 8
  }
}
