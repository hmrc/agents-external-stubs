package uk.gov.hmrc.agentsexternalstubs.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentsexternalstubs.support.WireMockSupport
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) =
    authenticated(request, Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn), isAgent = true)

  def authenticated[A](request: FakeRequest[A], enrolment: Enrolment, isAgent: Boolean): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
         |    { "authProviders": ["GovernmentGateway"] }
         |  ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"authorisedEnrolments": [
         |  { "key":"${enrolment.serviceName}", "identifiers": [
         |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |  ]}
         |]}
          """.stripMargin
    )
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))

  def givenAuthorisedFor(payload: String, responseBody: String): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)))

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))
  }

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
