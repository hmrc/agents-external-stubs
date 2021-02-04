package uk.gov.hmrc.agentsexternalstubs.controllers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, urlEqualTo}
import org.joda.time.LocalDate
import play.api.libs.json.{Json, _}
import play.api.test.FakeRequest
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.connectors.ExampleApiPlatformTestUserResponses
import uk.gov.hmrc.agentsexternalstubs.controllers.ErrorResponse._
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.Individual
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.{Nino, Vrn}

class DesStubControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with ExampleDesPayloads with WireMockSupport
    with ExampleApiPlatformTestUserResponses {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val repo = app.injector.instanceOf[RecordsRepository]
  lazy val controller= app.injector.instanceOf[DesStubController]

  "DesController" when {

    "POST /registration/relationship" should {
      "respond 200 when authorising for ITSA" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",  
                       |   "agentReferenceNumber": "ZARN1234567",  
                       |   "regime": "ITSA",
                       |   "authorisation": {    
                       |     "action": "Authorise",    
                       |     "isExclusiveAgent": true  
                       |   }
                       |}
          """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when authorising for ITSA through API gateway" in {
        SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        implicit val apiAuthContext: AuthContext = AuthContext.fromHeaders("X-Client-ID" -> "foo123")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",  
                       |   "agentReferenceNumber": "ZARN1234567",  
                       |   "regime": "ITSA",
                       |   "authorisation": {    
                       |     "action": "Authorise",    
                       |     "isExclusiveAgent": true  
                       |   }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when de-authorising an ITSA relationship" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",
                       |   "agentReferenceNumber": "ZARN1234567",
                       |   "regime": "ITSA",
                       |   "authorisation": {
                       |     "action": "De-Authorise"
                       |   }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when authorising for TRS with UTR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "2110118025",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "UTR",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
          """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when authorising for TRS with UTR through API gateway" in {
        SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        implicit val apiAuthContext: AuthContext = AuthContext.fromHeaders("X-Client-ID" -> "foo123")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "2110118025",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "UTR",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when de-authorising an TRS relationship with UTR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "2110118025",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "UTR",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when authorising for TRS with URN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "XXTRUST80000001",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "URN",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
          """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when authorising for TRS with URN through API gateway" in {
        SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        implicit val apiAuthContext: AuthContext = AuthContext.fromHeaders("X-Client-ID" -> "foo123")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "XXTRUST80000001",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "URN",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }

      "respond 200 when de-authorising an TRS relationship with URN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "XXTRUST80000001",
                       |   "agentReferenceNumber": "PARN0876123",
                       |   "idType": "URN",
                       |   "regime": "TRS",
                       |   "authorisation": {
                       |     "action": "Authorise",
                       |     "isExclusiveAgent": true
                       |     }
                       |}
                     """.stripMargin))
        result should haveStatus(200)
      }
    }

    "GET /registration/relationship" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        await(
          repo.store(
            RelationshipRecord(
              regime = "VATC",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "987654321",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship for UTR" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "UTR",
              refNumber = "1234567890",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "TRS", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship for URN" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "URN",
              refNumber = "XXTRUST80000001",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "TRS", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship/nino/:nino" should {
      "return 200 response if relationship exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createAgentResult = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createAgentResult should haveStatus(201)
        val createRelationshipResult = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createRelationshipResult should haveStatus(201)

        val result = DesStub.getLegacyRelationshipsByNino("AA123456A")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "agents",
          have.size(1),
          eachElement(
            haveProperty[String]("id"),
            haveProperty[String]("agentId"),
            haveProperty[String]("agentName"),
            haveProperty[String]("address1"),
            haveProperty[String]("address2"),
            haveProperty[Boolean]("isAgentAbroad")
          )
        )
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getLegacyRelationshipsByNino("HW827856C")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /registration/relationship/utr/:utr" should {
      "return 200 response if relationship exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createAgentResult = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createAgentResult should haveStatus(201)
        val createRelationshipResult = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createRelationshipResult should haveStatus(201)

        val result = DesStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Seq[JsObject]](
            "agents",
            have.size(1),
            eachElement(
              haveProperty[String]("id"),
              haveProperty[String]("agentId"),
              haveProperty[String]("agentName"),
              haveProperty[String]("address1"),
              haveProperty[String]("address2"),
              haveProperty[Boolean]("isAgentAbroad")
            )
          ))
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /trusts/agent-known-fact-check/:utr"   should {
        "respond 200 with trust details using UTR" in {
          implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
          val enrolmentKey = "HMRC-TERS-ORG~SAUTR~0123456789"
          Users.create(UserGenerator.organisation("foo1").withPrincipalEnrolment(enrolmentKey))
          val trustTaxIdentifier = "0123456789"

          val result = DesStub.getTrustKnownFactsUtr(trustTaxIdentifier)
          result should haveStatus(200)

          result.json.as[JsObject] should haveProperty[JsObject]("trustDetails",
            haveProperty[String]("utr"),
            haveProperty[String]("trustName"),
            haveProperty[String]("serviceName"))
        }
      }

    "GET /trusts/agent-known-fact-check/:urn"   should {
      "respond 200 with trust details using URN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-TERSNT-ORG~URN~XXTRUST80000001"
        Users.create(UserGenerator.organisation("foo1").withPrincipalEnrolment(enrolmentKey))
        val trustTaxIdentifier = "XXTRUST80000001"

        val result = DesStub.getTrustKnownFactsUrn(trustTaxIdentifier)
        result should haveStatus(200)

        result.json.as[JsObject] should haveProperty[JsObject]("trustDetails",
          haveProperty[String]("urn"),
          haveProperty[String]("trustName"),
          haveProperty[String]("serviceName"))
      }
    }

    "GET /registration/business-details/nino/:idNumber" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessDetails("nino", "AA123456A")
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("safeId"),
          haveProperty[String]("nino", be("AA123456A")),
          haveProperty[String]("mtdbsa"))
      }

      "return 200 response if record not found but user pulled from external source" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val nino = Generator.ninoNoSpacesGen.sample.get
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/nino/$nino"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testIndividualResponse(Nino(nino)))))
        val result = DesStub.getBusinessDetails("nino", nino)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("safeId"),
          haveProperty[String]("nino", be(nino)),
          haveProperty[String]("mtdbsa"))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getBusinessDetails("nino", "HW827856C")
        result should haveStatus(404)
      }

      "return 404 response if record not found on planet hmrc and api-platform-test-user is not available" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/nino/HW827856C"))
            .willReturn(aResponse()
              .withStatus(502)))
        val result = DesStub.getBusinessDetails("nino", "HW827856C")
        result should haveStatus(404)
      }

      "return 404 response if record not found on planet hmrc and api-platform-test-user returns 404" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/nino/HW827856C"))
            .willReturn(aResponse()
              .withStatus(404)))
        val result = DesStub.getBusinessDetails("nino", "HW827856C")
        result should haveStatus(404)
      }
    }

    "GET /registration/business-details/mtdbsa/:idNumber" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessDetails("mtdbsa", "123456789012345")
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("safeId"),
          haveProperty[String]("nino"),
          haveProperty[String]("mtdbsa"))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getBusinessDetails("mtdbsa", "999999999999999")
        result should haveStatus(404)
      }
    }

    "GET /vat/customer/vrn/:vrn/information" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult should haveStatus(201)

        val result = DesStub.getVatCustomerInformation("123456789")
        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[String]("vrn")
      }

      "return 200 response if record not found but organisation pulled from external source" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val vrn = Generator.vrnGen.sample.get
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/organisations/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testOrganisationResponse(Vrn(vrn)))))
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(404)))

        val result = DesStub.getVatCustomerInformation(vrn)
        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[String]("vrn", be(vrn))
      }

      "return 200 response if record not found but individual pulled from external source" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val vrn = Generator.vrnGen.sample.get
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/organisations/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(404)))
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testIndividualResponse(vrn = Vrn(vrn)))))

        val result = DesStub.getVatCustomerInformation(vrn)
        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[String]("vrn", be(vrn))
      }

      "return 200 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getVatCustomerInformation("999999999")
        result should haveStatus(200)
      }

      "return 200 response if record not found on planet hmrc and api-platform-test-user is not available" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val vrn = Generator.vrnGen.sample.get
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/organisations/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(502)))
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(502)))

        val result = DesStub.getVatCustomerInformation(vrn)
        result should haveStatus(200)
      }

      "return 200 response if record not found on planet hmrc and api-platform-test-user returns 404" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val vrn = Generator.vrnGen.sample.get
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/organisations/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(404)))
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/individuals/vrn/$vrn"))
            .willReturn(aResponse()
              .withStatus(404)))

        val result = DesStub.getVatCustomerInformation(vrn)
        result should haveStatus(200)
      }
    }

    "GET /vat/known-facts/control-list/:vrn" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult should haveStatus(201)

        val result = DesStub.getVatKnownFacts("999999999")
        result should haveStatus(404)
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult should haveStatus(201)

        val result = DesStub.getVatKnownFacts("123456789")
        result should haveStatus(200)
        val json = result.json.as[JsObject]
        json should haveProperty[String]("vrn")
        json should haveProperty[String]("dateOfReg")
      }


    }

    "GET /registration/personal-details/arn/:arn" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessPartnerRecord("arn", "AARN1234567")
        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[String]("agentReferenceNumber")
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getBusinessPartnerRecord("arn", "BARN1234567")
        result should haveStatus(404)
      }
    }

    "GET /registration/personal-details/utr/:utr" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessPartnerRecord("utr", "0123456789")
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentReferenceNumber"),
          haveProperty[JsObject]("addressDetails"))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getBusinessPartnerRecord("utr", "0123456789")
        result should haveStatus(404)
      }
    }

    "POST /registration/agents/utr/:utr" should {
      "subscribe agent to AgentServices and return ARN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessPartnerRecord(
          BusinessPartnerRecord
            .seed("foo")
            .withUtr(Some("0123456789"))
            .withAgentReferenceNumber(None)
            .withIndividual(Some(Individual.seed("foo"))),
          autoFill = false
        )
        createResult should haveStatus(201)

        val result = DesStub.subscribeToAgentServicesWithUtr("0123456789", Json.parse(validAgentSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("agentRegistrationNumber")
        )
      }

      "return 400 if utr not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServicesWithUtr("foo", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }

      "return 400 if utr not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServicesWithUtr("0123456789", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }
    }

    "POST /registration/agents/safeId/:safeId" should {
      "subscribe agent to AgentServices and return ARN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessPartnerRecord(
          BusinessPartnerRecord
            .seed("foo")
            .withSafeId("XE0001234567890")
            .withUtr(None)
            .withAgentReferenceNumber(None)
            .withIndividual(Some(Individual.seed("foo"))),
          autoFill = false
        )
        createResult should haveStatus(201)

        val result = DesStub.subscribeToAgentServicesWithSafeId("XE0001234567890", Json.parse(validAgentSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId", be("XE0001234567890")),
          haveProperty[String]("agentRegistrationNumber")
        )
      }

      "return 400 if safeId not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServicesWithSafeId("foo", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }

      "return 400 if safeId not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServicesWithSafeId("XE0001234567890", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }
    }

    "POST /registration/individual/utr/:utr" should {
      "register a new individual BPR with UTR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividual("utr", "0123456789", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("utr", be("0123456789")),
          haveProperty[JsObject]("individual") and notHaveProperty("organisation"),
          haveProperty[Boolean]("isAnAgent", be(false)),
          haveProperty[Boolean]("isAnASAgent", be(false)),
          haveProperty[JsObject]("address")
        )
      }

      "register a new individual BPR with NINO" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividual("nino", "HW827856C", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("nino", be("HW827856C")),
          haveProperty[JsObject]("individual") and notHaveProperty("organisation"),
          haveProperty[Boolean]("isAnAgent", be(false)),
          haveProperty[Boolean]("isAnASAgent", be(false)),
          haveProperty[JsObject]("address")
        )
      }

      "return an existing BPR if found by UTR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult = Records.createBusinessPartnerRecord(
          BusinessPartnerRecord
            .seed("foo")
            .withSafeId("XA0000000000001")
            .withUtr(Some("0123456789"))
            .withIndividual(Some(Individual.seed("foo")))
            .withIsAnAgent(false)
            .withIsAnASAgent(false)
            .withOrganisation(None)
        )
        createResult should haveStatus(201)

        val result = DesStub.registerIndividual("utr", "0123456789", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId", be("XA0000000000001")),
          haveProperty[String]("utr", be("0123456789")),
          haveProperty[JsObject]("individual") and notHaveProperty("organisation"),
          haveProperty[Boolean]("isAnAgent", be(false)),
          haveProperty[Boolean]("isAnASAgent", be(false))
        )
      }

      "return an existing BPR if found by NINO" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult = Records.createBusinessPartnerRecord(
          BusinessPartnerRecord
            .seed("foo")
            .withSafeId("XA0000000000001")
            .withNino(Some("HW827856C"))
            .withIndividual(Some(Individual.seed("foo")))
            .withIsAnAgent(false)
            .withIsAnASAgent(false)
            .withOrganisation(None)
        )
        createResult should haveStatus(201)

        val result = DesStub.registerIndividual("nino", "HW827856C", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId", be("XA0000000000001")),
          haveProperty[String]("nino", be("HW827856C")),
          haveProperty[JsObject]("individual") and notHaveProperty("organisation"),
          haveProperty[Boolean]("isAnAgent", be(false)),
          haveProperty[Boolean]("isAnASAgent", be(false))
        )
      }

      "return an existing BPR even if asAnAgent flags does not match" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult = Records.createBusinessPartnerRecord(
          BusinessPartnerRecord
            .seed("foo")
            .withSafeId("XA0000000000001")
            .withNino(Some("HW827856C"))
            .withIndividual(Some(Individual.seed("foo")))
            .withIsAnAgent(true)
            .withIsAnASAgent(false)
            .withOrganisation(None)
        )
        createResult should haveStatus(201)

        val result = DesStub.registerIndividual("nino", "HW827856C", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId", be("XA0000000000001")),
          haveProperty[String]("nino", be("HW827856C")),
          haveProperty[JsObject]("individual") and notHaveProperty("organisation"),
          haveProperty[Boolean]("isAnAgent", be(true)),
          haveProperty[Boolean]("isAnASAgent", be(false))
        )
      }
    }

    "GET /sa/agents/:agentref/client/:utr" should {
      "return 200 response if relationship exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createAgentResult = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createAgentResult should haveStatus(201)
        val createRelationshipResult = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createRelationshipResult should haveStatus(201)

        val result = DesStub.getSAAgentClientAuthorisationFlags("SA6012", "1234567890")
        result should haveStatus(200)
        result should haveValidJsonBody(haveProperty[Boolean]("Auth_64-8"), haveProperty[Boolean]("Auth_i64-8"))
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getSAAgentClientAuthorisationFlags("SA6012", "1234567890")
        result should haveStatus(404)
      }
    }

    "POST /registration/02.00.00/individual" should {
      "register a new individual BPR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividualWithoutID(Json.parse(validIndividualWithoutIDSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("sapNumber"),
          haveProperty[String]("processingDate")
        )
      }

      "return 400 if payload is for organisation" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividualWithoutID(Json.parse(validOrganisationWithoutIDSubmission))
        result should haveStatus(400)
      }

      "return 400 if payload is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividualWithoutID(Json.parse(invalidWithoutIDSubmission))
        result should haveStatus(400)
      }
    }

    "POST /registration/02.00.00/organisation" should {
      "register a new organisation BPR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerOrganisationWithoutID(Json.parse(validOrganisationWithoutIDSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId"),
          haveProperty[String]("sapNumber"),
          haveProperty[String]("processingDate")
        )
      }

      "return 400 if payload is for individual" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerOrganisationWithoutID(Json.parse(validIndividualWithoutIDSubmission))
        result should haveStatus(400)
      }

      "return 400 if payload is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividualWithoutID(Json.parse(invalidWithoutIDSubmission))
        result should haveStatus(400)
      }
    }

    "POST /agents/paye/:agentCode/clients/compare" should {
      "return 200 with agent's epaye client information (employers) for given empRefs" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val agent = UserGenerator.agent()
        Users.updateCurrent(agent).status shouldBe 202
        val agentCode = agent.agentCode.get
        val employerAuths = EmployerAuths.generate(agentCode).withAgentCode(agentCode)
        Records.createEmployerAuths(employerAuths).status shouldBe 201

        val payload = EmployerAuthsPayload(
          employerAuths.empAuthList.map(e => EmployerAuthsPayload.EmpRef(e.empRef.districtNumber, e.empRef.reference)))
        val result = DesStub.retrieveLegacyAgentClientPayeInformation(agentCode, payload)

        result should haveStatus(200)
        result should haveValidJsonBody(
          havePropertyArrayOf[JsObject](
            "empAuthList",
            haveProperty[JsObject]("empRef", haveProperty[String]("districtNumber"), haveProperty[String]("reference")),
            haveProperty[JsObject](
              "aoRef",
              haveProperty[String]("districtNumber"),
              haveProperty[String]("reference"),
              haveProperty[String]("payType"),
              haveProperty[String]("checkCode")),
            haveProperty[Boolean]("Auth_64-8"),
            haveProperty[Boolean]("Auth_OAA")
          )
        )
      }

      "return 204 if agent data exists but no matching empRefs found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val agent = UserGenerator.agent()
        Users.updateCurrent(agent).status shouldBe 202
        val agentCode = agent.agentCode.get
        val employerAuths = EmployerAuths.generate(agentCode).withAgentCode(agentCode)
        Records.createEmployerAuths(employerAuths).status shouldBe 201

        val payload = EmployerAuthsPayload(Seq(EmployerAuthsPayload.EmpRef("ABC", "1234567890")))
        val result = DesStub.retrieveLegacyAgentClientPayeInformation(agentCode, payload)

        result should haveStatus(204)
      }

      "return 404 if no agent data is not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val payload = EmployerAuthsPayload(Seq(EmployerAuthsPayload.EmpRef("ABC", "1234567890")))

        val result = DesStub.retrieveLegacyAgentClientPayeInformation("FOO123456", payload)
        result should haveStatus(404)
      }
    }

    "DELETE /agents/paye/:agentCode/clients/:taxOfficeNumber/:taxOfficeReference" should {
      "return 200 after removing given employer auth from agent's data" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val agent = UserGenerator.agent()
        Users.updateCurrent(agent).status shouldBe 202
        val agentCode = agent.agentCode.get
        val employerAuths = EmployerAuths(
          agentCode = agentCode,
          empAuthList = Seq(
            EmployerAuths.EmpAuth(
              empRef = EmployerAuths.EmpAuth.EmpRef("123", "111"),
              aoRef = EmployerAuths.EmpAuth.AoRef("123", "A", "1", "111"),
              `Auth_64-8` = true,
              Auth_OAA = false
            ),
            EmployerAuths.EmpAuth(
              empRef = EmployerAuths.EmpAuth.EmpRef("567", "222"),
              aoRef = EmployerAuths.EmpAuth.AoRef("567", "B", "2", "222"),
              `Auth_64-8` = false,
              Auth_OAA = true
            )
          )
        )
        val createRecordResult = Records.createEmployerAuths(employerAuths)
        createRecordResult.status shouldBe 201
        val recordUrl = createRecordResult.json.as[Links].self.get
        val recordResultBefore = get(recordUrl)
        recordResultBefore.status shouldBe 200

        val empRefToRemove = recordResultBefore.json.as[EmployerAuths].empAuthList.head.empRef

        val result = DesStub
          .removeLegacyAgentClientPayeRelationship(agentCode, empRefToRemove.districtNumber, empRefToRemove.reference)
        result.status shouldBe 200

        val recordResultAfter = get(recordUrl)
        recordResultAfter.status shouldBe 200

        recordResultAfter.json.as[EmployerAuths].empAuthList.find(_.empRef == empRefToRemove) shouldBe None
      }

      "return 200 after removing the only employer auth from agent's data" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val agent = UserGenerator.agent()
        Users.updateCurrent(agent).status shouldBe 202
        val agentCode = agent.agentCode.get
        val employerAuths = EmployerAuths(
          agentCode = agentCode,
          empAuthList = Seq(
            EmployerAuths.EmpAuth(
              empRef = EmployerAuths.EmpAuth.EmpRef("123", "111"),
              aoRef = EmployerAuths.EmpAuth.AoRef("123", "A", "1", "111"),
              `Auth_64-8` = true,
              Auth_OAA = false
            )
          )
        )
        val createRecordResult = Records.createEmployerAuths(employerAuths)
        createRecordResult.status shouldBe 201
        val recordUrl = createRecordResult.json.as[Links].self.get
        val recordResultBefore = get(recordUrl)
        recordResultBefore.status shouldBe 200

        val result = DesStub
          .removeLegacyAgentClientPayeRelationship(agentCode, "123", "111")
        result.status shouldBe 200

        val recordResultAfter = get(recordUrl)
        recordResultAfter.status shouldBe 404
      }

      "return 404 if could not find agent's data" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub
          .removeLegacyAgentClientPayeRelationship("ABC123456789", "123", "111")
        result.status shouldBe 404
      }

      "return 400 if invalid agentCode or empRef" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result1 = DesStub
          .removeLegacyAgentClientPayeRelationship("jdhjshdjshjadhsahjdh", "123", "111")
        result1.status shouldBe 400

        val result2 = DesStub
          .removeLegacyAgentClientPayeRelationship("ABC123456789", "aaa", "111")
        result2.status shouldBe 400

        val result3 = DesStub
          .removeLegacyAgentClientPayeRelationship("ABC123456789", "123", "hhahdjhasjdhjh")
        result3.status shouldBe 400
      }
    }

    "GET /corporation-tax/identifiers/:idType/:idValue" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload))
        createResult should haveStatus(201)

        val result = DesStub.getCtReference("crn", "AA123456")
        result should haveStatus(200)
        result should haveValidJsonBody(haveProperty[String]("CTUTR", be("0123456789")))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getCtReference("crn", "AA111111")
        result should haveStatus(404)
      }

      "return 400 response if invalid CRN" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result1 = DesStub.getCtReference("crn", "11AAAAAA")
        result1 should haveStatus(400)
        val result2 = DesStub.getCtReference("crn", "AAA11111")
        result2 should haveStatus(400)
        val result3 = DesStub.getCtReference("crn", "AA1111111")
        result3 should haveStatus(400)
      }
    }

    "GET /subscriptions/:regime/:idType/:cgtRef" should {
      "return CGT subscription details as expected" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("HMRC-CGT-PD", Some(Seq(Identifier("CGTPDRef", "XMCGTP707663428")))))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = get("/subscriptions/CGT/ZCGT/XMCGTP707663428")

        result2 should haveStatus(200)
        result2 should haveValidJsonBody(haveProperty[String]("regime", be("CGT")))
        result2 should haveValidJsonBody(haveProperty[JsObject](
          "subscriptionDetails",
          haveProperty[JsObject]("typeOfPersonDetails"),
          haveProperty[JsObject]("addressDetails")))
      }

      "handle invalid regime" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("HMRC-CGT-PD", Some(Seq(Identifier("CGTPDRef", "XMCGTP707663428")))))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = get("/subscriptions/xxx/ZCGT/XMCGTP707663428")

        result2 should haveStatus(400)
        result2 should haveValidJsonBody(haveProperty[String]("code", be("INVALID_REGIME")))
      }

      "handle invalid idType" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("HMRC-CGT-PD", Some(Seq(Identifier("CGTPDRef", "XMCGTP707663428")))))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = get("/subscriptions/CGT/xxx/XMCGTP707663428")

        result2 should haveStatus(400)
        result2 should haveValidJsonBody(haveProperty[String]("code", be("INVALID_IDTYPE")))
      }

      "handle invalid regime and idType" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("HMRC-CGT-PD", Some(Seq(Identifier("CGTPDRef", "XMCGTP707663428")))))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = get("/subscriptions/xxx/yyy/XMCGTP707663428")

        result2 should haveStatus(400)
        result2 should haveValidJsonBody(haveProperty[String]("code", be("INVALID_REQUEST")))
      }
    }
  }
}
