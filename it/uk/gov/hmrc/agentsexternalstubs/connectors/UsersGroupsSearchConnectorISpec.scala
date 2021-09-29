package uk.gov.hmrc.agentsexternalstubs.connectors

import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}

class UsersGroupsSearchConnectorISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[UsersGroupsSearchConnector]

  "UsersGroupsSearchConnector" when {

    "getGroupInfo" should {
      "return group information" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val userFoo = UserGenerator.agent(userId = "foo", groupId = "foo-group-1")
        Users.update(userFoo)

        val groupInfo = await(connector.getGroupInfo("foo-group-1"))
        groupInfo.groupId shouldBe "foo-group-1"
        groupInfo.affinityGroup shouldBe Some("Agent")
        groupInfo.agentCode.map(_.value) shouldBe userFoo.agentCode
      }
    }
  }
}
