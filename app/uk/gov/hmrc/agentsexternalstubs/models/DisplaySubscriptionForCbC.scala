/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.Gen
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentsexternalstubs.models
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

import java.time.LocalDateTime

//  JSON sample for request to EIS
//  { "displaySubscriptionForCbCRequest":{
//      "requestCommon": {
//      "regime": "CbC",
//      "conversationID": "d3937a26-a4ec-4f11-bd8d-a93fc0265701",
//      "receiptDate": "2020-09-15T09:38:00Z",
//      "acknowledgementReference": "8493893huer3ruihuow",
//      "originatingSystem": "MDTP",
//      "requestParameters": [
//        {
//          "paramName": "param name",
//          "paramValue": "param value"
//        }
//      ]
//    },
//    "requestDetail": {
//      "IDType": "CbC",
//      "IDNumber": "YUDD789429"
//    } }
//  }

case class CbCRequestCommon(
  regime: String = "CbC",
  conversationID: String = "d3937a26-a4ec-4f11-bd8d-a93fc0265701",
  receiptDate: LocalDateTime,
  acknowledgementReference: String = "8493893huer3ruihuow",
  originatingSystem: String = "MDTP",
  requestParameters: Option[Map[String, String]] = None // TODO fix error.expected.jsobject
)

object CbCRequestCommon {
  implicit val format: OFormat[CbCRequestCommon] = Json.format[CbCRequestCommon]
}

case class CbCRequestDetail(
  IDType: String = "CbC",
  IDNumber: String = "XACBC0123456789"
)

object CbCRequestDetail {
  implicit val format: OFormat[CbCRequestDetail] = Json.format[CbCRequestDetail]
}

case class DisplaySubscriptionForCbCRequest(requestCommon: CbCRequestCommon, requestDetail: CbCRequestDetail)

object DisplaySubscriptionForCbCRequest {
  implicit val format: OFormat[DisplaySubscriptionForCbCRequest] = Json.format[DisplaySubscriptionForCbCRequest]
}

/** Request payload originating from MTDP to display a Country by Country subscription */
case class DisplaySubscriptionForCbCRequestPayload(displaySubscriptionForCbCRequest: DisplaySubscriptionForCbCRequest)

object DisplaySubscriptionForCbCRequestPayload {
  implicit val format: OFormat[DisplaySubscriptionForCbCRequestPayload] =
    Json.format[DisplaySubscriptionForCbCRequestPayload]
}

// ************************************ //

//   JSON sample response
//  {
//    "displaySubscriptionForCbCResponse": {
//      "responseCommon": {
//        "status": "OK",
//        "processingDate": "2020-08-09T11:23:45Z"
//      },
//      "responseDetail": {
//        "subscriptionID": "yu789428932",
//        "tradingName": "Tools for Traders",
//        "isGBUser": true,
//        "primaryContact": [
//          {
//            "email": "Tim@toolsfortraders.com",
//            "phone": "078803423883",
//            "mobile": "078803423883",
//            "individual": {
//            "lastName": "Taylor",
//            "firstName": "Tim"}
//          }
//        ],
//        "secondaryContact": [
//          {
//            "email": "contact@toolsfortraders.com",
//            "organisation": {"organisationName": "Tools for Traders Limited"}
//          }
//        ]
//      }
//    }
//  }
case class IndividualContact(
  firstName: String,
  lastName: String,
  middleName: Option[String]
)

object IndividualContact {
  implicit val format: OFormat[IndividualContact] = Json.format[IndividualContact]
}

case class OrganisationContact(organisationName: String)

object OrganisationContact {
  implicit val format: OFormat[OrganisationContact] = Json.format[OrganisationContact]
}

case class CbcContactInformation(
  email: String,
  phone: Option[String],
  mobile: Option[String],
  individual: Option[IndividualContact],
  organisation: Option[OrganisationContact]
)

object CbcContactInformation {
  implicit val format: OFormat[CbcContactInformation] = Json.format[CbcContactInformation]
}

case class CbCResponseCommon(
  status: String,
  processingDate: LocalDateTime
)

object CbCResponseCommon {
  implicit val format: OFormat[CbCResponseCommon] = Json.format[CbCResponseCommon]
}

case class CbCResponseDetail(
  cbcId: String,
  tradingName: Option[String],
  isGBUser: Boolean,
  primaryContact: CbcContactInformation,
  secondaryContact: CbcContactInformation
)

object CbCResponseDetail {
  implicit val format: OFormat[CbCResponseDetail] = Json.format[CbCResponseDetail]
}

case class DisplaySubscriptionForCbCResponse(responseCommon: CbCResponseCommon, responseDetails: CbCResponseDetail)

object DisplaySubscriptionForCbCResponse {
  implicit val format: OFormat[DisplaySubscriptionForCbCResponse] = Json.format[DisplaySubscriptionForCbCResponse]
}

/** Response from EIS/ETMP for Country by Country subscription */
case class DisplaySubscriptionForCbC(displaySubscriptionForCbCResponse: DisplaySubscriptionForCbCResponse)

object DisplaySubscriptionForCbC {
  implicit val format: OFormat[DisplaySubscriptionForCbC] = Json.format[DisplaySubscriptionForCbC]
}

// TODO: Generate a DisplaySubscriptionForCbCRecord
//
// Seed
//  cbcId: CbcId = "YUDD789429",
//  ?? requestParameters: Option[Map[String, String]]
//
// Outputs Uk or nonUK, with or without trading name - but contacts seem mandatory?

//case class CbcSubscriptionDisplayRecord(
//                                         id: Option[String] = None,
//                                         cbcId: String,
//                                         tradingName: Option[String],
//                                         isGBUser: Boolean
//                                       ) extends Record {
//
//  override def uniqueKey: Option[String] = Option(cbcId).map(CbcSubscriptionDisplayRecord.uniqueKey)
//  override def lookupKeys: Seq[String] =
//    Seq(Option(cbcId).map(CbcSubscriptionDisplayRecord.cbcIdKey)).collect { case Some(x) => x }
//  override def withId(id: Option[String]): CbcSubscriptionDisplayRecord = copy(id = id)
//
//  def withCbcId(cbcId: String): CbcSubscriptionDisplayRecord = copy(cbcId = cbcId)
//
//}
//
//object CbcSubscriptionDisplayRecord extends RecordUtils[CbcSubscriptionDisplayRecord] {
//  def uniqueKey(key: String): String = s"""cbcId:${key.toUpperCase}"""
//  def CbcIdKey(key: String): String = s"""cbcId:${key.toUpperCase}"""
//
//  override val gen: Gen[CbcSubscriptionDisplayRecord] = _
//  override val validate: Validator[CbcSubscriptionDisplayRecord] = _
//  override val sanitizers: Seq[models.CbcSubscriptionDisplayRecord.Update] = _
//}
