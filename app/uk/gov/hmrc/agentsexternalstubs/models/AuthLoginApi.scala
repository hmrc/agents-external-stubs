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
import play.api.libs.json.{Format, Json, Reads}

object AuthLoginApi {

  case class Request(
    credId: String,
    affinityGroup: String,
    confidenceLevel: Option[Int] = None,
    credentialStrength: String,
    credentialRole: Option[String] = None,
    enrolments: Seq[Enrolment] = Seq.empty,
    delegatedEnrolments: Option[Seq[Request.DelegatedEnrolment]] = None,
    gatewayToken: Option[String] = None,
    groupIdentifier: Option[String] = None,
    nino: Option[String] = None,
    usersName: Option[String] = None,
    email: Option[String] = None,
    description: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentCode: Option[String] = None,
    agentId: Option[String] = None,
    itmpData: Option[Request.ItmpData] = None,
    gatewayInformation: Option[Request.GatewayInformation] = None,
    mdtpInformation: Option[Request.MdtpInformation] = None,
    unreadMessageCount: Option[Int] = None
  )

  object Request {

    def fromUserAndGroup(user: User, maybeGroup: Option[Group]): Request =
      Request(
        credId = s"${user.userId}${user.planetId.map(planet => s"@$planet").getOrElse("")}",
        affinityGroup = maybeGroup.fold(AG.Individual)(_.affinityGroup),
        confidenceLevel = user.confidenceLevel,
        credentialStrength = user.credentialStrength.getOrElse("strong"),
        credentialRole = user.credentialRole,
        enrolments = maybeGroup.fold(Seq.empty[Enrolment])(group => group.principalEnrolments),
        delegatedEnrolments = maybeGroup.flatMap(group => DelegatedEnrolment.from(group)),
        gatewayToken = None,
        groupIdentifier = user.groupId,
        nino = user.nino.map(_.value),
        usersName = user.name,
        email = None,
        description = None,
        agentFriendlyName = maybeGroup.flatMap(_.agentFriendlyName),
        agentCode = maybeGroup.flatMap(_.agentCode),
        agentId = maybeGroup.flatMap(_.agentId),
        itmpData = None,
        gatewayInformation = None,
        mdtpInformation = None,
        unreadMessageCount = Some(2)
      )

    case class DelegatedEnrolment(key: String, identifiers: Seq[Identifier], delegatedAuthRule: String)

    object DelegatedEnrolment {

      def from(group: Group): Option[Seq[DelegatedEnrolment]] =
        if (group.delegatedEnrolments.isEmpty)
          None
        else {
          val delegatedEnrolments =
            group.delegatedEnrolments
              .map(e =>
                delegatedAuthRuleFor(e.key).map(rule =>
                  DelegatedEnrolment(e.key, e.identifiers.getOrElse(Seq.empty), rule)
                )
              )
              .collect { case Some(e) => e }
          if (delegatedEnrolments.isEmpty)
            None
          else Some(delegatedEnrolments)
        }

      def delegatedAuthRuleFor(enrolmentKey: String): Option[String] =
        enrolmentKey match {
          case "IR-SA"         => Some("sa-auth")
          case "IR-PAYE"       => Some("epaye-auth")
          case "HMRC-MTD-IT"   => Some("mtd-it-auth")
          case "HMRC-ATED-ORG" => Some("ated-auth")
          case "HMRC-NI"       => Some("afi-auth")
          case "HMRC-MTD-VAT"  => Some("mtd-vat-auth")
          case "HMRC-TERS-ORG" => Some("trust-auth")
          case "HMRC-CGT-PD"   => Some("cgt-auth")
          case _               => None
        }

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
      countryCode: Option[String] = None
    )

    object ItmpAddress {
      implicit val formats = Json.format[ItmpAddress]
    }

    case class ItmpData(
      givenName: Option[String] = None,
      middleName: Option[String] = None,
      familyName: Option[String] = None,
      birthdate: Option[String] = None,
      address: Option[ItmpAddress] = None
    )

    object ItmpData {
      implicit val formats = Json.format[ItmpData]
    }

    implicit val formats: Format[Request] = Json.format[Request]

  }

  case class Response(authToken: String, sessionAuthorityUri: String)

  object Response {
    implicit val reads: Reads[Response] = Json.reads[Response]
  }
}
