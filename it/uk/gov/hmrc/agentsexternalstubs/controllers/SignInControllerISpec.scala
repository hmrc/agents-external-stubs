package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, MongoDbPerSuite, ServerBaseISpec, TestRequests}

class SignInControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "SignInController" when {

    "GET /agents-external-stubs/sign-in" should {
      "authenticate user and return session data" in {
        val result = SignIn.signIn("foo", "boo")
        result.status shouldBe 201
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate same user again and return new session data" in {
        val result1 = SignIn.signIn("foo", "boo")
        val result2 = SignIn.signIn("foo", "boo")
        result1.status shouldBe 201
        result2.status shouldBe 202
        result1.header(HeaderNames.LOCATION) should not be result2.header(HeaderNames.LOCATION)
      }
    }

    "GET /agents-external-stubs/session" should {
      "return session data" in {
        val result1 = SignIn.signIn("foo123", "boo")
        result1.status shouldBe 201
        val result2 = SignIn.authSessionFor(result1)
        (result2.json \ "userId").as[String] shouldBe "foo123"
        (result2.json \ "authToken").as[String] should not be empty
      }
    }

    "GET /agents-external-stubs/sign-out" should {
      "remove authentication" in {
        val authToken = SignIn.signInAndGetSession("foo", "boo").authToken
        val result = SignIn.signOut(AuthContext.withToken(authToken))
        result.status shouldBe 204
        result.header(HeaderNames.LOCATION) should be(empty)
      }
    }
  }
}
