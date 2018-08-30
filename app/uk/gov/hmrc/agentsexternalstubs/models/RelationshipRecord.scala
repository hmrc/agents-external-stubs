package uk.gov.hmrc.agentsexternalstubs.models
import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

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

  override def lookupKeys: Seq[String] =
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

object RelationshipRecord extends RecordUtils[RelationshipRecord] {

  implicit val formats: Format[RelationshipRecord] = Json.format[RelationshipRecord]
  implicit val recordType: RecordMetaData[RelationshipRecord] = RecordMetaData[RelationshipRecord](RelationshipRecord)

  def fullKey(regime: String, arn: String, idType: String, refNumber: String): String =
    s"FK/$regime/$arn/$idType/$refNumber"

  def agentKey(regime: String, arn: String): String = s"AK/$regime/$arn"

  def clientKey(regime: String, idType: String, refNumber: String): String =
    s"CK/$regime/$idType/$refNumber"

  def agentClientKey(arn: String, idType: String, refNumber: String): String =
    s"ACK/$arn/$idType/$refNumber"

  override val gen: Gen[RelationshipRecord] = for {
    regime <- Gen.oneOf("ITSA", "VATC")
    arn    <- Generator.arnGen
    idType <- regime match {
               case "VATC" => Gen.const("vrn")
               case _      => Gen.const("mtdbsa")
             }
    refNumber <- regime match {
                  case "VATC" => Generator.vrnGen
                  case _      => Generator.mtdbsaGen
                }
  } yield RelationshipRecord(regime, arn, idType, refNumber)

  override val sanitizers: Seq[Update] = Seq()
  override val validate: Validator[RelationshipRecord] = Validator()
}
