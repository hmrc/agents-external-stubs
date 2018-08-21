package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.LegacyRelationshipRecord._

case class LegacyRelationshipRecord(agents: Seq[LegacyAgent], id: Option[String] = None) extends Record {

  override def keys: Seq[String] = Seq()
  override def withId(id: Option[String]): LegacyRelationshipRecord = copy(id = id)
}

object LegacyRelationshipRecord {

  case class LegacyAgent(
    id: String,
    nino: Option[String] = None,
    agentId: String,
    agentOwnRef: Option[String] = None,
    hasAgent: Option[Boolean] = None,
    isRegisteredAgent: Option[Boolean] = None,
    govAgentId: Option[String] = None,
    agentName: String,
    agentPhoneNo: Option[String] = None,
    address1: String,
    address2: String,
    address3: Option[String] = None,
    address4: Option[String] = None,
    postcode: Option[String] = None,
    isAgentAbroad: Boolean = false,
    agentCeasedDate: Option[String] = None
  )

  implicit val formats1: Format[LegacyAgent] = Json.format[LegacyAgent]
  implicit val formats: Format[LegacyRelationshipRecord] = Json.format[LegacyRelationshipRecord]
}
