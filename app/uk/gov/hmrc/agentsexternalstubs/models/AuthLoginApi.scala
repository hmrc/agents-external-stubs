/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.{Format, Json, Reads}

object AuthLoginApi {

  case class Request(
    credId: String,
    affinityGroup: String,
    confidenceLevel: Option[Int],
    credentialStrength: String,
    credentialRole: Option[String],
    enrolments: Seq[Enrolment],
    delegatedEnrolments: Option[Seq[Request.DelegatedEnrolment]],
    gatewayToken: Option[String],
    groupIdentifier: Option[String],
    nino: Option[String],
    usersName: Option[String],
    email: Option[String],
    description: Option[String],
    agentFriendlyName: Option[String],
    agentCode: Option[String],
    agentId: Option[String],
    itmpData: Option[Request.ItmpData],
    gatewayInformation: Option[Request.GatewayInformation],
    mdtpInformation: Option[Request.MdtpInformation],
    unreadMessageCount: Option[Int])

  object Request {

    case class DelegatedEnrolment(key: String, identifiers: Seq[Identifier], delegatedAuthRule: String)
    object DelegatedEnrolment {
      implicit val formats: Format[DelegatedEnrolment] = Json.format[DelegatedEnrolment]
    }

    case class MdtpInformation(deviceId: String, sessionId: String)
    object MdtpInformation {
      implicit val formats: Format[MdtpInformation] = Json.format[MdtpInformation]
    }

    case class GatewayInformation(gatewayToken: Option[String])
    object GatewayInformation {
      implicit val formats: Format[GatewayInformation] = Json.format[GatewayInformation]
    }

    case class ItmpAddress(
      line1: Option[String] = None,
      line2: Option[String] = None,
      line3: Option[String] = None,
      line4: Option[String] = None,
      line5: Option[String] = None,
      postCode: Option[String] = None,
      countryName: Option[String] = None,
      countryCode: Option[String] = None)

    object ItmpAddress {
      implicit val formats = Json.format[ItmpAddress]
    }

    case class ItmpData(
      givenName: Option[String] = None,
      middleName: Option[String] = None,
      familyName: Option[String] = None,
      birthdate: Option[String] = None,
      address: Option[ItmpAddress] = None)

    object ItmpData {
      implicit val formats = Json.format[ItmpData]
    }

    implicit val formats: Format[Request] = Json.format[Request]

  }

  case class Response(authToken: String)

  object Response {
    implicit val reads: Reads[Response] = Json.reads[Response]
  }
}
