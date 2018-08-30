
package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord._

/**
  * ----------------------------------------------------------------------------
  * This VatCustomerInformationRecord code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */


// schema path: #
case class VatCustomerInformationRecord(
  vrn: String,
  approvedInformation: Option[ApprovedInformation] = None,
  inFlightInformation: Option[InFlightInformation] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Some(vrn).map(VatCustomerInformationRecord.uniqueKey)
  override def lookupKeys: Seq[String] = Seq()
  override def withId(id: Option[String]): Record = copy(id = id)
}


object VatCustomerInformationRecord extends RecordUtils[VatCustomerInformationRecord] {
  
  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[VatCustomerInformationRecord] = RecordMetaData[VatCustomerInformationRecord](this)
  def uniqueKey(key: String): String = s"""vrn:${key.toUpperCase}"""
  import Validator._
         
  override val gen: Gen[VatCustomerInformationRecord] = for {
    vrn <- Generator.regex("""^[0-9A-Za-z]{1,6}$""")
  approvedInformation <- Generator.biasedOptionGen(ApprovedInformation.gen)
  inFlightInformation <- Generator.biasedOptionGen(InFlightInformation.gen)
  } yield VatCustomerInformationRecord(vrn = vrn,
  approvedInformation = approvedInformation,
  inFlightInformation = inFlightInformation)
  
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

object ApprovedInformation extends RecordUtils[ApprovedInformation] {
  
  override val gen: Gen[ApprovedInformation] = for {
    customerDetails <- CustomerDetails.gen
  ppob <- PPOB.gen
  correspondenceContactDetails <- Generator.biasedOptionGen(CorrespondenceContactDetails.gen)
  bankDetails <- Generator.biasedOptionGen(BankDetails.gen)
  businessActivities <- Generator.biasedOptionGen(BusinessActivities.gen)
  flatRateScheme <- Generator.biasedOptionGen(FlatRateScheme.gen)
  deregistration <- Generator.biasedOptionGen(Deregistration.gen)
  returnPeriod <- Generator.biasedOptionGen(Period.gen)
  groupOrPartnerMbrs <- Generator.biasedOptionGen(Generator.nonEmptyListOfMaxN(3,GroupOrPartner.gen))
  } yield ApprovedInformation(customerDetails = customerDetails,
  PPOB = ppob,
  correspondenceContactDetails = correspondenceContactDetails,
  bankDetails = bankDetails,
  businessActivities = businessActivities,
  flatRateScheme = flatRateScheme,
  deregistration = deregistration,
  returnPeriod = returnPeriod,
  groupOrPartnerMbrs = groupOrPartnerMbrs)
  
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

object BankDetails extends RecordUtils[BankDetails] {
  
  override val gen: Gen[BankDetails] = for {
    iban <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,34))
  bic <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,11))
  accountHolderName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,60))
  bankAccountNumber <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{8}$"""))
  sortCode <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{6}$"""))
  buildingSocietyNumber <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,20))
  bankBuildSocietyName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,40))
  } yield BankDetails(IBAN = iban,
  BIC = bic,
  accountHolderName = accountHolderName,
  bankAccountNumber = bankAccountNumber,
  sortCode = sortCode,
  buildingSocietyNumber = buildingSocietyNumber,
  bankBuildSocietyName = bankBuildSocietyName)
  
  override val validate: Validator[BankDetails] = Validator(
    check(_.IBAN.lengthMinMaxInclusive(1,34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
    check(_.BIC.lengthMinMaxInclusive(1,11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
    check(_.accountHolderName.lengthMinMaxInclusive(1,60), "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
    check(_.bankAccountNumber.matches("""^[0-9]{8}$"""), """Invalid bankAccountNumber, does not matches regex ^[0-9]{8}$"""),
    check(_.sortCode.matches("""^[0-9]{6}$"""), """Invalid sortCode, does not matches regex ^[0-9]{6}$"""),
    check(_.buildingSocietyNumber.lengthMinMaxInclusive(1,20), "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
    check(_.bankBuildSocietyName.lengthMinMaxInclusive(1,40), "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
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

object BusinessActivities extends RecordUtils[BusinessActivities] {
  
  override val gen: Gen[BusinessActivities] = for {
    primaryMainCode <- Generator.regex("""^[0-9]{5}$""")
  mainCode2 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  mainCode3 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  mainCode4 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  } yield BusinessActivities(primaryMainCode = primaryMainCode,
  mainCode2 = mainCode2,
  mainCode3 = mainCode3,
  mainCode4 = mainCode4)
  
  override val validate: Validator[BusinessActivities] = Validator(
    check(_.primaryMainCode.matches("""^[0-9]{5}$"""), """Invalid primaryMainCode, does not matches regex ^[0-9]{5}$"""),
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

object ChangeIndicators extends RecordUtils[ChangeIndicators] {
  
  override val gen: Gen[ChangeIndicators] = for {
    customerDetails <- Generator.biasedBooleanGen
  ppobdetails <- Generator.biasedBooleanGen
  correspContactDetails <- Generator.biasedBooleanGen
  bankDetails <- Generator.biasedBooleanGen
  businessActivities <- Generator.biasedBooleanGen
  flatRateScheme <- Generator.biasedBooleanGen
  deRegistrationInfo <- Generator.biasedBooleanGen
  returnPeriods <- Generator.biasedBooleanGen
  groupOrPartners <- Generator.biasedBooleanGen
  } yield ChangeIndicators(customerDetails = customerDetails,
  PPOBDetails = ppobdetails,
  correspContactDetails = correspContactDetails,
  bankDetails = bankDetails,
  businessActivities = businessActivities,
  flatRateScheme = flatRateScheme,
  deRegistrationInfo = deRegistrationInfo,
  returnPeriods = returnPeriods,
  groupOrPartners = groupOrPartners)
  
  override val validate: Validator[ChangeIndicators] = Validator(
  
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

object ContactDetails extends RecordUtils[ContactDetails] {
  
  override val gen: Gen[ContactDetails] = for {
    primaryPhoneNumber <- Generator.biasedOptionGen(Generator.regex("""^[A-Z0-9 )/(*#-]+$"""))
  mobileNumber <- Generator.biasedOptionGen(Generator.regex("""^[A-Z0-9 )/(*#-]+$"""))
  faxNumber <- Generator.biasedOptionGen(Generator.regex("""^[A-Z0-9 )/(*#-]+$"""))
  emailAddress <- Generator.biasedOptionGen(Generator.stringMinMaxN(3,132))
  } yield ContactDetails(primaryPhoneNumber = primaryPhoneNumber,
  mobileNumber = mobileNumber,
  faxNumber = faxNumber,
  emailAddress = emailAddress)
  
  override val validate: Validator[ContactDetails] = Validator(
    check(_.primaryPhoneNumber.matches("""^[A-Z0-9 )/(*#-]+$"""), """Invalid primaryPhoneNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
    check(_.mobileNumber.matches("""^[A-Z0-9 )/(*#-]+$"""), """Invalid mobileNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
    check(_.faxNumber.matches("""^[A-Z0-9 )/(*#-]+$"""), """Invalid faxNumber, does not matches regex ^[A-Z0-9 )/(*#-]+$"""),
    check(_.emailAddress.lengthMinMaxInclusive(3,132), "Invalid length of emailAddress, should be between 3 and 132 inclusive")
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]
}

     

// schema path: #/definitions/correspondenceContactDetailsType
case class CorrespondenceContactDetails(
  address: Address,
  RLS: Option[String] = None,
  contactDetails: Option[ContactDetails] = None)

object CorrespondenceContactDetails extends RecordUtils[CorrespondenceContactDetails] {
  
  override val gen: Gen[CorrespondenceContactDetails] = for {
    address <- Address.gen
  rls <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009")))
  contactDetails <- Generator.biasedOptionGen(ContactDetails.gen)
  } yield CorrespondenceContactDetails(address = address,
  RLS = rls,
  contactDetails = contactDetails)
  
  override val validate: Validator[CorrespondenceContactDetails] = Validator(
    check(_.RLS.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009")), "Invalid RLS, does not match allowed values"),
   checkObjectIfSome(_.contactDetails, ContactDetails.validate)
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

object CustomerDetails extends RecordUtils[CustomerDetails] {
  
  override val gen: Gen[CustomerDetails] = for {
    organisationName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,105))
  individual <- Generator.biasedOptionGen(IndividualName.gen)
  dateOfBirth <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  tradingName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,160))
  mandationStatus <- Gen.oneOf(Seq("1","2","3","4"))
  registrationReason <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012","0013","0014")))
  effectiveRegistrationDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  businessStartDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  } yield CustomerDetails(organisationName = organisationName,
  individual = individual,
  dateOfBirth = dateOfBirth,
  tradingName = tradingName,
  mandationStatus = mandationStatus,
  registrationReason = registrationReason,
  effectiveRegistrationDate = effectiveRegistrationDate,
  businessStartDate = businessStartDate)
  
  override val validate: Validator[CustomerDetails] = Validator(
    check(_.organisationName.lengthMinMaxInclusive(1,105), "Invalid length of organisationName, should be between 1 and 105 inclusive"),
   checkObjectIfSome(_.individual, IndividualName.validate),
    check(_.dateOfBirth.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid dateOfBirth, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.tradingName.lengthMinMaxInclusive(1,160), "Invalid length of tradingName, should be between 1 and 160 inclusive"),
    check(_.mandationStatus.isOneOf(Seq("1","2","3","4")), "Invalid mandationStatus, does not match allowed values"),
    check(_.registrationReason.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012","0013","0014")), "Invalid registrationReason, does not match allowed values"),
    check(_.effectiveRegistrationDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid effectiveRegistrationDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.businessStartDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid businessStartDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[CustomerDetails] = Json.format[CustomerDetails]
}

     

// schema path: #/definitions/deregistrationType
case class Deregistration(
  deregistrationReason: Option[String] = None,
  effectDateOfCancellation: Option[String] = None,
  lastReturnDueDate: Option[String] = None)

object Deregistration extends RecordUtils[Deregistration] {
  
  override val gen: Gen[Deregistration] = for {
    deregistrationReason <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011")))
  effectDateOfCancellation <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  lastReturnDueDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  } yield Deregistration(deregistrationReason = deregistrationReason,
  effectDateOfCancellation = effectDateOfCancellation,
  lastReturnDueDate = lastReturnDueDate)
  
  override val validate: Validator[Deregistration] = Validator(
    check(_.deregistrationReason.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011")), "Invalid deregistrationReason, does not match allowed values"),
    check(_.effectDateOfCancellation.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid effectDateOfCancellation, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.lastReturnDueDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid lastReturnDueDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object FlatRateScheme extends RecordUtils[FlatRateScheme] {
  
  override val gen: Gen[FlatRateScheme] = for {
    frscategory <- Generator.biasedOptionGen(Gen.oneOf(Seq("001","002","003","004","005","006","007","008","009","010","011","012","013","014","015","016","017","018","019","020","021","022","023","024","025","026","027","028","029","030","031","032","033","034","035","036","037","038","039","040","041","042","043","044","045","046","047","048","049","050","051","052","053","054")))
  frspercentage <- Generator.biasedOptionGen(Gen.const(1))
  startDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  limitedCostTrader <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  } yield FlatRateScheme(FRSCategory = frscategory,
  FRSPercentage = frspercentage,
  startDate = startDate,
  limitedCostTrader = limitedCostTrader)
  
  override val validate: Validator[FlatRateScheme] = Validator(
    check(_.FRSCategory.isOneOf(Seq("001","002","003","004","005","006","007","008","009","010","011","012","013","014","015","016","017","018","019","020","021","022","023","024","025","026","027","028","029","030","031","032","033","034","035","036","037","038","039","040","041","042","043","044","045","046","047","048","049","050","051","052","053","054")), "Invalid FRSCategory, does not match allowed values"),
    check(_.startDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid startDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[FlatRateScheme] = Json.format[FlatRateScheme]
}

     

// schema path: #/definitions/formInformationType
case class FormInformation(
  formBundle: String,
  dateReceived: String)

object FormInformation extends RecordUtils[FormInformation] {
  
  override val gen: Gen[FormInformation] = for {
    formBundle <- Generator.regex("""^[0-9]{12}$""")
  dateReceived <- Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
  } yield FormInformation(formBundle = formBundle,
  dateReceived = dateReceived)
  
  override val validate: Validator[FormInformation] = Validator(
    check(_.formBundle.matches("""^[0-9]{12}$"""), """Invalid formBundle, does not matches regex ^[0-9]{12}$"""),
    check(_.dateReceived.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid dateReceived, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object GroupOrPartner extends RecordUtils[GroupOrPartner] {
  
  override val gen: Gen[GroupOrPartner] = for {
    typeOfRelationship <- Gen.oneOf(Seq("01","02","03","04"))
  organisationName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,105))
  individual <- Generator.biasedOptionGen(IndividualName.gen)
  sap_number <- Generator.regex("""^[0-9]{42}$""")
  } yield GroupOrPartner(typeOfRelationship = typeOfRelationship,
  organisationName = organisationName,
  individual = individual,
  SAP_Number = sap_number)
  
  override val validate: Validator[GroupOrPartner] = Validator(
    check(_.typeOfRelationship.isOneOf(Seq("01","02","03","04")), "Invalid typeOfRelationship, does not match allowed values"),
    check(_.organisationName.lengthMinMaxInclusive(1,105), "Invalid length of organisationName, should be between 1 and 105 inclusive"),
   checkObjectIfSome(_.individual, IndividualName.validate),
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

object InFlightBankDetails extends RecordUtils[InFlightBankDetails] {
  
  override val gen: Gen[InFlightBankDetails] = for {
    formInformation <- FormInformation.gen
  iban <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,34))
  bic <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,11))
  accountHolderName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,60))
  bankAccountNumber <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{8}$"""))
  sortCode <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{6}$"""))
  buildingSocietyNumber <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,20))
  bankBuildSocietyName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,40))
  } yield InFlightBankDetails(formInformation = formInformation,
  IBAN = iban,
  BIC = bic,
  accountHolderName = accountHolderName,
  bankAccountNumber = bankAccountNumber,
  sortCode = sortCode,
  buildingSocietyNumber = buildingSocietyNumber,
  bankBuildSocietyName = bankBuildSocietyName)
  
  override val validate: Validator[InFlightBankDetails] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.IBAN.lengthMinMaxInclusive(1,34), "Invalid length of IBAN, should be between 1 and 34 inclusive"),
    check(_.BIC.lengthMinMaxInclusive(1,11), "Invalid length of BIC, should be between 1 and 11 inclusive"),
    check(_.accountHolderName.lengthMinMaxInclusive(1,60), "Invalid length of accountHolderName, should be between 1 and 60 inclusive"),
    check(_.bankAccountNumber.matches("""^[0-9]{8}$"""), """Invalid bankAccountNumber, does not matches regex ^[0-9]{8}$"""),
    check(_.sortCode.matches("""^[0-9]{6}$"""), """Invalid sortCode, does not matches regex ^[0-9]{6}$"""),
    check(_.buildingSocietyNumber.lengthMinMaxInclusive(1,20), "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"),
    check(_.bankBuildSocietyName.lengthMinMaxInclusive(1,40), "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive")
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

object InFlightBusinessActivities extends RecordUtils[InFlightBusinessActivities] {
  
  override val gen: Gen[InFlightBusinessActivities] = for {
    formInformation <- FormInformation.gen
  primaryMainCode <- Generator.regex("""^[0-9]{5}$""")
  mainCode2 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  mainCode3 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  mainCode4 <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{5}$"""))
  } yield InFlightBusinessActivities(formInformation = formInformation,
  primaryMainCode = primaryMainCode,
  mainCode2 = mainCode2,
  mainCode3 = mainCode3,
  mainCode4 = mainCode4)
  
  override val validate: Validator[InFlightBusinessActivities] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.primaryMainCode.matches("""^[0-9]{5}$"""), """Invalid primaryMainCode, does not matches regex ^[0-9]{5}$"""),
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

object InFlightCorrespondenceContactDetails extends RecordUtils[InFlightCorrespondenceContactDetails] {
  
  override val gen: Gen[InFlightCorrespondenceContactDetails] = for {
    formInformation <- FormInformation.gen
  address <- Generator.biasedOptionGen(Address.gen)
  contactDetails <- Generator.biasedOptionGen(ContactDetails.gen)
  } yield InFlightCorrespondenceContactDetails(formInformation = formInformation,
  address = address,
  contactDetails = contactDetails)
  
  override val validate: Validator[InFlightCorrespondenceContactDetails] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
   checkObjectIfSome(_.contactDetails, ContactDetails.validate)
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[InFlightCorrespondenceContactDetails] = Json.format[InFlightCorrespondenceContactDetails]
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

object InFlightCustomerDetails extends RecordUtils[InFlightCustomerDetails] {
  
  override val gen: Gen[InFlightCustomerDetails] = for {
    formInformation <- FormInformation.gen
  organisationName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,105))
  individual <- Generator.biasedOptionGen(IndividualName.gen)
  dateOfBirth <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  tradingName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,160))
  mandationStatus <- Gen.oneOf(Seq("1","2","3","4"))
  registrationReason <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012","0013","0014")))
  effectiveRegistrationDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  } yield InFlightCustomerDetails(formInformation = formInformation,
  organisationName = organisationName,
  individual = individual,
  dateOfBirth = dateOfBirth,
  tradingName = tradingName,
  mandationStatus = mandationStatus,
  registrationReason = registrationReason,
  effectiveRegistrationDate = effectiveRegistrationDate)
  
  override val validate: Validator[InFlightCustomerDetails] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.organisationName.lengthMinMaxInclusive(1,105), "Invalid length of organisationName, should be between 1 and 105 inclusive"),
   checkObjectIfSome(_.individual, IndividualName.validate),
    check(_.dateOfBirth.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid dateOfBirth, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.tradingName.lengthMinMaxInclusive(1,160), "Invalid length of tradingName, should be between 1 and 160 inclusive"),
    check(_.mandationStatus.isOneOf(Seq("1","2","3","4")), "Invalid mandationStatus, does not match allowed values"),
    check(_.registrationReason.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012","0013","0014")), "Invalid registrationReason, does not match allowed values"),
    check(_.effectiveRegistrationDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid effectiveRegistrationDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object InFlightDeregistration extends RecordUtils[InFlightDeregistration] {
  
  override val gen: Gen[InFlightDeregistration] = for {
    formInformation <- FormInformation.gen
  deregistrationReason <- Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011"))
  deregDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  deregDateInFuture <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  } yield InFlightDeregistration(formInformation = formInformation,
  deregistrationReason = deregistrationReason,
  deregDate = deregDate,
  deregDateInFuture = deregDateInFuture)
  
  override val validate: Validator[InFlightDeregistration] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.deregistrationReason.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011")), "Invalid deregistrationReason, does not match allowed values"),
    check(_.deregDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid deregDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.deregDateInFuture.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid deregDateInFuture, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object InFlightFlatRateScheme extends RecordUtils[InFlightFlatRateScheme] {
  
  override val gen: Gen[InFlightFlatRateScheme] = for {
    formInformation <- FormInformation.gen
  frscategory <- Generator.biasedOptionGen(Gen.oneOf(Seq("001","002","003","004","005","006","007","008","009","010","011","012","013","014","015","016","017","018","019","020","021","022","023","024","025","026","027","028","029","030","031","032","033","034","035","036","037","038","039","040","041","042","043","044","045","046","047","048","049","050","051","052","053","054")))
  frspercentage <- Generator.biasedOptionGen(Gen.const(1))
  startDate <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  limitedCostTrader <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  } yield InFlightFlatRateScheme(formInformation = formInformation,
  FRSCategory = frscategory,
  FRSPercentage = frspercentage,
  startDate = startDate,
  limitedCostTrader = limitedCostTrader)
  
  override val validate: Validator[InFlightFlatRateScheme] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.FRSCategory.isOneOf(Seq("001","002","003","004","005","006","007","008","009","010","011","012","013","014","015","016","017","018","019","020","021","022","023","024","025","026","027","028","029","030","031","032","033","034","035","036","037","038","039","040","041","042","043","044","045","046","047","048","049","050","051","052","053","054")), "Invalid FRSCategory, does not match allowed values"),
    check(_.startDate.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid startDate, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object InFlightGroupOrPartner extends RecordUtils[InFlightGroupOrPartner] {
  
  override val gen: Gen[InFlightGroupOrPartner] = for {
    formInformation <- FormInformation.gen
  action <- Gen.oneOf(Seq("1","2","3","4"))
  sap_number <- Generator.biasedOptionGen(Generator.regex("""^[0-9]{42}$"""))
  typeOfRelationship <- Generator.biasedOptionGen(Gen.oneOf(Seq("01","02","03","04")))
  makeGrpMember <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  makeControllingBody <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  isControllingBody <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  organisationName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,160))
  tradingName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,160))
  individual <- Generator.biasedOptionGen(IndividualName.gen)
  ppob <- Generator.biasedOptionGen(PPOB.gen)
  } yield InFlightGroupOrPartner(formInformation = formInformation,
  action = action,
  SAP_Number = sap_number,
  typeOfRelationship = typeOfRelationship,
  makeGrpMember = makeGrpMember,
  makeControllingBody = makeControllingBody,
  isControllingBody = isControllingBody,
  organisationName = organisationName,
  tradingName = tradingName,
  individual = individual,
  PPOB = ppob)
  
  override val validate: Validator[InFlightGroupOrPartner] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.action.isOneOf(Seq("1","2","3","4")), "Invalid action, does not match allowed values"),
    check(_.SAP_Number.matches("""^[0-9]{42}$"""), """Invalid SAP_Number, does not matches regex ^[0-9]{42}$"""),
    check(_.typeOfRelationship.isOneOf(Seq("01","02","03","04")), "Invalid typeOfRelationship, does not match allowed values"),
    check(_.organisationName.lengthMinMaxInclusive(1,160), "Invalid length of organisationName, should be between 1 and 160 inclusive"),
    check(_.tradingName.lengthMinMaxInclusive(1,160), "Invalid length of tradingName, should be between 1 and 160 inclusive"),
   checkObjectIfSome(_.individual, IndividualName.validate),
   checkObjectIfSome(_.PPOB, PPOB.validate)
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[InFlightGroupOrPartner] = Json.format[InFlightGroupOrPartner]
}

     

// schema path: #/definitions/inFlightInformationType
case class InFlightInformation(
  changeIndicators: ChangeIndicators,
  inflightChanges: InflightChanges)

object InFlightInformation extends RecordUtils[InFlightInformation] {
  
  override val gen: Gen[InFlightInformation] = for {
    changeIndicators <- ChangeIndicators.gen
  inflightChanges <- InflightChanges.gen
  } yield InFlightInformation(changeIndicators = changeIndicators,
  inflightChanges = inflightChanges)
  
  override val validate: Validator[InFlightInformation] = Validator(
   checkObject(_.changeIndicators, ChangeIndicators.validate),
   checkObject(_.inflightChanges, InflightChanges.validate)
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

object InFlightPPOBDetails extends RecordUtils[InFlightPPOBDetails] {
  
  override val gen: Gen[InFlightPPOBDetails] = for {
    formInformation <- FormInformation.gen
  address <- Generator.biasedOptionGen(Address.gen)
  contactDetails <- Generator.biasedOptionGen(ContactDetails.gen)
  websiteAddress <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,132))
  } yield InFlightPPOBDetails(formInformation = formInformation,
  address = address,
  contactDetails = contactDetails,
  websiteAddress = websiteAddress)
  
  override val validate: Validator[InFlightPPOBDetails] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
   checkObjectIfSome(_.contactDetails, ContactDetails.validate),
    check(_.websiteAddress.lengthMinMaxInclusive(1,132), "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
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

object InFlightReturnPeriod extends RecordUtils[InFlightReturnPeriod] {
  
  override val gen: Gen[InFlightReturnPeriod] = for {
    formInformation <- FormInformation.gen
  changeReturnPeriod <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  nonStdTaxPeriodsRequested <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  ceaseNonStdTaxPeriods <- Generator.biasedOptionGen(Generator.biasedBooleanGen)
  stdReturnPeriod <- Generator.biasedOptionGen(Gen.oneOf(Seq("MA","MB","MC","MM")))
  nonStdTaxPeriods <- Generator.biasedOptionGen(NonStdTaxPeriods.gen)
  } yield InFlightReturnPeriod(formInformation = formInformation,
  changeReturnPeriod = changeReturnPeriod,
  nonStdTaxPeriodsRequested = nonStdTaxPeriodsRequested,
  ceaseNonStdTaxPeriods = ceaseNonStdTaxPeriods,
  stdReturnPeriod = stdReturnPeriod,
  nonStdTaxPeriods = nonStdTaxPeriods)
  
  override val validate: Validator[InFlightReturnPeriod] = Validator(
   checkObject(_.formInformation, FormInformation.validate),
    check(_.stdReturnPeriod.isOneOf(Seq("MA","MB","MC","MM")), "Invalid stdReturnPeriod, does not match allowed values"),
   checkObjectIfSome(_.nonStdTaxPeriods, NonStdTaxPeriods.validate)
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

object IndividualName extends RecordUtils[IndividualName] {
  
  override val gen: Gen[IndividualName] = for {
    title <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012")))
  firstName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,35))
  middleName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,35))
  lastName <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,35))
  } yield IndividualName(title = title,
  firstName = firstName,
  middleName = middleName,
  lastName = lastName)
  
  override val validate: Validator[IndividualName] = Validator(
    check(_.title.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009","0010","0011","0012")), "Invalid title, does not match allowed values"),
    check(_.firstName.lengthMinMaxInclusive(1,35), "Invalid length of firstName, should be between 1 and 35 inclusive"),
    check(_.middleName.lengthMinMaxInclusive(1,35), "Invalid length of middleName, should be between 1 and 35 inclusive"),
    check(_.lastName.lengthMinMaxInclusive(1,35), "Invalid length of lastName, should be between 1 and 35 inclusive")
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

object InflightChanges extends RecordUtils[InflightChanges] {
  
  override val gen: Gen[InflightChanges] = for {
    customerDetails <- Generator.biasedOptionGen(InFlightCustomerDetails.gen)
  ppobdetails <- Generator.biasedOptionGen(InFlightPPOBDetails.gen)
  correspondenceContactDetails <- Generator.biasedOptionGen(InFlightCorrespondenceContactDetails.gen)
  bankDetails <- Generator.biasedOptionGen(InFlightBankDetails.gen)
  businessActivities <- Generator.biasedOptionGen(InFlightBusinessActivities.gen)
  flatRateScheme <- Generator.biasedOptionGen(InFlightFlatRateScheme.gen)
  deregister <- Generator.biasedOptionGen(InFlightDeregistration.gen)
  returnPeriod <- Generator.biasedOptionGen(InFlightReturnPeriod.gen)
  groupOrPartner <- Generator.biasedOptionGen(Generator.nonEmptyListOfMaxN(3,InFlightGroupOrPartner.gen))
  } yield InflightChanges(customerDetails = customerDetails,
  PPOBDetails = ppobdetails,
  correspondenceContactDetails = correspondenceContactDetails,
  bankDetails = bankDetails,
  businessActivities = businessActivities,
  flatRateScheme = flatRateScheme,
  deregister = deregister,
  returnPeriod = returnPeriod,
  groupOrPartner = groupOrPartner)
  
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

object NonStdTaxPeriods extends RecordUtils[NonStdTaxPeriods] {
  
  override val gen: Gen[NonStdTaxPeriods] = for {
    period01 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period02 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period03 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period04 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period05 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period06 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period07 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period08 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period09 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period10 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period11 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period12 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period13 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period14 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period15 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period16 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period17 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period18 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period19 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period20 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period21 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  period22 <- Generator.biasedOptionGen(Generator.regex("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""))
  } yield NonStdTaxPeriods(period01 = period01,
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
  period22 = period22)
  
  override val validate: Validator[NonStdTaxPeriods] = Validator(
    check(_.period01.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period01, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period02.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period02, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period03.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period03, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period04.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period04, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period05.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period05, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period06.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period06, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period07.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period07, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period08.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period08, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period09.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period09, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period10.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period10, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period11.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period11, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period12.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period12, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period13.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period13, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period14.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period14, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period15.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period15, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period16.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period16, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period17.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period17, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period18.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period18, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period19.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period19, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period20.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period20, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period21.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period21, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""),
    check(_.period22.matches("""^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""), """Invalid period22, does not matches regex ^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$""")
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

object PPOB extends RecordUtils[PPOB] {
  
  override val gen: Gen[PPOB] = for {
    address <- Address.gen
  rls <- Generator.biasedOptionGen(Gen.oneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009")))
  contactDetails <- Generator.biasedOptionGen(ContactDetails.gen)
  websiteAddress <- Generator.biasedOptionGen(Generator.stringMinMaxN(1,132))
  } yield PPOB(address = address,
  RLS = rls,
  contactDetails = contactDetails,
  websiteAddress = websiteAddress)
  
  override val validate: Validator[PPOB] = Validator(
    check(_.RLS.isOneOf(Seq("0001","0002","0003","0004","0005","0006","0007","0008","0009")), "Invalid RLS, does not match allowed values"),
   checkObjectIfSome(_.contactDetails, ContactDetails.validate),
    check(_.websiteAddress.lengthMinMaxInclusive(1,132), "Invalid length of websiteAddress, should be between 1 and 132 inclusive")
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[PPOB] = Json.format[PPOB]
}

     

// schema path: #/definitions/periodType
case class Period(
  stdReturnPeriod: Option[String] = None,
  nonStdTaxPeriods: Option[NonStdTaxPeriods] = None)

object Period extends RecordUtils[Period] {
  
  override val gen: Gen[Period] = for {
    stdReturnPeriod <- Generator.biasedOptionGen(Gen.oneOf(Seq("MA","MB","MC","MM")))
  nonStdTaxPeriods <- Generator.biasedOptionGen(NonStdTaxPeriods.gen)
  } yield Period(stdReturnPeriod = stdReturnPeriod,
  nonStdTaxPeriods = nonStdTaxPeriods)
  
  override val validate: Validator[Period] = Validator(
    check(_.stdReturnPeriod.isOneOf(Seq("MA","MB","MC","MM")), "Invalid stdReturnPeriod, does not match allowed values"),
   checkObjectIfSome(_.nonStdTaxPeriods, NonStdTaxPeriods.validate)
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

object Address extends RecordUtils[Address] {
  
  override val gen: Gen[Address] = for {
    line1 <- Generator.regex("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$""")
  line2 <- Generator.regex("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$""")
  line3 <- Generator.biasedOptionGen(Generator.regex("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""))
  line4 <- Generator.biasedOptionGen(Generator.regex("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""))
  postCode <- Generator.regex("""^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$""")
  countryCode <- Gen.const("GB")
  } yield Address(line1 = line1,
  line2 = line2,
  line3 = line3,
  line4 = line4,
  postCode = postCode,
  countryCode = countryCode)
  
  override val validate: Validator[Address] = Validator(
    check(_.line1.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""), """Invalid line1, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
    check(_.line2.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""), """Invalid line2, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
    check(_.line3.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""), """Invalid line3, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
    check(_.line4.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""), """Invalid line4, does not matches regex ^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""),
    check(_.postCode.matches("""^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""), """Invalid postCode, does not matches regex ^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""),
    check(_.countryCode.isOneOf(Seq("GB")), "Invalid countryCode, does not match allowed values")
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[Address] = Json.format[Address]
}

     
  override val validate: Validator[VatCustomerInformationRecord] = Validator(
    check(_.vrn.matches("""^[0-9A-Za-z]{1,6}$"""), """Invalid vrn, does not matches regex ^[0-9A-Za-z]{1,6}$"""),
   checkObjectIfSome(_.approvedInformation, ApprovedInformation.validate),
   checkObjectIfSome(_.inFlightInformation, InFlightInformation.validate)
  )

override val sanitizers: Seq[Update] = Seq()

implicit val formats: Format[VatCustomerInformationRecord] = Json.format[VatCustomerInformationRecord]
}

     

     