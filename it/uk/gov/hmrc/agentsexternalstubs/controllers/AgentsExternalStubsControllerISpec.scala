package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.agentsexternalstubs.support.ServerBaseISpec

class AgentsExternalStubsControllerISpec extends ServerBaseISpec {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val wsClient = app.injector.instanceOf[WSClient]

  def testAuthAgentMtd(): WSResponse =
    wsClient
      .url(s"$url/test/auth/agent-mtd")
      .withHeaders(HeaderNames.AUTHORIZATION -> "Bearer ABC")
      .get()
      .futureValue

  "AgentsExternalStubsController" when {

    "GET /test/auth/agent-mtd" should {
      "respond with some data" in {
        val result = testAuthAgentMtd()
        result.status shouldBe 200
        result.json shouldBe Json.obj("value" -> "TARN0000001")
      }
    }
  }
}
