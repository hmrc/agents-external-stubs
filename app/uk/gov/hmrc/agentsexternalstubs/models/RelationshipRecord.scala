package uk.gov.hmrc.agentsexternalstubs.models
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}

case class RelationshipRecord(
  regime: String,
  arn: String,
  idType: String,
  refNumber: String,
  active: Boolean = false,
  relationshipType: Option[String] = None,
  authProfile: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None,
  id: Option[String] = None)
    extends Record {

  import RelationshipRecord._

  override def keys: Seq[String] =
    Seq(
      fullKey(regime, arn, idType, refNumber),
      agentKey(regime, arn),
      clientKey(regime, idType, refNumber),
      agentClientKey(arn, idType, refNumber),
      regime,
      arn,
      refNumber
    )

  override def withId(id: Option[String]): RelationshipRecord = copy(id = id)
}

object RelationshipRecord {

  implicit val formats: Format[RelationshipRecord] = Json.format[RelationshipRecord]

  def fullKey(regime: String, arn: String, idType: String, refNumber: String): String =
    s"FK/$regime/$arn/$idType/$refNumber"

  def agentKey(regime: String, arn: String): String = s"AK/$regime/$arn"

  def clientKey(regime: String, idType: String, refNumber: String): String =
    s"CK/$regime/$idType/$refNumber"

  def agentClientKey(arn: String, idType: String, refNumber: String): String =
    s"ACK/$arn/$idType/$refNumber"
}
