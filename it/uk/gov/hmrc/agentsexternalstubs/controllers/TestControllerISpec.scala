package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, ServerBaseISpec, TestRequests}

class TestControllerISpec extends ServerBaseISpec with TestRequests {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "TestController" when {

    "GET /agents-external-stubs/test/auth/agent-mtd" should {
      "respond with some data" in {
        val authSession = ThisApp.signInAndGetSession("foo", "boo")
        val result = TestMe.testAuthAgentMtd(AuthContext.from(authSession))
        result.status shouldBe 200
        result.json shouldBe Json.obj("value" -> "TARN0000001")
      }
    }
  }
}
