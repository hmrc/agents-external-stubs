/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json, OFormat, OWrites, Reads}

case class HipAgentSubscriptionResponse(
  success: AgentSubscriptionDisplayResponse
)

object HipAgentSubscriptionResponse {
  implicit val format: OFormat[HipAgentSubscriptionResponse] = Json.format[HipAgentSubscriptionResponse]
}

case class AgentSubscriptionDisplayResponse(
  processingDate: String,
  utr: Option[String],
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: String,
  phone: Option[String],
  email: String,
  suspensionStatus: String,
  regime: Seq[String],
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: Option[AgencyDetailsStatus],
  amlSupervisionUpdateStatus: Option[AgencyDetailsStatus],
  directorPartnerUpdateStatus: Option[AgencyDetailsStatus],
  acceptNewTermsStatus: Option[AgencyDetailsStatus],
  reriskStatus: Option[AgencyDetailsStatus]
)

object AgentSubscriptionDisplayResponse {

  implicit val reads: Reads[AgentSubscriptionDisplayResponse] = Json.reads[AgentSubscriptionDisplayResponse]
  implicit val writes: OWrites[AgentSubscriptionDisplayResponse] = OWrites { s =>
    val base: JsObject =
      Json.obj(
        "processingDate"   -> s.processingDate,
        "name"             -> s.name,
        "addr1"            -> s.addr1,
        "country"          -> s.country,
        "email"            -> s.email,
        "suspensionStatus" -> s.suspensionStatus
      ) ++
        Json.obj(
          "utr"                     -> s.utr,
          "addr2"                   -> s.addr2,
          "addr3"                   -> s.addr3,
          "addr4"                   -> s.addr4,
          "postcode"                -> s.postcode,
          "phone"                   -> s.phone,
          "supervisoryBody"         -> s.supervisoryBody,
          "membershipNumber"        -> s.membershipNumber,
          "evidenceObjectReference" -> s.evidenceObjectReference
        ) ++
        Json.obj("regime" -> s.regime)

    base ++
      AgencyDetailsStatus.toResponseField("updateDetails", s.updateDetailsStatus) ++
      AgencyDetailsStatus.toResponseField("amlSupervisionUpdate", s.amlSupervisionUpdateStatus) ++
      AgencyDetailsStatus.toResponseField("directorPartnerUpdate", s.directorPartnerUpdateStatus) ++
      AgencyDetailsStatus.toResponseField("acceptNewTerms", s.acceptNewTermsStatus) ++
      AgencyDetailsStatus.toResponseField("rerisk", s.reriskStatus)
  }

  implicit val format: OFormat[AgentSubscriptionDisplayResponse] =
    OFormat[AgentSubscriptionDisplayResponse](reads, writes)
}
