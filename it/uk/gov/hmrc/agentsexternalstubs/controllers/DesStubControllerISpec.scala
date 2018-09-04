package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, RelationshipRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class DesStubControllerISpec
    extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs with ExampleDesPayloads {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val repo = app.injector.instanceOf[RecordsRepository]

  "DesController" when {

    "POST /registration/relationship" should {
      "respond 200 when authorising for ITSA" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

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
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

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
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

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
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
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
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = DesStub.getLegacyRelationshipsByNino("HW827856C")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /registration/relationship/utr/:utr" should {
      "return 200 response if relationship does not exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
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
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = DesStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /registration/business-details/nino/:idNumber" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val createResult = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessDetails("nino", "AA123456A")
        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("safeId") and haveProperty[String]("safeId") and haveProperty[
          String]("nino") and haveProperty[String]("mtdbsa"))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = DesStub.getBusinessDetails("nino", "HW827856C")
        result should haveStatus(404)
      }
    }

    "GET /registration/business-details/mtdbsa/:idNumber" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val createResult = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult should haveStatus(201)

        val result = DesStub.getBusinessDetails("mtdbsa", "123456789012345")
        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("safeId") and haveProperty[String]("safeId") and haveProperty[
          String]("nino") and haveProperty[String]("mtdbsa"))
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = DesStub.getBusinessDetails("mtdbsa", "999999999999999")
        result should haveStatus(404)
      }
    }

    "GET /vat/customer/vrn/:vrn/information" should {
      "return 200 response if record found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val createResult = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult should haveStatus(201)

        val result = DesStub.getVatCustomerInformation("123456789")
        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[String]("vrn")
      }

      "return 404 response if record not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = DesStub.getVatCustomerInformation("999999999")
        result should haveStatus(200)
      }
    }
  }
}
