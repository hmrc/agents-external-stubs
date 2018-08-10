package uk.gov.hmrc.agentsexternalstubs.connectors

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, ServerBaseISpec, TestRequests}

class UsersGroupsSearchConnectorISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]
  val connector = app.injector.instanceOf[UsersGroupsSearchConnector]

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
