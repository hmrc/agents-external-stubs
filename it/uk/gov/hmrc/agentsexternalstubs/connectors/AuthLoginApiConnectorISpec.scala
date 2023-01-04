package uk.gov.hmrc.agentsexternalstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests, WireMockSupport}
import uk.gov.hmrc.http.{HttpGet, HttpPost}
import uk.gov.hmrc.http.HeaderCarrier

class AuthLoginApiConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs with WireMockSupport {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpPostWithGet = app.injector.instanceOf[HttpPost with HttpGet]
  lazy val connector = new AuthLoginApiConnector(TestAppConfig(wireMockBaseUrlAsString, wireMockPort), httpPostWithGet)

  "AuthLoginApiConnector" when {

    "loginToGovernmentGateway" should {
      "return new auth token" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        WireMock.stubFor(
          WireMock
            .post(urlEqualTo("/government-gateway/session/login"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader(HeaderNames.AUTHORIZATION, "Bearer foo123")
                .withHeader(HeaderNames.LOCATION, "http://test/123")
                .withBody("""
                  |{
                  |  "gatewayToken": "some-gateway-token"
                  |}
                  """.stripMargin)
            )
        )

        val authLoginApiRequest = AuthLoginApi.Request(
          credId = "foo@bar",
          affinityGroup = "Agent",
          confidenceLevel = Some(250),
          credentialStrength = "strong",
          credentialRole = Some("Admin"),
          enrolments = Seq.empty,
          delegatedEnrolments = None,
          gatewayToken = None,
          groupIdentifier = Some("groupId"),
          nino = Some("ABC123"),
          usersName = None,
          email = None,
          description = None,
          agentFriendlyName = Some("Agency"),
          agentCode = Some("ABC123"),
          agentId = Some("ABC 123"),
          itmpData = None,
          gatewayInformation = None,
          mdtpInformation = None,
          unreadMessageCount = Some(2)
        )

        val result = await(connector.loginToGovernmentGateway(authLoginApiRequest))

        result.authToken shouldBe "Bearer foo123"
      }
    }
  }
}
