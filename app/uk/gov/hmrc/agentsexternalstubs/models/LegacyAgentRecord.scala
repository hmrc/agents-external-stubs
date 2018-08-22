package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class LegacyAgentRecord(
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
  agentCeasedDate: Option[String] = None,
  id: Option[String] = None)
    extends Record {

  override def keys: Seq[String] = Seq(LegacyAgentRecord.agentIdKey(agentId))
  override def withId(id: Option[String]): LegacyAgentRecord = copy(id = id)
}

object LegacyAgentRecord {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"

  implicit val formats: Format[LegacyAgentRecord] = Json.format[LegacyAgentRecord]

}
