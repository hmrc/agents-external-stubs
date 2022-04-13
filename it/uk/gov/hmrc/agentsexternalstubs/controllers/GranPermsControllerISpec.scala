package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, GranPermsGenRequest, User, UserGenerator}
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

      val payload = GranPermsGenRequest("test", 3, 10, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(201)

      val json = result.json
      val createdAgents = (json \ "createdAgents").as[Seq[User]]
      val createdClients = (json \ "createdClients").as[Seq[User]]

      createdAgents.size shouldBe 3
      createdClients.size shouldBe 10

      createdAgents.map(_.groupId).distinct.size shouldBe 1 //they should all have the same groupId
      createdAgents.map(_.agentCode).distinct.size shouldBe 1 //they should all have the same agentCode
    }

    "return 400 BadRequest when specified number of agents is too large" in {

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
      Users.update(
        UserGenerator
          .agent(userId = session.userId)
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382")
      )

      val payload = GranPermsGenRequest("test", 6, 10, None, None, None, None)

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

      val payload = GranPermsGenRequest("test", 5, 11, None, None, None, None)

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

      val payload = GranPermsGenRequest("test", 5, 10, None, None, None, None)

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

    val payload = GranPermsGenRequest("test", 5, 10, None, None, None, None)

    val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

    result should haveStatus(401)
    result.body.contains("Currently logged-in user is not a group Admin.") shouldBe true
  }
}
