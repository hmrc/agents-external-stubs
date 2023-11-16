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
import uk.gov.hmrc.agentsexternalstubs.models.Validator.{Validator, check, checkIfOnlyOneSetIsDefined, checkProperty}

import java.time.LocalDateTime

//  JSON sample for request to EIS
//  { "displaySubscriptionForCBCRequest":{
//      "requestCommon": {
//      "regime": "CBC",
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
//      "IDType": "CBC",
//      "IDNumber": "YUDD789429"
//    } }
//  }

case class CbCRequestParams(paramName: String, paramValue: String)

object CbCRequestParams {
  implicit val format: OFormat[CbCRequestParams] = Json.format[CbCRequestParams]
}

case class CbCRequestCommon(
  regime: String = "CBC",
  conversationID: Option[String] = None,
  receiptDate: LocalDateTime,
  acknowledgementReference: String = "8493893huer3ruihuow",
  originatingSystem: String = "MDTP",
  requestParameters: Option[Array[CbCRequestParams]] = None
)

object CbCRequestCommon {
  implicit val format: OFormat[CbCRequestCommon] = Json.format[CbCRequestCommon]

  val cbcRegimeValidator: Validator[String] = check(_.equals("CBC"), s"invalid regime, only CbC supported")
  val cbcOriginSystemValidator: Validator[String] = check(_.equals("MDTP"), s"invalid origin, only MDTP supported")

  val validate: Validator[CbCRequestCommon] = Validator(
    checkProperty(_.regime, cbcRegimeValidator),
    checkProperty(_.originatingSystem, cbcOriginSystemValidator)
  )

}

case class CbCRequestDetail(
  IDType: String = "CBC",
  IDNumber: String
)

object CbCRequestDetail {
  implicit val format: OFormat[CbCRequestDetail] = Json.format[CbCRequestDetail]

  val idTypeValidator: Validator[String] = check(_.equals("CBC"), s"invalid id type")

  val validate: Validator[CbCRequestDetail] = Validator(
    checkProperty(_.IDNumber, check(CbcId.isValid, s"invalid cbcId")),
    checkProperty(_.IDType, idTypeValidator)
  )
}

case class DisplaySubscriptionForCBCRequest(requestCommon: CbCRequestCommon, requestDetail: CbCRequestDetail)

object DisplaySubscriptionForCBCRequest {
  implicit val format: OFormat[DisplaySubscriptionForCBCRequest] = Json.format[DisplaySubscriptionForCBCRequest]
}

/** Request payload originating from MTDP to display a Country by Country subscription */
case class DisplaySubscriptionForCbCRequestPayload(displaySubscriptionForCBCRequest: DisplaySubscriptionForCBCRequest)

object DisplaySubscriptionForCbCRequestPayload {
  implicit val format: OFormat[DisplaySubscriptionForCbCRequestPayload] =
    Json.format[DisplaySubscriptionForCbCRequestPayload]

  val validate: Validator[DisplaySubscriptionForCbCRequestPayload] = Validator(
    checkProperty(_.displaySubscriptionForCBCRequest.requestDetail, CbCRequestDetail.validate),
    checkProperty(_.displaySubscriptionForCBCRequest.requestCommon, CbCRequestCommon.validate)
  )
}

// ************************************ //

case class IndividualContact(
  firstName: String,
  lastName: String,
  middleName: Option[String]
)

object IndividualContact {
  implicit val format: OFormat[IndividualContact] = Json.format[IndividualContact]

  val gen: Gen[IndividualContact] = for {
    first  <- Generator.forename()
    last   <- Generator.surname
    middle <- Generator.optionGen(Generator.forename())
  } yield IndividualContact(first, last, middle)
}

case class OrganisationContact(organisationName: String)

object OrganisationContact {
  implicit val format: OFormat[OrganisationContact] = Json.format[OrganisationContact]

  val gen: Gen[OrganisationContact] = for {
    name <- Generator.tradingNameGen
  } yield OrganisationContact(name)
}

/** Must contain either individual or organisation contact */
case class CbcContactInformation(
  email: String,
  phone: Option[String],
  mobile: Option[String],
  individual: Option[IndividualContact],
  organisation: Option[OrganisationContact]
)

object CbcContactInformation {
  implicit val format: OFormat[CbcContactInformation] = Json.format[CbcContactInformation]

  val validate: Validator[CbcContactInformation] = Validator(
    checkIfOnlyOneSetIsDefined(
      Seq(Set(_.individual), Set(_.organisation)),
      "[{individual},{organisation}]"
    )
  )

  val gen: Gen[CbcContactInformation] = for {
    email   <- Generator.emailGen
    phone   <- Generator.biasedOptionGen(Generator.ukPhoneNumber)
    mobile  <- Generator.biasedOptionGen(Generator.ukPhoneNumber)
    contact <- Gen.oneOf(IndividualContact.gen, OrganisationContact.gen)
  } yield contact match {
    case individualC: IndividualContact =>
      CbcContactInformation(
        email = email,
        phone = phone,
        mobile = mobile,
        individual = Some(individualC),
        organisation = None
      )
    case orgC: OrganisationContact =>
      CbcContactInformation(email = email, phone = phone, mobile = mobile, individual = None, organisation = Some(orgC))
  }

}

case class CbCReturnParameters(paramName: String, paramValue: String)

object CbCReturnParameters {
  implicit val format: OFormat[CbCReturnParameters] = Json.format[CbCReturnParameters]
}

case class CbCResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: LocalDateTime,
  returnParameters: Option[Seq[CbCReturnParameters]]
)

object CbCResponseCommon {
  implicit val format: OFormat[CbCResponseCommon] = Json.format[CbCResponseCommon]
}

case class CbCResponseDetail(
  subscriptionID: String,
  tradingName: Option[String],
  isGBUser: Boolean,
  primaryContact: Seq[CbcContactInformation],
  secondaryContact: Seq[CbcContactInformation]
)

object CbCResponseDetail {
  implicit val format: OFormat[CbCResponseDetail] = Json.format[CbCResponseDetail]
}

case class DisplaySubscriptionForCBCResponse(responseCommon: CbCResponseCommon, responseDetail: CbCResponseDetail)

object DisplaySubscriptionForCBCResponse {
  implicit val format: OFormat[DisplaySubscriptionForCBCResponse] = Json.format[DisplaySubscriptionForCBCResponse]

  def fromRecord(record: CbcSubscriptionRecord): DisplaySubscriptionForCBCResponse =
    DisplaySubscriptionForCBCResponse(
      CbCResponseCommon("OK", None, LocalDateTime.now(), None),
      CbCResponseDetail(
        record.cbcId,
        record.tradingName,
        record.isGBUser,
        record.primaryContact,
        record.secondaryContact
      )
    )

}

/** Happy response from EIS/ETMP for Country by Country subscription */
case class DisplaySubscriptionForCbC(displaySubscriptionForCBCResponse: DisplaySubscriptionForCBCResponse)

object DisplaySubscriptionForCbC {
  implicit val format: OFormat[DisplaySubscriptionForCbC] = Json.format[DisplaySubscriptionForCbC]

  def fromRecord(record: CbcSubscriptionRecord): DisplaySubscriptionForCbC =
    DisplaySubscriptionForCbC(
      DisplaySubscriptionForCBCResponse.fromRecord(record)
    )
}

// JSON sample for error
//  {
//  "errorDetail": {
//    "timestamp": "2016-10-10T13:52:16Z",
//    "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
//    "errorCode": "409",
//    "errorMessage": "Duplicate submission",
//    "source": "Back End",
//    "sourceFaultDetail": {
//      "detail": [
//       "Duplicate submission"
//     ]
//    }
//  }}

case class CbCSourceFaultDetail(detail: Array[String])

object CbCSourceFaultDetail { implicit val format: OFormat[CbCSourceFaultDetail] = Json.format[CbCSourceFaultDetail] }

case class CbCErrorDetail(
  timestamp: LocalDateTime,
  correlationId: String = "d60de98c-f499-47f5-b2d6-e80966e8d19e",
  errorCode: String,
  errorMessage: String,
  source: String,
  sourceFaultDetail: CbCSourceFaultDetail
)

object CbCErrorDetail { implicit val format: OFormat[CbCErrorDetail] = Json.format[CbCErrorDetail] }

/** Error response from EIS/ETMP for Country by Country subscription */
case class DisplaySubscriptionForCbCError(errorDetail: CbCErrorDetail)

object DisplaySubscriptionForCbCError {
  implicit val format: OFormat[DisplaySubscriptionForCbCError] = Json.format[DisplaySubscriptionForCbCError]
}

//******************************//

// For Generating a CbCSubscriptionRecord
// Outputs UK or nonUK, with or without trading name - contacts mandatory
case class CbcSubscriptionRecord(
  id: Option[String] = None,
  cbcId: String,
  tradingName: Option[String],
  isGBUser: Boolean,
  primaryContact: Seq[CbcContactInformation],
  secondaryContact: Seq[CbcContactInformation]
) extends Record {

  override def uniqueKey: Option[String] = Option(cbcId).map(CbcSubscriptionRecord.uniqueKey)
  override def lookupKeys: Seq[String] =
    Seq(Option(cbcId).map(CbcSubscriptionRecord.cbcIdKey)).collect { case Some(x) => x }
  override def withId(id: Option[String]): CbcSubscriptionRecord = copy(id = id)

  def withCbcId(cbcId: String): CbcSubscriptionRecord = copy(cbcId = cbcId)
  def withIsUK(isUK: Boolean): CbcSubscriptionRecord = copy(isGBUser = isUK)
  def withEmail(email: String): CbcSubscriptionRecord =
    copy(primaryContact = List(primaryContact.head.copy(email = email)))

}

object CbcSubscriptionRecord extends RecordUtils[CbcSubscriptionRecord] {
  implicit val recordType: RecordMetaData[CbcSubscriptionRecord] = RecordMetaData[CbcSubscriptionRecord]

  def uniqueKey(key: String): String = s"""cbcId:${key.toUpperCase}"""
  def cbcIdKey(key: String): String = s"""cbcId:${key.toUpperCase}"""

  override val gen: Gen[CbcSubscriptionRecord] = for {
    cbcId            <- Generator.cbcIdGen
    tradingName      <- Generator.biasedOptionGen(Generator.tradingNameGen)
    isGBUser         <- Generator.booleanGen
    primaryContact   <- CbcContactInformation.gen
    secondaryContact <- CbcContactInformation.gen
  } yield CbcSubscriptionRecord(
    cbcId = cbcId,
    tradingName = tradingName,
    isGBUser = isGBUser,
    primaryContact = List(primaryContact),
    secondaryContact = List(secondaryContact)
  )

  def generateWith(cbcId: String, isUK: Boolean): CbcSubscriptionRecord =
    CbcSubscriptionRecord
      .generate(cbcId + isUK.toString) // create the generator seed out of the combination of cbc id and uk status
      .withCbcId(cbcId)
      .withIsUK(isUK)

  override val validate: Validator[CbcSubscriptionRecord] = Validator(
    checkProperty(_.cbcId, check(CbcId.isValid, s"invalid cbcId")),
    checkProperty(_.primaryContact.head, CbcContactInformation.validate),
    checkProperty(_.secondaryContact.head, CbcContactInformation.validate)
  )
  override val sanitizers = Seq()

  implicit val formats: Format[CbcSubscriptionRecord] = Json.format[CbcSubscriptionRecord]

}
