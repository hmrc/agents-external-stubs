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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

final case class HipAgentSubscriptionResponse(
  success: AgentSubscriptionDisplayResponse
)

object HipAgentSubscriptionResponse {
  implicit val format: OFormat[HipAgentSubscriptionResponse] = Json.format[HipAgentSubscriptionResponse]
}

final case class AgentSubscriptionDisplayResponse(
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
  regime: Option[Seq[String]],
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: AgencyDetailsStatus,
  amlSupervisionUpdateStatus: AgencyDetailsStatus,
  directorPartnerUpdateStatus: AgencyDetailsStatus,
  acceptNewTermsStatus: AgencyDetailsStatus,
  reriskStatus: AgencyDetailsStatus
)

object AgentSubscriptionDisplayResponse {

  private final case class BasePart(
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
    regime: Option[Seq[String]],
    supervisoryBody: Option[String],
    membershipNumber: Option[String],
    evidenceObjectReference: Option[String]
  )

  private object BasePart {
    implicit val format: OFormat[BasePart] = Json.format[BasePart]
  }

  private final case class StatusPart(
    updateDetailsStatus: AgencyDetailsStatus,
    amlSupervisionUpdateStatus: AgencyDetailsStatus,
    directorPartnerUpdateStatus: AgencyDetailsStatus,
    acceptNewTermsStatus: AgencyDetailsStatus,
    reriskStatus: AgencyDetailsStatus
  )

  private object StatusPart {

    implicit val reads: Reads[StatusPart] =
      (
        AgencyDetailsStatus.fromResponseField("updateDetails") and
          AgencyDetailsStatus.fromResponseField("amlSupervisionUpdate") and
          AgencyDetailsStatus.fromResponseField("directorPartnerUpdate") and
          AgencyDetailsStatus.fromResponseField("acceptNewTerms") and
          AgencyDetailsStatus.fromResponseField("rerisk")
      )(StatusPart.apply _)

    implicit val writes: OWrites[StatusPart] = OWrites { s =>
      Json.obj() ++
        AgencyDetailsStatus.toResponseField("updateDetails", s.updateDetailsStatus) ++
        AgencyDetailsStatus.toResponseField("amlSupervisionUpdate", s.amlSupervisionUpdateStatus) ++
        AgencyDetailsStatus.toResponseField("directorPartnerUpdate", s.directorPartnerUpdateStatus) ++
        AgencyDetailsStatus.toResponseField("acceptNewTerms", s.acceptNewTermsStatus) ++
        AgencyDetailsStatus.toResponseField("rerisk", s.reriskStatus)
    }
  }

  implicit val reads: Reads[AgentSubscriptionDisplayResponse] =
    (implicitly[Reads[BasePart]] and implicitly[Reads[StatusPart]]).apply { (b, st) =>
      AgentSubscriptionDisplayResponse(
        processingDate = b.processingDate,
        utr = b.utr,
        name = b.name,
        addr1 = b.addr1,
        addr2 = b.addr2,
        addr3 = b.addr3,
        addr4 = b.addr4,
        postcode = b.postcode,
        country = b.country,
        phone = b.phone,
        email = b.email,
        suspensionStatus = b.suspensionStatus,
        regime = b.regime,
        supervisoryBody = b.supervisoryBody,
        membershipNumber = b.membershipNumber,
        evidenceObjectReference = b.evidenceObjectReference,
        updateDetailsStatus = st.updateDetailsStatus,
        amlSupervisionUpdateStatus = st.amlSupervisionUpdateStatus,
        directorPartnerUpdateStatus = st.directorPartnerUpdateStatus,
        acceptNewTermsStatus = st.acceptNewTermsStatus,
        reriskStatus = st.reriskStatus
      )
    }

  implicit val writes: OWrites[AgentSubscriptionDisplayResponse] = OWrites { r =>
    val base = BasePart(
      processingDate = r.processingDate,
      utr = r.utr,
      name = r.name,
      addr1 = r.addr1,
      addr2 = r.addr2,
      addr3 = r.addr3,
      addr4 = r.addr4,
      postcode = r.postcode,
      country = r.country,
      phone = r.phone,
      email = r.email,
      suspensionStatus = r.suspensionStatus,
      regime = r.regime,
      supervisoryBody = r.supervisoryBody,
      membershipNumber = r.membershipNumber,
      evidenceObjectReference = r.evidenceObjectReference
    )

    val statuses = StatusPart(
      updateDetailsStatus = r.updateDetailsStatus,
      amlSupervisionUpdateStatus = r.amlSupervisionUpdateStatus,
      directorPartnerUpdateStatus = r.directorPartnerUpdateStatus,
      acceptNewTermsStatus = r.acceptNewTermsStatus,
      reriskStatus = r.reriskStatus
    )

    Json.toJsObject(base)(implicitly[OWrites[BasePart]]) ++ Json.toJsObject(statuses)(implicitly[OWrites[StatusPart]])
  }

  implicit val format: OFormat[AgentSubscriptionDisplayResponse] =
    OFormat[AgentSubscriptionDisplayResponse](reads, writes)
}
