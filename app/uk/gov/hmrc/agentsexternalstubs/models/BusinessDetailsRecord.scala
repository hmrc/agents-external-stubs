package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1171.json app/uk/gov/hmrc/agentsexternalstubs/models/BusinessDetailsRecord.scala BusinessDetailsRecord "
  * ----------------------------------------------------------------------------
  *
  *  BusinessDetailsRecord
  *  -  BusinessContactDetails
  *  -  BusinessData
  *  -  -  BusinessAddressDetails
  *  -  ForeignAddress
  *  -  PropertyData
  *  -  UkAddress
  */
case class BusinessDetailsRecord(
  safeId: String,
  nino: String,
  mtdbsa: String,
  propertyIncome: Boolean = false,
  businessData: Option[Seq[BusinessData]] = None,
  propertyData: Option[PropertyData] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(safeId).map(BusinessDetailsRecord.uniqueKey)
  override def lookupKeys: Seq[String] =
    Seq(Option(nino).map(BusinessDetailsRecord.ninoKey), Option(mtdbsa).map(BusinessDetailsRecord.mtdbsaKey)).collect {
      case Some(x) => x
    }
  override def withId(id: Option[String]): BusinessDetailsRecord = copy(id = id)

  def withSafeId(safeId: String): BusinessDetailsRecord = copy(safeId = safeId)
  def modifySafeId(pf: PartialFunction[String, String]): BusinessDetailsRecord =
    if (pf.isDefinedAt(safeId)) copy(safeId = pf(safeId)) else this
  def withNino(nino: String): BusinessDetailsRecord = copy(nino = nino)
  def modifyNino(pf: PartialFunction[String, String]): BusinessDetailsRecord =
    if (pf.isDefinedAt(nino)) copy(nino = pf(nino)) else this
  def withMtdbsa(mtdbsa: String): BusinessDetailsRecord = copy(mtdbsa = mtdbsa)
  def modifyMtdbsa(pf: PartialFunction[String, String]): BusinessDetailsRecord =
    if (pf.isDefinedAt(mtdbsa)) copy(mtdbsa = pf(mtdbsa)) else this
  def withPropertyIncome(propertyIncome: Boolean): BusinessDetailsRecord = copy(propertyIncome = propertyIncome)
  def modifyPropertyIncome(pf: PartialFunction[Boolean, Boolean]): BusinessDetailsRecord =
    if (pf.isDefinedAt(propertyIncome)) copy(propertyIncome = pf(propertyIncome)) else this
  def withBusinessData(businessData: Option[Seq[BusinessData]]): BusinessDetailsRecord =
    copy(businessData = businessData)
  def modifyBusinessData(
    pf: PartialFunction[Option[Seq[BusinessData]], Option[Seq[BusinessData]]]): BusinessDetailsRecord =
    if (pf.isDefinedAt(businessData)) copy(businessData = pf(businessData)) else this
  def withPropertyData(propertyData: Option[PropertyData]): BusinessDetailsRecord = copy(propertyData = propertyData)
  def modifyPropertyData(pf: PartialFunction[Option[PropertyData], Option[PropertyData]]): BusinessDetailsRecord =
    if (pf.isDefinedAt(propertyData)) copy(propertyData = pf(propertyData)) else this
}

object BusinessDetailsRecord extends RecordUtils[BusinessDetailsRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[BusinessDetailsRecord] = RecordMetaData[BusinessDetailsRecord](this)

  def uniqueKey(key: String): String = s"""safeId:${key.toUpperCase}"""
  def ninoKey(key: String): String = s"""nino:${key.toUpperCase}"""
  def mtdbsaKey(key: String): String = s"""mtdbsa:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  override val validate: Validator[BusinessDetailsRecord] = Validator(
    check(_.safeId.lengthMinMaxInclusive(1, 16), "Invalid length of safeId, should be between 1 and 16 inclusive"),
    check(_.nino.matches(Common.ninoPattern), s"""Invalid nino, does not matches regex ${Common.ninoPattern}"""),
    check(_.mtdbsa.lengthMinMaxInclusive(15, 16), "Invalid length of mtdbsa, should be between 15 and 16 inclusive"),
    checkEachIfSome(_.businessData, BusinessData.validate),
    checkObjectIfSome(_.propertyData, PropertyData.validate)
  )

  override val gen: Gen[BusinessDetailsRecord] = for {
    safeId         <- Generator.safeIdGen.suchThat(_.length >= 1).suchThat(_.length <= 16)
    nino           <- Generator.ninoNoSpacesGen
    mtdbsa         <- Generator.mtdbsaGen.suchThat(_.length >= 15).suchThat(_.length <= 16)
    propertyIncome <- Generator.booleanGen
  } yield
    BusinessDetailsRecord(
      safeId = safeId,
      nino = nino,
      mtdbsa = mtdbsa,
      propertyIncome = propertyIncome
    )

  val businessDataSanitizer: Update = seed =>
    entity =>
      entity.copy(
        businessData = entity.businessData
          .orElse(Generator.get(Generator.nonEmptyListOfMaxN(1, BusinessData.gen))(seed))
          .map(_.map(BusinessData.sanitize(seed))))

  val propertyDataSanitizer: Update = seed =>
    entity =>
      entity.copy(
        propertyData =
          entity.propertyData.orElse(Generator.get(PropertyData.gen)(seed)).map(PropertyData.sanitize(seed)))

  override val sanitizers: Seq[Update] = Seq(businessDataSanitizer, propertyDataSanitizer)

  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]

  case class BusinessContactDetails(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None) {

    def withPhoneNumber(phoneNumber: Option[String]): BusinessContactDetails = copy(phoneNumber = phoneNumber)
    def modifyPhoneNumber(pf: PartialFunction[Option[String], Option[String]]): BusinessContactDetails =
      if (pf.isDefinedAt(phoneNumber)) copy(phoneNumber = pf(phoneNumber)) else this
    def withMobileNumber(mobileNumber: Option[String]): BusinessContactDetails = copy(mobileNumber = mobileNumber)
    def modifyMobileNumber(pf: PartialFunction[Option[String], Option[String]]): BusinessContactDetails =
      if (pf.isDefinedAt(mobileNumber)) copy(mobileNumber = pf(mobileNumber)) else this
    def withFaxNumber(faxNumber: Option[String]): BusinessContactDetails = copy(faxNumber = faxNumber)
    def modifyFaxNumber(pf: PartialFunction[Option[String], Option[String]]): BusinessContactDetails =
      if (pf.isDefinedAt(faxNumber)) copy(faxNumber = pf(faxNumber)) else this
    def withEmailAddress(emailAddress: Option[String]): BusinessContactDetails = copy(emailAddress = emailAddress)
    def modifyEmailAddress(pf: PartialFunction[Option[String], Option[String]]): BusinessContactDetails =
      if (pf.isDefinedAt(emailAddress)) copy(emailAddress = pf(emailAddress)) else this
  }

  object BusinessContactDetails extends RecordUtils[BusinessContactDetails] {

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

    override val gen: Gen[BusinessContactDetails] = Gen const BusinessContactDetails(
      )

    val phoneNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          phoneNumber = entity.phoneNumber.orElse(
            Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val mobileNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          mobileNumber = entity.mobileNumber.orElse(
            Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val faxNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          faxNumber = entity.faxNumber.orElse(
            Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(
          emailAddress = entity.emailAddress.orElse(
            Generator.get(Generator.emailGen.suchThat(_.length >= 3).suchThat(_.length <= 132))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(phoneNumberSanitizer, mobileNumberSanitizer, faxNumberSanitizer, emailAddressSanitizer)

    implicit val formats: Format[BusinessContactDetails] = Json.format[BusinessContactDetails]

  }

  case class BusinessData(
    incomeSourceId: String,
    accountingPeriodStartDate: String,
    accountingPeriodEndDate: String,
    tradingName: Option[String] = None,
    businessAddressDetails: Option[BusinessData.BusinessAddressDetails] = None,
    businessContactDetails: Option[BusinessContactDetails] = None,
    tradingStartDate: Option[String] = None,
    cashOrAccruals: Option[String] = None,
    seasonal: Boolean = false,
    cessationDate: Option[String] = None,
    cessationReason: Option[String] = None,
    paperLess: Boolean = false) {

    def withIncomeSourceId(incomeSourceId: String): BusinessData = copy(incomeSourceId = incomeSourceId)
    def modifyIncomeSourceId(pf: PartialFunction[String, String]): BusinessData =
      if (pf.isDefinedAt(incomeSourceId)) copy(incomeSourceId = pf(incomeSourceId)) else this
    def withAccountingPeriodStartDate(accountingPeriodStartDate: String): BusinessData =
      copy(accountingPeriodStartDate = accountingPeriodStartDate)
    def modifyAccountingPeriodStartDate(pf: PartialFunction[String, String]): BusinessData =
      if (pf.isDefinedAt(accountingPeriodStartDate)) copy(accountingPeriodStartDate = pf(accountingPeriodStartDate))
      else this
    def withAccountingPeriodEndDate(accountingPeriodEndDate: String): BusinessData =
      copy(accountingPeriodEndDate = accountingPeriodEndDate)
    def modifyAccountingPeriodEndDate(pf: PartialFunction[String, String]): BusinessData =
      if (pf.isDefinedAt(accountingPeriodEndDate)) copy(accountingPeriodEndDate = pf(accountingPeriodEndDate)) else this
    def withTradingName(tradingName: Option[String]): BusinessData = copy(tradingName = tradingName)
    def modifyTradingName(pf: PartialFunction[Option[String], Option[String]]): BusinessData =
      if (pf.isDefinedAt(tradingName)) copy(tradingName = pf(tradingName)) else this
    def withBusinessAddressDetails(businessAddressDetails: Option[BusinessData.BusinessAddressDetails]): BusinessData =
      copy(businessAddressDetails = businessAddressDetails)
    def modifyBusinessAddressDetails(
      pf: PartialFunction[Option[BusinessData.BusinessAddressDetails], Option[BusinessData.BusinessAddressDetails]])
      : BusinessData =
      if (pf.isDefinedAt(businessAddressDetails)) copy(businessAddressDetails = pf(businessAddressDetails)) else this
    def withBusinessContactDetails(businessContactDetails: Option[BusinessContactDetails]): BusinessData =
      copy(businessContactDetails = businessContactDetails)
    def modifyBusinessContactDetails(
      pf: PartialFunction[Option[BusinessContactDetails], Option[BusinessContactDetails]]): BusinessData =
      if (pf.isDefinedAt(businessContactDetails)) copy(businessContactDetails = pf(businessContactDetails)) else this
    def withTradingStartDate(tradingStartDate: Option[String]): BusinessData = copy(tradingStartDate = tradingStartDate)
    def modifyTradingStartDate(pf: PartialFunction[Option[String], Option[String]]): BusinessData =
      if (pf.isDefinedAt(tradingStartDate)) copy(tradingStartDate = pf(tradingStartDate)) else this
    def withCashOrAccruals(cashOrAccruals: Option[String]): BusinessData = copy(cashOrAccruals = cashOrAccruals)
    def modifyCashOrAccruals(pf: PartialFunction[Option[String], Option[String]]): BusinessData =
      if (pf.isDefinedAt(cashOrAccruals)) copy(cashOrAccruals = pf(cashOrAccruals)) else this
    def withSeasonal(seasonal: Boolean): BusinessData = copy(seasonal = seasonal)
    def modifySeasonal(pf: PartialFunction[Boolean, Boolean]): BusinessData =
      if (pf.isDefinedAt(seasonal)) copy(seasonal = pf(seasonal)) else this
    def withCessationDate(cessationDate: Option[String]): BusinessData = copy(cessationDate = cessationDate)
    def modifyCessationDate(pf: PartialFunction[Option[String], Option[String]]): BusinessData =
      if (pf.isDefinedAt(cessationDate)) copy(cessationDate = pf(cessationDate)) else this
    def withCessationReason(cessationReason: Option[String]): BusinessData = copy(cessationReason = cessationReason)
    def modifyCessationReason(pf: PartialFunction[Option[String], Option[String]]): BusinessData =
      if (pf.isDefinedAt(cessationReason)) copy(cessationReason = pf(cessationReason)) else this
    def withPaperLess(paperLess: Boolean): BusinessData = copy(paperLess = paperLess)
    def modifyPaperLess(pf: PartialFunction[Boolean, Boolean]): BusinessData =
      if (pf.isDefinedAt(paperLess)) copy(paperLess = pf(paperLess)) else this
  }

  object BusinessData extends RecordUtils[BusinessData] {

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
      checkObjectIfSome(_.businessAddressDetails, BusinessAddressDetails.validate),
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

    override val gen: Gen[BusinessData] = for {
      incomeSourceId            <- Generator.stringMinMaxN(15, 16).suchThat(_.length >= 15).suchThat(_.length <= 16)
      accountingPeriodStartDate <- Generator.dateYYYYMMDDGen.variant("accountingperiodstart")
      accountingPeriodEndDate   <- Generator.dateYYYYMMDDGen.variant("accountingperiodend")
      seasonal                  <- Generator.booleanGen
      paperLess                 <- Generator.booleanGen
    } yield
      BusinessData(
        incomeSourceId = incomeSourceId,
        accountingPeriodStartDate = accountingPeriodStartDate,
        accountingPeriodEndDate = accountingPeriodEndDate,
        seasonal = seasonal,
        paperLess = paperLess
      )

    val tradingNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          tradingName = entity.tradingName.orElse(
            Generator.get(Generator.tradingNameGen.suchThat(_.length >= 1).suchThat(_.length <= 105))(seed)))

    val businessAddressDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessAddressDetails = entity.businessAddressDetails
            .orElse(Generator.get(BusinessAddressDetails.gen)(seed))
            .map(BusinessAddressDetails.sanitize(seed)))

    val businessContactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessContactDetails = entity.businessContactDetails
            .orElse(Generator.get(BusinessContactDetails.gen)(seed))
            .map(BusinessContactDetails.sanitize(seed)))

    val tradingStartDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          tradingStartDate =
            entity.tradingStartDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("tradingstart"))(seed)))

    val cashOrAccrualsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cashOrAccruals = entity.cashOrAccruals.orElse(Generator.get(Gen.oneOf(Common.cashOrAccrualsEnum))(seed)))

    val cessationDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationDate =
            entity.cessationDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("cessation"))(seed)))

    val cessationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationReason = entity.cessationReason.orElse(Generator.get(Gen.oneOf(Common.cessationReasonEnum))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      tradingNameSanitizer,
      businessAddressDetailsSanitizer,
      businessContactDetailsSanitizer,
      tradingStartDateSanitizer,
      cashOrAccrualsSanitizer,
      cessationDateSanitizer,
      cessationReasonSanitizer
    )

    implicit val formats: Format[BusinessData] = Json.format[BusinessData]

    sealed trait BusinessAddressDetails {
      def addressLine2: Option[String] = None
      def addressLine3: Option[String] = None
      def addressLine1: String
      def countryCode: String
      def addressLine4: Option[String] = None
    }

    object BusinessAddressDetails extends RecordUtils[BusinessAddressDetails] {

      override val validate: Validator[BusinessAddressDetails] = {
        case x: UkAddress      => UkAddress.validate(x)
        case x: ForeignAddress => ForeignAddress.validate(x)
      }

      override val gen: Gen[BusinessAddressDetails] = Gen.oneOf[BusinessAddressDetails](
        UkAddress.gen.map(_.asInstanceOf[BusinessAddressDetails]),
        ForeignAddress.gen.map(_.asInstanceOf[BusinessAddressDetails]))

      val sanitizer: Update = seed => {
        case x: UkAddress      => UkAddress.sanitize(seed)(x)
        case x: ForeignAddress => ForeignAddress.sanitize(seed)(x)
      }
      override val sanitizers: Seq[Update] = Seq(sanitizer)

      implicit val reads: Reads[BusinessAddressDetails] = new Reads[BusinessAddressDetails] {
        override def reads(json: JsValue): JsResult[BusinessAddressDetails] = {
          val r0 =
            UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
          val r1 = r0.orElse(
            ForeignAddress.formats
              .reads(json)
              .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
          r1.orElse(
            aggregateErrors(
              JsError(
                "Could not match json object to any variant of BusinessAddressDetails, i.e. UkAddress, ForeignAddress"),
              r0,
              r1))
        }

        private def aggregateErrors[T](errors: JsResult[T]*): JsError =
          errors.foldLeft(JsError())((a, r) =>
            r match {
              case e: JsError => JsError(a.errors ++ e.errors)
              case _          => a
          })
      }

      implicit val writes: Writes[BusinessAddressDetails] = new Writes[BusinessAddressDetails] {
        override def writes(o: BusinessAddressDetails): JsValue = o match {
          case x: UkAddress      => UkAddress.formats.writes(x)
          case x: ForeignAddress => ForeignAddress.formats.writes(x)
        }
      }

    }

  }

  case class ForeignAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    override val countryCode: String)
      extends BusinessData.BusinessAddressDetails {

    def withAddressLine1(addressLine1: String): ForeignAddress = copy(addressLine1 = addressLine1)
    def modifyAddressLine1(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(addressLine1)) copy(addressLine1 = pf(addressLine1)) else this
    def withAddressLine2(addressLine2: Option[String]): ForeignAddress = copy(addressLine2 = addressLine2)
    def modifyAddressLine2(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(addressLine2)) copy(addressLine2 = pf(addressLine2)) else this
    def withAddressLine3(addressLine3: Option[String]): ForeignAddress = copy(addressLine3 = addressLine3)
    def modifyAddressLine3(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(addressLine3)) copy(addressLine3 = pf(addressLine3)) else this
    def withAddressLine4(addressLine4: Option[String]): ForeignAddress = copy(addressLine4 = addressLine4)
    def modifyAddressLine4(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(addressLine4)) copy(addressLine4 = pf(addressLine4)) else this
    def withPostalCode(postalCode: Option[String]): ForeignAddress = copy(postalCode = postalCode)
    def modifyPostalCode(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(postalCode)) copy(postalCode = pf(postalCode)) else this
    def withCountryCode(countryCode: String): ForeignAddress = copy(countryCode = countryCode)
    def modifyCountryCode(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(countryCode)) copy(countryCode = pf(countryCode)) else this
  }

  object ForeignAddress extends RecordUtils[ForeignAddress] {

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

    override val gen: Gen[ForeignAddress] = for {
      addressLine1 <- Generator.address4Lines35Gen.map(_.line1).suchThat(_.length >= 1).suchThat(_.length <= 35)
      countryCode  <- Gen.oneOf(Common.countryCodeEnum0)
    } yield
      ForeignAddress(
        addressLine1 = addressLine1,
        countryCode = countryCode
      )

    val addressLine2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine2 = entity.addressLine2.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line2).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = entity.addressLine3.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line3).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine4 = entity.addressLine4.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line4).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val postalCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          postalCode = entity.postalCode.orElse(
            Generator.get(Generator.postcode.suchThat(_.length >= 1).suchThat(_.length <= 10))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer, postalCodeSanitizer)

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
    paperLess: Boolean = false) {

    def withIncomeSourceId(incomeSourceId: String): PropertyData = copy(incomeSourceId = incomeSourceId)
    def modifyIncomeSourceId(pf: PartialFunction[String, String]): PropertyData =
      if (pf.isDefinedAt(incomeSourceId)) copy(incomeSourceId = pf(incomeSourceId)) else this
    def withAccountingPeriodStartDate(accountingPeriodStartDate: String): PropertyData =
      copy(accountingPeriodStartDate = accountingPeriodStartDate)
    def modifyAccountingPeriodStartDate(pf: PartialFunction[String, String]): PropertyData =
      if (pf.isDefinedAt(accountingPeriodStartDate)) copy(accountingPeriodStartDate = pf(accountingPeriodStartDate))
      else this
    def withAccountingPeriodEndDate(accountingPeriodEndDate: String): PropertyData =
      copy(accountingPeriodEndDate = accountingPeriodEndDate)
    def modifyAccountingPeriodEndDate(pf: PartialFunction[String, String]): PropertyData =
      if (pf.isDefinedAt(accountingPeriodEndDate)) copy(accountingPeriodEndDate = pf(accountingPeriodEndDate)) else this
    def withNumPropRented(numPropRented: Option[String]): PropertyData = copy(numPropRented = numPropRented)
    def modifyNumPropRented(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(numPropRented)) copy(numPropRented = pf(numPropRented)) else this
    def withNumPropRentedUK(numPropRentedUK: Option[String]): PropertyData = copy(numPropRentedUK = numPropRentedUK)
    def modifyNumPropRentedUK(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(numPropRentedUK)) copy(numPropRentedUK = pf(numPropRentedUK)) else this
    def withNumPropRentedEEA(numPropRentedEEA: Option[String]): PropertyData = copy(numPropRentedEEA = numPropRentedEEA)
    def modifyNumPropRentedEEA(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(numPropRentedEEA)) copy(numPropRentedEEA = pf(numPropRentedEEA)) else this
    def withNumPropRentedNONEEA(numPropRentedNONEEA: Option[String]): PropertyData =
      copy(numPropRentedNONEEA = numPropRentedNONEEA)
    def modifyNumPropRentedNONEEA(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(numPropRentedNONEEA)) copy(numPropRentedNONEEA = pf(numPropRentedNONEEA)) else this
    def withEmailAddress(emailAddress: Option[String]): PropertyData = copy(emailAddress = emailAddress)
    def modifyEmailAddress(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(emailAddress)) copy(emailAddress = pf(emailAddress)) else this
    def withCessationDate(cessationDate: Option[String]): PropertyData = copy(cessationDate = cessationDate)
    def modifyCessationDate(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(cessationDate)) copy(cessationDate = pf(cessationDate)) else this
    def withCessationReason(cessationReason: Option[String]): PropertyData = copy(cessationReason = cessationReason)
    def modifyCessationReason(pf: PartialFunction[Option[String], Option[String]]): PropertyData =
      if (pf.isDefinedAt(cessationReason)) copy(cessationReason = pf(cessationReason)) else this
    def withPaperLess(paperLess: Boolean): PropertyData = copy(paperLess = paperLess)
    def modifyPaperLess(pf: PartialFunction[Boolean, Boolean]): PropertyData =
      if (pf.isDefinedAt(paperLess)) copy(paperLess = pf(paperLess)) else this
  }

  object PropertyData extends RecordUtils[PropertyData] {

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

    override val gen: Gen[PropertyData] = for {
      incomeSourceId            <- Generator.stringMinMaxN(15, 16).suchThat(_.length >= 15).suchThat(_.length <= 16)
      accountingPeriodStartDate <- Generator.dateYYYYMMDDGen.variant("accountingperiodstart")
      accountingPeriodEndDate   <- Generator.dateYYYYMMDDGen.variant("accountingperiodend")
      paperLess                 <- Generator.booleanGen
    } yield
      PropertyData(
        incomeSourceId = incomeSourceId,
        accountingPeriodStartDate = accountingPeriodStartDate,
        accountingPeriodEndDate = accountingPeriodEndDate,
        paperLess = paperLess
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
      entity =>
        entity.copy(
          emailAddress = entity.emailAddress.orElse(
            Generator.get(Generator.emailGen.suchThat(_.length >= 3).suchThat(_.length <= 132))(seed)))

    val cessationDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationDate =
            entity.cessationDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("cessation"))(seed)))

    val cessationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          cessationReason = entity.cessationReason.orElse(Generator.get(Gen.oneOf(Common.cessationReasonEnum))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      numPropRentedSanitizer,
      numPropRentedUKSanitizer,
      numPropRentedEEASanitizer,
      numPropRentedNONEEASanitizer,
      emailAddressSanitizer,
      cessationDateSanitizer,
      cessationReasonSanitizer
    )

    implicit val formats: Format[PropertyData] = Json.format[PropertyData]

  }

  case class UkAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: String,
    override val countryCode: String)
      extends BusinessData.BusinessAddressDetails {

    def withAddressLine1(addressLine1: String): UkAddress = copy(addressLine1 = addressLine1)
    def modifyAddressLine1(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(addressLine1)) copy(addressLine1 = pf(addressLine1)) else this
    def withAddressLine2(addressLine2: Option[String]): UkAddress = copy(addressLine2 = addressLine2)
    def modifyAddressLine2(pf: PartialFunction[Option[String], Option[String]]): UkAddress =
      if (pf.isDefinedAt(addressLine2)) copy(addressLine2 = pf(addressLine2)) else this
    def withAddressLine3(addressLine3: Option[String]): UkAddress = copy(addressLine3 = addressLine3)
    def modifyAddressLine3(pf: PartialFunction[Option[String], Option[String]]): UkAddress =
      if (pf.isDefinedAt(addressLine3)) copy(addressLine3 = pf(addressLine3)) else this
    def withAddressLine4(addressLine4: Option[String]): UkAddress = copy(addressLine4 = addressLine4)
    def modifyAddressLine4(pf: PartialFunction[Option[String], Option[String]]): UkAddress =
      if (pf.isDefinedAt(addressLine4)) copy(addressLine4 = pf(addressLine4)) else this
    def withPostalCode(postalCode: String): UkAddress = copy(postalCode = postalCode)
    def modifyPostalCode(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(postalCode)) copy(postalCode = pf(postalCode)) else this
    def withCountryCode(countryCode: String): UkAddress = copy(countryCode = countryCode)
    def modifyCountryCode(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(countryCode)) copy(countryCode = pf(countryCode)) else this
  }

  object UkAddress extends RecordUtils[UkAddress] {

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

    override val gen: Gen[UkAddress] = for {
      addressLine1 <- Generator.address4Lines35Gen.map(_.line1).suchThat(_.length >= 1).suchThat(_.length <= 35)
      postalCode   <- Generator.postcode.suchThat(_.length >= 1).suchThat(_.length <= 10)
      countryCode  <- Gen.const("GB")
    } yield
      UkAddress(
        addressLine1 = addressLine1,
        postalCode = postalCode,
        countryCode = countryCode
      )

    val addressLine2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine2 = entity.addressLine2.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line2).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = entity.addressLine3.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line3).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine4 = entity.addressLine4.orElse(Generator.get(
            Generator.address4Lines35Gen.map(_.line4).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    override val sanitizers: Seq[Update] = Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer)

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

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
