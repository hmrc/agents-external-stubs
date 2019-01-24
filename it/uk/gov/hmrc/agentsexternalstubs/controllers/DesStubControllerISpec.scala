package uk.gov.hmrc.agentsexternalstubs.controllers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, urlEqualTo}
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.connectors.ExampleApiPlatformTestUserResponses
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
    }

    "GET /registration/relationship" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

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
          have.size(1) and eachElement(
            haveProperty[String]("referenceNumber") and
              haveProperty[String]("agentReferenceNumber", be("ZARN1234567")) and
              haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")) and (haveProperty[JsObject](
              "individual",
              haveProperty[String]("firstName") and haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName")))
          )
        )
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
          have.size(1) and eachElement(
            haveProperty[String]("id") and haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[
              String]("address1") and haveProperty[String]("address2") and haveProperty[Boolean]("isAgentAbroad"))
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
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "agents",
          have.size(1) and eachElement(
            haveProperty[String]("id") and haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[
              String]("address1") and haveProperty[String]("address2") and haveProperty[Boolean]("isAgentAbroad"))
        )
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /registration/business-details/nino/:idNumber" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessDetails("nino", "AA123456A")
        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("safeId") and haveProperty[String]("safeId") and haveProperty[
          String]("nino", be("AA123456A")) and haveProperty[String]("mtdbsa"))
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
        result.json
          .as[JsObject] should (haveProperty[String]("safeId") and haveProperty[String]("safeId") and haveProperty[
          String]("nino", be(nino)) and haveProperty[String]("mtdbsa"))
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
        result.json
          .as[JsObject] should (haveProperty[String]("safeId") and haveProperty[String]("safeId") and haveProperty[
          String]("nino") and haveProperty[String]("mtdbsa"))
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
          haveProperty[String]("agentReferenceNumber") and haveProperty[JsObject]("addressDetails"))
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
            .withIndividual(Some(Individual.seed("foo"))))
        createResult should haveStatus(201)

        val result = DesStub.subscribeToAgentServices("0123456789", Json.parse(validAgentSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("agentRegistrationNumber")
        )
      }

      "return 400 if utr not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServices("foo", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }

      "return 400 if utr not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.subscribeToAgentServices("0123456789", Json.parse(validAgentSubmission))
        result should haveStatus(400)
      }
    }

    "POST /registration/individual/utr/:utr" should {
      "register a new individual BPR with UTR" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividual("utr", "0123456789", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("utr", be("0123456789")) and haveProperty[JsObject](
            "individual") and notHaveProperty("organisation") and haveProperty[Boolean]("isAnAgent", be(false)) and haveProperty[
            Boolean]("isAnASAgent", be(false)) and haveProperty[JsObject]("address")
        )
      }

      "register a new individual BPR with NINO" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = DesStub.registerIndividual("nino", "HW827856C", Json.parse(validIndividualSubmission))
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("nino", be("HW827856C")) and haveProperty[JsObject](
            "individual") and notHaveProperty("organisation") and haveProperty[Boolean]("isAnAgent", be(false)) and haveProperty[
            Boolean]("isAnASAgent", be(false)) and haveProperty[JsObject]("address")
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
          haveProperty[String]("safeId", be("XA0000000000001")) and haveProperty[String]("utr", be("0123456789")) and haveProperty[
            JsObject]("individual") and notHaveProperty("organisation") and haveProperty[Boolean](
            "isAnAgent",
            be(false)) and haveProperty[Boolean]("isAnASAgent", be(false))
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
          haveProperty[String]("safeId", be("XA0000000000001")) and haveProperty[String]("nino", be("HW827856C")) and haveProperty[
            JsObject]("individual") and notHaveProperty("organisation") and haveProperty[Boolean](
            "isAnAgent",
            be(false)) and haveProperty[Boolean]("isAnASAgent", be(false))
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
          haveProperty[String]("safeId", be("XA0000000000001")) and haveProperty[String]("nino", be("HW827856C")) and haveProperty[
            JsObject]("individual") and notHaveProperty("organisation") and haveProperty[Boolean]("isAnAgent", be(true)) and haveProperty[
            Boolean]("isAnASAgent", be(false))
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
        result should haveValidJsonBody(haveProperty[Boolean]("Auth_64-8") and haveProperty[Boolean]("Auth_i64-8"))
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = DesStub.getSAAgentClientAuthorisationFlags("SA6012", "1234567890")
        result should haveStatus(404)
      }
    }
  }
}
