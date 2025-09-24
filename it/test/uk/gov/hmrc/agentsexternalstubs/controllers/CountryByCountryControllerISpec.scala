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

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsArray, JsBoolean, JsObject}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.LocalDateTime
import java.util.UUID

class CountryByCountryControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val cbcId = "XACBC1234567890"
  val acknowledgementReference: String = UUID.randomUUID().toString.replace("-", "")

  "CountryByCountryController" when {

    "POST /dac6/dct50d/v1 (display cbc subscription) " should {
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
          s"/dac6/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCBCRequest(
              CbCRequestCommon("CBC", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = cbcId)
            )
          )
        )

        result should haveStatus(200)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCBCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCBCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val responseDetail = (result.json \ "displaySubscriptionForCBCResponse" \ "responseDetail").as[JsObject]
        responseDetail should haveProperty[String]("subscriptionID", be(cbcId))
        responseDetail should haveProperty[JsBoolean]("isGBUser")
        val primaryContact = (responseDetail \ "primaryContact").as[JsArray]
        primaryContact.head.as[JsObject] should haveProperty[String]("email", be(email))
      }

      "respond NOT_FOUND with error response if no record" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = post[DisplaySubscriptionForCbCRequestPayload](
          s"/dac6/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCBCRequest(
              CbCRequestCommon("CBC", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = cbcId)
            )
          )
        )

        result should haveStatus(404)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCBCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCBCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("NOT_OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val errorDetail = (result.json \ "displaySubscriptionForCBCResponse" \ "errorDetail").as[JsObject]
        errorDetail should haveProperty[String]("errorCode", be("404"))
        errorDetail should haveProperty[String]("errorMessage", be("Record not found"))
      }

      "respond BAD_REQUEST with error response if invalid payload" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = post[DisplaySubscriptionForCbCRequestPayload](
          s"/dac6/dct50d/v1",
          DisplaySubscriptionForCbCRequestPayload(
            DisplaySubscriptionForCBCRequest(
              CbCRequestCommon("bad payload", None, LocalDateTime.now(), acknowledgementReference),
              CbCRequestDetail(IDNumber = "foo")
            )
          )
        )

        result should haveStatus(400)
        val json = result.json
        json.as[JsObject] should haveProperty[JsObject]("displaySubscriptionForCBCResponse")
        val responseCommon = (result.json \ "displaySubscriptionForCBCResponse" \ "responseCommon").as[JsObject]
        responseCommon should haveProperty[String]("status", be("NOT_OK"))
        responseCommon should haveProperty[LocalDateTime]("processingDate")
        val errorDetail = (result.json \ "displaySubscriptionForCBCResponse" \ "errorDetail").as[JsObject]
        errorDetail should haveProperty[String]("errorCode", be("400"))
        errorDetail should haveProperty[String]("errorMessage", be("Invalid JSON document"))
      }

    }

  }
}
