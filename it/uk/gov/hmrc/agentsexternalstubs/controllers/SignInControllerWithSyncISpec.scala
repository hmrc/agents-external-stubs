package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, ServerBaseISpec, TestRequests}
import uk.gov.hmrc.agentsexternalstubs.support.WireMockSupport

class SignInControllerWithSyncISpec extends ServerBaseISpec with TestRequests with WireMockSupport {

  // TODO uncomment and fix
//  final override val playServer: TestPlayServer = new TestPlayServer {
//    override def configuration: Seq[(String, Any)] =
//      super.configuration ++ Seq(
//        "features.syncToAuthLoginApi"                   -> true,
//        "microservice.services.auth-login-api.host"     -> "localhost",
//        "microservice.services.auth-login-api.port"     -> wireMockPort,
//        "microservice.services.auth-login-api.protocol" -> "http"
//      )
//  }

  lazy val wsClient = app.injector.instanceOf[WSClient]

  "SignInController with feature.syncToAuthLoginApi=true" when {

    "GET /agents-external-stubs/sign-in" should {
      "authenticate user and return session data" in {
        givenSuccessfulAuthLoginStubResponse()
        val result = SignIn.signIn("foo", "boo", syncToAuthLoginApi = true)
        result should haveStatus(201)
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate anonymous user and return session data" in {
        givenSuccessfulAuthLoginStubResponse()
        val result = SignIn.signIn(syncToAuthLoginApi = true)
        result should haveStatus(201)
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate anonymous user and return current session data" in {
        givenSuccessfulAuthLoginStubResponse()
        val authToken = SignIn.signInAndGetSession("foo", "boo", syncToAuthLoginApi = true).authToken
        val result = SignIn.currentSession(AuthContext.fromToken(authToken))
        result.status shouldBe 200
      }

      "authenticate same user again and return new session data" in {
        givenSuccessfulAuthLoginStubResponse()
        val session1 = SignIn.signInAndGetSession("foo", "boo")
        val result2 = SignIn.signIn("foo", "boo", planetId = session1.planetId, syncToAuthLoginApi = true)
        result2 should haveStatus(202)
        result2.header(HeaderNames.LOCATION) should not be empty
      }
    }

    "GET /agents-external-stubs/session" should {
      "return session data" in {
        givenSuccessfulAuthLoginStubResponse()
        val result1 = SignIn.signIn("foo123", "boo", syncToAuthLoginApi = true)
        result1 should haveStatus(201)
        val result2 = SignIn.authSessionFor(result1)
        (result2.json \ "userId").as[String] shouldBe "foo123"
        (result2.json \ "authToken").as[String] should not be empty
      }
    }

    "GET /agents-external-stubs/sign-out" should {
      "remove authentication" in {
        givenSuccessfulAuthLoginStubResponse()
        val authToken = SignIn.signInAndGetSession("foo", "boo", syncToAuthLoginApi = true).authToken
        val result = SignIn.signOut(AuthContext.fromToken(authToken))
        result should haveStatus(204)
        result.header(HeaderNames.LOCATION) should be(empty)
      }
    }
  }

  import com.github.tomakehurst.wiremock.client.WireMock

  def givenSuccessfulAuthLoginStubResponse(): Unit =
    WireMock.stubFor(
      WireMock
        .post(WireMock.urlEqualTo("/government-gateway/session/login"))
        .willReturn(
          WireMock
            .aResponse()
            .withStatus(200)
            .withHeader("Authorization", s"Bearer ${java.util.UUID.randomUUID}")
            .withHeader("Location", "http://test/123")
        )
    )
}
