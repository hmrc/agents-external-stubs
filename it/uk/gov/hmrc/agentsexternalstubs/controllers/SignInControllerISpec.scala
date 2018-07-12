package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.http.{MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Writes}
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.SignInRequest
import uk.gov.hmrc.agentsexternalstubs.support.{MongoApp, ServerBaseISpec}

class SignInControllerISpec extends ServerBaseISpec with MongoApp {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val wsClient = app.injector.instanceOf[WSClient]

  def login(userId: String, password: String): WSResponse =
    wsClient
      .url(s"$url/agents-external-stubs/sign-in")
      .post(SignInRequest(userId, password))
      .futureValue

  def authSessionFor(loginResponse: WSResponse): WSResponse =
    wsClient
      .url(s"$url${loginResponse.header(HeaderNames.LOCATION).getOrElse("")}")
      .get()
      .futureValue

  "SignInController" when {

    "GET /agents-external-stubs/sign-in" should {
      "authenticate user and return session data" in {
        val result = login("foo", "boo")
        result.status shouldBe 201
        result.header(HeaderNames.LOCATION) should not be empty
      }

      "authenticate same user again and return new session data" in {
        val result1 = login("foo", "boo")
        val result2 = login("foo", "boo")
        result1.status shouldBe 201
        result2.status shouldBe 201
        result1.header(HeaderNames.LOCATION) should not be result2.header(HeaderNames.LOCATION)
      }
    }

    "GET /agents-external-stubs/session" should {
      "return session data" in {
        val result1 = login("foo123", "boo")
        result1.status shouldBe 201
        val result2 = authSessionFor(result1)
        (result2.json \ "userId").as[String] shouldBe "foo123"
        (result2.json \ "authToken").as[String] should not be empty
      }
    }
  }

  implicit def jsonWriteable[T](implicit writes: Writes[T], jsValueWriteable: Writeable[JsValue]): Writeable[T] =
    new Writeable[T](
      entity => jsValueWriteable.transform(writes.writes(entity)),
      Some(MimeTypes.JSON)
    )
}
