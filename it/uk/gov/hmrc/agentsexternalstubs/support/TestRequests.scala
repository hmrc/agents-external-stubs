package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{BodyWritable, WSClient, WSCookie, WSResponse}
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

  implicit def jsonBodyWritable[T](
    implicit writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)

  implicit def headerCarrier(implicit authSession: AuthenticatedSession): HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization(s"Bearer ${authSession.authToken}")))

  implicit def fromImplicitAuthenticatedSession(implicit authSession: AuthenticatedSession): AuthContext =
    AuthContext.fromTokenAndSessionId(authSession.authToken, authSession.sessionId)

  implicit def fromAuthenticatedSession(authSession: AuthenticatedSession): AuthContext =
    AuthContext.fromToken(authSession.authToken)

  def get(path: String)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHttpHeaders(authContext.headers: _*)
      .get()
      .futureValue

  def post[T: BodyWritable](path: String, payload: T)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHttpHeaders(authContext.headers: _*)
      .post[T](payload)
      .futureValue

  def put[T: BodyWritable](path: String, payload: T)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHttpHeaders(authContext.headers: _*)
      .put[T](payload)
      .futureValue

  def delete(path: String)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHttpHeaders(authContext.headers: _*)
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
      userId: String = null,
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
        .withHttpHeaders(Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON) ++ authContext.headers: _*)
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
        .withQueryStringParameters(
          Seq("affinityGroup" -> affinityGroup, "limit" -> limit.toString, "agentCode" -> agentCode).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def get(userId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/$userId")
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def updateCurrent(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withHttpHeaders(authContext.headers: _*)
        .put(Json.toJson(user))
        .futureValue

    def update(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/${user.userId}")
        .withHttpHeaders(authContext.headers: _*)
        .put(Json.toJson(user))
        .futureValue

    def create(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withHttpHeaders(authContext.headers: _*)
        .post(Json.toJson(user))
        .futureValue

    def delete(userId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/$userId")
        .withHttpHeaders(authContext.headers: _*)
        .delete()
        .futureValue

    def createApiPlatformTestUser[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post("/agents-external-stubs/users/api-platform", payload)
  }

  object UserDetailsStub {
    def getUser(id: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/user-details/id/$id")
  }

  object CitizenDetailsStub {
    def getCitizen(idName: String, taxId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/citizen-details/$idName/$taxId")

    def getDesignatoryDetails(nino: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/citizen-details/$nino/designatory-details")

    def getDesignatoryDetailsBasic(nino: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/citizen-details/$nino/designatory-details/basic")
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
        .withQueryStringParameters("agentCode" -> agentCode, "agentId" -> agentId)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object EnrolmentStoreProxyStub {
    def getUserIds(enrolmentKey: String, _type: String = "all")(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users")
        .withQueryStringParameters("type" -> _type)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def getGroupIds(enrolmentKey: String, _type: String = "all")(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups")
        .withQueryStringParameters("type" -> _type)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def allocateEnrolmentToGroup[T: BodyWritable](
      groupId: String,
      enrolmentKey: String,
      payload: T,
      `legacy-agentCode`: Option[String] = None)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey")
        .withQueryStringParameters(Seq("legacy-agentCode" -> `legacy-agentCode`).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHttpHeaders(authContext.headers: _*)
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
        .withQueryStringParameters(
          Seq("legacy-agentCode" -> `legacy-agentCode`, "keepAgentAllocations" -> keepAgentAllocations).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*)
        .withHttpHeaders(authContext.headers: _*)
        .delete()
        .futureValue

    def setKnownFacts[T: BodyWritable](enrolmentKey: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
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
        .withQueryStringParameters(
          Seq(
            "type"         -> Some(`type`),
            "service"      -> service,
            "start-record" -> `start-record`.map(_.toString),
            "max-records"  -> `max-records`.map(_.toString)).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*)
        .withHttpHeaders(authContext.headers: _*)
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
        .withQueryStringParameters(Seq(
          "type"               -> Some(`type`),
          "service"            -> service,
          "start-record"       -> `start-record`.map(_.toString),
          "max-records"        -> `max-records`.map(_.toString),
          "userId"             -> userId.map(_.toString),
          "unassigned-clients" -> `unassigned-clients`.map(_.toString)
        ).collect {
          case (name, Some(value: String)) => (name, value)
        }: _*)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object DesStub {

    def authoriseOrDeAuthoriseRelationship[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/registration/relationship")
        .withHttpHeaders(authContext.headers: _*)
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
        .withQueryStringParameters(Seq(
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
        .withHttpHeaders(authContext.headers: _*)
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

    def subscribeToAgentServicesWithUtr[T: BodyWritable](utr: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/agents/utr/$utr", payload)

    def subscribeToAgentServicesWithSafeId[T: BodyWritable](safeId: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/agents/safeId/$safeId", payload)

    def registerIndividual[T: BodyWritable](idType: String, idNumber: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/individual/$idType/$idNumber", payload)

    def registerOrganisation[T: BodyWritable](idType: String, idNumber: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/registration/organisation/$idType/$idNumber", payload)

    def getSAAgentClientAuthorisationFlags(agentref: String, utr: String)(
      implicit authContext: AuthContext): WSResponse =
      get(s"/sa/agents/$agentref/client/$utr")

    def registerIndividualWithoutID[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/individual", payload)

    def registerOrganisationWithoutID[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/organisation", payload)

    def retrieveLegacyAgentClientPayeInformation[T: BodyWritable](agentCode: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      post(s"/agents/paye/$agentCode/clients/compare", payload)

    def removeLegacyAgentClientPayeRelationship(agentCode: String, taxOfficeNumber: String, taxOfficeReference: String)(
      implicit authContext: AuthContext): WSResponse =
      delete(s"/agents/paye/$agentCode/clients/$taxOfficeNumber/$taxOfficeReference")
  }

  object DataStreamStubs {
    def writeAudit(event: String)(implicit authContext: AuthContext): WSResponse =
      post("/write/audit", event)

    def writeAuditMerged(event: String)(implicit authContext: AuthContext): WSResponse =
      post("/write/audit/merged", event)

  }

  object NiExemptionRegistrationStubs {
    def niBusinesses[T: BodyWritable](utr: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/ni-exemption-registration/ni-businesses/$utr", payload)
  }

  object SsoValidateDomain {
    def validate(domain: String): WSResponse =
      get(s"/sso/validate/domain/$domain")(NotAuthorized)
  }

  object Records {
    def getRecords()(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/records")

    def getRecord(recordId: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/records/$recordId")

    def updateRecord[T: BodyWritable](recordId: String, payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/$recordId")
        .withHttpHeaders(authContext.headers: _*)
        .put[T](payload)
        .futureValue

    def deleteRecord(recordId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/record/$recordId")
        .withHttpHeaders(authContext.headers: _*)
        .delete
        .futureValue

    def createBusinessDetails[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/records/business-details", payload)

    def generateBusinessDetails(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-details/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createLegacyAgent[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-agent")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateLegacyAgent(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-agent/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createLegacyRelationship[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-relationship")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateLegacyRelationship(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/legacy-relationship/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createVatCustomerInformation[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/vat-customer-information")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateVatCustomerInformation(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/vat-customer-information/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createBusinessPartnerRecord[T: BodyWritable](payload: T, autoFill: Boolean = true)(
      implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-partner-record")
        .withQueryStringParameters(("autoFill", autoFill.toString))
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateBusinessPartnerRecord(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-partner-record/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createRelationship[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/relationship")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateRelationship(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/relationship/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue

    def createEmployerAuths[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/employer-auths")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateEmployerAuths(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/employer-auths/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get
        .futureValue
  }

  object KnownFacts {
    def getKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/known-facts/$enrolmentKey")

    def createKnownFacts[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/known-facts", payload)

    def upsertKnownFacts[T: BodyWritable](enrolmentKey: String, payload: T)(
      implicit authContext: AuthContext): WSResponse =
      put(s"/agents-external-stubs/known-facts/$enrolmentKey", payload)

    def upsertKnownFactVerifier[T: BodyWritable](enrolmentKey: String, payload: T)(
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

    def createSpecialCase[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/special-cases", payload)

    def updateSpecialCase[T: BodyWritable](id: String, payload: T)(implicit authContext: AuthContext): WSResponse =
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
      cookie.name,
      cookie.value,
      cookie.maxAge.map(_.toInt).orElse(Some(-1)),
      cookie.path.getOrElse("/"),
      cookie.domain,
      cookie.secure,
      false
    )
}
