package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{HeaderNames, MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSClient, WSCookie, WSResponse}
import play.api.mvc.{Cookie, Cookies, Session}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, SignInRequest, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

trait AuthContext {
  self =>

  def headers: Seq[(String, String)]

  def withHeader(key: String, name: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = self.headers :+ (key -> name)
  }
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

  def fromSession(session: (String, String)*): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.COOKIE -> Cookies.encodeCookieHeader(Seq(Session.encodeAsCookie(new Session(session.toMap))))
    )
  }

  def fromHeaders(headerSeq: (String, String)*): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = headerSeq
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

  def post[T: Writeable](path: String, payload: T)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHeaders(authContext.headers: _*)
      .post[T](payload)
      .futureValue

  def put[T: Writeable](path: String, payload: T)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHeaders(authContext.headers: _*)
      .put[T](payload)
      .futureValue

  def delete(path: String)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHeaders(authContext.headers: _*)
      .delete()
      .futureValue

  object SignIn {
    def signIn(
      userId: String = null,
      password: String = null,
      providerType: String = null,
      planetId: String = null): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/sign-in")
        .post(SignInRequest(Option(userId), Option(password), Option(providerType), Option(planetId)))
        .futureValue

    def authSessionFor(loginResponse: WSResponse): WSResponse =
      wsClient
        .url(s"$url${loginResponse.header(HeaderNames.LOCATION).getOrElse("")}")
        .get()
        .futureValue

    def currentSession(implicit authContext: AuthContext): WSResponse =
      get("/agents-external-stubs/session/current")

    def signInAndGetSession(
      userId: String = UUID.randomUUID().toString,
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

    def getAuthority()(implicit authContext: AuthContext): WSResponse =
      get(s"/auth/authority")

    def getIds()(implicit authContext: AuthContext): WSResponse =
      get(s"/auth/_ids")

    def getEnrolments()(implicit authContext: AuthContext): WSResponse =
      get(s"/auth/_enrolments")
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
        .url(s"$url/agents-external-stubs/users")
        .withHeaders(authContext.headers: _*)
        .post(Json.toJson(user))
        .futureValue

    def delete(userId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/$userId")
        .withHeaders(authContext.headers: _*)
        .delete()
        .futureValue

    def createApiPlatformTestUser[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post("/agents-external-stubs/users/api-platform", payload)
  }

  object UserDetailsStub {
    def getUser(id: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/user-details/id/$id")
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

    def setKnownFacts[T: Writeable](enrolmentKey: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      put(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", payload)

    def removeKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey")

    def getUserEnrolments(
      userId: String,
      `type`: String = "principal",
      service: Option[String] = None,
      `start-record`: Option[Int] = None,
      `max-records`: Option[Int] = None)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/users/$userId/enrolments")
        .withQueryString(
          Seq(
            "type"         -> Some(`type`),
            "service"      -> service,
            "start-record" -> `start-record`.map(_.toString),
            "max-records"  -> `max-records`.map(_.toString)).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*)
        .withHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def getGroupEnrolments(
      groupId: String,
      `type`: String = "principal",
      service: Option[String] = None,
      `start-record`: Option[Int] = None,
      `max-records`: Option[Int] = None,
      userId: Option[String] = None,
      `unassigned-clients`: Option[Boolean] = None)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments")
        .withQueryString(Seq(
          "type"               -> Some(`type`),
          "service"            -> service,
          "start-record"       -> `start-record`.map(_.toString),
          "max-records"        -> `max-records`.map(_.toString),
          "userId"             -> userId.map(_.toString),
          "unassigned-clients" -> `unassigned-clients`.map(_.toString)
        ).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHeaders(authContext.headers: _*)
        .get()
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

    def getBusinessPartnerRecord(idType: String, idNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/personal-details/$idType/$idNumber")

    def subscribeToAgentServicesWithUtr[T: Writeable](utr: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/agents/utr/$utr", payload)

    def subscribeToAgentServicesWithSafeId[T: Writeable](safeId: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/agents/safeId/$safeId", payload)

    def registerIndividual[T: Writeable](idType: String, idNumber: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/individual/$idType/$idNumber", payload)

    def registerOrganisation[T: Writeable](idType: String, idNumber: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/organisation/$idType/$idNumber", payload)

    def getSAAgentClientAuthorisationFlags(agentref: String, utr: String)(
      implicit authContext: AuthContext): WSResponse =
      get(s"/sa/agents/$agentref/client/$utr")

    def registerIndividualWithoutID[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/individual", payload)

    def registerOrganisationWithoutID[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/organisation", payload)
  }

  object DataStreamStubs {
    def writeAudit(event: String)(implicit authContext: AuthContext): WSResponse =
      post("/write/audit", event)

    def writeAuditMerged(event: String)(implicit authContext: AuthContext): WSResponse =
      post("/write/audit/merged", event)

  }

  object NiExemptionRegistrationStubs {
    def niBusinesses[T: Writeable](utr: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/ni-exemption-registration/ni-businesses/$utr", payload)
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
      post(s"/agents-external-stubs/records/business-details", payload)

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

    def createBusinessPartnerRecord[T: Writeable](payload: T, autoFill: Boolean = true)(
      implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-partner-record")
        .withQueryString(("autoFill", autoFill.toString))
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateBusinessPartnerRecord(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-partner-record/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createRelationship[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/relationship")
        .withHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateRelationship(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/relationship/generate")
        .withQueryString("seed" -> seed, "minimal" -> minimal.toString)
        .withHeaders(authContext.headers: _*)
        .get
        .futureValue
  }

  object KnownFacts {
    def getKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/known-facts/$enrolmentKey")

    def createKnownFacts[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/known-facts", payload)

    def upsertKnownFacts[T: Writeable](enrolmentKey: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      put(s"/agents-external-stubs/known-facts/$enrolmentKey", payload)

    def upsertKnownFactVerifier[T: Writeable](enrolmentKey: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      put(s"/agents-external-stubs/known-facts/$enrolmentKey/verifier", payload)

    def deleteKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/agents-external-stubs/known-facts/$enrolmentKey")
  }

  object SpecialCases {
    def getAllSpecialCases(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/special-cases")

    def getSpecialCase(id: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/special-cases/$id")

    def createSpecialCase[T: Writeable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/special-cases", payload)

    def updateSpecialCase[T: Writeable](id: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      put(s"/agents-external-stubs/special-cases/$id", payload)

    def deleteSpecialCase(id: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/agents-external-stubs/special-cases/$id")
  }

  object Config {
    def getServices()(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/config/services")
  }

  object Planets {
    def destroy(planetId: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/agents-external-stubs/planets/$planetId")
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
