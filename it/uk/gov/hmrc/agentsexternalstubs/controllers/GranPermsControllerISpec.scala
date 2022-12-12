package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, Enrolment, EnrolmentKey, GranPermsGenRequest, Group, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.services.{GroupsService, UsersService}
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class GranPermsControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val groupsService = app.injector.instanceOf[GroupsService]

  private val testUserId: String = "testUserId"
  private val testPlanetId: String = "testPlanetId"

  "massGenerateAgentsAndClients" should {
    "return 201 Created with the request number of agent users and clients" in {
      usersService
        .createUser(
          UserGenerator
            .agent(testUserId)
            .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-AS-AGENT~AgentReferenceNumber~KARN3869382"))),
          affinityGroup = Some(AG.Agent),
          planetId = testPlanetId
        )
        .futureValue

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

      val payload = GranPermsGenRequest("test", 3, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(201)

      val json = result.json
      val createdAgents = (json \ "createdAgentsCount").as[Int]
      val createdClients = (json \ "createdClientsCount").as[Int]

      createdAgents shouldBe 3
      createdClients shouldBe 10

      val (_, Some(group)) = usersService.findUserAndGroup(session.userId, session.planetId).futureValue
      group.delegatedEnrolments.length shouldBe 10
    }

    "return 400 BadRequest when specified number of agents is too large" in {

      usersService
        .createUser(
          UserGenerator
            .agent(testUserId)
            .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-AS-AGENT~AgentReferenceNumber~KARN3869382"))),
          affinityGroup = Some(AG.Agent),
          planetId = testPlanetId
        )
        .futureValue

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

      val payload = GranPermsGenRequest("test", 6, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(400)
      result.body.contains("Too many agents requested.") shouldBe true
    }

    "return 400 BadRequest when specified number of clients is too large" in {

      usersService
        .createUser(
          UserGenerator
            .agent(testUserId)
            .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-AS-AGENT~AgentReferenceNumber~KARN3869382"))),
          affinityGroup = Some(AG.Agent),
          planetId = testPlanetId
        )
        .futureValue

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

      val payload = GranPermsGenRequest("test", 5, 11, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(400)
      result.body.contains("Too many clients requested.") shouldBe true
    }

    "return 401 Unauthorized when user is not an agent" in {

      usersService
        .createUser(
          UserGenerator.individual("notAnAgent"),
          affinityGroup = Some(AG.Individual),
          planetId = testPlanetId
        )
        .futureValue

      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("notAnAgent", planetId = testPlanetId)

      val payload = GranPermsGenRequest("test", 5, 10, false, None, None, None, None)

      val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

      result should haveStatus(401)
      result.body.contains("Currently logged-in user is not an Agent.") shouldBe true
    }
  }

  "return 401 Unauthorized when user is not an Admin" in {

    implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

    val group = groupsService
      .createGroup(
        Group(
          session.planetId,
          groupId = "group1",
          affinityGroup = AG.Agent,
          principalEnrolments = Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "KARN3869382"))
        ),
        session.planetId
      )
      .futureValue
    Users.create(UserGenerator.agent(userId = "foo1", groupId = "group1"), Some(AG.Agent))
    Users.create(
      UserGenerator.agent(userId = session.userId, groupId = "group1", credentialRole = "Assistant"),
      Some(AG.Agent)
    )

    Users.update(
      UserGenerator.agent(session.userId, credentialRole = "Assistant", groupId = group.groupId)
    )

    val payload = GranPermsGenRequest("test", 5, 10, false, None, None, None, None)

    val result = GranPermsStubs.massGenerateAgentsAndClients(payload)

    result should haveStatus(401)
    result.body.contains("Currently logged-in user is not a group Admin.") shouldBe true
  }

  "allow for correctly adding additional clients if the logged-in user has already some" in {
    usersService
      .createUser(
        UserGenerator
          .agent(testUserId)
          .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-AS-AGENT~AgentReferenceNumber~KARN3869382"))),
        affinityGroup = Some(AG.Agent),
        planetId = testPlanetId
      )
      .futureValue

    implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

    // Create some clients
    val payload1 = GranPermsGenRequest("test1", 0, 5, false, None, None, None, None)
    val result1 = GranPermsStubs.massGenerateAgentsAndClients(payload1)
    result1 should haveStatus(201)

    // Create some more clients for the same agent
    val payload2 = GranPermsGenRequest("test2", 0, 3, false, None, None, None, None)
    val result2 = GranPermsStubs.massGenerateAgentsAndClients(payload2)
    result2 should haveStatus(201)

    val (_, Some(group)) = usersService.findUserAndGroup(session.userId, session.planetId).futureValue
    group.delegatedEnrolments.length shouldBe 8
  }
}
