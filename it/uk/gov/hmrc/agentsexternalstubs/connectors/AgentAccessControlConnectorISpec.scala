package uk.gov.hmrc.agentsexternalstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

class AgentAccessControlConnectorISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with WireMockSupport {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpGet = app.injector.instanceOf[HttpGet]
  lazy val connector = new AgentAccessControlConnector(TestAppConfig(wireMockBaseUrlAsString, wireMockPort), httpGet)

  "AgentAccessControlConnector" when {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    "checking epaye delegated auth rule" should {
      "return true" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/epaye-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(200)))

        val result = await(connector.isAuthorisedForPaye("FOO", "BAR"))

        result shouldBe true
      }

      "return false" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/epaye-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(401)))

        val result = await(connector.isAuthorisedForPaye("FOO", "BAR"))

        result shouldBe false
      }
    }

    "checking sa delegated auth rule" should {
      "return true" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/sa-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(200)))

        val result = await(connector.isAuthorisedForSa("FOO", "BAR"))

        result shouldBe true
      }

      "return false" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/sa-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(401)))

        val result = await(connector.isAuthorisedForSa("FOO", "BAR"))

        result shouldBe false
      }
    }

    "checking mtd-it delegated auth rule" should {
      "return true" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/mtd-it-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(200)))

        val result = await(connector.isAuthorisedForMtdIt("FOO", "BAR"))

        result shouldBe true
      }

      "return false" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/mtd-it-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(401)))

        val result = await(connector.isAuthorisedForMtdIt("FOO", "BAR"))

        result shouldBe false
      }
    }

    "checking mtd-vat delegated auth rule" should {
      "return true" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/mtd-vat-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(200)))

        val result = await(connector.isAuthorisedForMtdVat("FOO", "BAR"))

        result shouldBe true
      }

      "return false" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/mtd-vat-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(401)))

        val result = await(connector.isAuthorisedForMtdVat("FOO", "BAR"))

        result shouldBe false
      }
    }

    "checking afi delegated auth rule" should {
      "return true" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/afi-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(200)))

        val result = await(connector.isAuthorisedForAfi("FOO", "BAR"))

        result shouldBe true
      }

      "return false" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/agent-access-control/afi-auth/agent/FOO/client/BAR"))
            .willReturn(aResponse()
              .withStatus(401)))

        val result = await(connector.isAuthorisedForAfi("FOO", "BAR"))

        result shouldBe false
      }
    }
  }
}
