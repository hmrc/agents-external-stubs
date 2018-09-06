package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class RecordsControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with ExampleDesPayloads {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "RecordsController" when {

    "GET /agents-external-stubs/records" should {
      "respond 200 with a list of records" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val createResult1 = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult1 should haveStatus(201)
        val createResult2 = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult2 should haveStatus(201)
        val createResult3 = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createResult3 should haveStatus(201)
        val createResult4 = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createResult4 should haveStatus(201)

        val result = Records.getRecords()
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[JsArray]("VatCustomerInformationRecord") and
            haveProperty[JsArray]("BusinessDetailsRecord") and
            haveProperty[JsArray]("LegacyRelationshipRecord") and
            haveProperty[JsArray]("LegacyAgentRecord")
        )
      }
    }

    "GET /agenqts-external-stubs/records/:recordId" should {
      "respond 200 with a record" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val createResult1 = Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload))
        createResult1 should haveStatus(201)
        val createResult2 = Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload))
        createResult2 should haveStatus(201)
        val createResult3 = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createResult3 should haveStatus(201)
        val createResult4 = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createResult4 should haveStatus(201)

        println(createResult1.json.toString())
        //val result = Records.getRecord()

      }
    }

    "GET /agents-external-stubs/records/business-details/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateBusinessDetails("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("nino") and haveProperty[String]("mtdbsa") and notHaveProperty(
            "propertyIncome") and notHaveProperty("businessData") and notHaveProperty("propertyData")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateBusinessDetails("bar", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[String]("nino") and haveProperty[String]("mtdbsa") and haveProperty[
            Boolean]("propertyIncome") and haveProperty[JsArray]("businessData") and haveProperty[JsObject](
            "propertyData")
        )
      }
    }

    "GET /agents-external-stubs/records/vat-customer-information/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateVatCustomerInformation("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("vrn")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateVatCustomerInformation("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("vrn") and haveProperty[JsObject]("approvedInformation") and haveProperty[JsObject](
            "inFlightInformation")
        )
      }
    }

    "GET /agents-external-stubs/records/legacy-agent/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateLegacyAgent("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[String]("address1") and haveProperty[
            String]("address2") and haveProperty[Boolean]("isAgentAbroad")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateLegacyAgent("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("agentName") and haveProperty[String]("address1") and haveProperty[
            String]("address2") and haveProperty[Boolean]("isAgentAbroad") and haveProperty[String]("agentPhoneNo") and haveProperty[
            String]("postcode")
        )
      }
    }

    "GET /agents-external-stubs/records/legacy-relationship/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateLegacyRelationship("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateLegacyRelationship("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("agentId") and haveProperty[String]("nino") and haveProperty[String]("utr")
        )
      }
    }

    "GET /agents-external-stubs/records/agent-record/generate" should {
      "respond 200 with a minimal auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateAgentRecord("foo", minimal = true)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[Boolean]("businessPartnerExists") and haveProperty[Boolean](
            "isAnAgent") and haveProperty[Boolean]("isAnASAgent") and haveProperty[Boolean]("isAnIndividual") and haveProperty[
            Boolean]("isAnOrganisation") and haveProperty[JsObject]("addressDetails")
        )
      }

      "respond 200 with a complete auto-generated entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = Records.generateAgentRecord("foo", minimal = false)
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("safeId") and haveProperty[Boolean]("businessPartnerExists") and haveProperty[Boolean](
            "isAnAgent") and haveProperty[Boolean]("isAnASAgent") and haveProperty[Boolean]("isAnIndividual") and haveProperty[
            Boolean]("isAnOrganisation") and haveProperty[JsObject]("addressDetails") and haveProperty[String](
            "agentReferenceNumber") and haveProperty[String]("utr")
        )
      }
    }
  }
}
