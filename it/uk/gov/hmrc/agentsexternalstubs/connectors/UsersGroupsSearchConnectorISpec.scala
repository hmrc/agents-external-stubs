package uk.gov.hmrc.agentsexternalstubs.connectors

import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, Group, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class UsersGroupsSearchConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[UsersGroupsSearchConnector]

  "UsersGroupsSearchConnector" when {

    "getGroupInfo" should {
      "return group information" in {
        val userFoo = UserGenerator.agent(userId = "foo", groupId = "foo-group-1")
        userService.createUser(userFoo, planetId = "testPlanetId", affinityGroup = Some(AG.Agent)).futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanetId")

        val currentUser = Users.get(session.userId).json.as[User]
        val maybeAgentCode = Groups.get(currentUser.groupId.get).json.as[Group].agentCode

        val groupInfo = await(connector.getGroupInfo("foo-group-1"))
        groupInfo.groupId shouldBe "foo-group-1"
        groupInfo.affinityGroup shouldBe Some("Agent")
        groupInfo.agentCode.map(_.value) shouldBe maybeAgentCode
      }
    }
  }
}
