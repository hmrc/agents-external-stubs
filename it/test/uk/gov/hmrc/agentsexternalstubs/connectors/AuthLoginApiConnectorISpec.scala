/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests, WireMockSupport}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier

class AuthLoginApiConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs with WireMockSupport {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val httpPost: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  lazy val connector = new AuthLoginApiConnector(TestAppConfig(wireMockBaseUrlAsString, wireMockPort), httpPost)

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
