package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, RelationshipRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class DesStubControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {

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

  }

  val validLegacyAgentPayload =
    """
      |{
      |    "agentId": "SA6012",
      |    "agentOwnRef": "abcdefghij",
      |    "hasAgent": false,
      |    "isRegisteredAgent": false,
      |    "govAgentId": "6WKC9BTJUTPH",
      |    "agentName": "Mr SA AGT_022",
      |    "agentPhoneNo": "03002003319",
      |    "address1": "Plaza 2",
      |    "address2": "Ironmasters Way",
      |    "address3": "Telford",
      |    "address4": "Shropshire",
      |    "postcode": "TF3 4NT",
      |    "isAgentAbroad": false,
      |    "agentCeasedDate": "2001-01-01"
      |}
    """.stripMargin

  val validLegacyRelationshipPayload =
    """
      |{
      |    "agentId": "SA6012",
      |    "nino": "AA123456A",
      |    "utr": "1234567890"
      |}
    """.stripMargin

  val validBusinessDetailsPayload = """
                                      |{
                                      |    "safeId": "XE00001234567890",
                                      |    "nino": "AA123456A",
                                      |    "mtdbsa": "123456789012345",
                                      |    "propertyIncome": false,
                                      |    "businessData": [
                                      |        {
                                      |            "incomeSourceId": "123456789012345",
                                      |            "accountingPeriodStartDate": "2001-01-01",
                                      |            "accountingPeriodEndDate": "2001-01-01",
                                      |            "tradingName": "RCDTS",
                                      |            "businessAddressDetails":
                                      |            {
                                      |                "addressLine1": "100 SuttonStreet",
                                      |                "addressLine2": "Wokingham",
                                      |                "addressLine3": "Surrey",
                                      |                "addressLine4": "London",
                                      |                "postalCode": "DH14EJ",
                                      |                "countryCode": "GB"
                                      |            },
                                      |            "businessContactDetails":
                                      |            {
                                      |                "phoneNumber": "01332752856",
                                      |                "mobileNumber": "07782565326",
                                      |                "faxNumber": "01332754256",
                                      |                "emailAddress": "stephen@manncorpone.co.uk"
                                      |            },
                                      |            "tradingStartDate": "2001-01-01",
                                      |            "cashOrAccruals": "cash",
                                      |            "seasonal": true,
                                      |            "cessationDate": "2001-01-01",
                                      |            "cessationReason": "002",
                                      |            "paperLess": true
                                      |        }
                                      |    ]
                                      |}
                                    """.stripMargin
}
