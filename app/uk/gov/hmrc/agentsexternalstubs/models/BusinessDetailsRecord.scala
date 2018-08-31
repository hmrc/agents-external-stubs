package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord._

/**
  * ----------------------------------------------------------------------------
  * This BusinessDetailsRecord code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
case class BusinessDetailsRecord(
  safeId: String,
  nino: String,
  mtdbsa: String,
  propertyIncome: Option[Boolean] = None,
  businessData: Option[Seq[BusinessData]] = None,
  propertyData: Option[PropertyData] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(safeId).map(BusinessDetailsRecord.uniqueKey)
  override def lookupKeys: Seq[String] =
    Seq(Option(nino).map(BusinessDetailsRecord.ninoKey), Option(mtdbsa).map(BusinessDetailsRecord.mtdbsaKey))
      .collect { case Some(x) => x }
  override def withId(id: Option[String]): Record = copy(id = id)
}

object BusinessDetailsRecord extends RecordUtils[BusinessDetailsRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[BusinessDetailsRecord] = RecordMetaData[BusinessDetailsRecord](this)

  def uniqueKey(key: String): String = s"""safeId:${key.toUpperCase}"""
  def ninoKey(key: String): String = s"""nino:${key.toUpperCase}"""
  def mtdbsaKey(key: String): String = s"""mtdbsa:${key.toUpperCase}"""

  import Validator._

  override val gen: Gen[BusinessDetailsRecord] = for {
    safeId <- Generator.safeIdGen
    nino   <- Generator.ninoNoSpacesGen
    mtdbsa <- Generator.mtdbsaGen
  } yield
    BusinessDetailsRecord(
      safeId = safeId,
      nino = nino,
      mtdbsa = mtdbsa
    )

  case class BusinessContactDetails(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None)

  object BusinessContactDetails extends RecordUtils[BusinessContactDetails] {

    override val gen: Gen[BusinessContactDetails] = Gen const BusinessContactDetails(
      )

    override val validate: Validator[BusinessContactDetails] = Validator(
      check(
        _.phoneNumber.matches(Common.phoneNumberPattern),
        s"""Invalid phoneNumber, does not matches regex ${Common.phoneNumberPattern}"""),
      check(
        _.mobileNumber.matches(Common.phoneNumberPattern),
        s"""Invalid mobileNumber, does not matches regex ${Common.phoneNumberPattern}"""),
      check(
        _.faxNumber.matches(Common.phoneNumberPattern),
        s"""Invalid faxNumber, does not matches regex ${Common.phoneNumberPattern}"""),
      check(
        _.emailAddress.lengthMinMaxInclusive(3, 132),
        "Invalid length of emailAddress, should be between 3 and 132 inclusive")
    )

    val phoneNumberSanitizer: Update = seed =>
      entity => entity.copy(phoneNumber = entity.phoneNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val mobileNumberSanitizer: Update = seed =>
      entity => entity.copy(mobileNumber = entity.mobileNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val faxNumberSanitizer: Update = seed =>
      entity => entity.copy(faxNumber = entity.faxNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity => entity.copy(emailAddress = entity.emailAddress.orElse(Generator.get(Generator.emailGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(
      phoneNumberSanitizer,
      mobileNumberSanitizer,
      faxNumberSanitizer,
      emailAddressSanitizer
    )

    implicit val formats: Format[BusinessContactDetails] = Json.format[BusinessContactDetails]

  }

  case class BusinessData(
    incomeSourceId: String,
    accountingPeriodStartDate: String,
    accountingPeriodEndDate: String,
    tradingName: Option[String] = None,
    businessAddressDetails: Option[UkAddress] = None,
    businessContactDetails: Option[BusinessContactDetails] = None,
    tradingStartDate: Option[String] = None,
    cashOrAccruals: Option[String] = None,
    seasonal: Option[Boolean] = None,
    cessationDate: Option[String] = None,
    cessationReason: Option[String] = None,
    paperLess: Option[Boolean] = None)

  object BusinessData extends RecordUtils[BusinessData] {

    override val gen: Gen[BusinessData] = for {
      incomeSourceId            <- Generator.stringMinMaxN(15, 16)
      accountingPeriodStartDate <- Generator.dateYYYYMMDDGen
      accountingPeriodEndDate   <- Generator.dateYYYYMMDDGen
    } yield
      BusinessData(
        incomeSourceId = incomeSourceId,
        accountingPeriodStartDate = accountingPeriodStartDate,
        accountingPeriodEndDate = accountingPeriodEndDate
      )

    override val validate: Validator[BusinessData] = Validator(
      check(
        _.incomeSourceId.lengthMinMaxInclusive(15, 16),
        "Invalid length of incomeSourceId, should be between 15 and 16 inclusive"),
      check(
        _.accountingPeriodStartDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid accountingPeriodStartDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.accountingPeriodEndDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid accountingPeriodEndDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 105),
        "Invalid length of tradingName, should be between 1 and 105 inclusive"),
      checkObjectIfSome(_.businessAddressDetails, UkAddress.validate),
      checkObjectIfSome(_.businessContactDetails, BusinessContactDetails.validate),
      check(
        _.tradingStartDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid tradingStartDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.cashOrAccruals.isOneOf(Common.cashOrAccrualsEnum),
        "Invalid cashOrAccruals, does not match allowed values"),
      check(
        _.cessationDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid cessationDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.cessationReason.isOneOf(Common.cessationReasonEnum),
        "Invalid cessationReason, does not match allowed values")
    )

    val tradingNameSanitizer: Update = seed =>
      entity => entity.copy(tradingName = entity.tradingName.orElse(Generator.get(Generator.company)(seed)))

    val businessAddressDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(businessAddressDetails = entity.businessAddressDetails.orElse(Generator.get(UkAddress.gen)(seed)))

    val businessContactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(businessContactDetails =
          entity.businessContactDetails.orElse(Generator.get(BusinessContactDetails.gen)(seed)))

    val tradingStartDateSanitizer: Update = seed =>
      entity =>
        entity.copy(tradingStartDate = entity.tradingStartDate.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val cashOrAccrualsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cashOrAccruals = entity.cashOrAccruals.orElse(Generator.get(Gen.oneOf(Common.cashOrAccrualsEnum))(seed)))

    val seasonalSanitizer: Update = seed =>
      entity => entity.copy(seasonal = entity.seasonal.orElse(Generator.get(Generator.biasedBooleanGen)(seed)))

    val cessationDateSanitizer: Update = seed =>
      entity => entity.copy(cessationDate = entity.cessationDate.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val cessationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationReason = entity.cessationReason.orElse(Generator.get(Gen.oneOf(Common.cessationReasonEnum))(seed)))

    val paperLessSanitizer: Update = seed =>
      entity => entity.copy(paperLess = entity.paperLess.orElse(Generator.get(Generator.biasedBooleanGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(
      tradingNameSanitizer,
      businessAddressDetailsSanitizer,
      businessContactDetailsSanitizer,
      tradingStartDateSanitizer,
      cashOrAccrualsSanitizer,
      seasonalSanitizer,
      cessationDateSanitizer,
      cessationReasonSanitizer,
      paperLessSanitizer
    )

    implicit val formats: Format[BusinessData] = Json.format[BusinessData]

  }

  case class ForeignAddress(
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    countryCode: String)

  object ForeignAddress extends RecordUtils[ForeignAddress] {

    override val gen: Gen[ForeignAddress] = for {
      addressLine1 <- Generator.stringMinMaxN(1, 35)
      countryCode  <- Gen.oneOf(Common.countryCodeEnum0)
    } yield
      ForeignAddress(
        addressLine1 = addressLine1,
        countryCode = countryCode
      )

    override val validate: Validator[ForeignAddress] = Validator(
      check(
        _.addressLine1.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine1, should be between 1 and 35 inclusive"),
      check(
        _.addressLine2.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine2, should be between 1 and 35 inclusive"),
      check(
        _.addressLine3.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine3, should be between 1 and 35 inclusive"),
      check(
        _.addressLine4.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine4, should be between 1 and 35 inclusive"),
      check(
        _.postalCode.lengthMinMaxInclusive(1, 10),
        "Invalid length of postalCode, should be between 1 and 10 inclusive"),
      check(_.countryCode.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")
    )

    val addressLine2Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine2 = entity.addressLine2.orElse(Generator.get(Generator.stringMinMaxN(1, 35))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine3 = entity.addressLine3.orElse(Generator.get(Generator.stringMinMaxN(1, 35))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine4 = entity.addressLine4.orElse(Generator.get(Generator.stringMinMaxN(1, 35))(seed)))

    val postalCodeSanitizer: Update = seed =>
      entity => entity.copy(postalCode = entity.postalCode.orElse(Generator.get(Generator.stringMinMaxN(1, 10))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      addressLine2Sanitizer,
      addressLine3Sanitizer,
      addressLine4Sanitizer,
      postalCodeSanitizer
    )

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class PropertyData(
    incomeSourceId: String,
    accountingPeriodStartDate: String,
    accountingPeriodEndDate: String,
    numPropRented: Option[String] = None,
    numPropRentedUK: Option[String] = None,
    numPropRentedEEA: Option[String] = None,
    numPropRentedNONEEA: Option[String] = None,
    emailAddress: Option[String] = None,
    cessationDate: Option[String] = None,
    cessationReason: Option[String] = None,
    paperLess: Option[Boolean] = None)

  object PropertyData extends RecordUtils[PropertyData] {

    override val gen: Gen[PropertyData] = for {
      incomeSourceId            <- Generator.stringMinMaxN(15, 16)
      accountingPeriodStartDate <- Generator.dateYYYYMMDDGen
      accountingPeriodEndDate   <- Generator.dateYYYYMMDDGen
    } yield
      PropertyData(
        incomeSourceId = incomeSourceId,
        accountingPeriodStartDate = accountingPeriodStartDate,
        accountingPeriodEndDate = accountingPeriodEndDate
      )

    override val validate: Validator[PropertyData] = Validator(
      check(
        _.incomeSourceId.lengthMinMaxInclusive(15, 16),
        "Invalid length of incomeSourceId, should be between 15 and 16 inclusive"),
      check(
        _.accountingPeriodStartDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid accountingPeriodStartDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.accountingPeriodEndDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid accountingPeriodEndDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.numPropRented.matches(Common.numPropRentedPattern),
        s"""Invalid numPropRented, does not matches regex ${Common.numPropRentedPattern}"""),
      check(
        _.numPropRentedUK.matches(Common.numPropRentedPattern),
        s"""Invalid numPropRentedUK, does not matches regex ${Common.numPropRentedPattern}"""),
      check(
        _.numPropRentedEEA.matches(Common.numPropRentedPattern),
        s"""Invalid numPropRentedEEA, does not matches regex ${Common.numPropRentedPattern}"""),
      check(
        _.numPropRentedNONEEA.matches(Common.numPropRentedPattern),
        s"""Invalid numPropRentedNONEEA, does not matches regex ${Common.numPropRentedPattern}"""),
      check(
        _.emailAddress.lengthMinMaxInclusive(3, 132),
        "Invalid length of emailAddress, should be between 3 and 132 inclusive"),
      check(
        _.cessationDate.matches(Common.accountingPeriodStartDatePattern),
        s"""Invalid cessationDate, does not matches regex ${Common.accountingPeriodStartDatePattern}"""
      ),
      check(
        _.cessationReason.isOneOf(Common.cessationReasonEnum),
        "Invalid cessationReason, does not match allowed values")
    )

    val numPropRentedSanitizer: Update = seed =>
      entity =>
        entity.copy(numPropRented =
          entity.numPropRented.orElse(Generator.get(Generator.regex(Common.numPropRentedPattern))(seed)))

    val numPropRentedUKSanitizer: Update = seed =>
      entity =>
        entity.copy(
          numPropRentedUK =
            entity.numPropRentedUK.orElse(Generator.get(Generator.regex(Common.numPropRentedPattern))(seed)))

    val numPropRentedEEASanitizer: Update = seed =>
      entity =>
        entity.copy(
          numPropRentedEEA =
            entity.numPropRentedEEA.orElse(Generator.get(Generator.regex(Common.numPropRentedPattern))(seed)))

    val numPropRentedNONEEASanitizer: Update = seed =>
      entity =>
        entity.copy(
          numPropRentedNONEEA =
            entity.numPropRentedNONEEA.orElse(Generator.get(Generator.regex(Common.numPropRentedPattern))(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity => entity.copy(emailAddress = entity.emailAddress.orElse(Generator.get(Generator.emailGen)(seed)))

    val cessationDateSanitizer: Update = seed =>
      entity => entity.copy(cessationDate = entity.cessationDate.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val cessationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationReason = entity.cessationReason.orElse(Generator.get(Gen.oneOf(Common.cessationReasonEnum))(seed)))

    val paperLessSanitizer: Update = seed =>
      entity => entity.copy(paperLess = entity.paperLess.orElse(Generator.get(Generator.biasedBooleanGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(
      numPropRentedSanitizer,
      numPropRentedUKSanitizer,
      numPropRentedEEASanitizer,
      numPropRentedNONEEASanitizer,
      emailAddressSanitizer,
      cessationDateSanitizer,
      cessationReasonSanitizer,
      paperLessSanitizer
    )

    implicit val formats: Format[PropertyData] = Json.format[PropertyData]

  }

  case class UkAddress(
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postalCode: String,
    countryCode: String)

  object UkAddress extends RecordUtils[UkAddress] {

    override val gen: Gen[UkAddress] = for {
      addressLine1 <- Generator.address4Lines35Gen.map(_.line1)
      postalCode   <- Generator.postcode
      countryCode  <- Gen.const("GB")
    } yield
      UkAddress(
        addressLine1 = addressLine1,
        postalCode = postalCode,
        countryCode = countryCode
      )

    override val validate: Validator[UkAddress] = Validator(
      check(
        _.addressLine1.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine1, should be between 1 and 35 inclusive"),
      check(
        _.addressLine2.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine2, should be between 1 and 35 inclusive"),
      check(
        _.addressLine3.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine3, should be between 1 and 35 inclusive"),
      check(
        _.addressLine4.lengthMinMaxInclusive(1, 35),
        "Invalid length of addressLine4, should be between 1 and 35 inclusive"),
      check(
        _.postalCode.lengthMinMaxInclusive(1, 10),
        "Invalid length of postalCode, should be between 1 and 10 inclusive"),
      check(_.countryCode.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")
    )

    val addressLine2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine2 = entity.addressLine2.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line2))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = entity.addressLine3.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine4 = entity.addressLine4.orElse(Generator.get(Generator.stringMinMaxN(1, 35))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      addressLine2Sanitizer,
      addressLine3Sanitizer,
      addressLine4Sanitizer
    )

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  override val validate: Validator[BusinessDetailsRecord] = Validator(
    check(_.safeId.lengthMinMaxInclusive(1, 16), "Invalid length of safeId, should be between 1 and 16 inclusive"),
    check(_.nino.matches(Common.ninoPattern), s"""Invalid nino, does not matches regex ${Common.ninoPattern}"""),
    check(_.mtdbsa.lengthMinMaxInclusive(15, 16), "Invalid length of mtdbsa, should be between 15 and 16 inclusive"),
    checkEachIfSome(_.businessData, BusinessData.validate),
    checkObjectIfSome(_.propertyData, PropertyData.validate)
  )

  val propertyIncomeSanitizer: Update = seed =>
    entity =>
      entity.copy(propertyIncome = entity.propertyIncome.orElse(Generator.get(Generator.biasedBooleanGen)(seed)))

  val businessDataSanitizer: Update = seed =>
    entity =>
      entity.copy(
        businessData =
          entity.businessData.orElse(Generator.get(Generator.nonEmptyListOfMaxN(3, BusinessData.gen))(seed)))

  val propertyDataSanitizer: Update = seed =>
    entity => entity.copy(propertyData = entity.propertyData.orElse(Generator.get(PropertyData.gen)(seed)))

  override val sanitizers: Seq[Update] = Seq(
    propertyIncomeSanitizer,
    businessDataSanitizer,
    propertyDataSanitizer
  )

  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]
  object Common {
    val ninoPattern = """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$"""
    val phoneNumberPattern = """^[A-Z0-9 )/(*#-]+$"""
    val numPropRentedPattern = """^[0-9]{1,3}$"""
    val accountingPeriodStartDatePattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val countryCodeEnum0 = Seq(
      "AD",
      "AE",
      "AF",
      "AG",
      "AI",
      "AL",
      "AM",
      "AN",
      "AO",
      "AQ",
      "AR",
      "AS",
      "AT",
      "AU",
      "AW",
      "AX",
      "AZ",
      "BA",
      "BB",
      "BD",
      "BE",
      "BF",
      "BG",
      "BH",
      "BI",
      "BJ",
      "BM",
      "BN",
      "BO",
      "BQ",
      "BR",
      "BS",
      "BT",
      "BV",
      "BW",
      "BY",
      "BZ",
      "CA",
      "CC",
      "CD",
      "CF",
      "CG",
      "CH",
      "CI",
      "CK",
      "CL",
      "CM",
      "CN",
      "CO",
      "CR",
      "CS",
      "CU",
      "CV",
      "CW",
      "CX",
      "CY",
      "CZ",
      "DE",
      "DJ",
      "DK",
      "DM",
      "DO",
      "DZ",
      "EC",
      "EE",
      "EG",
      "EH",
      "ER",
      "ES",
      "ET",
      "EU",
      "FI",
      "FJ",
      "FK",
      "FM",
      "FO",
      "FR",
      "GA",
      "GD",
      "GE",
      "GF",
      "GG",
      "GH",
      "GI",
      "GL",
      "GM",
      "GN",
      "GP",
      "GQ",
      "GR",
      "GS",
      "GT",
      "GU",
      "GW",
      "GY",
      "HK",
      "HM",
      "HN",
      "HR",
      "HT",
      "HU",
      "ID",
      "IE",
      "IL",
      "IM",
      "IN",
      "IO",
      "IQ",
      "IR",
      "IS",
      "IT",
      "JE",
      "JM",
      "JO",
      "JP",
      "KE",
      "KG",
      "KH",
      "KI",
      "KM",
      "KN",
      "KP",
      "KR",
      "KW",
      "KY",
      "KZ",
      "LA",
      "LB",
      "LC",
      "LI",
      "LK",
      "LR",
      "LS",
      "LT",
      "LU",
      "LV",
      "LY",
      "MA",
      "MC",
      "MD",
      "ME",
      "MF",
      "MG",
      "MH",
      "MK",
      "ML",
      "MM",
      "MN",
      "MO",
      "MP",
      "MQ",
      "MR",
      "MS",
      "MT",
      "MU",
      "MV",
      "MW",
      "MX",
      "MY",
      "MZ",
      "NA",
      "NC",
      "NE",
      "NF",
      "NG",
      "NI",
      "NL",
      "NO",
      "NP",
      "NR",
      "NT",
      "NU",
      "NZ",
      "OM",
      "PA",
      "PE",
      "PF",
      "PG",
      "PH",
      "PK",
      "PL",
      "PM",
      "PN",
      "PR",
      "PS",
      "PT",
      "PW",
      "PY",
      "QA",
      "RE",
      "RO",
      "RS",
      "RU",
      "RW",
      "SA",
      "SB",
      "SC",
      "SD",
      "SE",
      "SG",
      "SH",
      "SI",
      "SJ",
      "SK",
      "SL",
      "SM",
      "SN",
      "SO",
      "SR",
      "SS",
      "ST",
      "SV",
      "SX",
      "SY",
      "SZ",
      "TC",
      "TD",
      "TF",
      "TG",
      "TH",
      "TJ",
      "TK",
      "TL",
      "TM",
      "TN",
      "TO",
      "TP",
      "TR",
      "TT",
      "TV",
      "TW",
      "TZ",
      "UA",
      "UG",
      "UM",
      "UN",
      "US",
      "UY",
      "UZ",
      "VA",
      "VC",
      "VE",
      "VG",
      "VI",
      "VN",
      "VU",
      "WF",
      "WS",
      "YE",
      "YT",
      "ZA",
      "ZM",
      "ZW"
    )
    val countryCodeEnum1 = Seq("GB")
    val cashOrAccrualsEnum = Seq("cash", "accruals")
    val cessationReasonEnum = Seq("001", "002", "003", "004", "005", "006", "007", "008")
  }
}
