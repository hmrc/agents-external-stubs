package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.support._

class ProxyControllerISpec extends ServerBaseISpec with TestRequests with WireMockSupport {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "ProxyController" when {

    "POST /foo/abc/123" should {
      "pass the request to the foo target" in {
        implicit val authContext: AuthContext = NotAuthorized

        val response = post("/foo/abc/123?a=b", Json.parse("""{"foo":"bar"}"""))
        response.status shouldBe 400
      }
    }
  }
}
