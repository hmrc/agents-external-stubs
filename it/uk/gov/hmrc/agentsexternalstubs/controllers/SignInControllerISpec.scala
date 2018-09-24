package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, MongoDB, ServerBaseISpec, TestRequests}

class SignInControllerISpec extends ServerBaseISpec with MongoDB with TestRequests {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "SignInController" when {

    "GET /agents-external-stubs/sign-in" should {
      "authenticate user and return session data" in {
        val result = SignIn.signIn("foo", "boo")
        result should haveStatus(201)
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate anonymous user and return session data" in {
        val result = SignIn.signIn()
        result should haveStatus(201)
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate anonymous user and return current session data" in {
        val authToken = SignIn.signInAndGetSession("foo", "boo").authToken
        val result = SignIn.currentSession(AuthContext.fromToken(authToken))
        result.status shouldBe 200
      }

      "authenticate same user again and return new session data" in {
        val session1 = SignIn.signInAndGetSession("foo", "boo")
        val result2 = SignIn.signIn("foo", "boo", planetId = session1.planetId)
        result2 should haveStatus(202)
        result2.header(HeaderNames.LOCATION) should not be empty
      }
    }

    "GET /agents-external-stubs/session" should {
      "return session data" in {
        val result1 = SignIn.signIn("foo123", "boo")
        result1 should haveStatus(201)
        val result2 = SignIn.authSessionFor(result1)
        (result2.json \ "userId").as[String] shouldBe "foo123"
        (result2.json \ "authToken").as[String] should not be empty
      }
    }

    "GET /agents-external-stubs/sign-out" should {
      "remove authentication" in {
        val authToken = SignIn.signInAndGetSession("foo", "boo").authToken
        val result = SignIn.signOut(AuthContext.fromToken(authToken))
        result should haveStatus(204)
        result.header(HeaderNames.LOCATION) should be(empty)
      }
    }
  }
}
