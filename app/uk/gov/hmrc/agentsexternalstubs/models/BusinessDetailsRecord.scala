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

object BusinessDetailsRecord extends Sanitizer[BusinessDetailsRecord] {

  def ninoKey(nino: String): String = s"nino:${nino.replace(" ", "")}"
  def mtdbsaKey(mtdbsa: String): String = s"mtdbsa:${mtdbsa.replace(" ", "")}"

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
    check(_.nino.isRight(RegexPatterns.validNinoNoSpaces), "Invalid nino"),
    check(_.mtdbsa.isRight(RegexPatterns.validMtdbsa), "Invalid mtdbsa"),
    checkEachIfSome(_.businessData, validateBusinessData)
  )

  val safeIdGen = Generator.get(Generator.pattern("999999999999999"))

  override def seed(s: String): BusinessDetailsRecord = BusinessDetailsRecord(
    safeId = safeIdGen(s),
    nino = Generator.ninoNoSpaces(s).value,
    mtdbsa = Generator.mtdbsa(s).value
  )

  val businessDataSanitizer: Update = record =>
    record.copy(businessData = Some(record.businessData match {
      case Some(businessData) => businessData.map(BusinessData.sanitize)
      case None               => Seq(BusinessData.generate(record.mtdbsa))
    }))

  override val sanitizers: Seq[Update] = Seq(businessDataSanitizer)

  object BusinessData extends Sanitizer[BusinessData] {

    val incomeSourceIdGen = Generator.get(Generator.pattern("999999999999999"))
    val tradingNameGen = Generator.get(Generator.company)
    val tradingStartDateGen = Generator.get(Generator.date(1970, 2018).map(Generator.toJodaDate))

    override def seed(s: String): BusinessData = BusinessData(
      incomeSourceId = incomeSourceIdGen(s),
      accountingPeriodStartDate = LocalDate.parse("2001-01-01"),
      accountingPeriodEndDate = LocalDate.parse("2001-01-01")
    )

    val tradingNameSanitizer: Update = e =>
      e.copy(tradingName = e.tradingName.orElse(Option(tradingNameGen(e.incomeSourceId))))

    val tradingStartDateSanitizer: Update = e =>
      e.copy(tradingStartDate = e.tradingStartDate.orElse(Option(tradingStartDateGen(e.incomeSourceId))))

    val businessAddressDetailsSanitizer: Update = record =>
      record.copy(businessAddressDetails = Some(record.businessAddressDetails match {
        case Some(address) => BusinessAddress.sanitize(address)
        case None          => BusinessAddress.generate(record.incomeSourceId)
      }))

    val businessContactDetailsSanitizer: Update = record =>
      record.copy(businessContactDetails = Some(record.businessContactDetails match {
        case Some(contact) => BusinessContact.sanitize(contact)
        case None          => BusinessContact.generate(record.incomeSourceId)
      }))

    override val sanitizers: Seq[Update] =
      Seq(
        tradingNameSanitizer,
        tradingStartDateSanitizer,
        businessAddressDetailsSanitizer,
        businessContactDetailsSanitizer)
  }

  object BusinessAddress extends Sanitizer[BusinessAddress] {

    val addressLine1Gen = Generator.get(Generator.ukAddress.map(_.head))
    val addressLine2Gen = Generator.get(Generator.ukAddress.map(_(1)))
    val postalCodeGen = Generator.get(Generator.ukAddress.map(_(2)))

    override def seed(s: String): BusinessAddress = BusinessAddress(
      addressLine1 = addressLine1Gen(s),
      countryCode = "GB"
    )

    val addressLine2Sanitizer: Update = e =>
      e.copy(addressLine2 = e.addressLine2.orElse(Option(addressLine2Gen(e.addressLine1))))

    val postalCodeSanitizer: Update = e =>
      e.copy(postalCode = e.postalCode.orElse(Option(postalCodeGen(e.addressLine1))))

    override val sanitizers: Seq[Update] = Seq(addressLine2Sanitizer, postalCodeSanitizer)
  }

  object BusinessContact extends Sanitizer[BusinessContact] {

    val phoneNumberGen = Generator.get(Generator.ukPhoneNumber)

    override def seed(s: String): BusinessContact = BusinessContact(
      phoneNumber = Option(phoneNumberGen(s))
    )

    val phoneNumberSanitizer: Update = e => e.copy(phoneNumber = e.phoneNumber.orElse(Some("01332752856")))
    val emailAddressSanitizer: Update = e =>
      e.copy(
        emailAddress = e.emailAddress.orElse(Option(Generator.get(Generator.emailGen)(e.phoneNumber.getOrElse("0")))))

    override val sanitizers: Seq[Update] = Seq(phoneNumberSanitizer, emailAddressSanitizer)
  }

  implicit val formats1: Format[BusinessAddress] = Json.format[BusinessAddress]
  implicit val formats2: Format[BusinessContact] = Json.format[BusinessContact]
  implicit val formats3: Format[BusinessData] = Json.format[BusinessData]
  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]
}
