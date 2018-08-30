package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord._
import wolfendale.scalacheck.regexp.RegexpGen

/**
  * ----------------------------------------------------------------------------
  * This VatCustomerInformationRecord code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
// schema path: #
case class VatCustomerInformationRecord(
  approvedInformation: Option[ApprovedInformation] = None,
  inFlightInformation: Option[InFlightInformation] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = None
  override def lookupKeys: Seq[String] = Seq()
  override def withId(id: Option[String]): Record = copy(id = id)
}

object VatCustomerInformationRecord extends RecordHelper[VatCustomerInformationRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  val booleanGen = Gen.oneOf(true, false)

  import Validator._

  override val gen: Gen[VatCustomerInformationRecord] = for {
    approvedInformation <- Gen.option(ApprovedInformation.gen)
    inFlightInformation <- Gen.option(InFlightInformation.gen)
  } yield
    VatCustomerInformationRecord(approvedInformation = approvedInformation, inFlightInformation = inFlightInformation)

// schema path: #/definitions/approvedInformationType
  case class ApprovedInformation(
    customerDetails: CustomerDetails,
    PPOB: PPOB,
    correspondenceContactDetails: Option[CorrespondenceContactDetails] = None,
    bankDetails: Option[BankDetails] = None,
    businessActivities: Option[BusinessActivities] = None,
    flatRateScheme: Option[FlatRateScheme] = None,
    deregistration: Option[Deregistration] = None,
    returnPeriod: Option[Period] = None,
    groupOrPartnerMbrs: Option[Seq[GroupOrPartner]] = None)

  object ApprovedInformation extends RecordHelper[ApprovedInformation] {

    override val gen: Gen[ApprovedInformation] = for {
      customerDetails              <- CustomerDetails.gen
      ppob                         <- PPOB.gen
      correspondenceContactDetails <- Gen.option(CorrespondenceContactDetails.gen)
      bankDetails                  <- Gen.option(BankDetails.gen)
      businessActivities           <- Gen.option(BusinessActivities.gen)
      flatRateScheme               <- Gen.option(FlatRateScheme.gen)
      deregistration               <- Gen.option(Deregistration.gen)
      returnPeriod                 <- Gen.option(Period.gen)
      groupOrPartnerMbrs           <- Gen.option(Gen.nonEmptyListOf(GroupOrPartner.gen))
    } yield
      ApprovedInformation(
        customerDetails = customerDetails,
        PPOB = ppob,
        correspondenceContactDetails = correspondenceContactDetails,
        bankDetails = bankDetails,
        businessActivities = businessActivities,
        flatRateScheme = flatRateScheme,
        deregistration = deregistration,
        returnPeriod = returnPeriod,
        groupOrPartnerMbrs = groupOrPartnerMbrs
      )

    val validate: Validator[ApprovedInformation] = Validator(
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[ApprovedInformation] = Json.format[ApprovedInformation]
  }

// schema path: #/definitions/bankDetailsType
  case class BankDetails(
    IBAN: Option[String] = None,
    BIC: Option[String] = None,
    accountHolderName: Option[String] = None,
    bankAccountNumber: Option[String] = None,
    sortCode: Option[String] = None,
    buildingSocietyNumber: Option[String] = None,
    bankBuildSocietyName: Option[String] = None)

  object BankDetails extends RecordHelper[BankDetails] {

    override val gen: Gen[BankDetails] = for {
      iban                  <- Gen.option(Generator.stringMinMaxN(1, 34))
      bic                   <- Gen.option(Generator.stringMinMaxN(1, 11))
      accountHolderName     <- Gen.option(Generator.stringMinMaxN(1, 60))
      bankAccountNumber     <- Gen.option(RegexpGen.from("""^[0-9]{8}$"""))
      sortCode              <- Gen.option(RegexpGen.from("""^[0-9]{6}$"""))
      buildingSocietyNumber <- Gen.option(Generator.stringMinMaxN(1, 20))
      bankBuildSocietyName  <- Gen.option(Generator.stringMinMaxN(1, 40))
    } yield
      BankDetails(
        IBAN = iban,
        BIC = bic,
        accountHolderName = accountHolderName,
        bankAccountNumber = bankAccountNumber,
        sortCode = sortCode,
        buildingSocietyNumber = buildingSocietyNumber,
        bankBuildSocietyName = bankBuildSocietyName
      )

    val validate: Validator[BankDetails] = Validator(
      check(_.IBAN.lengthMinMaxInclusive(1, 34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
      check(_.BIC.lengthMinMaxInclusive(1, 11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
      check(
        _.accountHolderName.lengthMinMaxInclusive(1, 60),
        "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
      check(
        _.bankAccountNumber.matches("""^[0-9]{8}$"""),
        """Invalid bankAccountNumber, does not matches regex ^[0-9]{8}$"""),
      check(_.sortCode.matches("""^[0-9]{6}$"""), """Invalid sortCode, does not matches regex ^[0-9]{6}$"""),
      check(
        _.buildingSocietyNumber.lengthMinMaxInclusive(1, 20),
        "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
      check(
        _.bankBuildSocietyName.lengthMinMaxInclusive(1, 40),
        "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[BankDetails] = Json.format[BankDetails]
  }

// schema path: #/definitions/businessActivitiesType
  case class BusinessActivities(
    primaryMainCode: String,
    mainCode2: Option[String] = None,
    mainCode3: Option[String] = None,
    mainCode4: Option[String] = None)

  object BusinessActivities extends RecordHelper[BusinessActivities] {

    override val gen: Gen[BusinessActivities] = for {
      primaryMainCode <- RegexpGen.from("""^[0-9]{5}$""")
      mainCode2       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
      mainCode3       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
      mainCode4       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
    } yield
      BusinessActivities(
        primaryMainCode = primaryMainCode,
        mainCode2 = mainCode2,
        mainCode3 = mainCode3,
        mainCode4 = mainCode4)

    val validate: Validator[BusinessActivities] = Validator(
      check(
        _.primaryMainCode.matches("""^[0-9]{5}$"""),
        """Invalid primaryMainCode, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode2.matches("""^[0-9]{5}$"""), """Invalid mainCode2, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode3.matches("""^[0-9]{5}$"""), """Invalid mainCode3, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode4.matches("""^[0-9]{5}$"""), """Invalid mainCode4, does not matches regex ^[0-9]{5}$""")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[BusinessActivities] = Json.format[BusinessActivities]
  }

// schema path: #/definitions/changeIndicatorsType
  case class ChangeIndicators(
    customerDetails: Boolean,
    PPOBDetails: Boolean,
    correspContactDetails: Boolean,
    bankDetails: Boolean,
    businessActivities: Boolean,
    flatRateScheme: Boolean,
    deRegistrationInfo: Boolean,
    returnPeriods: Boolean,
    groupOrPartners: Boolean)

  object ChangeIndicators extends RecordHelper[ChangeIndicators] {

    override val gen: Gen[ChangeIndicators] = for {
      customerDetails       <- booleanGen
      ppobdetails           <- booleanGen
      correspContactDetails <- booleanGen
      bankDetails           <- booleanGen
      businessActivities    <- booleanGen
      flatRateScheme        <- booleanGen
      deRegistrationInfo    <- booleanGen
      returnPeriods         <- booleanGen
      groupOrPartners       <- booleanGen
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

    val validate: Validator[ChangeIndicators] = Validator(
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[ChangeIndicators] = Json.format[ChangeIndicators]
  }

// schema path: #/definitions/contactDetailsType
  case class ContactDetails(
    primaryPhoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None)

  object ContactDetails extends RecordHelper[ContactDetails] {

    override val gen: Gen[ContactDetails] = for {
      primaryPhoneNumber <- Gen.option(RegexpGen.from("""^[A-Z0-9 )/(*#-]+$"""))
      mobileNumber       <- Gen.option(RegexpGen.from("""^[A-Z0-9 )/(*#-]+$"""))
      faxNumber          <- Gen.option(RegexpGen.from("""^[A-Z0-9 )/(*#-]+$"""))
      emailAddress       <- Gen.option(Generator.stringMinMaxN(3, 132))
    } yield
      ContactDetails(
        primaryPhoneNumber = primaryPhoneNumber,
        mobileNumber = mobileNumber,
        faxNumber = faxNumber,
        emailAddress = emailAddress)

    val validate: Validator[ContactDetails] = Validator(
      check(
        _.primaryPhoneNumber.matches("""^[A-Z0-9 )/(*#-]+$"""),
        """Invalid primaryPhoneNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
      check(
        _.mobileNumber.matches("""^[A-Z0-9 )/(*#-]+$"""),
        """Invalid mobileNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
      check(
        _.faxNumber.matches("""^[A-Z0-9 )/(*#-]+$"""),
        """Invalid faxNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
      check(
        _.emailAddress.lengthMinMaxInclusive(3, 132),
        "Invalid length of emailAddress, should be between 3 and 132 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]
  }

// schema path: #/definitions/correspondenceContactDetailsType
  case class CorrespondenceContactDetails(
    address: Address,
    RLS: Option[String] = None,
    contactDetails: Option[ContactDetails] = None)

  object CorrespondenceContactDetails extends RecordHelper[CorrespondenceContactDetails] {

    override val gen: Gen[CorrespondenceContactDetails] = for {
      address        <- Address.gen
      rls            <- Gen.option(Gen.oneOf(Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")))
      contactDetails <- Gen.option(ContactDetails.gen)
    } yield CorrespondenceContactDetails(address = address, RLS = rls, contactDetails = contactDetails)

    val validate: Validator[CorrespondenceContactDetails] = Validator(
      check(
        _.RLS.isOneOf(Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")),
        "Invalid RLS, does not match allowed values")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[CorrespondenceContactDetails] = Json.format[CorrespondenceContactDetails]
  }

// schema path: #/definitions/customerDetailsType
  case class CustomerDetails(
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    dateOfBirth: Option[String] = None,
    tradingName: Option[String] = None,
    mandationStatus: String,
    registrationReason: Option[String] = None,
    effectiveRegistrationDate: Option[String] = None,
    businessStartDate: Option[String] = None)

  object CustomerDetails extends RecordHelper[CustomerDetails] {

    override val gen: Gen[CustomerDetails] = for {
      organisationName <- Gen.option(Generator.stringMinMaxN(1, 105))
      individual       <- Gen.option(IndividualName.gen)
      dateOfBirth      <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      tradingName      <- Gen.option(Generator.stringMinMaxN(1, 160))
      mandationStatus  <- Gen.oneOf(Seq("1", "2", "3", "4"))
      registrationReason <- Gen.option(
                             Gen.oneOf(
                               Seq(
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
                                 "0014")))
      effectiveRegistrationDate <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      businessStartDate         <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
    } yield
      CustomerDetails(
        organisationName = organisationName,
        individual = individual,
        dateOfBirth = dateOfBirth,
        tradingName = tradingName,
        mandationStatus = mandationStatus,
        registrationReason = registrationReason,
        effectiveRegistrationDate = effectiveRegistrationDate,
        businessStartDate = businessStartDate
      )

    val validate: Validator[CustomerDetails] = Validator(
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      check(
        _.dateOfBirth.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid dateOfBirth, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive"),
      check(
        _.mandationStatus.isOneOf(Seq("1", "2", "3", "4")),
        "Invalid mandationStatus, does not match allowed values"),
      check(
        _.registrationReason.isOneOf(
          Seq(
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
            "0014")),
        "Invalid registrationReason, does not match allowed values"
      ),
      check(
        _.effectiveRegistrationDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid effectiveRegistrationDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.businessStartDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid businessStartDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[CustomerDetails] = Json.format[CustomerDetails]
  }

// schema path: #/definitions/deregistrationType
  case class Deregistration(
    deregistrationReason: Option[String] = None,
    effectDateOfCancellation: Option[String] = None,
    lastReturnDueDate: Option[String] = None)

  object Deregistration extends RecordHelper[Deregistration] {

    override val gen: Gen[Deregistration] = for {
      deregistrationReason <- Gen.option(
                               Gen.oneOf(
                                 Seq(
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
                                   "0011")))
      effectDateOfCancellation <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      lastReturnDueDate        <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
    } yield
      Deregistration(
        deregistrationReason = deregistrationReason,
        effectDateOfCancellation = effectDateOfCancellation,
        lastReturnDueDate = lastReturnDueDate)

    val validate: Validator[Deregistration] = Validator(
      check(
        _.deregistrationReason.isOneOf(
          Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011")),
        "Invalid deregistrationReason, does not match allowed values"
      ),
      check(
        _.effectDateOfCancellation.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid effectDateOfCancellation, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.lastReturnDueDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid lastReturnDueDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[Deregistration] = Json.format[Deregistration]
  }

// schema path: #/definitions/flatRateSchemeType
  case class FlatRateScheme(
    FRSCategory: Option[String] = None,
    FRSPercentage: Option[Int] = None,
    startDate: Option[String] = None,
    limitedCostTrader: Option[Boolean] = None)

  object FlatRateScheme extends RecordHelper[FlatRateScheme] {

    override val gen: Gen[FlatRateScheme] = for {
      frscategory <- Gen.option(
                      Gen.oneOf(Seq(
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
                      )))
      frspercentage     <- Gen.option(Gen.const(1))
      startDate         <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      limitedCostTrader <- Gen.option(booleanGen)
    } yield
      FlatRateScheme(
        FRSCategory = frscategory,
        FRSPercentage = frspercentage,
        startDate = startDate,
        limitedCostTrader = limitedCostTrader)

    val validate: Validator[FlatRateScheme] = Validator(
      check(
        _.FRSCategory.isOneOf(
          Seq(
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
          )),
        "Invalid FRSCategory, does not match allowed values"
      ),
      check(
        _.startDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid startDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[FlatRateScheme] = Json.format[FlatRateScheme]
  }

// schema path: #/definitions/formInformationType
  case class FormInformation(formBundle: String, dateReceived: String)

  object FormInformation extends RecordHelper[FormInformation] {

    override val gen: Gen[FormInformation] = for {
      formBundle   <- RegexpGen.from("""^[0-9]{12}$""")
      dateReceived <- RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
    } yield FormInformation(formBundle = formBundle, dateReceived = dateReceived)

    val validate: Validator[FormInformation] = Validator(
      check(_.formBundle.matches("""^[0-9]{12}$"""), """Invalid formBundle, does not matches regex ^[0-9]{12}$"""),
      check(
        _.dateReceived.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid dateReceived, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[FormInformation] = Json.format[FormInformation]
  }

// schema path: #/definitions/groupOrPartnerType
  case class GroupOrPartner(
    typeOfRelationship: String,
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    SAP_Number: String)

  object GroupOrPartner extends RecordHelper[GroupOrPartner] {

    override val gen: Gen[GroupOrPartner] = for {
      typeOfRelationship <- Gen.oneOf(Seq("01", "02", "03", "04"))
      organisationName   <- Gen.option(Generator.stringMinMaxN(1, 105))
      individual         <- Gen.option(IndividualName.gen)
      sap_number         <- RegexpGen.from("""^[0-9]{42}$""")
    } yield
      GroupOrPartner(
        typeOfRelationship = typeOfRelationship,
        organisationName = organisationName,
        individual = individual,
        SAP_Number = sap_number)

    val validate: Validator[GroupOrPartner] = Validator(
      check(
        _.typeOfRelationship.isOneOf(Seq("01", "02", "03", "04")),
        "Invalid typeOfRelationship, does not match allowed values"),
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      check(_.SAP_Number.matches("""^[0-9]{42}$"""), """Invalid SAP_Number, does not matches regex ^[0-9]{42}$""")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[GroupOrPartner] = Json.format[GroupOrPartner]
  }

// schema path: #/definitions/inFlightBankDetailsType
  case class InFlightBankDetails(
    formInformation: FormInformation,
    IBAN: Option[String] = None,
    BIC: Option[String] = None,
    accountHolderName: Option[String] = None,
    bankAccountNumber: Option[String] = None,
    sortCode: Option[String] = None,
    buildingSocietyNumber: Option[String] = None,
    bankBuildSocietyName: Option[String] = None)

  object InFlightBankDetails extends RecordHelper[InFlightBankDetails] {

    override val gen: Gen[InFlightBankDetails] = for {
      formInformation       <- FormInformation.gen
      iban                  <- Gen.option(Generator.stringMinMaxN(1, 34))
      bic                   <- Gen.option(Generator.stringMinMaxN(1, 11))
      accountHolderName     <- Gen.option(Generator.stringMinMaxN(1, 60))
      bankAccountNumber     <- Gen.option(RegexpGen.from("""^[0-9]{8}$"""))
      sortCode              <- Gen.option(RegexpGen.from("""^[0-9]{6}$"""))
      buildingSocietyNumber <- Gen.option(Generator.stringMinMaxN(1, 20))
      bankBuildSocietyName  <- Gen.option(Generator.stringMinMaxN(1, 40))
    } yield
      InFlightBankDetails(
        formInformation = formInformation,
        IBAN = iban,
        BIC = bic,
        accountHolderName = accountHolderName,
        bankAccountNumber = bankAccountNumber,
        sortCode = sortCode,
        buildingSocietyNumber = buildingSocietyNumber,
        bankBuildSocietyName = bankBuildSocietyName
      )

    val validate: Validator[InFlightBankDetails] = Validator(
      check(_.IBAN.lengthMinMaxInclusive(1, 34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
      check(_.BIC.lengthMinMaxInclusive(1, 11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
      check(
        _.accountHolderName.lengthMinMaxInclusive(1, 60),
        "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
      check(
        _.bankAccountNumber.matches("""^[0-9]{8}$"""),
        """Invalid bankAccountNumber, does not matches regex ^[0-9]{8}$"""),
      check(_.sortCode.matches("""^[0-9]{6}$"""), """Invalid sortCode, does not matches regex ^[0-9]{6}$"""),
      check(
        _.buildingSocietyNumber.lengthMinMaxInclusive(1, 20),
        "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
      check(
        _.bankBuildSocietyName.lengthMinMaxInclusive(1, 40),
        "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightBankDetails] = Json.format[InFlightBankDetails]
  }

// schema path: #/definitions/inFlightBusinessActivitiesType
  case class InFlightBusinessActivities(
    formInformation: FormInformation,
    primaryMainCode: String,
    mainCode2: Option[String] = None,
    mainCode3: Option[String] = None,
    mainCode4: Option[String] = None)

  object InFlightBusinessActivities extends RecordHelper[InFlightBusinessActivities] {

    override val gen: Gen[InFlightBusinessActivities] = for {
      formInformation <- FormInformation.gen
      primaryMainCode <- RegexpGen.from("""^[0-9]{5}$""")
      mainCode2       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
      mainCode3       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
      mainCode4       <- Gen.option(RegexpGen.from("""^[0-9]{5}$"""))
    } yield
      InFlightBusinessActivities(
        formInformation = formInformation,
        primaryMainCode = primaryMainCode,
        mainCode2 = mainCode2,
        mainCode3 = mainCode3,
        mainCode4 = mainCode4)

    val validate: Validator[InFlightBusinessActivities] = Validator(
      check(
        _.primaryMainCode.matches("""^[0-9]{5}$"""),
        """Invalid primaryMainCode, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode2.matches("""^[0-9]{5}$"""), """Invalid mainCode2, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode3.matches("""^[0-9]{5}$"""), """Invalid mainCode3, does not matches regex ^[0-9]{5}$"""),
      check(_.mainCode4.matches("""^[0-9]{5}$"""), """Invalid mainCode4, does not matches regex ^[0-9]{5}$""")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightBusinessActivities] = Json.format[InFlightBusinessActivities]
  }

// schema path: #/definitions/inFlightCorrespondenceContactDetailsType
  case class InFlightCorrespondenceContactDetails(
    formInformation: FormInformation,
    address: Option[Address] = None,
    contactDetails: Option[ContactDetails] = None)

  object InFlightCorrespondenceContactDetails extends RecordHelper[InFlightCorrespondenceContactDetails] {

    override val gen: Gen[InFlightCorrespondenceContactDetails] = for {
      formInformation <- FormInformation.gen
      address         <- Gen.option(Address.gen)
      contactDetails  <- Gen.option(ContactDetails.gen)
    } yield
      InFlightCorrespondenceContactDetails(
        formInformation = formInformation,
        address = address,
        contactDetails = contactDetails)

    val validate: Validator[InFlightCorrespondenceContactDetails] = Validator(
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightCorrespondenceContactDetails] =
      Json.format[InFlightCorrespondenceContactDetails]
  }

// schema path: #/definitions/inFlightCustomerDetailsType
  case class InFlightCustomerDetails(
    formInformation: FormInformation,
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    dateOfBirth: Option[String] = None,
    tradingName: Option[String] = None,
    mandationStatus: String,
    registrationReason: Option[String] = None,
    effectiveRegistrationDate: Option[String] = None)

  object InFlightCustomerDetails extends RecordHelper[InFlightCustomerDetails] {

    override val gen: Gen[InFlightCustomerDetails] = for {
      formInformation  <- FormInformation.gen
      organisationName <- Gen.option(Generator.stringMinMaxN(1, 105))
      individual       <- Gen.option(IndividualName.gen)
      dateOfBirth      <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      tradingName      <- Gen.option(Generator.stringMinMaxN(1, 160))
      mandationStatus  <- Gen.oneOf(Seq("1", "2", "3", "4"))
      registrationReason <- Gen.option(
                             Gen.oneOf(
                               Seq(
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
                                 "0014")))
      effectiveRegistrationDate <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
    } yield
      InFlightCustomerDetails(
        formInformation = formInformation,
        organisationName = organisationName,
        individual = individual,
        dateOfBirth = dateOfBirth,
        tradingName = tradingName,
        mandationStatus = mandationStatus,
        registrationReason = registrationReason,
        effectiveRegistrationDate = effectiveRegistrationDate
      )

    val validate: Validator[InFlightCustomerDetails] = Validator(
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      check(
        _.dateOfBirth.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid dateOfBirth, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive"),
      check(
        _.mandationStatus.isOneOf(Seq("1", "2", "3", "4")),
        "Invalid mandationStatus, does not match allowed values"),
      check(
        _.registrationReason.isOneOf(
          Seq(
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
            "0014")),
        "Invalid registrationReason, does not match allowed values"
      ),
      check(
        _.effectiveRegistrationDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid effectiveRegistrationDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightCustomerDetails] = Json.format[InFlightCustomerDetails]
  }

// schema path: #/definitions/inFlightDeregistrationType
  case class InFlightDeregistration(
    formInformation: FormInformation,
    deregistrationReason: String,
    deregDate: Option[String] = None,
    deregDateInFuture: Option[String] = None)

  object InFlightDeregistration extends RecordHelper[InFlightDeregistration] {

    override val gen: Gen[InFlightDeregistration] = for {
      formInformation <- FormInformation.gen
      deregistrationReason <- Gen.oneOf(
                               Seq(
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
                                 "0011"))
      deregDate         <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      deregDateInFuture <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
    } yield
      InFlightDeregistration(
        formInformation = formInformation,
        deregistrationReason = deregistrationReason,
        deregDate = deregDate,
        deregDateInFuture = deregDateInFuture)

    val validate: Validator[InFlightDeregistration] = Validator(
      check(
        _.deregistrationReason.isOneOf(
          Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011")),
        "Invalid deregistrationReason, does not match allowed values"
      ),
      check(
        _.deregDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid deregDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.deregDateInFuture.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid deregDateInFuture, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightDeregistration] = Json.format[InFlightDeregistration]
  }

// schema path: #/definitions/inFlightFlatRateSchemeType
  case class InFlightFlatRateScheme(
    formInformation: FormInformation,
    FRSCategory: Option[String] = None,
    FRSPercentage: Option[Int] = None,
    startDate: Option[String] = None,
    limitedCostTrader: Option[Boolean] = None)

  object InFlightFlatRateScheme extends RecordHelper[InFlightFlatRateScheme] {

    override val gen: Gen[InFlightFlatRateScheme] = for {
      formInformation <- FormInformation.gen
      frscategory <- Gen.option(
                      Gen.oneOf(Seq(
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
                      )))
      frspercentage     <- Gen.option(Gen.const(1))
      startDate         <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      limitedCostTrader <- Gen.option(booleanGen)
    } yield
      InFlightFlatRateScheme(
        formInformation = formInformation,
        FRSCategory = frscategory,
        FRSPercentage = frspercentage,
        startDate = startDate,
        limitedCostTrader = limitedCostTrader)

    val validate: Validator[InFlightFlatRateScheme] = Validator(
      check(
        _.FRSCategory.isOneOf(
          Seq(
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
          )),
        "Invalid FRSCategory, does not match allowed values"
      ),
      check(
        _.startDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid startDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightFlatRateScheme] = Json.format[InFlightFlatRateScheme]
  }

// schema path: #/definitions/inFlightGroupOrPartnerType
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
    PPOB: Option[PPOB] = None)

  object InFlightGroupOrPartner extends RecordHelper[InFlightGroupOrPartner] {

    override val gen: Gen[InFlightGroupOrPartner] = for {
      formInformation     <- FormInformation.gen
      action              <- Gen.oneOf(Seq("1", "2", "3", "4"))
      sap_number          <- Gen.option(RegexpGen.from("""^[0-9]{42}$"""))
      typeOfRelationship  <- Gen.option(Gen.oneOf(Seq("01", "02", "03", "04")))
      makeGrpMember       <- Gen.option(booleanGen)
      makeControllingBody <- Gen.option(booleanGen)
      isControllingBody   <- Gen.option(booleanGen)
      organisationName    <- Gen.option(Generator.stringMinMaxN(1, 160))
      tradingName         <- Gen.option(Generator.stringMinMaxN(1, 160))
      individual          <- Gen.option(IndividualName.gen)
      ppob                <- Gen.option(PPOB.gen)
    } yield
      InFlightGroupOrPartner(
        formInformation = formInformation,
        action = action,
        SAP_Number = sap_number,
        typeOfRelationship = typeOfRelationship,
        makeGrpMember = makeGrpMember,
        makeControllingBody = makeControllingBody,
        isControllingBody = isControllingBody,
        organisationName = organisationName,
        tradingName = tradingName,
        individual = individual,
        PPOB = ppob
      )

    val validate: Validator[InFlightGroupOrPartner] = Validator(
      check(_.action.isOneOf(Seq("1", "2", "3", "4")), "Invalid action, does not match allowed values"),
      check(_.SAP_Number.matches("""^[0-9]{42}$"""), """Invalid SAP_Number, does not matches regex ^[0-9]{42}$"""),
      check(
        _.typeOfRelationship.isOneOf(Seq("01", "02", "03", "04")),
        "Invalid typeOfRelationship, does not match allowed values"),
      check(
        _.organisationName.lengthMinMaxInclusive(1, 160),
        "Invalid length of organisationName, should be between 1 and 160 inclusive"),
      check(
        _.tradingName.lengthMinMaxInclusive(1, 160),
        "Invalid length of tradingName, should be between 1 and 160 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightGroupOrPartner] = Json.format[InFlightGroupOrPartner]
  }

// schema path: #/definitions/inFlightInformationType
  case class InFlightInformation(changeIndicators: ChangeIndicators, inflightChanges: InflightChanges)

  object InFlightInformation extends RecordHelper[InFlightInformation] {

    override val gen: Gen[InFlightInformation] = for {
      changeIndicators <- ChangeIndicators.gen
      inflightChanges  <- InflightChanges.gen
    } yield InFlightInformation(changeIndicators = changeIndicators, inflightChanges = inflightChanges)

    val validate: Validator[InFlightInformation] = Validator(
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightInformation] = Json.format[InFlightInformation]
  }

// schema path: #/definitions/inFlightPPOBDetailsType
  case class InFlightPPOBDetails(
    formInformation: FormInformation,
    address: Option[Address] = None,
    contactDetails: Option[ContactDetails] = None,
    websiteAddress: Option[String] = None)

  object InFlightPPOBDetails extends RecordHelper[InFlightPPOBDetails] {

    override val gen: Gen[InFlightPPOBDetails] = for {
      formInformation <- FormInformation.gen
      address         <- Gen.option(Address.gen)
      contactDetails  <- Gen.option(ContactDetails.gen)
      websiteAddress  <- Gen.option(Generator.stringMinMaxN(1, 132))
    } yield
      InFlightPPOBDetails(
        formInformation = formInformation,
        address = address,
        contactDetails = contactDetails,
        websiteAddress = websiteAddress)

    val validate: Validator[InFlightPPOBDetails] = Validator(
      check(
        _.websiteAddress.lengthMinMaxInclusive(1, 132),
        "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightPPOBDetails] = Json.format[InFlightPPOBDetails]
  }

// schema path: #/definitions/inFlightReturnPeriod
  case class InFlightReturnPeriod(
    formInformation: FormInformation,
    changeReturnPeriod: Option[Boolean] = None,
    nonStdTaxPeriodsRequested: Option[Boolean] = None,
    ceaseNonStdTaxPeriods: Option[Boolean] = None,
    stdReturnPeriod: Option[String] = None,
    nonStdTaxPeriods: Option[NonStdTaxPeriods] = None)

  object InFlightReturnPeriod extends RecordHelper[InFlightReturnPeriod] {

    override val gen: Gen[InFlightReturnPeriod] = for {
      formInformation           <- FormInformation.gen
      changeReturnPeriod        <- Gen.option(booleanGen)
      nonStdTaxPeriodsRequested <- Gen.option(booleanGen)
      ceaseNonStdTaxPeriods     <- Gen.option(booleanGen)
      stdReturnPeriod           <- Gen.option(Gen.oneOf(Seq("MA", "MB", "MC", "MM")))
      nonStdTaxPeriods          <- Gen.option(NonStdTaxPeriods.gen)
    } yield
      InFlightReturnPeriod(
        formInformation = formInformation,
        changeReturnPeriod = changeReturnPeriod,
        nonStdTaxPeriodsRequested = nonStdTaxPeriodsRequested,
        ceaseNonStdTaxPeriods = ceaseNonStdTaxPeriods,
        stdReturnPeriod = stdReturnPeriod,
        nonStdTaxPeriods = nonStdTaxPeriods
      )

    val validate: Validator[InFlightReturnPeriod] = Validator(
      check(
        _.stdReturnPeriod.isOneOf(Seq("MA", "MB", "MC", "MM")),
        "Invalid stdReturnPeriod, does not match allowed values")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InFlightReturnPeriod] = Json.format[InFlightReturnPeriod]
  }

// schema path: #/definitions/individualNameType
  case class IndividualName(
    title: Option[String] = None,
    firstName: Option[String] = None,
    middleName: Option[String] = None,
    lastName: Option[String] = None)

  object IndividualName extends RecordHelper[IndividualName] {

    override val gen: Gen[IndividualName] = for {
      title <- Gen.option(
                Gen.oneOf(
                  Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011", "0012")))
      firstName  <- Gen.option(Generator.stringMinMaxN(1, 35))
      middleName <- Gen.option(Generator.stringMinMaxN(1, 35))
      lastName   <- Gen.option(Generator.stringMinMaxN(1, 35))
    } yield IndividualName(title = title, firstName = firstName, middleName = middleName, lastName = lastName)

    val validate: Validator[IndividualName] = Validator(
      check(
        _.title.isOneOf(
          Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010", "0011", "0012")),
        "Invalid title, does not match allowed values"
      ),
      check(
        _.firstName.lengthMinMaxInclusive(1, 35),
        "Invalid length of firstName, should be between 1 and 35 inclusive"),
      check(
        _.middleName.lengthMinMaxInclusive(1, 35),
        "Invalid length of middleName, should be between 1 and 35 inclusive"),
      check(_.lastName.lengthMinMaxInclusive(1, 35), "Invalid length of lastName, should be between 1 and 35 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[IndividualName] = Json.format[IndividualName]
  }

// schema path: #/definitions/inflightChangesType
  case class InflightChanges(
    customerDetails: Option[InFlightCustomerDetails] = None,
    PPOBDetails: Option[InFlightPPOBDetails] = None,
    correspondenceContactDetails: Option[InFlightCorrespondenceContactDetails] = None,
    bankDetails: Option[InFlightBankDetails] = None,
    businessActivities: Option[InFlightBusinessActivities] = None,
    flatRateScheme: Option[InFlightFlatRateScheme] = None,
    deregister: Option[InFlightDeregistration] = None,
    returnPeriod: Option[InFlightReturnPeriod] = None,
    groupOrPartner: Option[Seq[InFlightGroupOrPartner]] = None)

  object InflightChanges extends RecordHelper[InflightChanges] {

    override val gen: Gen[InflightChanges] = for {
      customerDetails              <- Gen.option(InFlightCustomerDetails.gen)
      ppobdetails                  <- Gen.option(InFlightPPOBDetails.gen)
      correspondenceContactDetails <- Gen.option(InFlightCorrespondenceContactDetails.gen)
      bankDetails                  <- Gen.option(InFlightBankDetails.gen)
      businessActivities           <- Gen.option(InFlightBusinessActivities.gen)
      flatRateScheme               <- Gen.option(InFlightFlatRateScheme.gen)
      deregister                   <- Gen.option(InFlightDeregistration.gen)
      returnPeriod                 <- Gen.option(InFlightReturnPeriod.gen)
      groupOrPartner               <- Gen.option(Gen.nonEmptyListOf(InFlightGroupOrPartner.gen))
    } yield
      InflightChanges(
        customerDetails = customerDetails,
        PPOBDetails = ppobdetails,
        correspondenceContactDetails = correspondenceContactDetails,
        bankDetails = bankDetails,
        businessActivities = businessActivities,
        flatRateScheme = flatRateScheme,
        deregister = deregister,
        returnPeriod = returnPeriod,
        groupOrPartner = groupOrPartner
      )

    val validate: Validator[InflightChanges] = Validator(
      )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[InflightChanges] = Json.format[InflightChanges]
  }

// schema path: #/definitions/nonStdTaxPeriodsType
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
    period22: Option[String] = None)

  object NonStdTaxPeriods extends RecordHelper[NonStdTaxPeriods] {

    override val gen: Gen[NonStdTaxPeriods] = for {
      period01 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period02 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period03 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period04 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period05 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period06 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period07 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period08 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period09 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period10 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period11 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period12 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period13 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period14 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period15 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period16 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period17 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period18 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period19 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period20 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period21 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
      period22 <- Gen.option(RegexpGen.from("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
    } yield
      NonStdTaxPeriods(
        period01 = period01,
        period02 = period02,
        period03 = period03,
        period04 = period04,
        period05 = period05,
        period06 = period06,
        period07 = period07,
        period08 = period08,
        period09 = period09,
        period10 = period10,
        period11 = period11,
        period12 = period12,
        period13 = period13,
        period14 = period14,
        period15 = period15,
        period16 = period16,
        period17 = period17,
        period18 = period18,
        period19 = period19,
        period20 = period20,
        period21 = period21,
        period22 = period22
      )

    val validate: Validator[NonStdTaxPeriods] = Validator(
      check(
        _.period01.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period01, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period02.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period02, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period03.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period03, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period04.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period04, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period05.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period05, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period06.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period06, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period07.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period07, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period08.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period08, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period09.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period09, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period10.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period10, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period11.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period11, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period12.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period12, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period13.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period13, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period14.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period14, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period15.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period15, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period16.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period16, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period17.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period17, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period18.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period18, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period19.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period19, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period20.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period20, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period21.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period21, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      ),
      check(
        _.period22.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
        """Invalid period22, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
      )
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[NonStdTaxPeriods] = Json.format[NonStdTaxPeriods]
  }

// schema path: #/definitions/PPOBType
  case class PPOB(
    address: Address,
    RLS: Option[String] = None,
    contactDetails: Option[ContactDetails] = None,
    websiteAddress: Option[String] = None)

  object PPOB extends RecordHelper[PPOB] {

    override val gen: Gen[PPOB] = for {
      address        <- Address.gen
      rls            <- Gen.option(Gen.oneOf(Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")))
      contactDetails <- Gen.option(ContactDetails.gen)
      websiteAddress <- Gen.option(Generator.stringMinMaxN(1, 132))
    } yield PPOB(address = address, RLS = rls, contactDetails = contactDetails, websiteAddress = websiteAddress)

    val validate: Validator[PPOB] = Validator(
      check(
        _.RLS.isOneOf(Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")),
        "Invalid RLS, does not match allowed values"),
      check(
        _.websiteAddress.lengthMinMaxInclusive(1, 132),
        "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[PPOB] = Json.format[PPOB]
  }

// schema path: #/definitions/periodType
  case class Period(stdReturnPeriod: Option[String] = None, nonStdTaxPeriods: Option[NonStdTaxPeriods] = None)

  object Period extends RecordHelper[Period] {

    override val gen: Gen[Period] = for {
      stdReturnPeriod  <- Gen.option(Gen.oneOf(Seq("MA", "MB", "MC", "MM")))
      nonStdTaxPeriods <- Gen.option(NonStdTaxPeriods.gen)
    } yield Period(stdReturnPeriod = stdReturnPeriod, nonStdTaxPeriods = nonStdTaxPeriods)

    val validate: Validator[Period] = Validator(
      check(
        _.stdReturnPeriod.isOneOf(Seq("MA", "MB", "MC", "MM")),
        "Invalid stdReturnPeriod, does not match allowed values")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[Period] = Json.format[Period]
  }

// schema path: #/definitions/ukAddressType
  case class Address(
    line1: String,
    line2: String,
    line3: Option[String] = None,
    line4: Option[String] = None,
    postCode: String,
    countryCode: String)

  object Address extends RecordHelper[Address] {

    override val gen: Gen[Address] = for {
      line1       <- RegexpGen.from("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$""")
      line2       <- RegexpGen.from("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$""")
      line3       <- Gen.option(RegexpGen.from("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""))
      line4       <- Gen.option(RegexpGen.from("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""))
      postCode    <- RegexpGen.from("""^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$""")
      countryCode <- Gen.const("GB")
    } yield
      Address(
        line1 = line1,
        line2 = line2,
        line3 = line3,
        line4 = line4,
        postCode = postCode,
        countryCode = countryCode)

    val validate: Validator[Address] = Validator(
      check(
        _.line1.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
        """Invalid line1, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
      check(
        _.line2.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
        """Invalid line2, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
      check(
        _.line3.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
        """Invalid line3, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
      check(
        _.line4.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
        """Invalid line4, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
      check(
        _.postCode.matches("""^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""),
        """Invalid postCode, does not matches regex ^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""
      ),
      check(_.countryCode.isOneOf(Seq("GB")), "Invalid countryCode, does not match allowed values")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[Address] = Json.format[Address]
  }

  val validate: Validator[VatCustomerInformationRecord] = Validator(
    )

  override val sanitizers: Seq[Update] = Seq()

  implicit val formats: Format[VatCustomerInformationRecord] = Json.format[VatCustomerInformationRecord]
}
