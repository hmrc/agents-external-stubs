package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class CreateAgentSubscriptionPayload(
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: String,
  phone: Option[String],
  email: String,
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: String,
  amlSupervisionUpdateStatus: String,
  directorPartnerUpdateStatus: String,
  acceptNewTermsStatus: String,
  reriskStatus: String
)

object CreateAgentSubscriptionPayload {
  implicit val format: Format[CreateAgentSubscriptionPayload] = Json.format[CreateAgentSubscriptionPayload]
}
