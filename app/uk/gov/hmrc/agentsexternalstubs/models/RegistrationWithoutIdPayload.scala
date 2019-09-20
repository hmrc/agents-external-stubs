package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.RegistrationWithoutIdPayload._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES2.json app/uk/gov/hmrc/agentsexternalstubs/models/RegistrationWithoutIdPayload.scala RegistrationWithoutIdPayload output:payload"
  * ----------------------------------------------------------------------------
  *
  *  RegistrationWithoutIdPayload
  *  -  Address
  *  -  ContactDetails
  *  -  ForeignAddress
  *  -  Identification
  *  -  Individual
  *  -  Organisation
  *  -  UkAddress
  */
case class RegistrationWithoutIdPayload(
  regime: String,
  acknowledgementReference: String,
  isAnAgent: Boolean = false,
  isAGroup: Boolean = false,
  identification: Option[Identification] = None,
  individual: Option[Individual] = None,
  organisation: Option[Organisation] = None,
  address: Address,
  contactDetails: ContactDetails) {

  def withRegime(regime: String): RegistrationWithoutIdPayload = copy(regime = regime)
  def modifyRegime(pf: PartialFunction[String, String]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(regime)) copy(regime = pf(regime)) else this
  def withAcknowledgementReference(acknowledgementReference: String): RegistrationWithoutIdPayload =
    copy(acknowledgementReference = acknowledgementReference)
  def modifyAcknowledgementReference(pf: PartialFunction[String, String]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(acknowledgementReference)) copy(acknowledgementReference = pf(acknowledgementReference))
    else this
  def withIsAnAgent(isAnAgent: Boolean): RegistrationWithoutIdPayload = copy(isAnAgent = isAnAgent)
  def modifyIsAnAgent(pf: PartialFunction[Boolean, Boolean]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(isAnAgent)) copy(isAnAgent = pf(isAnAgent)) else this
  def withIsAGroup(isAGroup: Boolean): RegistrationWithoutIdPayload = copy(isAGroup = isAGroup)
  def modifyIsAGroup(pf: PartialFunction[Boolean, Boolean]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(isAGroup)) copy(isAGroup = pf(isAGroup)) else this
  def withIdentification(identification: Option[Identification]): RegistrationWithoutIdPayload =
    copy(identification = identification)
  def modifyIdentification(
    pf: PartialFunction[Option[Identification], Option[Identification]]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(identification)) copy(identification = pf(identification)) else this
  def withIndividual(individual: Option[Individual]): RegistrationWithoutIdPayload = copy(individual = individual)
  def modifyIndividual(pf: PartialFunction[Option[Individual], Option[Individual]]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
  def withOrganisation(organisation: Option[Organisation]): RegistrationWithoutIdPayload =
    copy(organisation = organisation)
  def modifyOrganisation(
    pf: PartialFunction[Option[Organisation], Option[Organisation]]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(organisation)) copy(organisation = pf(organisation)) else this
  def withAddress(address: Address): RegistrationWithoutIdPayload = copy(address = address)
  def modifyAddress(pf: PartialFunction[Address, Address]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
  def withContactDetails(contactDetails: ContactDetails): RegistrationWithoutIdPayload =
    copy(contactDetails = contactDetails)
  def modifyContactDetails(pf: PartialFunction[ContactDetails, ContactDetails]): RegistrationWithoutIdPayload =
    if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
}

object RegistrationWithoutIdPayload {

  import Validator._

  val regimeValidator: Validator[String] =
    check(_.matches(Common.regimePattern), s"""Invalid regime, does not matches regex ${Common.regimePattern}""")
  val acknowledgementReferenceValidator: Validator[String] = check(
    _.matches(Common.acknowledgementReferencePattern),
    s"""Invalid acknowledgementReference, does not matches regex ${Common.acknowledgementReferencePattern}"""
  )
  val identificationValidator: Validator[Option[Identification]] = checkIfSome(identity, Identification.validate)
  val individualValidator: Validator[Option[Individual]] = checkIfSome(identity, Individual.validate)
  val organisationValidator: Validator[Option[Organisation]] = checkIfSome(identity, Organisation.validate)
  val addressValidator: Validator[Address] = checkProperty(identity, Address.validate)
  val contactDetailsValidator: Validator[ContactDetails] = checkProperty(identity, ContactDetails.validate)

  val validate: Validator[RegistrationWithoutIdPayload] = Validator(
    checkProperty(_.regime, regimeValidator),
    checkProperty(_.acknowledgementReference, acknowledgementReferenceValidator),
    checkProperty(_.identification, identificationValidator),
    checkProperty(_.individual, individualValidator),
    checkProperty(_.organisation, organisationValidator),
    checkProperty(_.address, addressValidator),
    checkProperty(_.contactDetails, contactDetailsValidator),
    checkIfOnlyOneSetIsDefined(Seq(Set(_.organisation), Set(_.individual)), "[{organisation},{individual}]")
  )

  implicit val formats: Format[RegistrationWithoutIdPayload] = Json.format[RegistrationWithoutIdPayload]

  sealed trait Address {
    def addressLine2: String
    def addressLine3: Option[String] = None
    def addressLine1: String
    def countryCode: String
    def addressLine4: Option[String] = None
  }

  object Address {

    val validate: Validator[Address] = {
      case x: ForeignAddress => ForeignAddress.validate(x)
      case x: UkAddress      => UkAddress.validate(x)
    }

    implicit val reads: Reads[Address] = new Reads[Address] {
      override def reads(json: JsValue): JsResult[Address] = {
        val r0 = ForeignAddress.formats
          .reads(json)
          .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of Address, i.e. ForeignAddress, UkAddress"),
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
        case x: ForeignAddress => ForeignAddress.formats.writes(x)
        case x: UkAddress      => UkAddress.formats.writes(x)
      }
    }

  }

  case class ContactDetails(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None) {

    def withPhoneNumber(phoneNumber: Option[String]): ContactDetails = copy(phoneNumber = phoneNumber)
    def modifyPhoneNumber(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(phoneNumber)) copy(phoneNumber = pf(phoneNumber)) else this
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

  object ContactDetails {

    val phoneNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.phoneNumberPattern),
      s"""Invalid phoneNumber, does not matches regex ${Common.phoneNumberPattern}""")
    val mobileNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.phoneNumberPattern),
      s"""Invalid mobileNumber, does not matches regex ${Common.phoneNumberPattern}""")
    val faxNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.phoneNumberPattern),
      s"""Invalid faxNumber, does not matches regex ${Common.phoneNumberPattern}""")
    val emailAddressValidator: Validator[Option[String]] =
      check(_.lengthMax(132), "Invalid length of emailAddress, maximum length should be 132")

    val validate: Validator[ContactDetails] = Validator(
      checkProperty(_.phoneNumber, phoneNumberValidator),
      checkProperty(_.mobileNumber, mobileNumberValidator),
      checkProperty(_.faxNumber, faxNumberValidator),
      checkProperty(_.emailAddress, emailAddressValidator)
    )

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]

  }

  case class ForeignAddress(
    override val addressLine1: String,
    override val addressLine2: String,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    override val countryCode: String)
      extends Address {

    def withAddressLine1(addressLine1: String): ForeignAddress = copy(addressLine1 = addressLine1)
    def modifyAddressLine1(pf: PartialFunction[String, String]): ForeignAddress =
      if (pf.isDefinedAt(addressLine1)) copy(addressLine1 = pf(addressLine1)) else this
    def withAddressLine2(addressLine2: String): ForeignAddress = copy(addressLine2 = addressLine2)
    def modifyAddressLine2(pf: PartialFunction[String, String]): ForeignAddress =
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

  object ForeignAddress {

    val addressLine1Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}""")
    val addressLine2Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}""")
    val addressLine3Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}""")
    val addressLine4Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}""")
    val postalCodeValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 10), "Invalid length of postalCode, should be between 1 and 10 inclusive")
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")

    val validate: Validator[ForeignAddress] = Validator(
      checkProperty(_.addressLine1, addressLine1Validator),
      checkProperty(_.addressLine2, addressLine2Validator),
      checkProperty(_.addressLine3, addressLine3Validator),
      checkProperty(_.addressLine4, addressLine4Validator),
      checkProperty(_.postalCode, postalCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
    )

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String) {

    def withIdNumber(idNumber: String): Identification = copy(idNumber = idNumber)
    def modifyIdNumber(pf: PartialFunction[String, String]): Identification =
      if (pf.isDefinedAt(idNumber)) copy(idNumber = pf(idNumber)) else this
    def withIssuingInstitution(issuingInstitution: String): Identification =
      copy(issuingInstitution = issuingInstitution)
    def modifyIssuingInstitution(pf: PartialFunction[String, String]): Identification =
      if (pf.isDefinedAt(issuingInstitution)) copy(issuingInstitution = pf(issuingInstitution)) else this
    def withIssuingCountryCode(issuingCountryCode: String): Identification =
      copy(issuingCountryCode = issuingCountryCode)
    def modifyIssuingCountryCode(pf: PartialFunction[String, String]): Identification =
      if (pf.isDefinedAt(issuingCountryCode)) copy(issuingCountryCode = pf(issuingCountryCode)) else this
  }

  object Identification {

    val idNumberValidator: Validator[String] = check(
      _.matches(Common.idNumberPattern),
      s"""Invalid idNumber, does not matches regex ${Common.idNumberPattern}""")
    val issuingInstitutionValidator: Validator[String] = check(
      _.matches(Common.issuingInstitutionPattern),
      s"""Invalid issuingInstitution, does not matches regex ${Common.issuingInstitutionPattern}""")
    val issuingCountryCodeValidator: Validator[String] = check(
      _.matches(Common.issuingCountryCodePattern),
      s"""Invalid issuingCountryCode, does not matches regex ${Common.issuingCountryCodePattern}""")

    val validate: Validator[Identification] = Validator(
      checkProperty(_.idNumber, idNumberValidator),
      checkProperty(_.issuingInstitution, issuingInstitutionValidator),
      checkProperty(_.issuingCountryCode, issuingCountryCodeValidator)
    )

    implicit val formats: Format[Identification] = Json.format[Identification]

  }

  case class Individual(
    firstName: String,
    middleName: Option[String] = None,
    lastName: String,
    dateOfBirth: String,
    itmpDateOfBirth: String) {

    def withFirstName(firstName: String): Individual = copy(firstName = firstName)
    def modifyFirstName(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(firstName)) copy(firstName = pf(firstName)) else this
    def withMiddleName(middleName: Option[String]): Individual = copy(middleName = middleName)
    def modifyMiddleName(pf: PartialFunction[Option[String], Option[String]]): Individual =
      if (pf.isDefinedAt(middleName)) copy(middleName = pf(middleName)) else this
    def withLastName(lastName: String): Individual = copy(lastName = lastName)
    def modifyLastName(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(lastName)) copy(lastName = pf(lastName)) else this
    def withDateOfBirth(dateOfBirth: String): Individual = copy(dateOfBirth = dateOfBirth)
    def withItmpDateOfBirth(itmpDateOfBirth: String): Individual = copy(itmpDateOfBirth = itmpDateOfBirth)
    def modifyDateOfBirth(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(dateOfBirth)) copy(dateOfBirth = pf(dateOfBirth)) else this
  }

  object Individual {

    val firstNameValidator: Validator[String] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid firstName, does not matches regex ${Common.firstNamePattern}""")
    val middleNameValidator: Validator[Option[String]] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid middleName, does not matches regex ${Common.firstNamePattern}""")
    val lastNameValidator: Validator[String] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid lastName, does not matches regex ${Common.firstNamePattern}""")
    val dateOfBirthValidator: Validator[String] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}""")

    val validate: Validator[Individual] = Validator(
      checkProperty(_.firstName, firstNameValidator),
      checkProperty(_.middleName, middleNameValidator),
      checkProperty(_.lastName, lastNameValidator),
      checkProperty(_.dateOfBirth, dateOfBirthValidator),
      checkProperty(_.itmpDateOfBirth, dateOfBirthValidator),
    )

    implicit val formats: Format[Individual] = Json.format[Individual]

  }

  case class Organisation(organisationName: String) {

    def withOrganisationName(organisationName: String): Organisation = copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[String, String]): Organisation =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
  }

  object Organisation {

    val organisationNameValidator: Validator[String] = check(
      _.matches(Common.organisationNamePattern),
      s"""Invalid organisationName, does not matches regex ${Common.organisationNamePattern}""")

    val validate: Validator[Organisation] = Validator(checkProperty(_.organisationName, organisationNameValidator))

    implicit val formats: Format[Organisation] = Json.format[Organisation]

  }

  case class UkAddress(
    override val addressLine1: String,
    override val addressLine2: String,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: String,
    override val countryCode: String)
      extends Address {

    def withAddressLine1(addressLine1: String): UkAddress = copy(addressLine1 = addressLine1)
    def modifyAddressLine1(pf: PartialFunction[String, String]): UkAddress =
      if (pf.isDefinedAt(addressLine1)) copy(addressLine1 = pf(addressLine1)) else this
    def withAddressLine2(addressLine2: String): UkAddress = copy(addressLine2 = addressLine2)
    def modifyAddressLine2(pf: PartialFunction[String, String]): UkAddress =
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

  object UkAddress {

    val addressLine1Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}""")
    val addressLine2Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}""")
    val addressLine3Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}""")
    val addressLine4Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}""")
    val postalCodeValidator: Validator[String] = check(
      _.matches(Common.postalCodePattern),
      s"""Invalid postalCode, does not matches regex ${Common.postalCodePattern}""")
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")

    val validate: Validator[UkAddress] = Validator(
      checkProperty(_.addressLine1, addressLine1Validator),
      checkProperty(_.addressLine2, addressLine2Validator),
      checkProperty(_.addressLine3, addressLine3Validator),
      checkProperty(_.addressLine4, addressLine4Validator),
      checkProperty(_.postalCode, postalCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
    )

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  object Common {
    val firstNamePattern = """^[a-zA-Z &`\-\'^]{1,35}$"""
    val postalCodePattern = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$"""
    val addressLinePattern = """^[A-Za-z0-9 \-,.&']{1,35}$"""
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
      "OR",
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
    val idNumberPattern = """^[a-zA-Z0-9 '&\-]{1,60}$"""
    val phoneNumberPattern = """^[A-Z0-9 )/(\-*#]+$"""
    val acknowledgementReferencePattern = """^[A-Za-z0-9 -]{1,32}$"""
    val dateOfBirthPattern =
      """^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$"""
    val organisationNamePattern = """^[a-zA-Z0-9- '&\/]{1,105}$"""
    val issuingInstitutionPattern = """^[a-zA-Z0-9 '&\-\/]{1,40}$"""
    val countryCodeEnum1 = Seq("GB")
    val issuingCountryCodePattern = """(?!^GB$)^[A-Z]{2}$"""
    val regimePattern = """^[A-Z]{3,10}$"""
  }
}
