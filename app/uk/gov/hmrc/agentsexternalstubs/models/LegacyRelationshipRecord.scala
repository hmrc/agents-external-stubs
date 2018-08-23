package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class LegacyRelationshipRecord(
  agentId: String,
  nino: Option[String] = None,
  utr: Option[String] = None,
  id: Option[String] = None)
    extends Record {

  override def lookupKeys: Seq[String] =
    Seq(
      nino.map(LegacyRelationshipRecord.ninoKey),
      utr.map(LegacyRelationshipRecord.utrKey),
      Option(LegacyRelationshipRecord.agentIdKey(agentId))).collect {
      case Some(x) => x
    }
  override def withId(id: Option[String]): LegacyRelationshipRecord = copy(id = id)
}

object LegacyRelationshipRecord {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"
  def ninoKey(nino: String): String = s"nino:$nino"
  def utrKey(utr: String): String = s"utr:$utr"

  import Validator._

  val validate: Validator[LegacyRelationshipRecord] = Validator(
    check(_.agentId.sizeMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.nino.isRight(RegexPatterns.validNino), "Invalid nino"),
    check(_.utr.isRight(RegexPatterns.validUtr), "Invalid utr"),
    check(r => r.nino.isDefined || r.utr.isDefined, "Missing client identifier: nino or utr")
  )

  implicit val formats: Format[LegacyRelationshipRecord] = Json.format[LegacyRelationshipRecord]
}
