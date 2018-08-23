package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData

case class BusinessDetailsRecord(
  safeId: String,
  nino: String,
  mtdbsa: String,
  propertyIncome: Option[Boolean] = None,
  businessData: Option[Seq[BusinessData]] = None,
  id: Option[String] = None)
    extends Record {

  override def uniqueKey: Option[String] = Option(safeId)
  override def lookupKeys: Seq[String] =
    Seq(BusinessDetailsRecord.ninoKey(nino), BusinessDetailsRecord.mtdbsaKey(mtdbsa))
  override def withId(id: Option[String]): BusinessDetailsRecord = copy(id = id)
}

object BusinessDetailsRecord {

  def ninoKey(nino: String): String = s"nino:$nino"
  def mtdbsaKey(mtdbsa: String): String = s"mtdbsa:$mtdbsa"

  case class BusinessAddress(
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    countryCode: String)

  case class BusinessContact(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None)

  case class BusinessData(
    incomeSourceId: String,
    accountingPeriodStartDate: LocalDate,
    accountingPeriodEndDate: LocalDate,
    tradingName: Option[String] = None,
    businessAddressDetails: Option[BusinessAddress] = None,
    businessContactDetails: Option[BusinessContact] = None,
    tradingStartDate: Option[LocalDate] = None,
    cashOrAccruals: Option[String] = None,
    seasonal: Option[Boolean] = None,
    cessationDate: Option[LocalDate] = None,
    cessationReason: Option[String] = None,
    paperLess: Option[Boolean] = None
  )

  import Validator._

  val validateBusinessAddress: Validator[BusinessAddress] = Validator(
    check(_.addressLine1.sizeMinMaxInclusive(1, 35), "Invalid addressLine1"),
    check(_.addressLine2.sizeMinMaxInclusive(1, 35), "Invalid addressLine2"),
    check(_.addressLine3.sizeMinMaxInclusive(1, 35), "Invalid addressLine3"),
    check(_.addressLine4.sizeMinMaxInclusive(1, 35), "Invalid addressLine4"),
    check(_.postalCode.sizeMinMaxInclusive(1, 10), "Invalid postalCode"),
    check(_.countryCode.matches("^[A-Z]{2}$"), "Invalid countryCode")
  )

  val validateBusinessContact: Validator[BusinessContact] = Validator(
    check(_.phoneNumber.sizeMinMaxInclusive(1, 24), "Invalid phoneNumber"),
    check(_.phoneNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid phoneNumber"),
    check(_.mobileNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid mobileNumber"),
    check(_.faxNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid faxNumber"),
    check(_.emailAddress.sizeMinMaxInclusive(3, 132), "Invalid emailAddress")
  )

  val validateBusinessData: Validator[BusinessData] = Validator(
    check(_.incomeSourceId.sizeMinMaxInclusive(15, 16), "Invalid incomeSourceId"),
    check(_.tradingName.sizeMinMaxInclusive(1, 105), "Invalid tradingName"),
    checkObjectIfSome(_.businessAddressDetails, validateBusinessAddress),
    checkObjectIfSome(_.businessContactDetails, validateBusinessContact),
    check(_.cashOrAccruals.isTrue(v => v == "cash" || v == "accruals"), "Invalid cashOrAccruals")
  )

  val validate: Validator[BusinessDetailsRecord] = Validator(
    check(_.safeId.sizeMinMaxInclusive(1, 16), "Invalid safeId"),
    check(_.nino.isRight(RegexPatterns.validNino), "Invalid nino"),
    check(_.mtdbsa.isRight(RegexPatterns.validMtdbsa), "Invalid mtdbsa"),
    checkEachIfSome(_.businessData, validateBusinessData)
  )

  implicit val formats1: Format[BusinessAddress] = Json.format[BusinessAddress]
  implicit val formats2: Format[BusinessContact] = Json.format[BusinessContact]
  implicit val formats3: Format[BusinessData] = Json.format[BusinessData]
  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]

}
