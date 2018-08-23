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

  override def uniqueKey: Option[String] = Option(agentId)
  override def lookupKeys: Seq[String] = Seq(LegacyAgentRecord.agentIdKey(agentId))

  override def withId(id: Option[String]): LegacyAgentRecord = copy(id = id)
}

object LegacyAgentRecord {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"

  import Validator._

  val validate: Validator[LegacyAgentRecord] = Validator(
    check(_.agentId.sizeMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.agentOwnRef.sizeMinMaxInclusive(1, 20), "Invalid agentOwnRef"),
    check(_.govAgentId.sizeMinMaxInclusive(1, 12), "Invalid govAgentId"),
    check(_.agentName.sizeMinMaxInclusive(1, 56), "Invalid agentName"),
    check(_.agentPhoneNo.sizeMinMaxInclusive(1, 20), "Invalid agentPhoneNo"),
    check(_.address1.sizeMinMaxInclusive(1, 28), "Invalid address1"),
    check(_.address2.sizeMinMaxInclusive(1, 28), "Invalid address2"),
    check(_.address3.sizeMinMaxInclusive(1, 28), "Invalid address3"),
    check(_.address4.sizeMinMaxInclusive(1, 28), "Invalid address4"),
    check(_.postcode.isRight(RegexPatterns.validPostcode), "Invalid postcode")
  )

  implicit val formats: Format[LegacyAgentRecord] = Json.format[LegacyAgentRecord]

}
