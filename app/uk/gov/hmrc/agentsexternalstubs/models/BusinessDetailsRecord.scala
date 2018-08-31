package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import org.scalacheck.Gen
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

object BusinessDetailsRecord extends RecordUtils[BusinessDetailsRecord] {

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

  override val validate: Validator[BusinessDetailsRecord] = Validator(
    check(_.safeId.lengthMinMaxInclusive(1, 16), "Invalid safeId"),
    check(_.nino.isRight(RegexPatterns.validNinoNoSpaces), "Invalid nino"),
    check(_.mtdbsa.isRight(RegexPatterns.validMtdbsa), "Invalid mtdbsa"),
    checkEachIfSome(_.businessData, BusinessData.validate)
  )

  val safeIdGen = Generator.pattern("999999999999999")

  override val gen: Gen[BusinessDetailsRecord] =
    for {
      safeId <- safeIdGen
      nino   <- Generator.ninoNoSpacesGen
      mtdbsa <- Generator.mtdbsaGen
    } yield BusinessDetailsRecord(safeId, nino, mtdbsa)

  val businessDataSanitizer: Update = seed =>
    record => {
      record.copy(businessData = Some(record.businessData match {
        case Some(businessData) => businessData.map(BusinessData.sanitize(seed))
        case None               => Seq(BusinessData.generate(seed))
      }))
  }

  override val sanitizers: Seq[Update] = Seq(businessDataSanitizer)

  object BusinessData extends RecordUtils[BusinessData] {

    val incomeSourceIdGen = Generator.pattern("999999999999999")
    val tradingNameGen = Generator.company
    val tradingStartDateGen = Generator.date(1970, 2018).map(Generator.toJodaDate)

    override val gen: Gen[BusinessData] =
      for {
        incomeSourceId            <- incomeSourceIdGen
        accountingPeriodStartDate <- Gen.const(LocalDate.parse("2001-01-01"))
        accountingPeriodEndDate   <- Gen.const(LocalDate.parse("2001-01-01"))
      } yield
        BusinessData(
          incomeSourceId = incomeSourceId,
          accountingPeriodStartDate = accountingPeriodStartDate,
          accountingPeriodEndDate = accountingPeriodEndDate)

    override val validate: Validator[BusinessData] = Validator(
      check(_.incomeSourceId.lengthMinMaxInclusive(15, 16), "Invalid incomeSourceId"),
      check(_.tradingName.lengthMinMaxInclusive(1, 105), "Invalid tradingName"),
      checkObjectIfSome(_.businessAddressDetails, BusinessAddress.validate),
      checkObjectIfSome(_.businessContactDetails, BusinessContact.validate),
      check(_.cashOrAccruals.isTrue(v => v == "cash" || v == "accruals"), "Invalid cashOrAccruals")
    )

    val tradingNameSanitizer: Update = seed =>
      e => e.copy(tradingName = e.tradingName.orElse(Generator.get(tradingNameGen)(seed)))

    val tradingStartDateSanitizer: Update = seed =>
      e => e.copy(tradingStartDate = e.tradingStartDate.orElse(Generator.get(tradingStartDateGen)(seed)))

    val businessAddressDetailsSanitizer: Update = seed =>
      record =>
        record.copy(businessAddressDetails = Some(record.businessAddressDetails match {
          case Some(address) => BusinessAddress.sanitize(seed)(address)
          case None          => BusinessAddress.generate(seed)
        }))

    val businessContactDetailsSanitizer: Update = seed =>
      record =>
        record.copy(businessContactDetails = Some(record.businessContactDetails match {
          case Some(contact) => BusinessContact.sanitize(seed)(contact)
          case None          => BusinessContact.generate(seed)
        }))

    override val sanitizers: Seq[Update] =
      Seq(
        tradingNameSanitizer,
        tradingStartDateSanitizer,
        businessAddressDetailsSanitizer,
        businessContactDetailsSanitizer)
  }

  object BusinessAddress extends RecordUtils[BusinessAddress] {

    val addressLine1Gen = Generator.ukAddress.map(_.head)
    val addressLine2Gen = Generator.ukAddress.map(_(1))
    val postalCodeGen = Generator.ukAddress.map(_(2))

    override val gen: Gen[BusinessAddress] =
      for {
        addressLine1 <- addressLine1Gen
        countryCode  <- Gen.const("GB")
      } yield BusinessAddress(addressLine1 = addressLine1, countryCode = countryCode)

    override val validate: Validator[BusinessAddress] = Validator(
      check(_.addressLine1.lengthMinMaxInclusive(1, 35), "Invalid addressLine1"),
      check(_.addressLine2.lengthMinMaxInclusive(1, 35), "Invalid addressLine2"),
      check(_.addressLine3.lengthMinMaxInclusive(1, 35), "Invalid addressLine3"),
      check(_.addressLine4.lengthMinMaxInclusive(1, 35), "Invalid addressLine4"),
      check(_.postalCode.lengthMinMaxInclusive(1, 10), "Invalid postalCode"),
      check(_.countryCode.matches("^[A-Z]{2}$"), "Invalid countryCode")
    )

    val addressLine2Sanitizer: Update = seed =>
      e => e.copy(addressLine2 = e.addressLine2.orElse(Generator.get(addressLine2Gen)(seed)))

    val postalCodeSanitizer: Update = seed =>
      e => e.copy(postalCode = e.postalCode.orElse(Generator.get(postalCodeGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(addressLine2Sanitizer, postalCodeSanitizer)
  }

  object BusinessContact extends RecordUtils[BusinessContact] {

    val phoneNumberGen = Generator.ukPhoneNumber

    override val gen: Gen[BusinessContact] =
      for {
        phoneNumber <- Gen.option(phoneNumberGen)
      } yield
        BusinessContact(
          phoneNumber = phoneNumber
        )

    override val validate: Validator[BusinessContact] = Validator(
      check(_.phoneNumber.lengthMinMaxInclusive(1, 24), "Invalid phoneNumber"),
      check(_.phoneNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid phoneNumber"),
      check(_.mobileNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid mobileNumber"),
      check(_.faxNumber.matches("^[A-Z0-9 )/(*#-]+$"), "Invalid faxNumber"),
      check(_.emailAddress.lengthMinMaxInclusive(3, 132), "Invalid emailAddress")
    )

    val phoneNumberSanitizer: Update = seed => e => e.copy(phoneNumber = e.phoneNumber.orElse(Some("01332752856")))
    val emailAddressSanitizer: Update = seed =>
      e => e.copy(emailAddress = e.emailAddress.orElse(Generator.get(Generator.emailGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(phoneNumberSanitizer, emailAddressSanitizer)
  }

  implicit val formats1: Format[BusinessAddress] = Json.format[BusinessAddress]
  implicit val formats2: Format[BusinessContact] = Json.format[BusinessContact]
  implicit val formats3: Format[BusinessData] = Json.format[BusinessData]
  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]
  implicit val recordType: RecordMetaData[BusinessDetailsRecord] =
    RecordMetaData[BusinessDetailsRecord](this)
}
