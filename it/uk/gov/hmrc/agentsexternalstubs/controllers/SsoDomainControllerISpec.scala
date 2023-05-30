package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class SsoDomainControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "SsoValidateDomainController" when {

    "GET /sso/validate/domain/localhost" should {
      "return NoContent" in {
        val response = SsoValidateDomain.validate("localhost")
        response.status shouldBe 204

      }
    }

    "GET /sso/domains" should {
      "returns allowlisted domains" in {
        val response = SsoGetDomains.getDomains
        response.status shouldBe 200
        Json
          .parse(response.body)
          .toString shouldBe """{"internalDomains":["localhost"],"externalDomains":["127.0.0.1","online-qa.ibt.hmrc.gov.uk","ibt.hmrc.gov.uk"]}"""

      }
    }

  }

}
