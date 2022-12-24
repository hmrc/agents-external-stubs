package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class ConfigControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient = app.injector.instanceOf[WSClient]

  "ConfigController" when {

    "GET /agents-external-stubs/config/services" should {
      "return 200 with services json body" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "A")
        val result = Config.getServices()
        result should (haveStatus(200) and haveValidJsonBody(
          haveProperty[Seq[JsObject]]("services")
        ))
      }
    }
  }
}
