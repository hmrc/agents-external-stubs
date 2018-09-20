package uk.gov.hmrc.agentsexternalstubs.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.models.{AuthoriseRequest, AuthoriseResponse}
import uk.gov.hmrc.agentsexternalstubs.support.WireMockSupport

trait AuthStubs {
  me: WireMockSupport =>

  def givenAuthorisedFor(payload: AuthoriseRequest, response: AuthoriseResponse): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(Json.toJson(payload).toString, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(response).toString)))

  def givenUnauthorised =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
