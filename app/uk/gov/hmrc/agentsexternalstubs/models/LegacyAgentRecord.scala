package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.Gen
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.{BusinessAddress, BusinessContact}
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

object LegacyAgentRecord extends RecordUtils[LegacyAgentRecord] {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"

  import Validator._

  val validate: Validator[LegacyAgentRecord] = Validator(
    check(_.agentId.lengthMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.agentOwnRef.lengthMinMaxInclusive(1, 20), "Invalid agentOwnRef"),
    check(_.govAgentId.lengthMinMaxInclusive(1, 12), "Invalid govAgentId"),
    check(_.agentName.lengthMinMaxInclusive(1, 56), "Invalid agentName"),
    check(_.agentPhoneNo.lengthMinMaxInclusive(1, 20), "Invalid agentPhoneNo"),
    check(_.address1.lengthMinMaxInclusive(1, 28), "Invalid address1"),
    check(_.address2.lengthMinMaxInclusive(1, 28), "Invalid address2"),
    check(_.address3.lengthMinMaxInclusive(1, 28), "Invalid address3"),
    check(_.address4.lengthMinMaxInclusive(1, 28), "Invalid address4"),
    check(_.postcode.isRight(RegexPatterns.validPostcode), "Invalid postcode")
  )

  implicit val formats: Format[LegacyAgentRecord] = Json.format[LegacyAgentRecord]
  implicit val recordType: RecordMetaData[LegacyAgentRecord] = RecordMetaData[LegacyAgentRecord](LegacyAgentRecord)

  val agentIdGen = Generator.pattern("999999")

  override val gen: Gen[LegacyAgentRecord] =
    for {
      agentId   <- agentIdGen
      agentName <- UserGenerator.nameForAgentGen
      address1  <- BusinessAddress.addressLine1Gen
      address2  <- BusinessAddress.addressLine2Gen
    } yield
      LegacyAgentRecord(
        agentId = agentId,
        agentName = agentName,
        address1 = address1,
        address2 = address2
      )

  val agentPhoneNoSanitizer: Update = e =>
    e.copy(agentPhoneNo = e.agentPhoneNo.orElse(Some(Generator.get(BusinessContact.phoneNumberGen)(e.agentId))))

  val postcodeSanitizer: Update = e =>
    e.copy(postcode = e.postcode.orElse(Some(Generator.get(BusinessAddress.postalCodeGen)(e.agentId))))

  override val sanitizers: Seq[Update] = Seq(agentPhoneNoSanitizer, postcodeSanitizer)
}
