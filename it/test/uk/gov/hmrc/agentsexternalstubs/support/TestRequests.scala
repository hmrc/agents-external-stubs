/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import play.api.http.{CookiesConfiguration, HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.libs.ws.{BodyWritable, WSClient, WSCookie, WSResponse}
import play.api.mvc.{Cookie, DefaultCookieHeaderEncoding}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.EnrolmentsFromKnownFactsRequest
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import java.util.UUID

trait AuthContext {
  self =>

  def headers: Seq[(String, String)]

  def withHeader(key: String, name: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = self.headers :+ (key -> name)
  }
}

object AuthContext {

  val cookieEncoding = new DefaultCookieHeaderEncoding(CookiesConfiguration())

  def fromToken(authToken: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer $authToken"
    )
  }

  def fromTokenAndSessionId(authToken: String, sessionId: String): AuthContext = new AuthContext {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer $authToken",
      uk.gov.hmrc.http.HeaderNames.xSessionId -> sessionId
    )
  }

  def fromCookies(response: WSResponse): AuthContext = new AuthContext with CookieConverter {
    override def headers: Seq[(String, String)] = Seq(
      HeaderNames.COOKIE -> cookieEncoding.encodeCookieHeader(response.cookies.toList.map(x => asCookie(x)))
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

  implicit def jsonBodyWritable[T](implicit
                                   writes: Writes[T],
                                   jsValueBodyWritable: BodyWritable[JsValue]
                                  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)

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

  def post(path: String)(implicit authContext: AuthContext): WSResponse =
    wsClient
      .url(s"$url$path")
      .withHttpHeaders(authContext.headers: _*)
      .post("")
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
    def currentSession(implicit authContext: AuthContext): WSResponse =
      get("/agents-external-stubs/session/current")

    def signInAndGetSession(
                             userId: String = null,
                             password: String = "p@ssw0rd",
                             planetId: String = UUID.randomUUID().toString,
                             syncToAuthLoginApi: Boolean = false
                           ): AuthenticatedSession = {
      val signedIn = signIn(userId, password, planetId = planetId, syncToAuthLoginApi = syncToAuthLoginApi)
      val session = authSessionFor(signedIn)
      session.json.as[AuthenticatedSession]
    }

    def signIn(
                userId: String = null,
                password: String = null,
                providerType: String = null,
                planetId: String = null,
                syncToAuthLoginApi: Boolean = false,
                affinityGroup: Option[String] = Some(AG.Individual)
              ): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/sign-in?userIdFromPool")
        .post(
          SignInRequest(
            Option(userId),
            Option(password),
            Option(providerType),
            Option(planetId),
            if (syncToAuthLoginApi) Some(true) else None,
            newUserAffinityGroup = affinityGroup
          )
        )
        .futureValue

    def authSessionFor(loginResponse: WSResponse): WSResponse =
      wsClient
        .url(s"$url${loginResponse.header(HeaderNames.LOCATION).getOrElse("")}")
        .get()
        .futureValue

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

    def getAll(
                limit: Option[Int] = None,
                userId: Option[String] = None,
                groupId: Option[String] = None,
                agentCode: Option[String] = None,
                affinityGroup: Option[String] = None,
                principalEnrolmentService: Option[String] = None
              )(implicit
                authContext: AuthContext
              ): WSResponse = {
      val queryParams = Seq(
        "affinityGroup" -> affinityGroup,
        "limit" -> limit.map(_.toString),
        "groupId" -> groupId,
        "agentCode" -> agentCode,
        "principalEnrolmentService" -> principalEnrolmentService,
        "userId" -> userId
      ).collect { case (name, Some(value: String)) =>
        (name, value)
      }
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withQueryStringParameters(queryParams: _*)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue
    }

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

    def updateCurrentLegacy(userJson: JsObject)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users")
        .withHttpHeaders(authContext.headers: _*)
        .put(userJson)
        .futureValue

    def update(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users/${user.userId}?userIdFromPool")
        .withHttpHeaders(authContext.headers: _*)
        .put(Json.toJson(user))
        .futureValue

    /*
     Utility function to create both user and group with the given enrolments.
     If the user has any assigned enrolments, they will be added to the group's allocated enrolment as well, for consistency.
     */
    def create(
                user: User,
                affinityGroup: Option[String],
                agentCode: Option[String] = None,
                agentFriendlyName: Option[String] = None,
                agentId: Option[String] = None
              )(implicit authContext: AuthContext): WSResponse = {
      val maybeNewGroup = affinityGroup.map(ag =>
        GroupGenerator
          .generate(user.planetId.getOrElse(""), affinityGroup = ag, groupId = user.groupId)
          .copy(
            agentCode = agentCode,
            agentFriendlyName = agentFriendlyName,
            agentId = agentId,
            principalEnrolments = user.assignedPrincipalEnrolments.map(Enrolment.from(_)),
            delegatedEnrolments = user.assignedDelegatedEnrolments.map(Enrolment.from(_))
          )
      )
      maybeNewGroup.foreach(newGroup => Groups.create(newGroup))
      Users.unsafeCreate(
        user.copy(
          groupId = maybeNewGroup.map(_.groupId)
        )
      )
    }

    // Only use this to add a user to an existing group
    def unsafeCreate(user: User)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/users?userIdFromPool")
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

    def reindexAllUsers(implicit authContext: AuthContext): WSResponse =
      post("/agents-external-stubs/users/re-index")
  }

  object Groups {

    def getAll(affinityGroup: Option[String] = None, limit: Option[Int] = None, agentCode: Option[String] = None)(
      implicit authContext: AuthContext
    ): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups")
        .withQueryStringParameters(
          Seq("affinityGroup" -> affinityGroup, "limit" -> limit.toString, "agentCode" -> agentCode).collect {
            case (name, Some(value: String)) => (name, value)
          }: _*
        )
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def get(groupId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups/$groupId")
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def updateCurrent(group: Group)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups")
        .withHttpHeaders(authContext.headers: _*)
        .put(Json.toJson(group))
        .futureValue

    def update(group: Group)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups/${group.groupId}")
        .withHttpHeaders(authContext.headers: _*)
        .put(Json.toJson(group))
        .futureValue

    def create(group: Group)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups")
        .withHttpHeaders(authContext.headers: _*)
        .post(Json.toJson(group))
        .futureValue

    def delete(groupId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/groups/$groupId")
        .withHttpHeaders(authContext.headers: _*)
        .delete()
        .futureValue

    def reindexAllGroups(implicit authContext: AuthContext): WSResponse =
      post("/agents-external-stubs/groups/re-index")
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

    def getDelegatedEnrolments(groupId: String)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/delegated")
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

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
                                                   `legacy-agentCode`: Option[String] = None
                                                 )(implicit authContext: AuthContext): WSResponse =
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
          }: _*
        )
        .withHttpHeaders(authContext.headers: _*)
        .delete()
        .futureValue

    def setKnownFacts[T: BodyWritable](enrolmentKey: String, payload: T)(implicit
                                                                         authContext: AuthContext
    ): WSResponse =
      put(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", payload)

    def queryKnownFacts(payload: EnrolmentsFromKnownFactsRequest)(implicit
                                                                  authContext: AuthContext
    ): WSResponse =
      post(s"/enrolment-store-proxy/enrolment-store/enrolments", payload)

    def removeKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey")

    def getUserEnrolments(
                           userId: String,
                           `type`: String = "principal",
                           service: Option[String] = None,
                           `start-record`: Option[Int] = None,
                           `max-records`: Option[Int] = None
                         )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/users/$userId/enrolments")
        .withQueryStringParameters(
          Seq(
            "type" -> Some(`type`),
            "service" -> service,
            "start-record" -> `start-record`.map(_.toString),
            "max-records" -> `max-records`.map(_.toString)
          ).collect { case (name, Some(value: String)) =>
            (name, value)
          }: _*
        )
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
                            `unassigned-clients`: Option[Boolean] = None
                          )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments")
        .withQueryStringParameters(
          Seq(
            "type" -> Some(`type`),
            "service" -> service,
            "start-record" -> `start-record`.map(_.toString),
            "max-records" -> `max-records`.map(_.toString),
            "userId" -> userId.map(_.toString),
            "unassigned-clients" -> `unassigned-clients`.map(_.toString)
          ).collect { case (name, Some(value: String)) =>
            (name, value)
          }: _*
        )
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def assignUser(userId: String, enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      post(s"/tax-enrolments/users/$userId/enrolments/$enrolmentKey")

    def deassignUser(userId: String, enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/tax-enrolments/users/$userId/enrolments/$enrolmentKey")

    def setEnrolmentFriendlyName[T: BodyWritable](groupId: String, enrolmentKey: String, payload: T)(implicit
                                                                                                     authContext: AuthContext
    ): WSResponse =
      put(s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey/friendly_name", payload)
  }

  object HipStub {
    def displayAgentRelationship(
                                  regime: Option[String] = Some("VAT"),
                                  isAnAgent: Option[Boolean] = Some(true),
                                  activeOnly: Option[Boolean] = Some(true),
                                  idType: Option[String] = None,
                                  refNumber: Option[String] = None,
                                  arn: Option[String] = Some("AARN1234567"),
                                  dateFrom: Option[String] = None,
                                  dateTo: Option[String] = None,
                                  relationshipType: Option[String] = None,
                                  authProfile: Option[String] = None,
                                  transmittingSystemHeader: Option[String] = Some("HIP"),
                                  originatingSystemHeader: Option[String] = Some("MDTP"),
                                  correlationIdHeader: Option[String] = Some("dc87872c-3fe2-4dbf-ab72-0bfe8bccc502"),
                                  receiptDateHeader: Option[String] = Some("2024-11-22T12:54:24Z")
                                )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/etmp/RESTAdapter/rosm/agent-relationship")
        .withQueryStringParameters(
          Seq(
            "idType" -> idType,
            "refNumber" -> refNumber,
            "arn" -> arn,
            "isAnAgent" -> isAnAgent.map(_.toString),
            "activeOnly" -> activeOnly.map(_.toString),
            "regime" -> regime,
            "dateFrom" -> dateFrom,
            "dateTo" -> dateTo,
            "relationshipType" -> relationshipType,
            "authProfile" -> authProfile
          ).collect { case (name, Some(value: String)) =>
            (name, value)
          }: _*
        )
        .withHttpHeaders(
          authContext.headers ++
            Seq(
              "X-Transmitting-System" -> transmittingSystemHeader,
              "X-Originating-System" -> originatingSystemHeader,
              "correlationid" -> correlationIdHeader,
              "X-Receipt-Date" -> receiptDateHeader
            ).collect { case (name, Some(value: String)) =>
              (name, value)
            }: _*
        )
        .get()
        .futureValue


    def getSubscription(
                         arn: String = "ZARN1234567",
                         transmittingSystemHeader: Option[String] = Some("HIP"),
                         originatingSystemHeader: Option[String] = Some("MDTP"),
                         correlationIdHeader: Option[String] = Some("dc87872c-3fe2-4dbf-ab72-0bfe8bccc502"),
                         receiptDateHeader: Option[String] = Some("2024-11-22T12:54:24Z")
                       )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/etmp/RESTAdapter/generic/agent/subscription/$arn")
        .withHttpHeaders(
          authContext.headers ++
            Seq(
              "X-Transmitting-System" -> transmittingSystemHeader,
              "X-Originating-System" -> originatingSystemHeader,
              "correlationid" -> correlationIdHeader,
              "X-Receipt-Date" -> receiptDateHeader
            ).collect { case (name, Some(value: String)) =>
              (name, value)
            }: _*
        )
        .get()
        .futureValue


    def updateAgentRelationship(
                                 regime: String = "ITSA",
                                 idType: Option[String] = Some("MTDBSA"),
                                 refNumber: String = "1234",
                                 arn: String = "AARN1234567",
                                 isExclusiveAgent: Boolean = true,
                                 action: String = "0001",
                                 relationshipType: Option[String] = Some("ZA01"),
                                 authProfile: Option[String] = Some("ALL00001"),
                                 transmittingSystemHeader: Option[String] = Some("HIP"),
                                 originatingSystemHeader: Option[String] = Some("MDTP"),
                                 correlationIdHeader: Option[String] = Some("dc87872c-3fe2-4dbf-ab72-0bfe8bccc502"),
                                 receiptDateHeader: Option[String] = Some("2024-11-22T12:54:24Z")
                               )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/etmp/RESTAdapter/rosm/agent-relationship")
        .withHttpHeaders(
          authContext.headers ++
            Seq(
              "X-Transmitting-System" -> transmittingSystemHeader,
              "X-Originating-System" -> originatingSystemHeader,
              "correlationid" -> correlationIdHeader,
              "X-Receipt-Date" -> receiptDateHeader
            ).collect { case (name, Some(value: String)) =>
              (name, value)
            }: _*
        )
        .post(
          Json.toJson(
            UpdateRelationshipPayload(
              regime = regime,
              refNumber = refNumber,
              idType = idType,
              arn = arn,
              action = action,
              isExclusiveAgent = isExclusiveAgent,
              relationshipType = relationshipType,
              authProfile = authProfile
            )
          )
        )
        .futureValue

    def itsaTaxPayerBusinessDetails(
                                     nino: Option[String] = Some("AB732851A"),
                                     mtdReference: Option[String] = Some("WOHV90190595538"),
                                     transmittingSystemHeader: Option[String] = Some("HIP"),
                                     originatingSystemHeader: Option[String] = Some("MDTP"),
                                     correlationIdHeader: Option[String] = Some("dc87872c-3fe2-4dbf-ab72-0bfe8bccc502"),
                                     receiptDateHeader: Option[String] = Some("2024-11-22T12:54:24Z"),
                                     messageTypeHeader: Option[String] = Some("TaxpayerDisplay"),
                                     regimeTypeHeader: Option[String] = Some("ITSA")
                                   )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/etmp/RESTAdapter/itsa/taxpayer/business-details")
        .withQueryStringParameters(
          Seq(
            "nino" -> nino,
            "mtdReference" -> mtdReference
          ).collect { case (name, Some(value: String)) =>
            (name, value)
          }: _*
        )
        .withHttpHeaders(
          authContext.headers ++
            Seq(
              "X-Transmitting-System" -> transmittingSystemHeader,
              "X-Originating-System" -> originatingSystemHeader,
              "correlationid" -> correlationIdHeader,
              "X-Receipt-Date" -> receiptDateHeader,
              "X-Message-Type" -> messageTypeHeader,
              "X-Regime-Type" -> regimeTypeHeader
            ).collect { case (name, Some(value: String)) =>
              (name, value)
            }: _*
        )
        .get()
        .futureValue

    def createAgentSubscription(
                                 safeId: String,
                                 name: String = "Mi6",
                                 addr1: String = "10 New Street",
                                 addr2: Option[String] = None,
                                 addr3: Option[String] = None,
                                 addr4: Option[String] = None,
                                 postcode: Option[String] = Some("AA11AA"),
                                 country: String = "GB",
                                 phone: Option[String] = Some("01911234567"),
                                 email: String = "test@example.com",
                                 supervisoryBody: Option[String] = None,
                                 membershipNumber: Option[String] = None,
                                 evidenceObjectReference: Option[String] = None,
                                 updateDetailsStatus: String = "ACCEPTED",
                                 amlSupervisionUpdateStatus: String = "ACCEPTED",
                                 directorPartnerUpdateStatus: String = "ACCEPTED",
                                 acceptNewTermsStatus: String = "ACCEPTED",
                                 reriskStatus: String = "ACCEPTED",
                                 correlationIdHeader: Option[String] = Some("f0bd1f32-de51-45cc-9b18-0520d6e3ab1a"),
                                 transmittingSystemHeader: Option[String] = Some("HIP"),
                                 originatingSystemHeader: Option[String] = Some("MDTP"),
                                 receiptDateHeader: Option[String] = Some("2025-01-30T23:59:59Z")
                               )(implicit authContext: AuthContext): WSResponse = {
      val payload = Json.obj(
        "name" -> name,
        "addr1" -> addr1,
        "addr2" -> addr2,
        "addr3" -> addr3,
        "addr4" -> addr4,
        "postcode" -> postcode,
        "country" -> country,
        "phone" -> phone,
        "email" -> email,
        "supervisoryBody" -> supervisoryBody,
        "membershipNumber" -> membershipNumber,
        "evidenceObjectReference" -> evidenceObjectReference,
        "updateDetailsStatus" -> updateDetailsStatus,
        "amlSupervisionUpdateStatus" -> amlSupervisionUpdateStatus,
        "directorPartnerUpdateStatus" -> directorPartnerUpdateStatus,
        "acceptNewTermsStatus" -> acceptNewTermsStatus,
        "reriskStatus" -> reriskStatus
      )

      wsClient
        .url(s"$url/etmp/RESTAdapter/generic/agent/subscription/$safeId")
        .withHttpHeaders(
          authContext.headers ++
            Seq(
              "correlationid" -> correlationIdHeader,
              "X-Transmitting-System" -> transmittingSystemHeader,
              "X-Originating-System" -> originatingSystemHeader,
              "X-Receipt-Date" -> receiptDateHeader
            ).collect { case (k, Some(v)) => k -> v }: _*
        )
        .post(payload)
        .futureValue
    }
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
                         referenceNumber: Option[String] = None,
                         arn: Option[String] = None,
                         from: Option[String] = None,
                         to: Option[String] = None
                       )(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/registration/relationship")
        .withQueryStringParameters(
          Seq(
            "idtype" -> `idtype`,
            "ref-no" -> `ref-no`,
            "referenceNumber" -> referenceNumber,
            "arn" -> arn,
            "agent" -> Some(agent.toString),
            "active-only" -> Some(`active-only`.toString),
            "regime" -> Some(regime),
            "from" -> from,
            "to" -> to
          ).collect { case (name, Some(value: String)) =>
            (name, value)
          }: _*
        )
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue


    def getLegacyRelationshipsByNino(nino: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/relationship/nino/$nino")

    def getLegacyRelationshipsByUtr(utr: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/relationship/utr/$utr")

    def getTrustKnownFacts(utr: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/trusts/agent-known-fact-check/$utr")

    def getBusinessDetails(idType: String, idNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/business-details/$idType/$idNumber")

    def getVatCustomerInformation(vrn: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/vat/customer/vrn/$vrn/information")

    def getVatKnownFacts(vrn: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/vat/known-facts/control-list/$vrn")

    def getBusinessPartnerRecord(idType: String, idNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/registration/personal-details/$idType/$idNumber")

    def subscribeToAgentServicesWithUtr[T: BodyWritable](utr: String, payload: T)(implicit
                                                                                  authContext: AuthContext
    ): WSResponse =
      post(s"/registration/agents/utr/$utr", payload)

    def subscribeToAgentServicesWithSafeId[T: BodyWritable](safeId: String, payload: T)(implicit
                                                                                        authContext: AuthContext
    ): WSResponse =
      post(s"/registration/agents/safeId/$safeId", payload)

    def registerIndividual[T: BodyWritable](idType: String, idNumber: String, payload: T)(implicit
                                                                                          authContext: AuthContext
    ): WSResponse =
      post(s"/registration/individual/$idType/$idNumber", payload)

    def registerOrganisation[T: BodyWritable](idType: String, idNumber: String, payload: T)(implicit
                                                                                            authContext: AuthContext
    ): WSResponse =
      post(s"/registration/organisation/$idType/$idNumber", payload)

    def getSAAgentClientAuthorisationFlags(agentref: String, utr: String)(implicit
                                                                          authContext: AuthContext
    ): WSResponse =
      get(s"/sa/agents/$agentref/client/$utr")

    def registerIndividualWithoutID[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/individual", payload)

    def registerOrganisationWithoutID[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/registration/02.00.00/organisation", payload)

    def retrieveLegacyAgentClientPayeInformation[T: BodyWritable](agentCode: String, payload: T)(implicit
                                                                                                 authContext: AuthContext
    ): WSResponse =
      post(s"/agents/paye/$agentCode/clients/compare", payload)

    def removeLegacyAgentClientPayeRelationship(agentCode: String, taxOfficeNumber: String, taxOfficeReference: String)(
      implicit authContext: AuthContext
    ): WSResponse =
      delete(s"/agents/paye/$agentCode/clients/$taxOfficeNumber/$taxOfficeReference")

    def getCtReference(idType: String, idValue: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/corporation-tax/identifiers/$idType/$idValue")

    def getTrustKnownFactsUtr(utr: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/trusts/agent-known-fact-check/UTR/$utr")

    def getTrustKnownFactsUrn(urn: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/trusts/agent-known-fact-check/URN/$urn")

    def getTrustKnownFactsUrnIncorrectly(urn: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/trusts/agent-known-fact-check/$urn")

    def getAmlsSubscriptionStatus(amlsRegistrationNumber: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/anti-money-laundering/subscription/$amlsRegistrationNumber/status")

    def getPPTSubscriptionDisplayRecord(regime: String, pptReferenceNumber: String)(implicit
                                                                                    authContext: AuthContext
    ): WSResponse =
      get(s"/plastic-packaging-tax/subscriptions/$regime/$pptReferenceNumber/display")

    def getPillar2Record(plrReference: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/pillar2/subscription/$plrReference")

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

  object SsoGetDomains {
    def getDomains: WSResponse =
      get(s"/sso/domains")(NotAuthorized)
  }

  object GranPermsStubs {
    def massGenerateAgentsAndClients(payload: GranPermsGenRequest)(implicit authContext: AuthContext): WSResponse =
      post("/agents-external-stubs/test/gran-perms/generate-users", payload)
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
        .delete()
        .futureValue

    def createBusinessDetails[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/records/business-details", payload)

    def generateBusinessDetails(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/business-details/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get()
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
        .get()
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
        .get()
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
        .get()
        .futureValue

    def createBusinessPartnerRecord[T: BodyWritable](payload: T, autoFill: Boolean = true)(implicit
                                                                                           authContext: AuthContext
    ): WSResponse =
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
        .get()
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
        .get()
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
        .get()
        .futureValue

    def createPPTSubscriptionDisplayRecord[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/ppt-subscription")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def createPillar2Record[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/pillar2-subscription")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generatePPTSubscriptionDisplayRecord(seed: String, minimal: Boolean)(implicit
                                                                             authContext: AuthContext
    ): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/ppt-subscription/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue

    def createCbcSubscriptionRecord[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/cbc-subscription")
        .withHttpHeaders(authContext.headers: _*)
        .post[T](payload)
        .futureValue

    def generateCbcSubscriptionRecord(seed: String, minimal: Boolean)(implicit authContext: AuthContext): WSResponse =
      wsClient
        .url(s"$url/agents-external-stubs/records/cbc-subscription/generate")
        .withQueryStringParameters("seed" -> seed, "minimal" -> minimal.toString)
        .withHttpHeaders(authContext.headers: _*)
        .get()
        .futureValue
  }

  object KnownFacts {
    def getKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      get(s"/agents-external-stubs/known-facts/$enrolmentKey")

    def createKnownFacts[T: BodyWritable](payload: T)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/known-facts", payload)

    def upsertKnownFacts[T: BodyWritable](enrolmentKey: String, payload: T)(implicit
                                                                            authContext: AuthContext
    ): WSResponse =
      put(s"/agents-external-stubs/known-facts/$enrolmentKey", payload)

    def upsertKnownFactVerifier[T: BodyWritable](enrolmentKey: String, payload: T)(implicit
                                                                                   authContext: AuthContext
    ): WSResponse =
      put(s"/agents-external-stubs/known-facts/$enrolmentKey/verifier", payload)

    def deleteKnownFacts(enrolmentKey: String)(implicit authContext: AuthContext): WSResponse =
      delete(s"/agents-external-stubs/known-facts/$enrolmentKey")

    def createPAYEKnownFacts(agentId: String)(implicit authContext: AuthContext): WSResponse =
      post(s"/agents-external-stubs/known-facts/regime/PAYE/$agentId")
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
      httpOnly = false
    )
}
