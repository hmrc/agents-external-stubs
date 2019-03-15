package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class SsoValidateDomainControllerISpec extends ServerBaseISpec with TestRequests {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "SsoValidateDomainController" when {

    "GET /sso/validate/domain/localhost" should {
      "return NoContent" in {
        val response = SsoValidateDomain.validate("localhost")
        response.status shouldBe 204

      }
    }

  }

}
