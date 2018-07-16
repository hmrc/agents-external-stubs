package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSClient, WSCookie, WSResponse}
import play.api.mvc.{Cookie, Cookies}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, SignInRequest}

trait AuthContext {
  def headers: Seq[(String, String)]
}

object AuthContext {

  def from(authSession: AuthenticatedSession): AuthContext =
    withToken(authSession.authToken)

  def withToken(authToken: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer $authToken"
    )
  }

  def fromCookies(response: WSResponse): AuthContext = new AuthContext with CookieConverter {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.COOKIE -> Cookies.encodeCookieHeader(response.cookies.map(x => asCookie(x)))
    )
  }
}

case object NotAuthorized extends AuthContext {
  def headers: Seq[(String, String)] = Seq.empty
}

trait TestRequests extends ScalaFutures {
  def url: String
  def wsClient: WSClient

  import JsonWriteable._

  object ThisApp {
    def signIn(userId: String, password: String): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/sign-in")
        .post(SignInRequest(userId, password))
        .futureValue

    def authSessionFor(loginResponse: WSResponse): WSResponse =
      wsClient
        .url(s"$url${loginResponse.header(HeaderNames.LOCATION).getOrElse("")}")
        .get()
        .futureValue

    def signInAndGetSession(userId: String, password: String): AuthenticatedSession = {
      val signedIn = signIn(userId, password)
      val session = authSessionFor(signedIn)
      session.json.as[AuthenticatedSession]
    }
  }

  object TestMe {
    def testAuthAgentMtd(authContext: AuthContext = NotAuthorized): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/test/auth/agent-mtd")
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object AuthStub {
    def authorise(body: String, authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/auth/authorise")
        .withHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON) ++ authContext.headers: _*)
        .post(Json.parse(body))
        .futureValue
  }

}

object JsonWriteable {
  implicit def jsonWriteable[T](implicit writes: Writes[T], jsValueWriteable: Writeable[JsValue]): Writeable[T] =
    new Writeable[T](
      entity => jsValueWriteable.transform(writes.writes(entity)),
      Some(MimeTypes.JSON)
    )
}

trait CookieConverter {
  def asCookie(cookie: WSCookie): Cookie =
    Cookie(
      cookie.name.getOrElse(""),
      cookie.value.getOrElse(""),
      cookie.maxAge.map(_.toInt).orElse(Some(-1)),
      cookie.path,
      Option(cookie.domain),
      cookie.secure,
      false
    )
}
