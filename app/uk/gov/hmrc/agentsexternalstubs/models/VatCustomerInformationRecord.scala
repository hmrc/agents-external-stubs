package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1136.json app/uk/gov/hmrc/agentsexternalstubs/models/VatCustomerInformationRecord.scala VatCustomerInformationRecord "
  * ----------------------------------------------------------------------------
  *
  *  VatCustomerInformationRecord
  *  -  Address
  *  -  ApprovedInformation
  *  -  BankDetails
  *  -  BusinessActivities
  *  -  ChangeIndicators
  *  -  ContactDetails
  *  -  CorrespondenceContactDetails
  *  -  CustomerDetails
  *  -  Deregistration
  *  -  FlatRateScheme
  *  -  ForeignAddress
  *  -  FormInformation
  *  -  GroupOrPartner
  *  -  InFlightBankDetails
  *  -  InFlightBusinessActivities
  *  -  InFlightCorrespondenceContactDetails
  *  -  InFlightCustomerDetails
  *  -  InFlightDeregistration
  *  -  InFlightFlatRateScheme
  *  -  InFlightGroupOrPartner
  *  -  InFlightInformation
  *  -  InFlightPPOBDetails
  *  -  InFlightReturnPeriod
  *  -  IndividualName
  *  -  InflightChanges
  *  -  NonStdTaxPeriods
  *  -  PPOB
  *  -  Period
  *  -  UkAddress
  */
case class VatCustomerInformationRecord(
  vrn: String,
  approvedInformation: Option[ApprovedInformation] = None,
  inFlightInformation: Option[InFlightInformation] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(vrn).map(VatCustomerInformationRecord.uniqueKey)
  override def lookupKeys: Seq[String] = Seq()
  override def withId(id: Option[String]): VatCustomerInformationRecord = copy(id = id)

  def withVrn(vrn: String): VatCustomerInformationRecord = copy(vrn = vrn)
  def modifyVrn(pf: PartialFunction[String, String]): VatCustomerInformationRecord =
    if (pf.isDefinedAt(vrn)) copy(vrn = pf(vrn)) else this
  def withApprovedInformation(approvedInformation: Option[ApprovedInformation]): VatCustomerInformationRecord =
    copy(approvedInformation = approvedInformation)
  def modifyApprovedInformation(
    pf: PartialFunction[Option[ApprovedInformation], Option[ApprovedInformation]]): VatCustomerInformationRecord =
    if (pf.isDefinedAt(approvedInformation)) copy(approvedInformation = pf(approvedInformation)) else this
  def withInFlightInformation(inFlightInformation: Option[InFlightInformation]): VatCustomerInformationRecord =
    copy(inFlightInformation = inFlightInformation)
  def modifyInFlightInformation(
    pf: PartialFunction[Option[InFlightInformation], Option[InFlightInformation]]): VatCustomerInformationRecord =
    if (pf.isDefinedAt(inFlightInformation)) copy(inFlightInformation = pf(inFlightInformation)) else this
}

object VatCustomerInformationRecord extends RecordUtils[VatCustomerInformationRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[VatCustomerInformationRecord] =
    RecordMetaData[VatCustomerInformationRecord](this)

  def uniqueKey(key: String): String = s"""vrn:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  override val validate: Validator[VatCustomerInformationRecord] = Validator(
    check(_.vrn.matches(Common.vrnPattern), s"""Invalid vrn, does not matches regex ${Common.vrnPattern}"""),
    checkObjectIfSome(_.approvedInformation, ApprovedInformation.validate),
    checkObjectIfSome(_.inFlightInformation, InFlightInformation.validate)
  )

  override val gen: Gen[VatCustomerInformationRecord] = for {
    vrn <- Generator.vrnGen
  } yield
    VatCustomerInformationRecord(
      vrn = vrn
    )

  val approvedInformationSanitizer: Update = seed =>
    entity =>
      entity.copy(
        approvedInformation = entity.approvedInformation
          .orElse(Generator.get(ApprovedInformation.gen)(seed))
          .map(ApprovedInformation.sanitize(seed)))

  val inFlightInformationSanitizer: Update = seed =>
    entity =>
      entity.copy(
        inFlightInformation = entity.inFlightInformation
          .orElse(Generator.get(InFlightInformation.gen)(seed))
          .map(InFlightInformation.sanitize(seed)))

  override val sanitizers: Seq[Update] = Seq(approvedInformationSanitizer, inFlightInformationSanitizer)

  implicit val formats: Format[VatCustomerInformationRecord] = Json.format[VatCustomerInformationRecord]

  sealed trait Address {
    def line1: String
    def line4: Option[String] = None
    def line2: String
    def line3: Option[String] = None
    def countryCode: String
  }

  object Address extends RecordUtils[Address] {

    override val validate: Validator[Address] = {
      case x: UkAddress      => UkAddress.validate(x)
      case x: ForeignAddress => ForeignAddress.validate(x)
    }

    override val gen: Gen[Address] =
      Gen.oneOf[Address](UkAddress.gen.map(_.asInstanceOf[Address]), ForeignAddress.gen.map(_.asInstanceOf[Address]))

    val sanitizer: Update = seed => {
      case x: UkAddress      => UkAddress.sanitize(seed)(x)
      case x: ForeignAddress => ForeignAddress.sanitize(seed)(x)
    }
    override val sanitizers: Seq[Update] = Seq(sanitizer)

    implicit val reads: Reads[Address] = new Reads[Address] {
      override def reads(json: JsValue): JsResult[Address] = {
        val r0 =
          UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          ForeignAddress.formats
            .reads(json)
            .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of Address, i.e. UkAddress, ForeignAddress"),
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

    implicit val writes: Writes[Address] = new Writes[Address] {
      override def writes(o: Address): JsValue = o match {
        case x: UkAddress      => UkAddress.formats.writes(x)
        case x: ForeignAddress => ForeignAddress.formats.writes(x)
      }
    }

  }

  case class ApprovedInformation(
    customerDetails: CustomerDetails,
    PPOB: PPOB,
    correspondenceContactDetails: Option[CorrespondenceContactDetails] = None,
    bankDetails: Option[BankDetails] = None,
    businessActivities: Option[BusinessActivities] = None,
    flatRateScheme: Option[FlatRateScheme] = None,
    deregistration: Option[Deregistration] = None,
    returnPeriod: Option[Period] = None,
    groupOrPartnerMbrs: Option[Seq[GroupOrPartner]] = None) {

    def withCustomerDetails(customerDetails: CustomerDetails): ApprovedInformation =
      copy(customerDetails = customerDetails)
    def modifyCustomerDetails(pf: PartialFunction[CustomerDetails, CustomerDetails]): ApprovedInformation =
      if (pf.isDefinedAt(customerDetails)) copy(customerDetails = pf(customerDetails)) else this
    def withPPOB(PPOB: PPOB): ApprovedInformation = copy(PPOB = PPOB)
    def modifyPPOB(pf: PartialFunction[PPOB, PPOB]): ApprovedInformation =
      if (pf.isDefinedAt(PPOB)) copy(PPOB = pf(PPOB)) else this
    def withCorrespondenceContactDetails(
      correspondenceContactDetails: Option[CorrespondenceContactDetails]): ApprovedInformation =
      copy(correspondenceContactDetails = correspondenceContactDetails)
    def modifyCorrespondenceContactDetails(
      pf: PartialFunction[Option[CorrespondenceContactDetails], Option[CorrespondenceContactDetails]])
      : ApprovedInformation =
      if (pf.isDefinedAt(correspondenceContactDetails))
        copy(correspondenceContactDetails = pf(correspondenceContactDetails))
      else this
    def withBankDetails(bankDetails: Option[BankDetails]): ApprovedInformation = copy(bankDetails = bankDetails)
    def modifyBankDetails(pf: PartialFunction[Option[BankDetails], Option[BankDetails]]): ApprovedInformation =
      if (pf.isDefinedAt(bankDetails)) copy(bankDetails = pf(bankDetails)) else this
    def withBusinessActivities(businessActivities: Option[BusinessActivities]): ApprovedInformation =
      copy(businessActivities = businessActivities)
    def modifyBusinessActivities(
      pf: PartialFunction[Option[BusinessActivities], Option[BusinessActivities]]): ApprovedInformation =
      if (pf.isDefinedAt(businessActivities)) copy(businessActivities = pf(businessActivities)) else this
    def withFlatRateScheme(flatRateScheme: Option[FlatRateScheme]): ApprovedInformation =
      copy(flatRateScheme = flatRateScheme)
    def modifyFlatRateScheme(pf: PartialFunction[Option[FlatRateScheme], Option[FlatRateScheme]]): ApprovedInformation =
      if (pf.isDefinedAt(flatRateScheme)) copy(flatRateScheme = pf(flatRateScheme)) else this
    def withDeregistration(deregistration: Option[Deregistration]): ApprovedInformation =
      copy(deregistration = deregistration)
    def modifyDeregistration(pf: PartialFunction[Option[Deregistration], Option[Deregistration]]): ApprovedInformation =
      if (pf.isDefinedAt(deregistration)) copy(deregistration = pf(deregistration)) else this
    def withReturnPeriod(returnPeriod: Option[Period]): ApprovedInformation = copy(returnPeriod = returnPeriod)
    def modifyReturnPeriod(pf: PartialFunction[Option[Period], Option[Period]]): ApprovedInformation =
      if (pf.isDefinedAt(returnPeriod)) copy(returnPeriod = pf(returnPeriod)) else this
    def withGroupOrPartnerMbrs(groupOrPartnerMbrs: Option[Seq[GroupOrPartner]]): ApprovedInformation =
      copy(groupOrPartnerMbrs = groupOrPartnerMbrs)
    def modifyGroupOrPartnerMbrs(
      pf: PartialFunction[Option[Seq[GroupOrPartner]], Option[Seq[GroupOrPartner]]]): ApprovedInformation =
      if (pf.isDefinedAt(groupOrPartnerMbrs)) copy(groupOrPartnerMbrs = pf(groupOrPartnerMbrs)) else this
  }

  object ApprovedInformation extends RecordUtils[ApprovedInformation] {

    override val validate: Validator[ApprovedInformation] = Validator(
      checkObject(_.customerDetails, CustomerDetails.validate),
      checkObject(_.PPOB, PPOB.validate),
      checkObjectIfSome(_.correspondenceContactDetails, CorrespondenceContactDetails.validate),
      checkObjectIfSome(_.bankDetails, BankDetails.validate),
      checkObjectIfSome(_.businessActivities, BusinessActivities.validate),
      checkObjectIfSome(_.flatRateScheme, FlatRateScheme.validate),
      checkObjectIfSome(_.deregistration, Deregistration.validate),
      checkObjectIfSome(_.returnPeriod, Period.validate),
      checkEachIfSome(_.groupOrPartnerMbrs, GroupOrPartner.validate)
    )

    override val gen: Gen[ApprovedInformation] = for {
      customerDetails <- CustomerDetails.gen
      ppob            <- PPOB.gen
    } yield
      ApprovedInformation(
        customerDetails = customerDetails,
        PPOB = ppob
      )

    val customerDetailsSanitizer: Update = seed =>
      entity => entity.copy(customerDetails = CustomerDetails.sanitize(seed)(entity.customerDetails))

    val PPOBSanitizer: Update = seed => entity => entity.copy(PPOB = PPOB.sanitize(seed)(entity.PPOB))

    val correspondenceContactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          correspondenceContactDetails = entity.correspondenceContactDetails
            .orElse(Generator.get(CorrespondenceContactDetails.gen)(seed))
            .map(CorrespondenceContactDetails.sanitize(seed)))

    val bankDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankDetails = entity.bankDetails.orElse(Generator.get(BankDetails.gen)(seed)).map(BankDetails.sanitize(seed)))

    val businessActivitiesSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessActivities = entity.businessActivities
            .orElse(Generator.get(BusinessActivities.gen)(seed))
            .map(BusinessActivities.sanitize(seed)))

    val flatRateSchemeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          flatRateScheme =
            entity.flatRateScheme.orElse(Generator.get(FlatRateScheme.gen)(seed)).map(FlatRateScheme.sanitize(seed)))

    val deregistrationSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregistration =
            entity.deregistration.orElse(Generator.get(Deregistration.gen)(seed)).map(Deregistration.sanitize(seed)))

    val returnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          returnPeriod = entity.returnPeriod.orElse(Generator.get(Period.gen)(seed)).map(Period.sanitize(seed)))

    val groupOrPartnerMbrsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          groupOrPartnerMbrs = entity.groupOrPartnerMbrs
            .orElse(Generator.get(Generator.nonEmptyListOfMaxN(2, GroupOrPartner.gen))(seed))
            .map(_.map(GroupOrPartner.sanitize(seed))))

    override val sanitizers: Seq[Update] = Seq(
      customerDetailsSanitizer,
      PPOBSanitizer,
      correspondenceContactDetailsSanitizer,
      bankDetailsSanitizer,
      businessActivitiesSanitizer,
      flatRateSchemeSanitizer,
      deregistrationSanitizer,
      returnPeriodSanitizer,
      groupOrPartnerMbrsSanitizer
    )

    implicit val formats: Format[ApprovedInformation] = Json.format[ApprovedInformation]

  }

  case class BankDetails(
    IBAN: Option[String] = None,
    BIC: Option[String] = None,
    accountHolderName: Option[String] = None,
    bankAccountNumber: Option[String] = None,
    sortCode: Option[String] = None,
    buildingSocietyNumber: Option[String] = None,
    bankBuildSocietyName: Option[String] = None) {

    def withIBAN(IBAN: Option[String]): BankDetails = copy(IBAN = IBAN)
    def modifyIBAN(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(IBAN)) copy(IBAN = pf(IBAN)) else this
    def withBIC(BIC: Option[String]): BankDetails = copy(BIC = BIC)
    def modifyBIC(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(BIC)) copy(BIC = pf(BIC)) else this
    def withAccountHolderName(accountHolderName: Option[String]): BankDetails =
      copy(accountHolderName = accountHolderName)
    def modifyAccountHolderName(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(accountHolderName)) copy(accountHolderName = pf(accountHolderName)) else this
    def withBankAccountNumber(bankAccountNumber: Option[String]): BankDetails =
      copy(bankAccountNumber = bankAccountNumber)
    def modifyBankAccountNumber(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(bankAccountNumber)) copy(bankAccountNumber = pf(bankAccountNumber)) else this
    def withSortCode(sortCode: Option[String]): BankDetails = copy(sortCode = sortCode)
    def modifySortCode(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(sortCode)) copy(sortCode = pf(sortCode)) else this
    def withBuildingSocietyNumber(buildingSocietyNumber: Option[String]): BankDetails =
      copy(buildingSocietyNumber = buildingSocietyNumber)
    def modifyBuildingSocietyNumber(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(buildingSocietyNumber)) copy(buildingSocietyNumber = pf(buildingSocietyNumber)) else this
    def withBankBuildSocietyName(bankBuildSocietyName: Option[String]): BankDetails =
      copy(bankBuildSocietyName = bankBuildSocietyName)
    def modifyBankBuildSocietyName(pf: PartialFunction[Option[String], Option[String]]): BankDetails =
      if (pf.isDefinedAt(bankBuildSocietyName)) copy(bankBuildSocietyName = pf(bankBuildSocietyName)) else this
  }

  object BankDetails extends RecordUtils[BankDetails] {

    override val validate: Validator[BankDetails] = Validator(
      check(_.IBAN.lengthMinMaxInclusive(1, 34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
      check(_.BIC.lengthMinMaxInclusive(1, 11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
      check(
        _.accountHolderName.lengthMinMaxInclusive(1, 60),
        "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
      check(
        _.bankAccountNumber.matches(Common.bankAccountNumberPattern),
        s"""Invalid bankAccountNumber, does not matches regex ${Common.bankAccountNumberPattern}"""
      ),
      check(
        _.sortCode.matches(Common.sortCodePattern),
        s"""Invalid sortCode, does not matches regex ${Common.sortCodePattern}"""),
      check(
        _.buildingSocietyNumber.lengthMinMaxInclusive(1, 20),
        "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
      check(
        _.bankBuildSocietyName.lengthMinMaxInclusive(1, 40),
        "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
    )

    override val gen: Gen[BankDetails] = Gen const BankDetails(
      )

    val IBANSanitizer: Update = seed =>
      entity => entity.copy(IBAN = entity.IBAN.orElse(Generator.get(Generator.stringMinMaxN(1, 34))(seed)))

    val BICSanitizer: Update = seed =>
      entity => entity.copy(BIC = entity.BIC.orElse(Generator.get(Generator.stringMinMaxN(1, 11))(seed)))

    val accountHolderNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          accountHolderName = entity.accountHolderName.orElse(Generator.get(Generator.stringMinMaxN(1, 60))(seed)))

    val bankAccountNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankAccountNumber =
            entity.bankAccountNumber.orElse(Generator.get(Generator.regex(Common.bankAccountNumberPattern))(seed)))

    val sortCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(sortCode = entity.sortCode.orElse(Generator.get(Generator.regex(Common.sortCodePattern))(seed)))

    val buildingSocietyNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          buildingSocietyNumber =
            entity.buildingSocietyNumber.orElse(Generator.get(Generator.stringMinMaxN(1, 20))(seed)))

    val bankBuildSocietyNameSanitizer: Update = seed =>
      entity =>
        entity.copy(bankBuildSocietyName =
          entity.bankBuildSocietyName.orElse(Generator.get(Generator.stringMinMaxN(1, 40))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      IBANSanitizer,
      BICSanitizer,
      accountHolderNameSanitizer,
      bankAccountNumberSanitizer,
      sortCodeSanitizer,
      buildingSocietyNumberSanitizer,
      bankBuildSocietyNameSanitizer
    )

    implicit val formats: Format[BankDetails] = Json.format[BankDetails]

  }

  case class BusinessActivities(
    primaryMainCode: String,
    mainCode2: Option[String] = None,
    mainCode3: Option[String] = None,
    mainCode4: Option[String] = None) {

    def withPrimaryMainCode(primaryMainCode: String): BusinessActivities = copy(primaryMainCode = primaryMainCode)
    def modifyPrimaryMainCode(pf: PartialFunction[String, String]): BusinessActivities =
      if (pf.isDefinedAt(primaryMainCode)) copy(primaryMainCode = pf(primaryMainCode)) else this
    def withMainCode2(mainCode2: Option[String]): BusinessActivities = copy(mainCode2 = mainCode2)
    def modifyMainCode2(pf: PartialFunction[Option[String], Option[String]]): BusinessActivities =
      if (pf.isDefinedAt(mainCode2)) copy(mainCode2 = pf(mainCode2)) else this
    def withMainCode3(mainCode3: Option[String]): BusinessActivities = copy(mainCode3 = mainCode3)
    def modifyMainCode3(pf: PartialFunction[Option[String], Option[String]]): BusinessActivities =
      if (pf.isDefinedAt(mainCode3)) copy(mainCode3 = pf(mainCode3)) else this
    def withMainCode4(mainCode4: Option[String]): BusinessActivities = copy(mainCode4 = mainCode4)
    def modifyMainCode4(pf: PartialFunction[Option[String], Option[String]]): BusinessActivities =
      if (pf.isDefinedAt(mainCode4)) copy(mainCode4 = pf(mainCode4)) else this
  }

  object BusinessActivities extends RecordUtils[BusinessActivities] {

    override val validate: Validator[BusinessActivities] = Validator(
      check(
        _.primaryMainCode.matches(Common.primaryMainCodePattern),
        s"""Invalid primaryMainCode, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode2.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode2, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode3.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode3, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode4.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode4, does not matches regex ${Common.primaryMainCodePattern}""")
    )

    override val gen: Gen[BusinessActivities] = for {
      primaryMainCode <- Generator.regex(Common.primaryMainCodePattern)
    } yield
      BusinessActivities(
        primaryMainCode = primaryMainCode
      )

    val mainCode2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode2 = entity.mainCode2.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    val mainCode3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode3 = entity.mainCode3.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    val mainCode4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode4 = entity.mainCode4.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    override val sanitizers: Seq[Update] = Seq(mainCode2Sanitizer, mainCode3Sanitizer, mainCode4Sanitizer)

    implicit val formats: Format[BusinessActivities] = Json.format[BusinessActivities]

  }

  case class ChangeIndicators(
    customerDetails: Boolean,
    PPOBDetails: Boolean,
    correspContactDetails: Boolean,
    bankDetails: Boolean,
    businessActivities: Boolean,
    flatRateScheme: Boolean,
    deRegistrationInfo: Boolean,
    returnPeriods: Boolean,
    groupOrPartners: Boolean) {

    def withCustomerDetails(customerDetails: Boolean): ChangeIndicators = copy(customerDetails = customerDetails)
    def modifyCustomerDetails(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(customerDetails)) copy(customerDetails = pf(customerDetails)) else this
    def withPPOBDetails(PPOBDetails: Boolean): ChangeIndicators = copy(PPOBDetails = PPOBDetails)
    def modifyPPOBDetails(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(PPOBDetails)) copy(PPOBDetails = pf(PPOBDetails)) else this
    def withCorrespContactDetails(correspContactDetails: Boolean): ChangeIndicators =
      copy(correspContactDetails = correspContactDetails)
    def modifyCorrespContactDetails(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(correspContactDetails)) copy(correspContactDetails = pf(correspContactDetails)) else this
    def withBankDetails(bankDetails: Boolean): ChangeIndicators = copy(bankDetails = bankDetails)
    def modifyBankDetails(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(bankDetails)) copy(bankDetails = pf(bankDetails)) else this
    def withBusinessActivities(businessActivities: Boolean): ChangeIndicators =
      copy(businessActivities = businessActivities)
    def modifyBusinessActivities(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(businessActivities)) copy(businessActivities = pf(businessActivities)) else this
    def withFlatRateScheme(flatRateScheme: Boolean): ChangeIndicators = copy(flatRateScheme = flatRateScheme)
    def modifyFlatRateScheme(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(flatRateScheme)) copy(flatRateScheme = pf(flatRateScheme)) else this
    def withDeRegistrationInfo(deRegistrationInfo: Boolean): ChangeIndicators =
      copy(deRegistrationInfo = deRegistrationInfo)
    def modifyDeRegistrationInfo(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(deRegistrationInfo)) copy(deRegistrationInfo = pf(deRegistrationInfo)) else this
    def withReturnPeriods(returnPeriods: Boolean): ChangeIndicators = copy(returnPeriods = returnPeriods)
    def modifyReturnPeriods(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(returnPeriods)) copy(returnPeriods = pf(returnPeriods)) else this
    def withGroupOrPartners(groupOrPartners: Boolean): ChangeIndicators = copy(groupOrPartners = groupOrPartners)
    def modifyGroupOrPartners(pf: PartialFunction[Boolean, Boolean]): ChangeIndicators =
      if (pf.isDefinedAt(groupOrPartners)) copy(groupOrPartners = pf(groupOrPartners)) else this
  }

  object ChangeIndicators extends RecordUtils[ChangeIndicators] {

    override val validate: Validator[ChangeIndicators] = Validator()

    override val gen: Gen[ChangeIndicators] = for {
      customerDetails       <- Generator.booleanGen
      ppobdetails           <- Generator.booleanGen
      correspContactDetails <- Generator.booleanGen
      bankDetails           <- Generator.booleanGen
      businessActivities    <- Generator.booleanGen
      flatRateScheme        <- Generator.booleanGen
      deRegistrationInfo    <- Generator.booleanGen
      returnPeriods         <- Generator.booleanGen
      groupOrPartners       <- Generator.booleanGen
    } yield
      ChangeIndicators(
        customerDetails = customerDetails,
        PPOBDetails = ppobdetails,
        correspContactDetails = correspContactDetails,
        bankDetails = bankDetails,
        businessActivities = businessActivities,
        flatRateScheme = flatRateScheme,
        deRegistrationInfo = deRegistrationInfo,
        returnPeriods = returnPeriods,
        groupOrPartners = groupOrPartners
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[ChangeIndicators] = Json.format[ChangeIndicators]

  }

  case class ContactDetails(
    primaryPhoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None) {

    def withPrimaryPhoneNumber(primaryPhoneNumber: Option[String]): ContactDetails =
      copy(primaryPhoneNumber = primaryPhoneNumber)
    def modifyPrimaryPhoneNumber(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(primaryPhoneNumber)) copy(primaryPhoneNumber = pf(primaryPhoneNumber)) else this
    def withMobileNumber(mobileNumber: Option[String]): ContactDetails = copy(mobileNumber = mobileNumber)
    def modifyMobileNumber(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(mobileNumber)) copy(mobileNumber = pf(mobileNumber)) else this
    def withFaxNumber(faxNumber: Option[String]): ContactDetails = copy(faxNumber = faxNumber)
    def modifyFaxNumber(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(faxNumber)) copy(faxNumber = pf(faxNumber)) else this
    def withEmailAddress(emailAddress: Option[String]): ContactDetails = copy(emailAddress = emailAddress)
    def modifyEmailAddress(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(emailAddress)) copy(emailAddress = pf(emailAddress)) else this
  }

  object ContactDetails extends RecordUtils[ContactDetails] {

    override val validate: Validator[ContactDetails] = Validator(
      check(
        _.primaryPhoneNumber.matches(Common.primaryPhoneNumberPattern),
        s"""Invalid primaryPhoneNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""
      ),
      check(
        _.mobileNumber.matches(Common.primaryPhoneNumberPattern),
        s"""Invalid mobileNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""),
      check(
        _.faxNumber.matches(Common.primaryPhoneNumberPattern),
        s"""Invalid faxNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""),
      check(
        _.emailAddress.lengthMinMaxInclusive(3, 132),
        "Invalid length of emailAddress, should be between 3 and 132 inclusive")
    )

    override val gen: Gen[ContactDetails] = Gen const ContactDetails(
      )

    val primaryPhoneNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          primaryPhoneNumber =
            entity.primaryPhoneNumber.orElse(Generator.get(Generator.ukPhoneNumber.variant("primary"))(seed)))

    val mobileNumberSanitizer: Update = seed =>
      entity => entity.copy(mobileNumber = entity.mobileNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val faxNumberSanitizer: Update = seed =>
      entity => entity.copy(faxNumber = entity.faxNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity => entity.copy(emailAddress = entity.emailAddress.orElse(Generator.get(Generator.emailGen)(seed)))

    override val sanitizers: Seq[Update] =
      Seq(primaryPhoneNumberSanitizer, mobileNumberSanitizer, faxNumberSanitizer, emailAddressSanitizer)

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]

  }

  case class CorrespondenceContactDetails(
    address: Address,
    RLS: Option[String] = None,
    contactDetails: Option[ContactDetails] = None) {

    def withAddress(address: Address): CorrespondenceContactDetails = copy(address = address)
    def modifyAddress(pf: PartialFunction[Address, Address]): CorrespondenceContactDetails =
      if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
    def withRLS(RLS: Option[String]): CorrespondenceContactDetails = copy(RLS = RLS)
    def modifyRLS(pf: PartialFunction[Option[String], Option[String]]): CorrespondenceContactDetails =
      if (pf.isDefinedAt(RLS)) copy(RLS = pf(RLS)) else this
    def withContactDetails(contactDetails: Option[ContactDetails]): CorrespondenceContactDetails =
      copy(contactDetails = contactDetails)
    def modifyContactDetails(
      pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]): CorrespondenceContactDetails =
      if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
  }

  object CorrespondenceContactDetails extends RecordUtils[CorrespondenceContactDetails] {

    override val validate: Validator[CorrespondenceContactDetails] = Validator(
      checkObject(_.address, Address.validate),
      check(_.RLS.isOneOf(Common.RLSEnum), "Invalid RLS, does not match allowed values"),
      checkObjectIfSome(_.contactDetails, ContactDetails.validate)
    )

    override val gen: Gen[CorrespondenceContactDetails] = for {
      address <- Address.gen
    } yield
      CorrespondenceContactDetails(
        address = address
      )

    val RLSSanitizer: Update = seed =>
      entity => entity.copy(RLS = entity.RLS.orElse(Generator.get(Gen.oneOf(Common.RLSEnum))(seed)))

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails =
            entity.contactDetails.orElse(Generator.get(ContactDetails.gen)(seed)).map(ContactDetails.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(RLSSanitizer, contactDetailsSanitizer)

    implicit val formats: Format[CorrespondenceContactDetails] = Json.format[CorrespondenceContactDetails]

  }

  case class CustomerDetails(
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    dateOfBirth: Option[String] = None,
    tradingName: Option[String] = None,
    mandationStatus: String,
    registrationReason: Option[String] = None,
    effectiveRegistrationDate: Option[String] = None,
    businessStartDate: Option[String] = None) {

    def withOrganisationName(organisationName: Option[String]): CustomerDetails =
      copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withIndividual(individual: Option[IndividualName]): CustomerDetails = copy(individual = individual)
    def modifyIndividual(pf: PartialFunction[Option[IndividualName], Option[IndividualName]]): CustomerDetails =
      if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
    def withDateOfBirth(dateOfBirth: Option[String]): CustomerDetails = copy(dateOfBirth = dateOfBirth)
    def modifyDateOfBirth(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(dateOfBirth)) copy(dateOfBirth = pf(dateOfBirth)) else this
    def withTradingName(tradingName: Option[String]): CustomerDetails = copy(tradingName = tradingName)
    def modifyTradingName(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(tradingName)) copy(tradingName = pf(tradingName)) else this
    def withMandationStatus(mandationStatus: String): CustomerDetails = copy(mandationStatus = mandationStatus)
    def modifyMandationStatus(pf: PartialFunction[String, String]): CustomerDetails =
      if (pf.isDefinedAt(mandationStatus)) copy(mandationStatus = pf(mandationStatus)) else this
    def withRegistrationReason(registrationReason: Option[String]): CustomerDetails =
      copy(registrationReason = registrationReason)
    def modifyRegistrationReason(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(registrationReason)) copy(registrationReason = pf(registrationReason)) else this
    def withEffectiveRegistrationDate(effectiveRegistrationDate: Option[String]): CustomerDetails =
      copy(effectiveRegistrationDate = effectiveRegistrationDate)
    def modifyEffectiveRegistrationDate(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(effectiveRegistrationDate)) copy(effectiveRegistrationDate = pf(effectiveRegistrationDate))
      else this
    def withBusinessStartDate(businessStartDate: Option[String]): CustomerDetails =
      copy(businessStartDate = businessStartDate)
    def modifyBusinessStartDate(pf: PartialFunction[Option[String], Option[String]]): CustomerDetails =
      if (pf.isDefinedAt(businessStartDate)) copy(businessStartDate = pf(businessStartDate)) else this
  }

  object CustomerDetails extends RecordUtils[CustomerDetails] {

    override val validate: Validator[CustomerDetails] = Validator(
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      checkObjectIfSome(_.individual, IndividualName.validate),
      check(
        _.dateOfBirth.matches(Common.dateOfBirthPattern),
        s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive"),
      check(_.mandationStatus.isOneOf(Common.actionEnum), "Invalid mandationStatus, does not match allowed values"),
      check(
        _.registrationReason.isOneOf(Common.registrationReasonEnum),
        "Invalid registrationReason, does not match allowed values"),
      check(
        _.effectiveRegistrationDate.matches(Common.dateOfBirthPattern),
        s"""Invalid effectiveRegistrationDate, does not matches regex ${Common.dateOfBirthPattern}"""
      ),
      check(
        _.businessStartDate.matches(Common.dateOfBirthPattern),
        s"""Invalid businessStartDate, does not matches regex ${Common.dateOfBirthPattern}"""),
      checkIfAtLeastOneIsDefined(Seq(_.individual, _.organisationName))
    )

    override val gen: Gen[CustomerDetails] = for {
      mandationStatus <- Gen.oneOf(Common.actionEnum)
    } yield
      CustomerDetails(
        mandationStatus = mandationStatus
      )

    val organisationNameSanitizer: Update = seed =>
      entity => entity.copy(organisationName = entity.organisationName.orElse(Generator.get(Generator.company)(seed)))

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual =
            entity.individual.orElse(Generator.get(IndividualName.gen)(seed)).map(IndividualName.sanitize(seed)))

    val dateOfBirthSanitizer: Update = seed =>
      entity =>
        entity.copy(
          dateOfBirth = entity.dateOfBirth.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("ofbirth"))(seed)))

    val tradingNameSanitizer: Update = seed =>
      entity => entity.copy(tradingName = entity.tradingName.orElse(Generator.get(Generator.tradingNameGen)(seed)))

    val registrationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          registrationReason =
            entity.registrationReason.orElse(Generator.get(Gen.oneOf(Common.registrationReasonEnum))(seed)))

    val effectiveRegistrationDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          effectiveRegistrationDate = entity.effectiveRegistrationDate.orElse(
            Generator.get(Generator.dateYYYYMMDDGen.variant("effectiveregistration"))(seed)))

    val businessStartDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessStartDate =
            entity.businessStartDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("businessstart"))(seed)))

    val individualOrOrganisationNameSanitizer: Update = seed =>
      entity =>
        entity.individual
          .orElse(entity.organisationName)
          .map(_ => entity)
          .getOrElse(
            Generator.get(Gen.chooseNum(0, 1))(seed) match {
              case Some(0) => individualSanitizer(seed)(entity)
              case _       => organisationNameSanitizer(seed)(entity)
            }
      )

    override val sanitizers: Seq[Update] = Seq(
      dateOfBirthSanitizer,
      tradingNameSanitizer,
      registrationReasonSanitizer,
      effectiveRegistrationDateSanitizer,
      businessStartDateSanitizer,
      individualOrOrganisationNameSanitizer
    )

    implicit val formats: Format[CustomerDetails] = Json.format[CustomerDetails]

  }

  case class Deregistration(
    deregistrationReason: Option[String] = None,
    effectDateOfCancellation: Option[String] = None,
    lastReturnDueDate: Option[String] = None) {

    def withDeregistrationReason(deregistrationReason: Option[String]): Deregistration =
      copy(deregistrationReason = deregistrationReason)
    def modifyDeregistrationReason(pf: PartialFunction[Option[String], Option[String]]): Deregistration =
      if (pf.isDefinedAt(deregistrationReason)) copy(deregistrationReason = pf(deregistrationReason)) else this
    def withEffectDateOfCancellation(effectDateOfCancellation: Option[String]): Deregistration =
      copy(effectDateOfCancellation = effectDateOfCancellation)
    def modifyEffectDateOfCancellation(pf: PartialFunction[Option[String], Option[String]]): Deregistration =
      if (pf.isDefinedAt(effectDateOfCancellation)) copy(effectDateOfCancellation = pf(effectDateOfCancellation))
      else this
    def withLastReturnDueDate(lastReturnDueDate: Option[String]): Deregistration =
      copy(lastReturnDueDate = lastReturnDueDate)
    def modifyLastReturnDueDate(pf: PartialFunction[Option[String], Option[String]]): Deregistration =
      if (pf.isDefinedAt(lastReturnDueDate)) copy(lastReturnDueDate = pf(lastReturnDueDate)) else this
  }

  object Deregistration extends RecordUtils[Deregistration] {

    override val validate: Validator[Deregistration] = Validator(
      check(
        _.deregistrationReason.isOneOf(Common.deregistrationReasonEnum),
        "Invalid deregistrationReason, does not match allowed values"),
      check(
        _.effectDateOfCancellation.matches(Common.dateOfBirthPattern),
        s"""Invalid effectDateOfCancellation, does not matches regex ${Common.dateOfBirthPattern}"""
      ),
      check(
        _.lastReturnDueDate.matches(Common.dateOfBirthPattern),
        s"""Invalid lastReturnDueDate, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[Deregistration] = Gen const Deregistration(
      )

    val deregistrationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregistrationReason =
            entity.deregistrationReason.orElse(Generator.get(Gen.oneOf(Common.deregistrationReasonEnum))(seed)))

    val effectDateOfCancellationSanitizer: Update = seed =>
      entity =>
        entity.copy(
          effectDateOfCancellation = entity.effectDateOfCancellation.orElse(
            Generator.get(Generator.dateYYYYMMDDGen.variant("effect-ofcancellation"))(seed)))

    val lastReturnDueDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          lastReturnDueDate =
            entity.lastReturnDueDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("lastreturndue"))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(deregistrationReasonSanitizer, effectDateOfCancellationSanitizer, lastReturnDueDateSanitizer)

    implicit val formats: Format[Deregistration] = Json.format[Deregistration]

  }

  case class FlatRateScheme(
    FRSCategory: Option[String] = None,
    FRSPercentage: Option[BigDecimal] = None,
    startDate: Option[String] = None,
    limitedCostTrader: Option[Boolean] = None) {

    def withFRSCategory(FRSCategory: Option[String]): FlatRateScheme = copy(FRSCategory = FRSCategory)
    def modifyFRSCategory(pf: PartialFunction[Option[String], Option[String]]): FlatRateScheme =
      if (pf.isDefinedAt(FRSCategory)) copy(FRSCategory = pf(FRSCategory)) else this
    def withFRSPercentage(FRSPercentage: Option[BigDecimal]): FlatRateScheme = copy(FRSPercentage = FRSPercentage)
    def modifyFRSPercentage(pf: PartialFunction[Option[BigDecimal], Option[BigDecimal]]): FlatRateScheme =
      if (pf.isDefinedAt(FRSPercentage)) copy(FRSPercentage = pf(FRSPercentage)) else this
    def withStartDate(startDate: Option[String]): FlatRateScheme = copy(startDate = startDate)
    def modifyStartDate(pf: PartialFunction[Option[String], Option[String]]): FlatRateScheme =
      if (pf.isDefinedAt(startDate)) copy(startDate = pf(startDate)) else this
    def withLimitedCostTrader(limitedCostTrader: Option[Boolean]): FlatRateScheme =
      copy(limitedCostTrader = limitedCostTrader)
    def modifyLimitedCostTrader(pf: PartialFunction[Option[Boolean], Option[Boolean]]): FlatRateScheme =
      if (pf.isDefinedAt(limitedCostTrader)) copy(limitedCostTrader = pf(limitedCostTrader)) else this
  }

  object FlatRateScheme extends RecordUtils[FlatRateScheme] {

    override val validate: Validator[FlatRateScheme] = Validator(
      check(_.FRSCategory.isOneOf(Common.FRSCategoryEnum), "Invalid FRSCategory, does not match allowed values"),
      check(
        _.FRSPercentage.inRange(BigDecimal(0), BigDecimal(999.99), Some(BigDecimal(0.01))),
        "Invalid number FRSPercentage, must be in range <0,999.99>"),
      check(
        _.startDate.matches(Common.dateOfBirthPattern),
        s"""Invalid startDate, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[FlatRateScheme] = Gen const FlatRateScheme(
      )

    val FRSCategorySanitizer: Update = seed =>
      entity =>
        entity.copy(FRSCategory = entity.FRSCategory.orElse(Generator.get(Gen.oneOf(Common.FRSCategoryEnum))(seed)))

    val FRSPercentageSanitizer: Update = seed =>
      entity =>
        entity.copy(
          FRSPercentage =
            entity.FRSPercentage.orElse(Generator.get(Generator.chooseBigDecimal(0, 999.99, Some(0.01)))(seed)))

    val startDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          startDate = entity.startDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("start"))(seed)))

    val limitedCostTraderSanitizer: Update = seed =>
      entity =>
        entity.copy(limitedCostTrader = entity.limitedCostTrader.orElse(Generator.get(Generator.booleanGen)(seed)))

    override val sanitizers: Seq[Update] =
      Seq(FRSCategorySanitizer, FRSPercentageSanitizer, startDateSanitizer, limitedCostTraderSanitizer)

    implicit val formats: Format[FlatRateScheme] = Json.format[FlatRateScheme]

  }

  case class ForeignAddress(
    override val line1: String,
    override val line2: String,
    override val line3: Option[String] = None,
    override val line4: Option[String] = None,
    postCode: Option[String] = None,
    override val countryCode: String)
      extends Address {

    def withLine1(line1: String): ForeignAddress = copy(line1 = line1)
    def modifyLine1(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(line1)) copy(line1 = pf(line1)) else this
    def withLine2(line2: String): ForeignAddress = copy(line2 = line2)
    def modifyLine2(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(line2)) copy(line2 = pf(line2)) else this
    def withLine3(line3: Option[String]): ForeignAddress = copy(line3 = line3)
    def modifyLine3(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(line3)) copy(line3 = pf(line3)) else this
    def withLine4(line4: Option[String]): ForeignAddress = copy(line4 = line4)
    def modifyLine4(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(line4)) copy(line4 = pf(line4)) else this
    def withPostCode(postCode: Option[String]): ForeignAddress = copy(postCode = postCode)
    def modifyPostCode(pf: PartialFunction[Option[String], Option[String]]): ForeignAddress =
      if (pf.isDefinedAt(postCode)) copy(postCode = pf(postCode)) else this
    def withCountryCode(countryCode: String): ForeignAddress = copy(countryCode = countryCode)
    def modifyCountryCode(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(countryCode)) copy(countryCode = pf(countryCode)) else this
  }

  object ForeignAddress extends RecordUtils[ForeignAddress] {

    override val validate: Validator[ForeignAddress] = Validator(
      check(_.line1.matches(Common.linePattern), s"""Invalid line1, does not matches regex ${Common.linePattern}"""),
      check(_.line2.matches(Common.linePattern), s"""Invalid line2, does not matches regex ${Common.linePattern}"""),
      check(_.line3.matches(Common.linePattern), s"""Invalid line3, does not matches regex ${Common.linePattern}"""),
      check(_.line4.matches(Common.linePattern), s"""Invalid line4, does not matches regex ${Common.linePattern}"""),
      check(
        _.postCode.matches(Common.postCodePattern),
        s"""Invalid postCode, does not matches regex ${Common.postCodePattern}"""),
      check(_.countryCode.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")
    )

    override val gen: Gen[ForeignAddress] = for {
      line1       <- Generator.address4Lines35Gen.map(_.line1)
      line2       <- Generator.address4Lines35Gen.map(_.line2)
      countryCode <- Gen.oneOf(Common.countryCodeEnum0)
    } yield
      ForeignAddress(
        line1 = line1,
        line2 = line2,
        countryCode = countryCode
      )

    val line3Sanitizer: Update = seed =>
      entity => entity.copy(line3 = entity.line3.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed)))

    val line4Sanitizer: Update = seed =>
      entity => entity.copy(line4 = entity.line4.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed)))

    val postCodeSanitizer: Update = seed =>
      entity => entity.copy(postCode = entity.postCode.orElse(Generator.get(Generator.postcode)(seed)))

    override val sanitizers: Seq[Update] = Seq(line3Sanitizer, line4Sanitizer, postCodeSanitizer)

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class FormInformation(formBundle: String, dateReceived: String) {

    def withFormBundle(formBundle: String): FormInformation = copy(formBundle = formBundle)
    def modifyFormBundle(pf: PartialFunction[String, String]): FormInformation =
      if (pf.isDefinedAt(formBundle)) copy(formBundle = pf(formBundle)) else this
    def withDateReceived(dateReceived: String): FormInformation = copy(dateReceived = dateReceived)
    def modifyDateReceived(pf: PartialFunction[String, String]): FormInformation =
      if (pf.isDefinedAt(dateReceived)) copy(dateReceived = pf(dateReceived)) else this
  }

  object FormInformation extends RecordUtils[FormInformation] {

    override val validate: Validator[FormInformation] = Validator(
      check(
        _.formBundle.matches(Common.formBundlePattern),
        s"""Invalid formBundle, does not matches regex ${Common.formBundlePattern}"""),
      check(
        _.dateReceived.matches(Common.dateOfBirthPattern),
        s"""Invalid dateReceived, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[FormInformation] = for {
      formBundle   <- Generator.regex(Common.formBundlePattern)
      dateReceived <- Generator.dateYYYYMMDDGen.variant("received")
    } yield
      FormInformation(
        formBundle = formBundle,
        dateReceived = dateReceived
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[FormInformation] = Json.format[FormInformation]

  }

  case class GroupOrPartner(
    typeOfRelationship: String,
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    SAP_Number: String) {

    def withTypeOfRelationship(typeOfRelationship: String): GroupOrPartner =
      copy(typeOfRelationship = typeOfRelationship)
    def modifyTypeOfRelationship(pf: PartialFunction[String, String]): GroupOrPartner =
      if (pf.isDefinedAt(typeOfRelationship)) copy(typeOfRelationship = pf(typeOfRelationship)) else this
    def withOrganisationName(organisationName: Option[String]): GroupOrPartner =
      copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[Option[String], Option[String]]): GroupOrPartner =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withIndividual(individual: Option[IndividualName]): GroupOrPartner = copy(individual = individual)
    def modifyIndividual(pf: PartialFunction[Option[IndividualName], Option[IndividualName]]): GroupOrPartner =
      if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
    def withSAP_Number(SAP_Number: String): GroupOrPartner = copy(SAP_Number = SAP_Number)
    def modifySAP_Number(pf: PartialFunction[String, String]): GroupOrPartner =
      if (pf.isDefinedAt(SAP_Number)) copy(SAP_Number = pf(SAP_Number)) else this
  }

  object GroupOrPartner extends RecordUtils[GroupOrPartner] {

    override val validate: Validator[GroupOrPartner] = Validator(
      check(
        _.typeOfRelationship.isOneOf(Common.typeOfRelationshipEnum),
        "Invalid typeOfRelationship, does not match allowed values"),
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      checkObjectIfSome(_.individual, IndividualName.validate),
      check(
        _.SAP_Number.matches(Common.SAP_NumberPattern),
        s"""Invalid SAP_Number, does not matches regex ${Common.SAP_NumberPattern}""")
    )

    override val gen: Gen[GroupOrPartner] = for {
      typeOfRelationship <- Gen.oneOf(Common.typeOfRelationshipEnum)
      sap_number         <- Generator.regex(Common.SAP_NumberPattern)
    } yield
      GroupOrPartner(
        typeOfRelationship = typeOfRelationship,
        SAP_Number = sap_number
      )

    val organisationNameSanitizer: Update = seed =>
      entity => entity.copy(organisationName = entity.organisationName.orElse(Generator.get(Generator.company)(seed)))

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual =
            entity.individual.orElse(Generator.get(IndividualName.gen)(seed)).map(IndividualName.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(organisationNameSanitizer, individualSanitizer)

    implicit val formats: Format[GroupOrPartner] = Json.format[GroupOrPartner]

  }

  case class InFlightBankDetails(
    formInformation: FormInformation,
    IBAN: Option[String] = None,
    BIC: Option[String] = None,
    accountHolderName: Option[String] = None,
    bankAccountNumber: Option[String] = None,
    sortCode: Option[String] = None,
    buildingSocietyNumber: Option[String] = None,
    bankBuildSocietyName: Option[String] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightBankDetails =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightBankDetails =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withIBAN(IBAN: Option[String]): InFlightBankDetails = copy(IBAN = IBAN)
    def modifyIBAN(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(IBAN)) copy(IBAN = pf(IBAN)) else this
    def withBIC(BIC: Option[String]): InFlightBankDetails = copy(BIC = BIC)
    def modifyBIC(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(BIC)) copy(BIC = pf(BIC)) else this
    def withAccountHolderName(accountHolderName: Option[String]): InFlightBankDetails =
      copy(accountHolderName = accountHolderName)
    def modifyAccountHolderName(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(accountHolderName)) copy(accountHolderName = pf(accountHolderName)) else this
    def withBankAccountNumber(bankAccountNumber: Option[String]): InFlightBankDetails =
      copy(bankAccountNumber = bankAccountNumber)
    def modifyBankAccountNumber(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(bankAccountNumber)) copy(bankAccountNumber = pf(bankAccountNumber)) else this
    def withSortCode(sortCode: Option[String]): InFlightBankDetails = copy(sortCode = sortCode)
    def modifySortCode(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(sortCode)) copy(sortCode = pf(sortCode)) else this
    def withBuildingSocietyNumber(buildingSocietyNumber: Option[String]): InFlightBankDetails =
      copy(buildingSocietyNumber = buildingSocietyNumber)
    def modifyBuildingSocietyNumber(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(buildingSocietyNumber)) copy(buildingSocietyNumber = pf(buildingSocietyNumber)) else this
    def withBankBuildSocietyName(bankBuildSocietyName: Option[String]): InFlightBankDetails =
      copy(bankBuildSocietyName = bankBuildSocietyName)
    def modifyBankBuildSocietyName(pf: PartialFunction[Option[String], Option[String]]): InFlightBankDetails =
      if (pf.isDefinedAt(bankBuildSocietyName)) copy(bankBuildSocietyName = pf(bankBuildSocietyName)) else this
  }

  object InFlightBankDetails extends RecordUtils[InFlightBankDetails] {

    override val validate: Validator[InFlightBankDetails] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(_.IBAN.lengthMinMaxInclusive(1, 34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
      check(_.BIC.lengthMinMaxInclusive(1, 11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
      check(
        _.accountHolderName.lengthMinMaxInclusive(1, 60),
        "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
      check(
        _.bankAccountNumber.matches(Common.bankAccountNumberPattern),
        s"""Invalid bankAccountNumber, does not matches regex ${Common.bankAccountNumberPattern}"""
      ),
      check(
        _.sortCode.matches(Common.sortCodePattern),
        s"""Invalid sortCode, does not matches regex ${Common.sortCodePattern}"""),
      check(
        _.buildingSocietyNumber.lengthMinMaxInclusive(1, 20),
        "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
      check(
        _.bankBuildSocietyName.lengthMinMaxInclusive(1, 40),
        "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
    )

    override val gen: Gen[InFlightBankDetails] = for {
      formInformation <- FormInformation.gen
    } yield
      InFlightBankDetails(
        formInformation = formInformation
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val IBANSanitizer: Update = seed =>
      entity => entity.copy(IBAN = entity.IBAN.orElse(Generator.get(Generator.stringMinMaxN(1, 34))(seed)))

    val BICSanitizer: Update = seed =>
      entity => entity.copy(BIC = entity.BIC.orElse(Generator.get(Generator.stringMinMaxN(1, 11))(seed)))

    val accountHolderNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          accountHolderName = entity.accountHolderName.orElse(Generator.get(Generator.stringMinMaxN(1, 60))(seed)))

    val bankAccountNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankAccountNumber =
            entity.bankAccountNumber.orElse(Generator.get(Generator.regex(Common.bankAccountNumberPattern))(seed)))

    val sortCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(sortCode = entity.sortCode.orElse(Generator.get(Generator.regex(Common.sortCodePattern))(seed)))

    val buildingSocietyNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          buildingSocietyNumber =
            entity.buildingSocietyNumber.orElse(Generator.get(Generator.stringMinMaxN(1, 20))(seed)))

    val bankBuildSocietyNameSanitizer: Update = seed =>
      entity =>
        entity.copy(bankBuildSocietyName =
          entity.bankBuildSocietyName.orElse(Generator.get(Generator.stringMinMaxN(1, 40))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      formInformationSanitizer,
      IBANSanitizer,
      BICSanitizer,
      accountHolderNameSanitizer,
      bankAccountNumberSanitizer,
      sortCodeSanitizer,
      buildingSocietyNumberSanitizer,
      bankBuildSocietyNameSanitizer
    )

    implicit val formats: Format[InFlightBankDetails] = Json.format[InFlightBankDetails]

  }

  case class InFlightBusinessActivities(
    formInformation: FormInformation,
    primaryMainCode: String,
    mainCode2: Option[String] = None,
    mainCode3: Option[String] = None,
    mainCode4: Option[String] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightBusinessActivities =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightBusinessActivities =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withPrimaryMainCode(primaryMainCode: String): InFlightBusinessActivities =
      copy(primaryMainCode = primaryMainCode)
    def modifyPrimaryMainCode(pf: PartialFunction[String, String]): InFlightBusinessActivities =
      if (pf.isDefinedAt(primaryMainCode)) copy(primaryMainCode = pf(primaryMainCode)) else this
    def withMainCode2(mainCode2: Option[String]): InFlightBusinessActivities = copy(mainCode2 = mainCode2)
    def modifyMainCode2(pf: PartialFunction[Option[String], Option[String]]): InFlightBusinessActivities =
      if (pf.isDefinedAt(mainCode2)) copy(mainCode2 = pf(mainCode2)) else this
    def withMainCode3(mainCode3: Option[String]): InFlightBusinessActivities = copy(mainCode3 = mainCode3)
    def modifyMainCode3(pf: PartialFunction[Option[String], Option[String]]): InFlightBusinessActivities =
      if (pf.isDefinedAt(mainCode3)) copy(mainCode3 = pf(mainCode3)) else this
    def withMainCode4(mainCode4: Option[String]): InFlightBusinessActivities = copy(mainCode4 = mainCode4)
    def modifyMainCode4(pf: PartialFunction[Option[String], Option[String]]): InFlightBusinessActivities =
      if (pf.isDefinedAt(mainCode4)) copy(mainCode4 = pf(mainCode4)) else this
  }

  object InFlightBusinessActivities extends RecordUtils[InFlightBusinessActivities] {

    override val validate: Validator[InFlightBusinessActivities] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(
        _.primaryMainCode.matches(Common.primaryMainCodePattern),
        s"""Invalid primaryMainCode, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode2.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode2, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode3.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode3, does not matches regex ${Common.primaryMainCodePattern}"""),
      check(
        _.mainCode4.matches(Common.primaryMainCodePattern),
        s"""Invalid mainCode4, does not matches regex ${Common.primaryMainCodePattern}""")
    )

    override val gen: Gen[InFlightBusinessActivities] = for {
      formInformation <- FormInformation.gen
      primaryMainCode <- Generator.regex(Common.primaryMainCodePattern)
    } yield
      InFlightBusinessActivities(
        formInformation = formInformation,
        primaryMainCode = primaryMainCode
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val mainCode2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode2 = entity.mainCode2.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    val mainCode3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode3 = entity.mainCode3.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    val mainCode4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode4 = entity.mainCode4.orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(formInformationSanitizer, mainCode2Sanitizer, mainCode3Sanitizer, mainCode4Sanitizer)

    implicit val formats: Format[InFlightBusinessActivities] = Json.format[InFlightBusinessActivities]

  }

  case class InFlightCorrespondenceContactDetails(
    formInformation: FormInformation,
    address: Option[Address] = None,
    contactDetails: Option[ContactDetails] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightCorrespondenceContactDetails =
      copy(formInformation = formInformation)
    def modifyFormInformation(
      pf: PartialFunction[FormInformation, FormInformation]): InFlightCorrespondenceContactDetails =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withAddress(address: Option[Address]): InFlightCorrespondenceContactDetails = copy(address = address)
    def modifyAddress(pf: PartialFunction[Option[Address], Option[Address]]): InFlightCorrespondenceContactDetails =
      if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
    def withContactDetails(contactDetails: Option[ContactDetails]): InFlightCorrespondenceContactDetails =
      copy(contactDetails = contactDetails)
    def modifyContactDetails(
      pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]): InFlightCorrespondenceContactDetails =
      if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
  }

  object InFlightCorrespondenceContactDetails extends RecordUtils[InFlightCorrespondenceContactDetails] {

    override val validate: Validator[InFlightCorrespondenceContactDetails] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      checkObjectIfSome(_.address, Address.validate),
      checkObjectIfSome(_.contactDetails, ContactDetails.validate)
    )

    override val gen: Gen[InFlightCorrespondenceContactDetails] = for {
      formInformation <- FormInformation.gen
    } yield
      InFlightCorrespondenceContactDetails(
        formInformation = formInformation
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val addressSanitizer: Update = seed =>
      entity =>
        entity.copy(address = entity.address.orElse(Generator.get(Address.gen)(seed)).map(Address.sanitize(seed)))

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails =
            entity.contactDetails.orElse(Generator.get(ContactDetails.gen)(seed)).map(ContactDetails.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(formInformationSanitizer, addressSanitizer, contactDetailsSanitizer)

    implicit val formats: Format[InFlightCorrespondenceContactDetails] =
      Json.format[InFlightCorrespondenceContactDetails]

  }

  case class InFlightCustomerDetails(
    formInformation: FormInformation,
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    dateOfBirth: Option[String] = None,
    tradingName: Option[String] = None,
    mandationStatus: String,
    registrationReason: Option[String] = None,
    effectiveRegistrationDate: Option[String] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightCustomerDetails =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightCustomerDetails =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withOrganisationName(organisationName: Option[String]): InFlightCustomerDetails =
      copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[Option[String], Option[String]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withIndividual(individual: Option[IndividualName]): InFlightCustomerDetails = copy(individual = individual)
    def modifyIndividual(pf: PartialFunction[Option[IndividualName], Option[IndividualName]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
    def withDateOfBirth(dateOfBirth: Option[String]): InFlightCustomerDetails = copy(dateOfBirth = dateOfBirth)
    def modifyDateOfBirth(pf: PartialFunction[Option[String], Option[String]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(dateOfBirth)) copy(dateOfBirth = pf(dateOfBirth)) else this
    def withTradingName(tradingName: Option[String]): InFlightCustomerDetails = copy(tradingName = tradingName)
    def modifyTradingName(pf: PartialFunction[Option[String], Option[String]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(tradingName)) copy(tradingName = pf(tradingName)) else this
    def withMandationStatus(mandationStatus: String): InFlightCustomerDetails = copy(mandationStatus = mandationStatus)
    def modifyMandationStatus(pf: PartialFunction[String, String]): InFlightCustomerDetails =
      if (pf.isDefinedAt(mandationStatus)) copy(mandationStatus = pf(mandationStatus)) else this
    def withRegistrationReason(registrationReason: Option[String]): InFlightCustomerDetails =
      copy(registrationReason = registrationReason)
    def modifyRegistrationReason(pf: PartialFunction[Option[String], Option[String]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(registrationReason)) copy(registrationReason = pf(registrationReason)) else this
    def withEffectiveRegistrationDate(effectiveRegistrationDate: Option[String]): InFlightCustomerDetails =
      copy(effectiveRegistrationDate = effectiveRegistrationDate)
    def modifyEffectiveRegistrationDate(pf: PartialFunction[Option[String], Option[String]]): InFlightCustomerDetails =
      if (pf.isDefinedAt(effectiveRegistrationDate)) copy(effectiveRegistrationDate = pf(effectiveRegistrationDate))
      else this
  }

  object InFlightCustomerDetails extends RecordUtils[InFlightCustomerDetails] {

    override val validate: Validator[InFlightCustomerDetails] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      checkObjectIfSome(_.individual, IndividualName.validate),
      check(
        _.dateOfBirth.matches(Common.dateOfBirthPattern),
        s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive"),
      check(_.mandationStatus.isOneOf(Common.actionEnum), "Invalid mandationStatus, does not match allowed values"),
      check(
        _.registrationReason.isOneOf(Common.registrationReasonEnum),
        "Invalid registrationReason, does not match allowed values"),
      check(
        _.effectiveRegistrationDate.matches(Common.dateOfBirthPattern),
        s"""Invalid effectiveRegistrationDate, does not matches regex ${Common.dateOfBirthPattern}"""
      )
    )

    override val gen: Gen[InFlightCustomerDetails] = for {
      formInformation <- FormInformation.gen
      mandationStatus <- Gen.oneOf(Common.actionEnum)
    } yield
      InFlightCustomerDetails(
        formInformation = formInformation,
        mandationStatus = mandationStatus
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val organisationNameSanitizer: Update = seed =>
      entity => entity.copy(organisationName = entity.organisationName.orElse(Generator.get(Generator.company)(seed)))

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual =
            entity.individual.orElse(Generator.get(IndividualName.gen)(seed)).map(IndividualName.sanitize(seed)))

    val dateOfBirthSanitizer: Update = seed =>
      entity =>
        entity.copy(
          dateOfBirth = entity.dateOfBirth.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("ofbirth"))(seed)))

    val tradingNameSanitizer: Update = seed =>
      entity => entity.copy(tradingName = entity.tradingName.orElse(Generator.get(Generator.tradingNameGen)(seed)))

    val registrationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          registrationReason =
            entity.registrationReason.orElse(Generator.get(Gen.oneOf(Common.registrationReasonEnum))(seed)))

    val effectiveRegistrationDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          effectiveRegistrationDate = entity.effectiveRegistrationDate.orElse(
            Generator.get(Generator.dateYYYYMMDDGen.variant("effectiveregistration"))(seed)))

    override val sanitizers: Seq[Update] = Seq(
      formInformationSanitizer,
      organisationNameSanitizer,
      individualSanitizer,
      dateOfBirthSanitizer,
      tradingNameSanitizer,
      registrationReasonSanitizer,
      effectiveRegistrationDateSanitizer
    )

    implicit val formats: Format[InFlightCustomerDetails] = Json.format[InFlightCustomerDetails]

  }

  case class InFlightDeregistration(
    formInformation: FormInformation,
    deregistrationReason: String,
    deregDate: Option[String] = None,
    deregDateInFuture: Option[String] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightDeregistration =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightDeregistration =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withDeregistrationReason(deregistrationReason: String): InFlightDeregistration =
      copy(deregistrationReason = deregistrationReason)
    def modifyDeregistrationReason(pf: PartialFunction[String, String]): InFlightDeregistration =
      if (pf.isDefinedAt(deregistrationReason)) copy(deregistrationReason = pf(deregistrationReason)) else this
    def withDeregDate(deregDate: Option[String]): InFlightDeregistration = copy(deregDate = deregDate)
    def modifyDeregDate(pf: PartialFunction[Option[String], Option[String]]): InFlightDeregistration =
      if (pf.isDefinedAt(deregDate)) copy(deregDate = pf(deregDate)) else this
    def withDeregDateInFuture(deregDateInFuture: Option[String]): InFlightDeregistration =
      copy(deregDateInFuture = deregDateInFuture)
    def modifyDeregDateInFuture(pf: PartialFunction[Option[String], Option[String]]): InFlightDeregistration =
      if (pf.isDefinedAt(deregDateInFuture)) copy(deregDateInFuture = pf(deregDateInFuture)) else this
  }

  object InFlightDeregistration extends RecordUtils[InFlightDeregistration] {

    override val validate: Validator[InFlightDeregistration] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(
        _.deregistrationReason.isOneOf(Common.deregistrationReasonEnum),
        "Invalid deregistrationReason, does not match allowed values"),
      check(
        _.deregDate.matches(Common.dateOfBirthPattern),
        s"""Invalid deregDate, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.deregDateInFuture.matches(Common.dateOfBirthPattern),
        s"""Invalid deregDateInFuture, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[InFlightDeregistration] = for {
      formInformation      <- FormInformation.gen
      deregistrationReason <- Gen.oneOf(Common.deregistrationReasonEnum)
    } yield
      InFlightDeregistration(
        formInformation = formInformation,
        deregistrationReason = deregistrationReason
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val deregDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregDate = entity.deregDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("dereg"))(seed)))

    val deregDateInFutureSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregDateInFuture =
            entity.deregDateInFuture.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("dereg-infuture"))(seed)))

    override val sanitizers: Seq[Update] = Seq(formInformationSanitizer, deregDateSanitizer, deregDateInFutureSanitizer)

    implicit val formats: Format[InFlightDeregistration] = Json.format[InFlightDeregistration]

  }

  case class InFlightFlatRateScheme(
    formInformation: FormInformation,
    FRSCategory: Option[String] = None,
    FRSPercentage: Option[BigDecimal] = None,
    startDate: Option[String] = None,
    limitedCostTrader: Option[Boolean] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightFlatRateScheme =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightFlatRateScheme =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withFRSCategory(FRSCategory: Option[String]): InFlightFlatRateScheme = copy(FRSCategory = FRSCategory)
    def modifyFRSCategory(pf: PartialFunction[Option[String], Option[String]]): InFlightFlatRateScheme =
      if (pf.isDefinedAt(FRSCategory)) copy(FRSCategory = pf(FRSCategory)) else this
    def withFRSPercentage(FRSPercentage: Option[BigDecimal]): InFlightFlatRateScheme =
      copy(FRSPercentage = FRSPercentage)
    def modifyFRSPercentage(pf: PartialFunction[Option[BigDecimal], Option[BigDecimal]]): InFlightFlatRateScheme =
      if (pf.isDefinedAt(FRSPercentage)) copy(FRSPercentage = pf(FRSPercentage)) else this
    def withStartDate(startDate: Option[String]): InFlightFlatRateScheme = copy(startDate = startDate)
    def modifyStartDate(pf: PartialFunction[Option[String], Option[String]]): InFlightFlatRateScheme =
      if (pf.isDefinedAt(startDate)) copy(startDate = pf(startDate)) else this
    def withLimitedCostTrader(limitedCostTrader: Option[Boolean]): InFlightFlatRateScheme =
      copy(limitedCostTrader = limitedCostTrader)
    def modifyLimitedCostTrader(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightFlatRateScheme =
      if (pf.isDefinedAt(limitedCostTrader)) copy(limitedCostTrader = pf(limitedCostTrader)) else this
  }

  object InFlightFlatRateScheme extends RecordUtils[InFlightFlatRateScheme] {

    override val validate: Validator[InFlightFlatRateScheme] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(_.FRSCategory.isOneOf(Common.FRSCategoryEnum), "Invalid FRSCategory, does not match allowed values"),
      check(
        _.FRSPercentage.inRange(BigDecimal(0), BigDecimal(999.99), Some(BigDecimal(0.01))),
        "Invalid number FRSPercentage, must be in range <0,999.99>"),
      check(
        _.startDate.matches(Common.dateOfBirthPattern),
        s"""Invalid startDate, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[InFlightFlatRateScheme] = for {
      formInformation <- FormInformation.gen
    } yield
      InFlightFlatRateScheme(
        formInformation = formInformation
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val FRSCategorySanitizer: Update = seed =>
      entity =>
        entity.copy(FRSCategory = entity.FRSCategory.orElse(Generator.get(Gen.oneOf(Common.FRSCategoryEnum))(seed)))

    val FRSPercentageSanitizer: Update = seed =>
      entity =>
        entity.copy(
          FRSPercentage =
            entity.FRSPercentage.orElse(Generator.get(Generator.chooseBigDecimal(0, 999.99, Some(0.01)))(seed)))

    val startDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          startDate = entity.startDate.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("start"))(seed)))

    val limitedCostTraderSanitizer: Update = seed =>
      entity =>
        entity.copy(limitedCostTrader = entity.limitedCostTrader.orElse(Generator.get(Generator.booleanGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(
      formInformationSanitizer,
      FRSCategorySanitizer,
      FRSPercentageSanitizer,
      startDateSanitizer,
      limitedCostTraderSanitizer)

    implicit val formats: Format[InFlightFlatRateScheme] = Json.format[InFlightFlatRateScheme]

  }

  case class InFlightGroupOrPartner(
    formInformation: FormInformation,
    action: String,
    SAP_Number: Option[String] = None,
    typeOfRelationship: Option[String] = None,
    makeGrpMember: Option[Boolean] = None,
    makeControllingBody: Option[Boolean] = None,
    isControllingBody: Option[Boolean] = None,
    organisationName: Option[String] = None,
    tradingName: Option[String] = None,
    individual: Option[IndividualName] = None,
    PPOB: Option[PPOB] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightGroupOrPartner =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withAction(action: String): InFlightGroupOrPartner = copy(action = action)
    def modifyAction(pf: PartialFunction[String, String]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(action)) copy(action = pf(action)) else this
    def withSAP_Number(SAP_Number: Option[String]): InFlightGroupOrPartner = copy(SAP_Number = SAP_Number)
    def modifySAP_Number(pf: PartialFunction[Option[String], Option[String]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(SAP_Number)) copy(SAP_Number = pf(SAP_Number)) else this
    def withTypeOfRelationship(typeOfRelationship: Option[String]): InFlightGroupOrPartner =
      copy(typeOfRelationship = typeOfRelationship)
    def modifyTypeOfRelationship(pf: PartialFunction[Option[String], Option[String]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(typeOfRelationship)) copy(typeOfRelationship = pf(typeOfRelationship)) else this
    def withMakeGrpMember(makeGrpMember: Option[Boolean]): InFlightGroupOrPartner = copy(makeGrpMember = makeGrpMember)
    def modifyMakeGrpMember(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(makeGrpMember)) copy(makeGrpMember = pf(makeGrpMember)) else this
    def withMakeControllingBody(makeControllingBody: Option[Boolean]): InFlightGroupOrPartner =
      copy(makeControllingBody = makeControllingBody)
    def modifyMakeControllingBody(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(makeControllingBody)) copy(makeControllingBody = pf(makeControllingBody)) else this
    def withIsControllingBody(isControllingBody: Option[Boolean]): InFlightGroupOrPartner =
      copy(isControllingBody = isControllingBody)
    def modifyIsControllingBody(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(isControllingBody)) copy(isControllingBody = pf(isControllingBody)) else this
    def withOrganisationName(organisationName: Option[String]): InFlightGroupOrPartner =
      copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[Option[String], Option[String]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withTradingName(tradingName: Option[String]): InFlightGroupOrPartner = copy(tradingName = tradingName)
    def modifyTradingName(pf: PartialFunction[Option[String], Option[String]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(tradingName)) copy(tradingName = pf(tradingName)) else this
    def withIndividual(individual: Option[IndividualName]): InFlightGroupOrPartner = copy(individual = individual)
    def modifyIndividual(pf: PartialFunction[Option[IndividualName], Option[IndividualName]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
    def withPPOB(PPOB: Option[PPOB]): InFlightGroupOrPartner = copy(PPOB = PPOB)
    def modifyPPOB(pf: PartialFunction[Option[PPOB], Option[PPOB]]): InFlightGroupOrPartner =
      if (pf.isDefinedAt(PPOB)) copy(PPOB = pf(PPOB)) else this
  }

  object InFlightGroupOrPartner extends RecordUtils[InFlightGroupOrPartner] {

    override val validate: Validator[InFlightGroupOrPartner] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(_.action.isOneOf(Common.actionEnum), "Invalid action, does not match allowed values"),
      check(
        _.SAP_Number.matches(Common.SAP_NumberPattern),
        s"""Invalid SAP_Number, does not matches regex ${Common.SAP_NumberPattern}"""),
      check(
        _.typeOfRelationship.isOneOf(Common.typeOfRelationshipEnum),
        "Invalid typeOfRelationship, does not match allowed values"),
      check(
        _.organisationName.lengthMinMaxInclusive(1, 160),
        "Invalid length of organisationName, should be between 1 and 160 inclusive"),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive"),
      checkObjectIfSome(_.individual, IndividualName.validate),
      checkObjectIfSome(_.PPOB, PPOB.validate)
    )

    override val gen: Gen[InFlightGroupOrPartner] = for {
      formInformation <- FormInformation.gen
      action          <- Gen.oneOf(Common.actionEnum)
    } yield
      InFlightGroupOrPartner(
        formInformation = formInformation,
        action = action
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val SAP_NumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          SAP_Number = entity.SAP_Number.orElse(Generator.get(Generator.regex(Common.SAP_NumberPattern))(seed)))

    val typeOfRelationshipSanitizer: Update = seed =>
      entity =>
        entity.copy(
          typeOfRelationship =
            entity.typeOfRelationship.orElse(Generator.get(Gen.oneOf(Common.typeOfRelationshipEnum))(seed)))

    val makeGrpMemberSanitizer: Update = seed =>
      entity => entity.copy(makeGrpMember = entity.makeGrpMember.orElse(Generator.get(Generator.booleanGen)(seed)))

    val makeControllingBodySanitizer: Update = seed =>
      entity =>
        entity.copy(makeControllingBody = entity.makeControllingBody.orElse(Generator.get(Generator.booleanGen)(seed)))

    val isControllingBodySanitizer: Update = seed =>
      entity =>
        entity.copy(isControllingBody = entity.isControllingBody.orElse(Generator.get(Generator.booleanGen)(seed)))

    val organisationNameSanitizer: Update = seed =>
      entity => entity.copy(organisationName = entity.organisationName.orElse(Generator.get(Generator.company)(seed)))

    val tradingNameSanitizer: Update = seed =>
      entity => entity.copy(tradingName = entity.tradingName.orElse(Generator.get(Generator.tradingNameGen)(seed)))

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual =
            entity.individual.orElse(Generator.get(IndividualName.gen)(seed)).map(IndividualName.sanitize(seed)))

    val PPOBSanitizer: Update = seed =>
      entity => entity.copy(PPOB = entity.PPOB.orElse(Generator.get(PPOB.gen)(seed)).map(PPOB.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(
      formInformationSanitizer,
      SAP_NumberSanitizer,
      typeOfRelationshipSanitizer,
      makeGrpMemberSanitizer,
      makeControllingBodySanitizer,
      isControllingBodySanitizer,
      organisationNameSanitizer,
      tradingNameSanitizer,
      individualSanitizer,
      PPOBSanitizer
    )

    implicit val formats: Format[InFlightGroupOrPartner] = Json.format[InFlightGroupOrPartner]

  }

  case class InFlightInformation(changeIndicators: ChangeIndicators, inflightChanges: InflightChanges) {

    def withChangeIndicators(changeIndicators: ChangeIndicators): InFlightInformation =
      copy(changeIndicators = changeIndicators)
    def modifyChangeIndicators(pf: PartialFunction[ChangeIndicators, ChangeIndicators]): InFlightInformation =
      if (pf.isDefinedAt(changeIndicators)) copy(changeIndicators = pf(changeIndicators)) else this
    def withInflightChanges(inflightChanges: InflightChanges): InFlightInformation =
      copy(inflightChanges = inflightChanges)
    def modifyInflightChanges(pf: PartialFunction[InflightChanges, InflightChanges]): InFlightInformation =
      if (pf.isDefinedAt(inflightChanges)) copy(inflightChanges = pf(inflightChanges)) else this
  }

  object InFlightInformation extends RecordUtils[InFlightInformation] {

    override val validate: Validator[InFlightInformation] = Validator(
      checkObject(_.changeIndicators, ChangeIndicators.validate),
      checkObject(_.inflightChanges, InflightChanges.validate))

    override val gen: Gen[InFlightInformation] = for {
      changeIndicators <- ChangeIndicators.gen
      inflightChanges  <- InflightChanges.gen
    } yield
      InFlightInformation(
        changeIndicators = changeIndicators,
        inflightChanges = inflightChanges
      )

    val changeIndicatorsSanitizer: Update = seed =>
      entity => entity.copy(changeIndicators = ChangeIndicators.sanitize(seed)(entity.changeIndicators))

    val inflightChangesSanitizer: Update = seed =>
      entity => entity.copy(inflightChanges = InflightChanges.sanitize(seed)(entity.inflightChanges))

    override val sanitizers: Seq[Update] = Seq(changeIndicatorsSanitizer, inflightChangesSanitizer)

    implicit val formats: Format[InFlightInformation] = Json.format[InFlightInformation]

  }

  case class InFlightPPOBDetails(
    formInformation: FormInformation,
    address: Option[Address] = None,
    contactDetails: Option[ContactDetails] = None,
    websiteAddress: Option[String] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightPPOBDetails =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightPPOBDetails =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withAddress(address: Option[Address]): InFlightPPOBDetails = copy(address = address)
    def modifyAddress(pf: PartialFunction[Option[Address], Option[Address]]): InFlightPPOBDetails =
      if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
    def withContactDetails(contactDetails: Option[ContactDetails]): InFlightPPOBDetails =
      copy(contactDetails = contactDetails)
    def modifyContactDetails(pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]): InFlightPPOBDetails =
      if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
    def withWebsiteAddress(websiteAddress: Option[String]): InFlightPPOBDetails = copy(websiteAddress = websiteAddress)
    def modifyWebsiteAddress(pf: PartialFunction[Option[String], Option[String]]): InFlightPPOBDetails =
      if (pf.isDefinedAt(websiteAddress)) copy(websiteAddress = pf(websiteAddress)) else this
  }

  object InFlightPPOBDetails extends RecordUtils[InFlightPPOBDetails] {

    override val validate: Validator[InFlightPPOBDetails] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      checkObjectIfSome(_.address, Address.validate),
      checkObjectIfSome(_.contactDetails, ContactDetails.validate),
      check(
        _.websiteAddress.lengthMinMaxInclusive(1, 132),
        "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
    )

    override val gen: Gen[InFlightPPOBDetails] = for {
      formInformation <- FormInformation.gen
    } yield
      InFlightPPOBDetails(
        formInformation = formInformation
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val addressSanitizer: Update = seed =>
      entity =>
        entity.copy(address = entity.address.orElse(Generator.get(Address.gen)(seed)).map(Address.sanitize(seed)))

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails =
            entity.contactDetails.orElse(Generator.get(ContactDetails.gen)(seed)).map(ContactDetails.sanitize(seed)))

    val websiteAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(websiteAddress = entity.websiteAddress.orElse(Generator.get(Generator.stringMinMaxN(1, 132))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(formInformationSanitizer, addressSanitizer, contactDetailsSanitizer, websiteAddressSanitizer)

    implicit val formats: Format[InFlightPPOBDetails] = Json.format[InFlightPPOBDetails]

  }

  case class InFlightReturnPeriod(
    formInformation: FormInformation,
    changeReturnPeriod: Option[Boolean] = None,
    nonStdTaxPeriodsRequested: Option[Boolean] = None,
    ceaseNonStdTaxPeriods: Option[Boolean] = None,
    stdReturnPeriod: Option[String] = None,
    nonStdTaxPeriods: Option[NonStdTaxPeriods] = None) {

    def withFormInformation(formInformation: FormInformation): InFlightReturnPeriod =
      copy(formInformation = formInformation)
    def modifyFormInformation(pf: PartialFunction[FormInformation, FormInformation]): InFlightReturnPeriod =
      if (pf.isDefinedAt(formInformation)) copy(formInformation = pf(formInformation)) else this
    def withChangeReturnPeriod(changeReturnPeriod: Option[Boolean]): InFlightReturnPeriod =
      copy(changeReturnPeriod = changeReturnPeriod)
    def modifyChangeReturnPeriod(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightReturnPeriod =
      if (pf.isDefinedAt(changeReturnPeriod)) copy(changeReturnPeriod = pf(changeReturnPeriod)) else this
    def withNonStdTaxPeriodsRequested(nonStdTaxPeriodsRequested: Option[Boolean]): InFlightReturnPeriod =
      copy(nonStdTaxPeriodsRequested = nonStdTaxPeriodsRequested)
    def modifyNonStdTaxPeriodsRequested(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightReturnPeriod =
      if (pf.isDefinedAt(nonStdTaxPeriodsRequested)) copy(nonStdTaxPeriodsRequested = pf(nonStdTaxPeriodsRequested))
      else this
    def withCeaseNonStdTaxPeriods(ceaseNonStdTaxPeriods: Option[Boolean]): InFlightReturnPeriod =
      copy(ceaseNonStdTaxPeriods = ceaseNonStdTaxPeriods)
    def modifyCeaseNonStdTaxPeriods(pf: PartialFunction[Option[Boolean], Option[Boolean]]): InFlightReturnPeriod =
      if (pf.isDefinedAt(ceaseNonStdTaxPeriods)) copy(ceaseNonStdTaxPeriods = pf(ceaseNonStdTaxPeriods)) else this
    def withStdReturnPeriod(stdReturnPeriod: Option[String]): InFlightReturnPeriod =
      copy(stdReturnPeriod = stdReturnPeriod)
    def modifyStdReturnPeriod(pf: PartialFunction[Option[String], Option[String]]): InFlightReturnPeriod =
      if (pf.isDefinedAt(stdReturnPeriod)) copy(stdReturnPeriod = pf(stdReturnPeriod)) else this
    def withNonStdTaxPeriods(nonStdTaxPeriods: Option[NonStdTaxPeriods]): InFlightReturnPeriod =
      copy(nonStdTaxPeriods = nonStdTaxPeriods)
    def modifyNonStdTaxPeriods(
      pf: PartialFunction[Option[NonStdTaxPeriods], Option[NonStdTaxPeriods]]): InFlightReturnPeriod =
      if (pf.isDefinedAt(nonStdTaxPeriods)) copy(nonStdTaxPeriods = pf(nonStdTaxPeriods)) else this
  }

  object InFlightReturnPeriod extends RecordUtils[InFlightReturnPeriod] {

    override val validate: Validator[InFlightReturnPeriod] = Validator(
      checkObject(_.formInformation, FormInformation.validate),
      check(
        _.stdReturnPeriod.isOneOf(Common.stdReturnPeriodEnum),
        "Invalid stdReturnPeriod, does not match allowed values"),
      checkObjectIfSome(_.nonStdTaxPeriods, NonStdTaxPeriods.validate)
    )

    override val gen: Gen[InFlightReturnPeriod] = for {
      formInformation <- FormInformation.gen
    } yield
      InFlightReturnPeriod(
        formInformation = formInformation
      )

    val formInformationSanitizer: Update = seed =>
      entity => entity.copy(formInformation = FormInformation.sanitize(seed)(entity.formInformation))

    val changeReturnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(changeReturnPeriod = entity.changeReturnPeriod.orElse(Generator.get(Generator.booleanGen)(seed)))

    val nonStdTaxPeriodsRequestedSanitizer: Update = seed =>
      entity =>
        entity.copy(nonStdTaxPeriodsRequested =
          entity.nonStdTaxPeriodsRequested.orElse(Generator.get(Generator.booleanGen)(seed)))

    val ceaseNonStdTaxPeriodsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          ceaseNonStdTaxPeriods = entity.ceaseNonStdTaxPeriods.orElse(Generator.get(Generator.booleanGen)(seed)))

    val stdReturnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          stdReturnPeriod = entity.stdReturnPeriod.orElse(Generator.get(Gen.oneOf(Common.stdReturnPeriodEnum))(seed)))

    val nonStdTaxPeriodsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          nonStdTaxPeriods = entity.nonStdTaxPeriods
            .orElse(Generator.get(NonStdTaxPeriods.gen)(seed))
            .map(NonStdTaxPeriods.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(
      formInformationSanitizer,
      changeReturnPeriodSanitizer,
      nonStdTaxPeriodsRequestedSanitizer,
      ceaseNonStdTaxPeriodsSanitizer,
      stdReturnPeriodSanitizer,
      nonStdTaxPeriodsSanitizer
    )

    implicit val formats: Format[InFlightReturnPeriod] = Json.format[InFlightReturnPeriod]

  }

  case class IndividualName(
    title: Option[String] = None,
    firstName: Option[String] = None,
    middleName: Option[String] = None,
    lastName: Option[String] = None) {

    def withTitle(title: Option[String]): IndividualName = copy(title = title)
    def modifyTitle(pf: PartialFunction[Option[String], Option[String]]): IndividualName =
      if (pf.isDefinedAt(title)) copy(title = pf(title)) else this
    def withFirstName(firstName: Option[String]): IndividualName = copy(firstName = firstName)
    def modifyFirstName(pf: PartialFunction[Option[String], Option[String]]): IndividualName =
      if (pf.isDefinedAt(firstName)) copy(firstName = pf(firstName)) else this
    def withMiddleName(middleName: Option[String]): IndividualName = copy(middleName = middleName)
    def modifyMiddleName(pf: PartialFunction[Option[String], Option[String]]): IndividualName =
      if (pf.isDefinedAt(middleName)) copy(middleName = pf(middleName)) else this
    def withLastName(lastName: Option[String]): IndividualName = copy(lastName = lastName)
    def modifyLastName(pf: PartialFunction[Option[String], Option[String]]): IndividualName =
      if (pf.isDefinedAt(lastName)) copy(lastName = pf(lastName)) else this
  }

  object IndividualName extends RecordUtils[IndividualName] {

    override val validate: Validator[IndividualName] = Validator(
      check(_.title.isOneOf(Common.titleEnum), "Invalid title, does not match allowed values"),
      check(
        _.firstName.lengthMinMaxInclusive(1, 35),
        "Invalid length of firstName, should be between 1 and 35 inclusive"),
      check(
        _.middleName.lengthMinMaxInclusive(1, 35),
        "Invalid length of middleName, should be between 1 and 35 inclusive"),
      check(_.lastName.lengthMinMaxInclusive(1, 35), "Invalid length of lastName, should be between 1 and 35 inclusive")
    )

    override val gen: Gen[IndividualName] = Gen const IndividualName(
      )

    val titleSanitizer: Update = seed =>
      entity => entity.copy(title = entity.title.orElse(Generator.get(Gen.oneOf(Common.titleEnum))(seed)))

    val firstNameSanitizer: Update = seed =>
      entity => entity.copy(firstName = entity.firstName.orElse(Generator.get(Generator.forename())(seed)))

    val middleNameSanitizer: Update = seed =>
      entity =>
        entity.copy(middleName = entity.middleName.orElse(Generator.get(Generator.forename().variant("middle"))(seed)))

    val lastNameSanitizer: Update = seed =>
      entity => entity.copy(lastName = entity.lastName.orElse(Generator.get(Generator.surname)(seed)))

    override val sanitizers: Seq[Update] =
      Seq(titleSanitizer, firstNameSanitizer, middleNameSanitizer, lastNameSanitizer)

    implicit val formats: Format[IndividualName] = Json.format[IndividualName]

  }

  case class InflightChanges(
    customerDetails: Option[InFlightCustomerDetails] = None,
    PPOBDetails: Option[InFlightPPOBDetails] = None,
    correspondenceContactDetails: Option[InFlightCorrespondenceContactDetails] = None,
    bankDetails: Option[InFlightBankDetails] = None,
    businessActivities: Option[InFlightBusinessActivities] = None,
    flatRateScheme: Option[InFlightFlatRateScheme] = None,
    deregister: Option[InFlightDeregistration] = None,
    returnPeriod: Option[InFlightReturnPeriod] = None,
    groupOrPartner: Option[Seq[InFlightGroupOrPartner]] = None) {

    def withCustomerDetails(customerDetails: Option[InFlightCustomerDetails]): InflightChanges =
      copy(customerDetails = customerDetails)
    def modifyCustomerDetails(
      pf: PartialFunction[Option[InFlightCustomerDetails], Option[InFlightCustomerDetails]]): InflightChanges =
      if (pf.isDefinedAt(customerDetails)) copy(customerDetails = pf(customerDetails)) else this
    def withPPOBDetails(PPOBDetails: Option[InFlightPPOBDetails]): InflightChanges = copy(PPOBDetails = PPOBDetails)
    def modifyPPOBDetails(
      pf: PartialFunction[Option[InFlightPPOBDetails], Option[InFlightPPOBDetails]]): InflightChanges =
      if (pf.isDefinedAt(PPOBDetails)) copy(PPOBDetails = pf(PPOBDetails)) else this
    def withCorrespondenceContactDetails(
      correspondenceContactDetails: Option[InFlightCorrespondenceContactDetails]): InflightChanges =
      copy(correspondenceContactDetails = correspondenceContactDetails)
    def modifyCorrespondenceContactDetails(
      pf: PartialFunction[Option[InFlightCorrespondenceContactDetails], Option[InFlightCorrespondenceContactDetails]])
      : InflightChanges =
      if (pf.isDefinedAt(correspondenceContactDetails))
        copy(correspondenceContactDetails = pf(correspondenceContactDetails))
      else this
    def withBankDetails(bankDetails: Option[InFlightBankDetails]): InflightChanges = copy(bankDetails = bankDetails)
    def modifyBankDetails(
      pf: PartialFunction[Option[InFlightBankDetails], Option[InFlightBankDetails]]): InflightChanges =
      if (pf.isDefinedAt(bankDetails)) copy(bankDetails = pf(bankDetails)) else this
    def withBusinessActivities(businessActivities: Option[InFlightBusinessActivities]): InflightChanges =
      copy(businessActivities = businessActivities)
    def modifyBusinessActivities(
      pf: PartialFunction[Option[InFlightBusinessActivities], Option[InFlightBusinessActivities]]): InflightChanges =
      if (pf.isDefinedAt(businessActivities)) copy(businessActivities = pf(businessActivities)) else this
    def withFlatRateScheme(flatRateScheme: Option[InFlightFlatRateScheme]): InflightChanges =
      copy(flatRateScheme = flatRateScheme)
    def modifyFlatRateScheme(
      pf: PartialFunction[Option[InFlightFlatRateScheme], Option[InFlightFlatRateScheme]]): InflightChanges =
      if (pf.isDefinedAt(flatRateScheme)) copy(flatRateScheme = pf(flatRateScheme)) else this
    def withDeregister(deregister: Option[InFlightDeregistration]): InflightChanges = copy(deregister = deregister)
    def modifyDeregister(
      pf: PartialFunction[Option[InFlightDeregistration], Option[InFlightDeregistration]]): InflightChanges =
      if (pf.isDefinedAt(deregister)) copy(deregister = pf(deregister)) else this
    def withReturnPeriod(returnPeriod: Option[InFlightReturnPeriod]): InflightChanges =
      copy(returnPeriod = returnPeriod)
    def modifyReturnPeriod(
      pf: PartialFunction[Option[InFlightReturnPeriod], Option[InFlightReturnPeriod]]): InflightChanges =
      if (pf.isDefinedAt(returnPeriod)) copy(returnPeriod = pf(returnPeriod)) else this
    def withGroupOrPartner(groupOrPartner: Option[Seq[InFlightGroupOrPartner]]): InflightChanges =
      copy(groupOrPartner = groupOrPartner)
    def modifyGroupOrPartner(
      pf: PartialFunction[Option[Seq[InFlightGroupOrPartner]], Option[Seq[InFlightGroupOrPartner]]]): InflightChanges =
      if (pf.isDefinedAt(groupOrPartner)) copy(groupOrPartner = pf(groupOrPartner)) else this
  }

  object InflightChanges extends RecordUtils[InflightChanges] {

    override val validate: Validator[InflightChanges] = Validator(
      checkObjectIfSome(_.customerDetails, InFlightCustomerDetails.validate),
      checkObjectIfSome(_.PPOBDetails, InFlightPPOBDetails.validate),
      checkObjectIfSome(_.correspondenceContactDetails, InFlightCorrespondenceContactDetails.validate),
      checkObjectIfSome(_.bankDetails, InFlightBankDetails.validate),
      checkObjectIfSome(_.businessActivities, InFlightBusinessActivities.validate),
      checkObjectIfSome(_.flatRateScheme, InFlightFlatRateScheme.validate),
      checkObjectIfSome(_.deregister, InFlightDeregistration.validate),
      checkObjectIfSome(_.returnPeriod, InFlightReturnPeriod.validate),
      checkEachIfSome(_.groupOrPartner, InFlightGroupOrPartner.validate)
    )

    override val gen: Gen[InflightChanges] = Gen const InflightChanges(
      )

    val customerDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          customerDetails = entity.customerDetails
            .orElse(Generator.get(InFlightCustomerDetails.gen)(seed))
            .map(InFlightCustomerDetails.sanitize(seed)))

    val PPOBDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          PPOBDetails = entity.PPOBDetails
            .orElse(Generator.get(InFlightPPOBDetails.gen)(seed))
            .map(InFlightPPOBDetails.sanitize(seed)))

    val correspondenceContactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          correspondenceContactDetails = entity.correspondenceContactDetails
            .orElse(Generator.get(InFlightCorrespondenceContactDetails.gen)(seed))
            .map(InFlightCorrespondenceContactDetails.sanitize(seed)))

    val bankDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankDetails = entity.bankDetails
            .orElse(Generator.get(InFlightBankDetails.gen)(seed))
            .map(InFlightBankDetails.sanitize(seed)))

    val businessActivitiesSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessActivities = entity.businessActivities
            .orElse(Generator.get(InFlightBusinessActivities.gen)(seed))
            .map(InFlightBusinessActivities.sanitize(seed)))

    val flatRateSchemeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          flatRateScheme = entity.flatRateScheme
            .orElse(Generator.get(InFlightFlatRateScheme.gen)(seed))
            .map(InFlightFlatRateScheme.sanitize(seed)))

    val deregisterSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregister = entity.deregister
            .orElse(Generator.get(InFlightDeregistration.gen)(seed))
            .map(InFlightDeregistration.sanitize(seed)))

    val returnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          returnPeriod = entity.returnPeriod
            .orElse(Generator.get(InFlightReturnPeriod.gen)(seed))
            .map(InFlightReturnPeriod.sanitize(seed)))

    val groupOrPartnerSanitizer: Update = seed =>
      entity =>
        entity.copy(
          groupOrPartner = entity.groupOrPartner
            .orElse(Generator.get(Generator.nonEmptyListOfMaxN(2, InFlightGroupOrPartner.gen))(seed))
            .map(_.map(InFlightGroupOrPartner.sanitize(seed))))

    override val sanitizers: Seq[Update] = Seq(
      customerDetailsSanitizer,
      PPOBDetailsSanitizer,
      correspondenceContactDetailsSanitizer,
      bankDetailsSanitizer,
      businessActivitiesSanitizer,
      flatRateSchemeSanitizer,
      deregisterSanitizer,
      returnPeriodSanitizer,
      groupOrPartnerSanitizer
    )

    implicit val formats: Format[InflightChanges] = Json.format[InflightChanges]

  }

  case class NonStdTaxPeriods(
    period01: Option[String] = None,
    period02: Option[String] = None,
    period03: Option[String] = None,
    period04: Option[String] = None,
    period05: Option[String] = None,
    period06: Option[String] = None,
    period07: Option[String] = None,
    period08: Option[String] = None,
    period09: Option[String] = None,
    period10: Option[String] = None,
    period11: Option[String] = None,
    period12: Option[String] = None,
    period13: Option[String] = None,
    period14: Option[String] = None,
    period15: Option[String] = None,
    period16: Option[String] = None,
    period17: Option[String] = None,
    period18: Option[String] = None,
    period19: Option[String] = None,
    period20: Option[String] = None,
    period21: Option[String] = None,
    period22: Option[String] = None) {

    def withPeriod01(period01: Option[String]): NonStdTaxPeriods = copy(period01 = period01)
    def modifyPeriod01(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period01)) copy(period01 = pf(period01)) else this
    def withPeriod02(period02: Option[String]): NonStdTaxPeriods = copy(period02 = period02)
    def modifyPeriod02(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period02)) copy(period02 = pf(period02)) else this
    def withPeriod03(period03: Option[String]): NonStdTaxPeriods = copy(period03 = period03)
    def modifyPeriod03(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period03)) copy(period03 = pf(period03)) else this
    def withPeriod04(period04: Option[String]): NonStdTaxPeriods = copy(period04 = period04)
    def modifyPeriod04(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period04)) copy(period04 = pf(period04)) else this
    def withPeriod05(period05: Option[String]): NonStdTaxPeriods = copy(period05 = period05)
    def modifyPeriod05(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period05)) copy(period05 = pf(period05)) else this
    def withPeriod06(period06: Option[String]): NonStdTaxPeriods = copy(period06 = period06)
    def modifyPeriod06(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period06)) copy(period06 = pf(period06)) else this
    def withPeriod07(period07: Option[String]): NonStdTaxPeriods = copy(period07 = period07)
    def modifyPeriod07(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period07)) copy(period07 = pf(period07)) else this
    def withPeriod08(period08: Option[String]): NonStdTaxPeriods = copy(period08 = period08)
    def modifyPeriod08(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period08)) copy(period08 = pf(period08)) else this
    def withPeriod09(period09: Option[String]): NonStdTaxPeriods = copy(period09 = period09)
    def modifyPeriod09(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period09)) copy(period09 = pf(period09)) else this
    def withPeriod10(period10: Option[String]): NonStdTaxPeriods = copy(period10 = period10)
    def modifyPeriod10(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period10)) copy(period10 = pf(period10)) else this
    def withPeriod11(period11: Option[String]): NonStdTaxPeriods = copy(period11 = period11)
    def modifyPeriod11(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period11)) copy(period11 = pf(period11)) else this
    def withPeriod12(period12: Option[String]): NonStdTaxPeriods = copy(period12 = period12)
    def modifyPeriod12(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period12)) copy(period12 = pf(period12)) else this
    def withPeriod13(period13: Option[String]): NonStdTaxPeriods = copy(period13 = period13)
    def modifyPeriod13(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period13)) copy(period13 = pf(period13)) else this
    def withPeriod14(period14: Option[String]): NonStdTaxPeriods = copy(period14 = period14)
    def modifyPeriod14(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period14)) copy(period14 = pf(period14)) else this
    def withPeriod15(period15: Option[String]): NonStdTaxPeriods = copy(period15 = period15)
    def modifyPeriod15(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period15)) copy(period15 = pf(period15)) else this
    def withPeriod16(period16: Option[String]): NonStdTaxPeriods = copy(period16 = period16)
    def modifyPeriod16(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period16)) copy(period16 = pf(period16)) else this
    def withPeriod17(period17: Option[String]): NonStdTaxPeriods = copy(period17 = period17)
    def modifyPeriod17(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period17)) copy(period17 = pf(period17)) else this
    def withPeriod18(period18: Option[String]): NonStdTaxPeriods = copy(period18 = period18)
    def modifyPeriod18(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period18)) copy(period18 = pf(period18)) else this
    def withPeriod19(period19: Option[String]): NonStdTaxPeriods = copy(period19 = period19)
    def modifyPeriod19(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period19)) copy(period19 = pf(period19)) else this
    def withPeriod20(period20: Option[String]): NonStdTaxPeriods = copy(period20 = period20)
    def modifyPeriod20(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period20)) copy(period20 = pf(period20)) else this
    def withPeriod21(period21: Option[String]): NonStdTaxPeriods = copy(period21 = period21)
    def modifyPeriod21(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period21)) copy(period21 = pf(period21)) else this
    def withPeriod22(period22: Option[String]): NonStdTaxPeriods = copy(period22 = period22)
    def modifyPeriod22(pf: PartialFunction[Option[String], Option[String]]): NonStdTaxPeriods =
      if (pf.isDefinedAt(period22)) copy(period22 = pf(period22)) else this
  }

  object NonStdTaxPeriods extends RecordUtils[NonStdTaxPeriods] {

    override val validate: Validator[NonStdTaxPeriods] = Validator(
      check(
        _.period01.matches(Common.dateOfBirthPattern),
        s"""Invalid period01, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period02.matches(Common.dateOfBirthPattern),
        s"""Invalid period02, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period03.matches(Common.dateOfBirthPattern),
        s"""Invalid period03, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period04.matches(Common.dateOfBirthPattern),
        s"""Invalid period04, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period05.matches(Common.dateOfBirthPattern),
        s"""Invalid period05, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period06.matches(Common.dateOfBirthPattern),
        s"""Invalid period06, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period07.matches(Common.dateOfBirthPattern),
        s"""Invalid period07, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period08.matches(Common.dateOfBirthPattern),
        s"""Invalid period08, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period09.matches(Common.dateOfBirthPattern),
        s"""Invalid period09, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period10.matches(Common.dateOfBirthPattern),
        s"""Invalid period10, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period11.matches(Common.dateOfBirthPattern),
        s"""Invalid period11, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period12.matches(Common.dateOfBirthPattern),
        s"""Invalid period12, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period13.matches(Common.dateOfBirthPattern),
        s"""Invalid period13, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period14.matches(Common.dateOfBirthPattern),
        s"""Invalid period14, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period15.matches(Common.dateOfBirthPattern),
        s"""Invalid period15, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period16.matches(Common.dateOfBirthPattern),
        s"""Invalid period16, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period17.matches(Common.dateOfBirthPattern),
        s"""Invalid period17, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period18.matches(Common.dateOfBirthPattern),
        s"""Invalid period18, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period19.matches(Common.dateOfBirthPattern),
        s"""Invalid period19, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period20.matches(Common.dateOfBirthPattern),
        s"""Invalid period20, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period21.matches(Common.dateOfBirthPattern),
        s"""Invalid period21, does not matches regex ${Common.dateOfBirthPattern}"""),
      check(
        _.period22.matches(Common.dateOfBirthPattern),
        s"""Invalid period22, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    override val gen: Gen[NonStdTaxPeriods] = Gen const NonStdTaxPeriods(
      )

    val period01Sanitizer: Update = seed =>
      entity => entity.copy(period01 = entity.period01.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period02Sanitizer: Update = seed =>
      entity => entity.copy(period02 = entity.period02.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period03Sanitizer: Update = seed =>
      entity => entity.copy(period03 = entity.period03.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period04Sanitizer: Update = seed =>
      entity => entity.copy(period04 = entity.period04.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period05Sanitizer: Update = seed =>
      entity => entity.copy(period05 = entity.period05.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period06Sanitizer: Update = seed =>
      entity => entity.copy(period06 = entity.period06.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period07Sanitizer: Update = seed =>
      entity => entity.copy(period07 = entity.period07.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period08Sanitizer: Update = seed =>
      entity => entity.copy(period08 = entity.period08.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period09Sanitizer: Update = seed =>
      entity => entity.copy(period09 = entity.period09.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period10Sanitizer: Update = seed =>
      entity => entity.copy(period10 = entity.period10.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period11Sanitizer: Update = seed =>
      entity => entity.copy(period11 = entity.period11.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period12Sanitizer: Update = seed =>
      entity => entity.copy(period12 = entity.period12.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period13Sanitizer: Update = seed =>
      entity => entity.copy(period13 = entity.period13.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period14Sanitizer: Update = seed =>
      entity => entity.copy(period14 = entity.period14.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period15Sanitizer: Update = seed =>
      entity => entity.copy(period15 = entity.period15.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period16Sanitizer: Update = seed =>
      entity => entity.copy(period16 = entity.period16.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period17Sanitizer: Update = seed =>
      entity => entity.copy(period17 = entity.period17.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period18Sanitizer: Update = seed =>
      entity => entity.copy(period18 = entity.period18.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period19Sanitizer: Update = seed =>
      entity => entity.copy(period19 = entity.period19.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period20Sanitizer: Update = seed =>
      entity => entity.copy(period20 = entity.period20.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period21Sanitizer: Update = seed =>
      entity => entity.copy(period21 = entity.period21.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    val period22Sanitizer: Update = seed =>
      entity => entity.copy(period22 = entity.period22.orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed)))

    override val sanitizers: Seq[Update] = Seq(
      period01Sanitizer,
      period02Sanitizer,
      period03Sanitizer,
      period04Sanitizer,
      period05Sanitizer,
      period06Sanitizer,
      period07Sanitizer,
      period08Sanitizer,
      period09Sanitizer,
      period10Sanitizer,
      period11Sanitizer,
      period12Sanitizer,
      period13Sanitizer,
      period14Sanitizer,
      period15Sanitizer,
      period16Sanitizer,
      period17Sanitizer,
      period18Sanitizer,
      period19Sanitizer,
      period20Sanitizer,
      period21Sanitizer,
      period22Sanitizer
    )

    implicit val formats: Format[NonStdTaxPeriods] = Json.format[NonStdTaxPeriods]

  }

  case class PPOB(
    address: Address,
    RLS: Option[String] = None,
    contactDetails: Option[ContactDetails] = None,
    websiteAddress: Option[String] = None) {

    def withAddress(address: Address): PPOB = copy(address = address)
    def modifyAddress(pf: PartialFunction[Address, Address]): PPOB =
      if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
    def withRLS(RLS: Option[String]): PPOB = copy(RLS = RLS)
    def modifyRLS(pf: PartialFunction[Option[String], Option[String]]): PPOB =
      if (pf.isDefinedAt(RLS)) copy(RLS = pf(RLS)) else this
    def withContactDetails(contactDetails: Option[ContactDetails]): PPOB = copy(contactDetails = contactDetails)
    def modifyContactDetails(pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]): PPOB =
      if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
    def withWebsiteAddress(websiteAddress: Option[String]): PPOB = copy(websiteAddress = websiteAddress)
    def modifyWebsiteAddress(pf: PartialFunction[Option[String], Option[String]]): PPOB =
      if (pf.isDefinedAt(websiteAddress)) copy(websiteAddress = pf(websiteAddress)) else this
  }

  object PPOB extends RecordUtils[PPOB] {

    override val validate: Validator[PPOB] = Validator(
      checkObject(_.address, Address.validate),
      check(_.RLS.isOneOf(Common.RLSEnum), "Invalid RLS, does not match allowed values"),
      checkObjectIfSome(_.contactDetails, ContactDetails.validate),
      check(
        _.websiteAddress.lengthMinMaxInclusive(1, 132),
        "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
    )

    override val gen: Gen[PPOB] = for {
      address <- Address.gen
    } yield
      PPOB(
        address = address
      )

    val RLSSanitizer: Update = seed =>
      entity => entity.copy(RLS = entity.RLS.orElse(Generator.get(Gen.oneOf(Common.RLSEnum))(seed)))

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails =
            entity.contactDetails.orElse(Generator.get(ContactDetails.gen)(seed)).map(ContactDetails.sanitize(seed)))

    val websiteAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(websiteAddress = entity.websiteAddress.orElse(Generator.get(Generator.stringMinMaxN(1, 132))(seed)))

    override val sanitizers: Seq[Update] = Seq(RLSSanitizer, contactDetailsSanitizer, websiteAddressSanitizer)

    implicit val formats: Format[PPOB] = Json.format[PPOB]

  }

  case class Period(stdReturnPeriod: Option[String] = None, nonStdTaxPeriods: Option[NonStdTaxPeriods] = None) {

    def withStdReturnPeriod(stdReturnPeriod: Option[String]): Period = copy(stdReturnPeriod = stdReturnPeriod)
    def modifyStdReturnPeriod(pf: PartialFunction[Option[String], Option[String]]): Period =
      if (pf.isDefinedAt(stdReturnPeriod)) copy(stdReturnPeriod = pf(stdReturnPeriod)) else this
    def withNonStdTaxPeriods(nonStdTaxPeriods: Option[NonStdTaxPeriods]): Period =
      copy(nonStdTaxPeriods = nonStdTaxPeriods)
    def modifyNonStdTaxPeriods(pf: PartialFunction[Option[NonStdTaxPeriods], Option[NonStdTaxPeriods]]): Period =
      if (pf.isDefinedAt(nonStdTaxPeriods)) copy(nonStdTaxPeriods = pf(nonStdTaxPeriods)) else this
  }

  object Period extends RecordUtils[Period] {

    override val validate: Validator[Period] = Validator(
      check(
        _.stdReturnPeriod.isOneOf(Common.stdReturnPeriodEnum),
        "Invalid stdReturnPeriod, does not match allowed values"),
      checkObjectIfSome(_.nonStdTaxPeriods, NonStdTaxPeriods.validate)
    )

    override val gen: Gen[Period] = Gen const Period(
      )

    val stdReturnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          stdReturnPeriod = entity.stdReturnPeriod.orElse(Generator.get(Gen.oneOf(Common.stdReturnPeriodEnum))(seed)))

    val nonStdTaxPeriodsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          nonStdTaxPeriods = entity.nonStdTaxPeriods
            .orElse(Generator.get(NonStdTaxPeriods.gen)(seed))
            .map(NonStdTaxPeriods.sanitize(seed)))

    override val sanitizers: Seq[Update] = Seq(stdReturnPeriodSanitizer, nonStdTaxPeriodsSanitizer)

    implicit val formats: Format[Period] = Json.format[Period]

  }

  case class UkAddress(
    override val line1: String,
    override val line2: String,
    override val line3: Option[String] = None,
    override val line4: Option[String] = None,
    postCode: String,
    override val countryCode: String)
      extends Address {

    def withLine1(line1: String): UkAddress = copy(line1 = line1)
    def modifyLine1(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(line1)) copy(line1 = pf(line1)) else this
    def withLine2(line2: String): UkAddress = copy(line2 = line2)
    def modifyLine2(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(line2)) copy(line2 = pf(line2)) else this
    def withLine3(line3: Option[String]): UkAddress = copy(line3 = line3)
    def modifyLine3(pf: PartialFunction[Option[String], Option[String]]): UkAddress =
      if (pf.isDefinedAt(line3)) copy(line3 = pf(line3)) else this
    def withLine4(line4: Option[String]): UkAddress = copy(line4 = line4)
    def modifyLine4(pf: PartialFunction[Option[String], Option[String]]): UkAddress =
      if (pf.isDefinedAt(line4)) copy(line4 = pf(line4)) else this
    def withPostCode(postCode: String): UkAddress = copy(postCode = postCode)
    def modifyPostCode(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(postCode)) copy(postCode = pf(postCode)) else this
    def withCountryCode(countryCode: String): UkAddress = copy(countryCode = countryCode)
    def modifyCountryCode(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(countryCode)) copy(countryCode = pf(countryCode)) else this
  }

  object UkAddress extends RecordUtils[UkAddress] {

    override val validate: Validator[UkAddress] = Validator(
      check(_.line1.matches(Common.linePattern), s"""Invalid line1, does not matches regex ${Common.linePattern}"""),
      check(_.line2.matches(Common.linePattern), s"""Invalid line2, does not matches regex ${Common.linePattern}"""),
      check(_.line3.matches(Common.linePattern), s"""Invalid line3, does not matches regex ${Common.linePattern}"""),
      check(_.line4.matches(Common.linePattern), s"""Invalid line4, does not matches regex ${Common.linePattern}"""),
      check(
        _.postCode.matches(Common.postCodePattern),
        s"""Invalid postCode, does not matches regex ${Common.postCodePattern}"""),
      check(_.countryCode.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")
    )

    override val gen: Gen[UkAddress] = for {
      line1       <- Generator.address4Lines35Gen.map(_.line1)
      line2       <- Generator.address4Lines35Gen.map(_.line2)
      postCode    <- Generator.postcode
      countryCode <- Gen.const("GB")
    } yield
      UkAddress(
        line1 = line1,
        line2 = line2,
        postCode = postCode,
        countryCode = countryCode
      )

    val line3Sanitizer: Update = seed =>
      entity => entity.copy(line3 = entity.line3.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed)))

    val line4Sanitizer: Update = seed =>
      entity => entity.copy(line4 = entity.line4.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed)))

    override val sanitizers: Seq[Update] = Seq(line3Sanitizer, line4Sanitizer)

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  object Common {
    val RLSEnum = Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")
    val actionEnum = Seq("1", "2", "3", "4")
    val postCodePattern = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""
    val primaryPhoneNumberPattern = """^[A-Z0-9 )/(*#-]+$"""
    val dateOfBirthPattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val registrationReasonEnum = Seq(
      "0001",
      "0002",
      "0003",
      "0004",
      "0005",
      "0006",
      "0007",
      "0008",
      "0009",
      "0010",
      "0011",
      "0012",
      "0013",
      "0014")
    val linePattern = """^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""
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
    val stdReturnPeriodEnum = Seq("MA", "MB", "MC", "MM")
    val primaryMainCodePattern = """^[0-9]{5}$"""
    val deregistrationReasonEnum =
      Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011")
    val typeOfRelationshipEnum = Seq("01", "02", "03", "04")
    val bankAccountNumberPattern = """^[0-9]{8}$"""
    val vrnPattern = """^[0-9]{9}$"""
    val FRSCategoryEnum = Seq(
      "001",
      "002",
      "003",
      "004",
      "005",
      "006",
      "007",
      "008",
      "009",
      "010",
      "011",
      "012",
      "013",
      "014",
      "015",
      "016",
      "017",
      "018",
      "019",
      "020",
      "021",
      "022",
      "023",
      "024",
      "025",
      "026",
      "027",
      "028",
      "029",
      "030",
      "031",
      "032",
      "033",
      "034",
      "035",
      "036",
      "037",
      "038",
      "039",
      "040",
      "041",
      "042",
      "043",
      "044",
      "045",
      "046",
      "047",
      "048",
      "049",
      "050",
      "051",
      "052",
      "053",
      "054"
    )
    val titleEnum = Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011", "0012")
    val countryCodeEnum1 = Seq("GB")
    val formBundlePattern = """^[0-9]{12}$"""
    val sortCodePattern = """^[0-9]{6}$"""
    val SAP_NumberPattern = """^[0-9]{42}$"""
  }
}
