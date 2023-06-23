package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsBoolean, JsObject}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, CbCRequestCommon, CbCRequestDetail,  CbcSubscriptionRecord, DisplaySubscriptionForCbCRequest, DisplaySubscriptionForCbCRequestPayload}
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.LocalDateTime
import java.util.UUID

class CountryByCountryControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val cbcId = "XACBC1234567890"
  val acknowledgementReference: String = UUID.randomUUID().toString.replace("-", "")

  "CountryByCountryController" when {

    "POST /dac/dct50d/v1 (display cbc subscription) " should {
      "respond 200 with country by country subscription details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val email = "test@foo.uk"

        //gen a record
        val createRecord = Records.createCbcSubscriptionRecord(
          CbcSubscriptionRecord
            .seed("foo")
            .withCbcId(cbcId)
            .withEmail(email)
        )
        createRecord should haveStatus(201)

        val result = post[DisplaySubscriptionForCbCRequestPayload](
          s"/dac/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCbCRequest(
              CbCRequestCommon("CbC", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = cbcId)
            )
          )

        )

        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCbCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCbCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val responseDetail = (result.json \ "displaySubscriptionForCbCResponse" \ "responseDetail").as[JsObject]
        responseDetail should haveProperty[String]("cbcId", be(cbcId))
        responseDetail should haveProperty[JsBoolean]("isGBUser")
        val primaryContact = (responseDetail \ "primaryContact").as[JsObject]
        primaryContact should haveProperty[String]("email", be(email))
      }

      "respond NOT_FOUND with error response if no record" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = post[DisplaySubscriptionForCbCRequestPayload](
          s"/dac/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCbCRequest(
              CbCRequestCommon("CbC", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = cbcId)
            )
          )
        )

        result should haveStatus(404)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCbCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCbCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("NOT_OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val errorDetail = (result.json \ "displaySubscriptionForCbCResponse" \ "errorDetail").as[JsObject]
        errorDetail should haveProperty[String]("errorCode", be("404"))
        errorDetail should haveProperty[String]("errorMessage", be("Record not found"))
      }

      "respond BAD_REQUEST with error response if invalid payload" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = post[DisplaySubscriptionForCbCRequestPayload](
          s"/dac/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCbCRequest(
              CbCRequestCommon("bad payload", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = "foo")
            )
          )
        )

        result should haveStatus(400)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCbCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCbCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("NOT_OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val errorDetail = (result.json \ "displaySubscriptionForCbCResponse" \ "errorDetail").as[JsObject]
        errorDetail should haveProperty[String]("errorCode", be("400"))
        errorDetail should haveProperty[String]("errorMessage", be("Invalid JSON document"))
      }

    }

  }
}
