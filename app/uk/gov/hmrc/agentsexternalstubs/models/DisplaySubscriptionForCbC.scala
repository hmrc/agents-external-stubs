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

import play.api.libs.json.{Json, OFormat}

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
  conversationID: Option[String] = Some("d3937a26-a4ec-4f11-bd8d-a93fc0265701"),
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
