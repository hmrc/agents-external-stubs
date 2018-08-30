package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.Gen
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

object LegacyRelationshipRecord extends RecordUtils[LegacyRelationshipRecord] {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"
  def ninoKey(nino: String): String = s"nino:${nino.replace(" ", "")}"
  def utrKey(utr: String): String = s"utr:${utr.replace(" ", "")}"

  import Validator._

  val validate: Validator[LegacyRelationshipRecord] = Validator(
    check(_.agentId.lengthMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.nino.isRight(RegexPatterns.validNinoNoSpaces), "Invalid nino"),
    check(_.utr.isRight(RegexPatterns.validUtr), "Invalid utr"),
    check(r => r.nino.isDefined || r.utr.isDefined, "Missing client identifier: nino or utr")
  )

  implicit val formats: Format[LegacyRelationshipRecord] = Json.format[LegacyRelationshipRecord]
  implicit val recordType: RecordMetaData[LegacyRelationshipRecord] =
    RecordMetaData[LegacyRelationshipRecord](LegacyRelationshipRecord)

  val agentIdGen = Generator.pattern("999999")

  override val gen: Gen[LegacyRelationshipRecord] =
    for {
      agentId <- agentIdGen
    } yield
      LegacyRelationshipRecord(
        agentId = agentId
      )

  val ninoSanitizer: Update = e => e.copy(nino = e.nino.orElse(Some(Generator.ninoNoSpaces(e.agentId).value)))
  val utrSanitizer: Update = e => e.copy(utr = e.utr.orElse(Some(Generator.utr(e.agentId).value)))

  override val sanitizers: Seq[Update] = Seq(ninoSanitizer, utrSanitizer)
}
