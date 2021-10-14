package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, BusinessDetailsRecord, Record}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class RecordsControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with ExampleDesPayloads {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "RecordsController" when {

    "GET /agents-external-stubs/records" should {
      "respond 200 with a list of records" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult1 = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult1 should haveStatus(201)
        val createResult2 = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult2 should haveStatus(201)
        val createResult3 = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createResult3 should haveStatus(201)
        val createResult4 = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createResult4 should haveStatus(201)
        val createResult5 = Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload))
        createResult5 should haveStatus(201)
        val createResult6 = Records.createRelationship(Json.parse(validRelationshipPayload))
        createResult6 should haveStatus(201)
        val createResult7 = Records.createPPTSubscriptionDisplayRecord(Json.parse(validPPTSubscriptionDisplayPayload))
        createResult7 should haveStatus(201)

        val result = Records.getRecords()
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[JsArray]("VatCustomerInformationRecord") and
            haveProperty[JsArray]("BusinessDetailsRecord") and
            haveProperty[JsArray]("LegacyRelationshipRecord") and
            haveProperty[JsArray]("LegacyAgentRecord") and
            haveProperty[JsArray]("BusinessPartnerRecord") and
            haveProperty[JsArray]("RelationshipRecord") and
            haveProperty[JsArray]("PPTSubscriptionDisplayRecord")
        )
      }
    }

    "GET /agents-external-stubs/records/:recordId" should {
      "respond 200 with a record" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult1 = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult1 should haveStatus(201)
        val createResult2 = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult2 should haveStatus(201)
        val createResult3 = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createResult3 should haveStatus(201)
        val createResult4 = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createResult4 should haveStatus(201)

        val result1 = get(createResult1.json.as[Links].rel("self").getOrElse(fail()))
        result1 should haveStatus(200)
        result1.json.asOpt[BusinessDetailsRecord] shouldBe defined
        val result2 = get(createResult2.json.as[Links].rel("self").getOrElse(fail()))
        result2 should haveStatus(200)
        val result3 = get(createResult3.json.as[Links].rel("self").getOrElse(fail()))
        result3 should haveStatus(200)
        val result4 = get(createResult4.json.as[Links].rel("self").getOrElse(fail()))
        result4 should haveStatus(200)
      }
    }

    "PUT /agents-external-stubs/records/:recordId" should {
      "update a record and return 202" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val createResult1 = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult1 should haveStatus(201)
        val getResult1 = get(createResult1.json.as[Links].rel("self").getOrElse(fail()))
        getResult1 should haveStatus(200)

        val record = getResult1.json
          .as[BusinessDetailsRecord]
          .modifyMtdbsa { case s => s.reverse }

        val result = Records.updateRecord(record.id.get, Record.toJson(record))
        result should haveStatus(202)

        val getResult2 = get(result.header(HeaderNames.LOCATION).get)
        getResult2 should haveStatus(200)
        getResult2.json.as[BusinessDetailsRecord].mtdbsa shouldBe "123456789012345".reverse
      }
    }

    "GET /agents-external-stubs/records/business-details/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateBusinessDetails("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("nino") and haveProperty[String](
            "mtdbsa"
          ) and haveProperty[Boolean]("propertyIncome") and notHaveProperty("businessData") and notHaveProperty(
            "propertyData"
          )
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateBusinessDetails("bar", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("nino") and haveProperty[String](
            "mtdbsa"
          ) and haveProperty[Boolean]("propertyIncome") and haveProperty[JsArray]("businessData") and haveProperty[
            JsObject
          ]("propertyData")
        )
      }
    }

    "GET /agents-external-stubs/records/vat-customer-information/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateVatCustomerInformation("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("vrn")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateVatCustomerInformation("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("vrn") and haveProperty[JsObject]("approvedInformation")
        )
      }
    }

    "GET /agents-external-stubs/records/legacy-agent/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateLegacyAgent("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[String](
            "address1"
          ) and haveProperty[String]("address2") and haveProperty[Boolean]("isAgentAbroad")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateLegacyAgent("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[String](
            "address1"
          ) and haveProperty[String]("address2") and haveProperty[Boolean]("isAgentAbroad") and haveProperty[String](
            "agentPhoneNo"
          ) and haveProperty[String]("postcode")
        )
      }
    }

    "GET /agents-external-stubs/records/legacy-relationship/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateLegacyRelationship("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateLegacyRelationship("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("nino") and haveProperty[String]("utr")
        )
      }
    }

    "GET /agents-external-stubs/records/business-partner-record/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateBusinessPartnerRecord("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[Boolean]("businessPartnerExists") and haveProperty[Boolean](
            "isAnAgent"
          ) and haveProperty[Boolean]("isAnASAgent") and haveProperty[Boolean]("isAnIndividual") and haveProperty[
            Boolean
          ]("isAnOrganisation") and haveProperty[JsObject]("addressDetails")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generateBusinessPartnerRecord("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[Boolean]("businessPartnerExists") and haveProperty[Boolean](
            "isAnAgent"
          ) and haveProperty[Boolean]("isAnASAgent") and haveProperty[Boolean]("isAnIndividual") and haveProperty[
            Boolean
          ]("isAnOrganisation") and haveProperty[JsObject]("addressDetails") and haveProperty[String](
            "agentReferenceNumber"
          ) and haveProperty[String]("utr")
        )
      }
    }

    "GET /agents-external-stubs/records/ppt-registration/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generatePPTSubscriptionDisplayRecord("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("pptReference")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = Records.generatePPTSubscriptionDisplayRecord("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("pptReference")
        )
      }
    }
  }
}
