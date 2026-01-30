package uk.gov.hmrc.agentsexternalstubs.services

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, Generator, HipSubscribeAgentServicesPayload, SubscribeAgentServicesPayload}

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

  case class Response(safeId: String, agentRegistrationNumber: String)

  object Response {
    implicit val writes: Writes[Response] = Json.writes[Response]
  }
}
