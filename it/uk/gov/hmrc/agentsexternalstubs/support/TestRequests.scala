package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSClient, WSCookie, WSResponse}
import play.api.mvc.{Cookie, Cookies}
import uk.gov.hmrc.agentsexternalstubs.models.{AffinityGroup, AuthenticatedSession, SignInRequest, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

trait AuthContext {
  def headers: Seq[(String, String)]
}

object AuthContext {

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

  implicit def headerCarrier(implicit authSession: AuthenticatedSession): HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization(s"Bearer ${authSession.authToken}")))

  implicit def fromImplicitAuthenticatedSession(implicit authSession: AuthenticatedSession): AuthContext =
    AuthContext.withToken(authSession.authToken)

  implicit def fromAuthenticatedSession(authSession: AuthenticatedSession): AuthContext =
    AuthContext.withToken(authSession.authToken)

  object SignIn {
    def signIn(
      userId: String,
      password: String = "p@ssw0rd",
      providerType: String = "GovernmentGateway",
      planetId: String = "juniper"): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/sign-in")
        .post(SignInRequest(userId, password, providerType, planetId))
        .futureValue

    def authSessionFor(loginResponse: WSResponse): WSResponse =
      wsClient
        .url(s"$url${loginResponse.header(HeaderNames.LOCATION).getOrElse("")}")
        .get()
        .futureValue

    def signInAndGetSession(
      userId: String,
      password: String = "p@ssw0rd",
      planetId: String = "juniper"): AuthenticatedSession = {
      val signedIn = signIn(userId, password, planetId = planetId)
      val session = authSessionFor(signedIn)
      session.json.as[AuthenticatedSession]
    }

    def signOut(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/sign-out")
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object TestMe {
    def testAuthAgentMtd(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/test/auth/agent-mtd")
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def testAuthClientMtdIt(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/test/auth/client-mtd-it")
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object AuthStub {
    def authorise(body: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/auth/authorise")
        .withHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON) ++ authContext.headers: _*)
        .post(Json.parse(body))
        .futureValue
  }

  object Users {

    def getAll(affinityGroup: Option[String] = None, limit: Option[Int] = None)(
      implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withQueryString(Seq("affinityGroup" -> affinityGroup, "limit" -> limit.toString).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def get(userId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/$userId")
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def update(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/${user.userId}")
        .withHeaders(authContext.headers: _*)
        .put(Json.toJson(user))
        .futureValue

    def create(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/")
        .withHeaders(authContext.headers: _*)
        .post(Json.toJson(user))
        .futureValue

    def delete(userId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/$userId")
        .withHeaders(authContext.headers: _*)
        .delete()
        .futureValue
  }

  object CitizenDetailsStub {
    def getCitizen(idName: String, taxId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/citizen-details/$idName/$taxId")
        .withHeaders(authContext.headers: _*)
        .get()
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
