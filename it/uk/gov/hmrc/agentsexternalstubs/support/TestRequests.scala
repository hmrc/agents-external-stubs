package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSClient, WSCookie, WSResponse}
import play.api.mvc.{Cookie, Cookies}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, SignInRequest, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

trait AuthContext {
  def headers: Seq[(String, String)]
}

object AuthContext {

  def fromToken(authToken: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer $authToken"
    )
  }

  def fromTokenAndSessionId(authToken: String, sessionId: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.AUTHORIZATION               -> s"Bearer $authToken",
      uk.gov.hmrc.http.HeaderNames.xSessionId -> sessionId
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

  implicit def jsonWriteable[T](implicit writes: Writes[T], jsValueWriteable: Writeable[JsValue]): Writeable[T] =
    new Writeable[T](
      entity => jsValueWriteable.transform(writes.writes(entity)),
      Some(MimeTypes.JSON)
    )

  implicit def headerCarrier(implicit authSession: AuthenticatedSession): HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization(s"Bearer ${authSession.authToken}")))

  implicit def fromImplicitAuthenticatedSession(implicit authSession: AuthenticatedSession): AuthContext =
    AuthContext.fromTokenAndSessionId(authSession.authToken, authSession.sessionId)

  implicit def fromAuthenticatedSession(authSession: AuthenticatedSession): AuthContext =
    AuthContext.fromToken(authSession.authToken)

  def get(path: String)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHeaders(authContext.headers: _*)
      .get()
      .futureValue

  object SignIn {
    def signIn(
      userId: String,
      password: String = "p@ssw0rd",
      providerType: String = "GovernmentGateway",
      planetId: String = UUID.randomUUID().toString): WSResponse =
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
      userId: String = "foo",
      password: String = "p@ssw0rd",
      planetId: String = UUID.randomUUID().toString): AuthenticatedSession = {
      val signedIn = signIn(userId, password, planetId = planetId)
      val session = authSessionFor(signedIn)
      session.json.as[AuthenticatedSession]
    }

    def signOut(implicit authContext: AuthContext): WSResponse = get(s"/agents-external-stubs/sign-out")
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

    def getAll(affinityGroup: Option[String] = None, limit: Option[Int] = None, agentCode: Option[String] = None)(
      implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withQueryString(
          Seq("affinityGroup" -> affinityGroup, "limit" -> limit.toString, "agentCode" -> agentCode).collect {
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
      get(s"/citizen-details/$idName/$taxId")
  }

  object UsersGroupSearchStub {
    def getUser(userId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/users-groups-search/users/$userId")

    def getGroup(groupId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/users-groups-search/groups/$groupId")

    def getGroupUsers(groupId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/users-groups-search/groups/$groupId/users")

    def getGroupByAgentCode(agentCode: String, agentId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/users-groups-search/groups")
        .withQueryString("agentCode" -> agentCode, "agentId" -> agentId)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object EnrolmentStoreProxyStub {
    def getUserIds(enrolmentKey: String, _type: String = "all")(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users")
        .withQueryString("type" -> _type)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def getGroupIds(enrolmentKey: String, _type: String = "all")(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups")
        .withQueryString("type" -> _type)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def allocateEnrolmentToGroup[T: Writeable](
      groupId: String,
      enrolmentKey: String,
      payload: T,
      `legacy-agentCode`: Option[String] = None)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey")
        .withQueryString(Seq("legacy-agentCode" -> `legacy-agentCode`).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def deallocateEnrolmentFromGroup(
      groupId: String,
      enrolmentKey: String,
      `legacy-agentCode`: Option[String] = None,
      keepAgentAllocations: Option[String] = None
    )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey")
        .withQueryString(
          Seq("legacy-agentCode" -> `legacy-agentCode`, "keepAgentAllocations" -> keepAgentAllocations).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*)
        .withHeaders(authContext.headers: _*)
        .delete()
        .futureValue
  }

  object DesStub {
    def authoriseOrDeAuthoriseRelationship[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/registration/relationship")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def getRelationship(
      regime: String,
      agent: Boolean,
      `active-only`: Boolean,
      idtype: Option[String] = None,
      `ref-no`: Option[String] = None,
      arn: Option[String] = None,
      from: Option[String] = None,
      to: Option[String] = None)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/registration/relationship")
        .withQueryString(Seq(
          "idtype"      -> `idtype`,
          "ref-no"      -> `ref-no`,
          "arn"         -> arn,
          "agent"       -> Some(agent.toString),
          "active-only" -> Some(`active-only`.toString),
          "regime"      -> Some(regime),
          "from"        -> from,
          "to"          -> to
        ).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def getLegacyRelationshipsByNino(nino: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/relationship/nino/$nino")

    def getLegacyRelationshipsByUtr(utr: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/relationship/utr/$utr")

    def getBusinessDetails(idType: String, idNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/business-details/$idType/$idNumber")

    def getVatCustomerInformation(vrn: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/vat/customer/vrn/$vrn/information")

    def getAgentRecord(idType: String, idNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/personal-details/$idType/$idNumber")

    def subscribeToAgentServices[T: Writeable](utr: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/registration/agents/utr/$utr")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue
  }

  object Records {
    def getRecords()(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/records")

    def getRecord(recordId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/records/$recordId")

    def updateRecord[T: Writeable](recordId: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/$recordId")
        .withHeaders(authContext.headers: _*)
        .put[T](payload)
        .futureValue

    def deleteRecord(recordId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/record/$recordId")
        .withHeaders(authContext.headers: _*)
        .delete
        .futureValue

    def createBusinessDetails[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-details")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateBusinessDetails(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-details/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createLegacyAgent[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-agent")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateLegacyAgent(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-agent/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createLegacyRelationship[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-relationship")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateLegacyRelationship(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-relationship/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createVatCustomerInformation[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/vat-customer-information")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateVatCustomerInformation(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/vat-customer-information/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createAgentRecord[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/agent-record")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateAgentRecord(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/agent-record/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue
  }

  object Config {
    def getServices()(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/config/services")
  }

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
