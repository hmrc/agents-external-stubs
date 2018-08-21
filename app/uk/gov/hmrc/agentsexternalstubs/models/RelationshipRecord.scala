package uk.gov.hmrc.agentsexternalstubs.models
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}

case class RelationshipRecord(
  regime: String,
  arn: String,
  idType: String,
  refNumber: String,
  active: Boolean,
  relationshipType: Option[String] = None,
  authProfile: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None)
    extends Record {

  override def keys = Seq(RelationshipRecord.key(regime, arn, idType, refNumber), regime, arn, refNumber)
}
object RelationshipRecord {
  implicit val formats: Format[RelationshipRecord] = Json.format[RelationshipRecord]

  def key(regime: String, arn: String, idType: String, refNumber: String): String =
    s"RelationshipRecord/$regime/$arn/$idType/$refNumber"
}
