/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.services

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.agentsexternalstubs.models._

import java.time.LocalDateTime

object SubscribeAgentService {

  def toBusinessPartnerRecord(
    payload: SubscribeAgentServicesPayload,
    existingRecord: BusinessPartnerRecord
  ): BusinessPartnerRecord = {
    val address = payload.agencyAddress match {
      case SubscribeAgentServicesPayload.UkAddress(l1, l2, l3, l4, pc, cc) =>
        BusinessPartnerRecord.UkAddress(l1, l2, l3, l4, pc, cc)
      case SubscribeAgentServicesPayload.ForeignAddress(l1, l2, l3, l4, pc, cc) =>
        BusinessPartnerRecord.ForeignAddress(l1, l2, l3, l4, pc, cc)
    }
    existingRecord
      .modifyAgentReferenceNumber { case None =>
        Some(Generator.arn(existingRecord.utr.getOrElse(existingRecord.safeId)).value)
      }
      .withAgencyDetails(
        Some(
          BusinessPartnerRecord
            .AgencyDetails()
            .withAgencyName(Option(payload.agencyName))
            .withAgencyAddress(Some(address))
            .withAgencyEmail(payload.agencyEmail)
            .withAgencyTelephoneNumber(payload.telephoneNumber)
        )
      )
      .modifyContactDetails { case Some(contactDetails) =>
        Some(
          contactDetails
            .withPhoneNumber(payload.telephoneNumber)
            .withEmailAddress(payload.agencyEmail)
        )
      }
      .withAddressDetails(address)
      .withIsAnAgent(true)
      .withIsAnASAgent(true)
  }

  def toBusinessPartnerRecord(
    payload: HipSubscribeAgentServicesPayload,
    existingRecord: BusinessPartnerRecord
  ): BusinessPartnerRecord = {
    val address = BusinessPartnerRecord.HipAddress(
      payload.addr1,
      payload.addr2,
      payload.addr3,
      payload.addr4,
      payload.postcode,
      payload.country
    )

    val (
      updateDetailsStatus,
      amlSupervisionUpdateStatus,
      directorPartnerUpdateStatus,
      acceptNewTermsStatus,
      reriskStatus
    ) = (
      UpdateDetailsStatus(AgencyDetailsStatusValue.fromString(payload.updateDetailsStatus)),
      AmlSupervisionUpdateStatus(AgencyDetailsStatusValue.fromString(payload.amlSupervisionUpdateStatus)),
      DirectorPartnerUpdateStatus(AgencyDetailsStatusValue.fromString(payload.directorPartnerUpdateStatus)),
      AcceptNewTermsStatus(AgencyDetailsStatusValue.fromString(payload.acceptNewTermsStatus)),
      ReriskStatus(AgencyDetailsStatusValue.fromString(payload.reriskStatus))
    )

    existingRecord
      .modifyAgentReferenceNumber { case None =>
        Some(Generator.arn(existingRecord.utr.getOrElse(existingRecord.safeId)).value)
      }
      .withAgencyDetails(
        Some(
          BusinessPartnerRecord
            .AgencyDetails()
            .withAgencyName(Option(payload.name))
            .withAgencyAddress(Some(address))
            .withAgencyEmail(Some(payload.email))
            .withAgencyTelephoneNumber(payload.phone)
            .withSupervisoryBody(payload.supervisoryBody)
            .withMembershipNumber(payload.membershipNumber)
            .withEvidenceObjectReference(payload.evidenceObjectReference)
            .withUpdateDetailsStatus(Some(updateDetailsStatus))
            .withAmlSupervisionUpdateStatus(Some(amlSupervisionUpdateStatus))
            .withDirectorPartnerUpdateStatus(Some(directorPartnerUpdateStatus))
            .withAcceptNewTermsStatus(Some(acceptNewTermsStatus))
            .withReriskStatus(Some(reriskStatus))
        )
      )
      .modifyContactDetails { case Some(contactDetails) =>
        Some(
          contactDetails
            .withPhoneNumber(payload.phone)
            .withEmailAddress(Some(payload.email))
        )
      }
      .withAddressDetails(address)
      .withIsAnAgent(true)
      .withIsAnASAgent(true)
  }

  sealed trait SubscribeAgentServiceResponse

  case class DesIfResponse(safeId: String, agentRegistrationNumber: String) extends SubscribeAgentServiceResponse

  object DesIfResponse {
    implicit val writes: Writes[DesIfResponse] = Json.writes[DesIfResponse]
  }

  case class HipResponse(processingDate: LocalDateTime, arn: String) extends SubscribeAgentServiceResponse

  object HipResponse {
    implicit val writes: Writes[HipResponse] = Json.writes[HipResponse]
  }
}
