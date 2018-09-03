package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.AgentRecord._

/**
  * ----------------------------------------------------------------------------
  * This AgentRecord code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
case class AgentRecord(
  businessPartnerExists: Boolean,
  safeId: String,
  agentReferenceNumber: Option[String] = None,
  isAnAgent: Boolean,
  isAnASAgent: Boolean,
  isAnIndividual: Boolean,
  individual: Option[Individual] = None,
  isAnOrganisation: Boolean,
  organisation: Option[Organisation] = None,
  addressDetails: AddressDetails,
  contactDetails: Option[ContactDetails] = None,
  agencyDetails: Option[AgencyDetails] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = agentReferenceNumber.map(AgentRecord.uniqueKey)
  override def lookupKeys: Seq[String] = Seq()
  override def withId(id: Option[String]): Record = copy(id = id)
}

object AgentRecord extends RecordUtils[AgentRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[AgentRecord] = RecordMetaData[AgentRecord](this)

  def uniqueKey(key: String): String = s"""agentReferenceNumber:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  override val gen: Gen[AgentRecord] = for {
    businessPartnerExists <- Generator.biasedBooleanGen
    safeId                <- Generator.safeIdGen
    isAnAgent             <- Generator.biasedBooleanGen
    isAnASAgent           <- Generator.biasedBooleanGen
    isAnIndividual        <- Generator.biasedBooleanGen
    isAnOrganisation      <- Generator.biasedBooleanGen
    addressDetails        <- UkAddress.gen
  } yield
    AgentRecord(
      businessPartnerExists = businessPartnerExists,
      safeId = safeId,
      isAnAgent = isAnAgent,
      isAnASAgent = isAnASAgent,
      isAnIndividual = isAnIndividual,
      isAnOrganisation = isAnOrganisation,
      addressDetails = addressDetails
    )

  sealed trait AddressDetails {
    def addressLine2: Option[String] = None
    def addressLine3: Option[String] = None
    def addressLine1: String
    def countryCode: String
    def addressLine4: Option[String] = None
  }

  object AddressDetails extends RecordUtils[AddressDetails] {

    override val gen: Gen[AddressDetails] = Gen.oneOf[AddressDetails](
      UkAddress.gen.map(_.asInstanceOf[AddressDetails]),
      ForeignAddress.gen.map(_.asInstanceOf[AddressDetails]))

    override val validate: Validator[AddressDetails] = {
      case x: UkAddress      => UkAddress.validate(x)
      case x: ForeignAddress => ForeignAddress.validate(x)
    }

    override val sanitizers: Seq[Update] = Seq()

    implicit val reads: Reads[AddressDetails] = new Reads[AddressDetails] {
      override def reads(json: JsValue): JsResult[AddressDetails] = {
        val r0 =
          UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          ForeignAddress.formats
            .reads(json)
            .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of AddressDetails, i.e. UkAddress, ForeignAddress"),
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

    implicit val writes: Writes[AddressDetails] = new Writes[AddressDetails] {
      override def writes(o: AddressDetails): JsValue = o match {
        case x: UkAddress      => UkAddress.formats.writes(x)
        case x: ForeignAddress => ForeignAddress.formats.writes(x)
      }
    }

  }

  case class AgencyDetails(
    agencyName: Option[String] = None,
    agencyAddress: Option[AgencyDetails.AgencyAddress] = None,
    agencyEmail: Option[String] = None)

  object AgencyDetails extends RecordUtils[AgencyDetails] {

    override val gen: Gen[AgencyDetails] = Gen const AgencyDetails(
      )

    sealed trait AgencyAddress {
      def addressLine2: Option[String] = None
      def addressLine3: Option[String] = None
      def addressLine1: String
      def countryCode: String
      def addressLine4: Option[String] = None
    }

    object AgencyAddress extends RecordUtils[AgencyAddress] {

      override val gen: Gen[AgencyAddress] = Gen.oneOf[AgencyAddress](
        ForeignAddress.gen.map(_.asInstanceOf[AgencyAddress]),
        UkAddress.gen.map(_.asInstanceOf[AgencyAddress]))

      override val validate: Validator[AgencyAddress] = {
        case x: ForeignAddress => ForeignAddress.validate(x)
        case x: UkAddress      => UkAddress.validate(x)
      }

      override val sanitizers: Seq[Update] = Seq()

      implicit val reads: Reads[AgencyAddress] = new Reads[AgencyAddress] {
        override def reads(json: JsValue): JsResult[AgencyAddress] = {
          val r0 = ForeignAddress.formats
            .reads(json)
            .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
          val r1 = r0.orElse(
            UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
          r1.orElse(
            aggregateErrors(
              JsError("Could not match json object to any variant of AgencyAddress, i.e. ForeignAddress, UkAddress"),
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

      implicit val writes: Writes[AgencyAddress] = new Writes[AgencyAddress] {
        override def writes(o: AgencyAddress): JsValue = o match {
          case x: ForeignAddress => ForeignAddress.formats.writes(x)
          case x: UkAddress      => UkAddress.formats.writes(x)
        }
      }

    }

    override val validate: Validator[AgencyDetails] = Validator(
      check(
        _.agencyName.lengthMinMaxInclusive(1, 40),
        "Invalid length of agencyName, should be between 1 and 40 inclusive"),
      checkObjectIfSome(_.agencyAddress, AgencyAddress.validate),
      check(
        _.agencyEmail.lengthMinMaxInclusive(1, 132),
        "Invalid length of agencyEmail, should be between 1 and 132 inclusive")
    )

    val agencyNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          agencyName = entity.agencyName.orElse(Generator.get(UserGenerator.agencyNameGen.map(_.take(40)))(seed)))

    val agencyAddressSanitizer: Update = seed =>
      entity => entity.copy(agencyAddress = entity.agencyAddress.orElse(Generator.get(ForeignAddress.gen)(seed)))

    val agencyEmailSanitizer: Update = seed =>
      entity =>
        entity.copy(agencyEmail = entity.agencyEmail.orElse(Generator.get(Generator.emailGen.variant("agency"))(seed)))

    override val sanitizers: Seq[Update] = Seq(agencyNameSanitizer, agencyAddressSanitizer, agencyEmailSanitizer)

    implicit val formats: Format[AgencyDetails] = Json.format[AgencyDetails]

  }

  case class ContactDetails(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None)

  object ContactDetails extends RecordUtils[ContactDetails] {

    override val gen: Gen[ContactDetails] = Gen const ContactDetails(
      )

    override val validate: Validator[ContactDetails] = Validator(
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
        _.emailAddress.lengthMinMaxInclusive(1, 132),
        "Invalid length of emailAddress, should be between 1 and 132 inclusive")
    )

    val phoneNumberSanitizer: Update = seed =>
      entity => entity.copy(phoneNumber = entity.phoneNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val mobileNumberSanitizer: Update = seed =>
      entity => entity.copy(mobileNumber = entity.mobileNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val faxNumberSanitizer: Update = seed =>
      entity => entity.copy(faxNumber = entity.faxNumber.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity => entity.copy(emailAddress = entity.emailAddress.orElse(Generator.get(Generator.emailGen)(seed)))

    override val sanitizers: Seq[Update] =
      Seq(phoneNumberSanitizer, mobileNumberSanitizer, faxNumberSanitizer, emailAddressSanitizer)

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]

  }

  case class ForeignAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    override val countryCode: String)
      extends AddressDetails with AgencyDetails.AgencyAddress

  object ForeignAddress extends RecordUtils[ForeignAddress] {

    override val gen: Gen[ForeignAddress] = for {
      addressLine1 <- Generator.address4Lines35Gen.map(_.line1)
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
        entity.copy(
          addressLine2 = entity.addressLine2.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line2))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = entity.addressLine3.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine4 = entity.addressLine4.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed)))

    val postalCodeSanitizer: Update = seed =>
      entity => entity.copy(postalCode = entity.postalCode.orElse(Generator.get(Generator.postcode)(seed)))

    override val sanitizers: Seq[Update] =
      Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer, postalCodeSanitizer)

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class Individual(firstName: String, middleName: Option[String] = None, lastName: String, dateOfBirth: String)

  object Individual extends RecordUtils[Individual] {

    override val gen: Gen[Individual] = for {
      firstName   <- Generator.forename()
      lastName    <- Generator.surname
      dateOfBirth <- Generator.dateYYYYMMDDGen.variant("ofbirth")
    } yield
      Individual(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth
      )

    override val validate: Validator[Individual] = Validator(
      check(
        _.firstName.lengthMinMaxInclusive(1, 35),
        "Invalid length of firstName, should be between 1 and 35 inclusive"),
      check(
        _.middleName.lengthMinMaxInclusive(1, 35),
        "Invalid length of middleName, should be between 1 and 35 inclusive"),
      check(
        _.lastName.lengthMinMaxInclusive(1, 35),
        "Invalid length of lastName, should be between 1 and 35 inclusive"),
      check(
        _.dateOfBirth.matches(Common.dateOfBirthPattern),
        s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    val middleNameSanitizer: Update = seed =>
      entity =>
        entity.copy(middleName = entity.middleName.orElse(Generator.get(Generator.forename().variant("middle"))(seed)))

    override val sanitizers: Seq[Update] = Seq(middleNameSanitizer)

    implicit val formats: Format[Individual] = Json.format[Individual]

  }

  case class Organisation(organisationName: String, isAGroup: Boolean, organisationType: String)

  object Organisation extends RecordUtils[Organisation] {

    override val gen: Gen[Organisation] = for {
      organisationName <- Generator.company
      isAGroup         <- Generator.biasedBooleanGen
      organisationType <- Generator.regex(Common.organisationTypePattern)
    } yield
      Organisation(
        organisationName = organisationName,
        isAGroup = isAGroup,
        organisationType = organisationType
      )

    override val validate: Validator[Organisation] = Validator(
      check(
        _.organisationName.lengthMinMaxInclusive(1, 105),
        "Invalid length of organisationName, should be between 1 and 105 inclusive"),
      check(
        _.organisationType.matches(Common.organisationTypePattern),
        s"""Invalid organisationType, does not matches regex ${Common.organisationTypePattern}""")
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[Organisation] = Json.format[Organisation]

  }

  case class UkAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: String,
    override val countryCode: String)
      extends AddressDetails with AgencyDetails.AgencyAddress

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
        entity.copy(
          addressLine4 = entity.addressLine4.orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed)))

    override val sanitizers: Seq[Update] = Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer)

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  override val validate: Validator[AgentRecord] = Validator(
    check(
      _.safeId.matches(Common.safeIdPattern),
      s"""Invalid safeId, does not matches regex ${Common.safeIdPattern}"""),
    check(
      _.agentReferenceNumber.matches(Common.agentReferenceNumberPattern),
      s"""Invalid agentReferenceNumber, does not matches regex ${Common.agentReferenceNumberPattern}"""
    ),
    checkObjectIfSome(_.individual, Individual.validate),
    checkObjectIfSome(_.organisation, Organisation.validate),
    checkObject(_.addressDetails, AddressDetails.validate),
    checkObjectIfSome(_.contactDetails, ContactDetails.validate),
    checkObjectIfSome(_.agencyDetails, AgencyDetails.validate),
    checkIfAtLeastOneIsDefined(Seq(_.organisation, _.individual))
  )

  val agentReferenceNumberSanitizer: Update = seed =>
    entity =>
      entity.copy(agentReferenceNumber = entity.agentReferenceNumber.orElse(Generator.get(Generator.arnGen)(seed)))

  val individualSanitizer: Update = seed =>
    entity => entity.copy(individual = entity.individual.orElse(Generator.get(Individual.gen)(seed)))

  val organisationSanitizer: Update = seed =>
    entity => entity.copy(organisation = entity.organisation.orElse(Generator.get(Organisation.gen)(seed)))

  val contactDetailsSanitizer: Update = seed =>
    entity => entity.copy(contactDetails = entity.contactDetails.orElse(Generator.get(ContactDetails.gen)(seed)))

  val agencyDetailsSanitizer: Update = seed =>
    entity => entity.copy(agencyDetails = entity.agencyDetails.orElse(Generator.get(AgencyDetails.gen)(seed)))

  val organisationOrIndividualSanitizer: Update = seed =>
    entity => {
      Generator.get(Gen.chooseNum(0, 1))(seed) match {
        case Some(0) => organisationSanitizer(seed)(entity)
        case _       => individualSanitizer(seed)(entity)
      }
  }

  override val sanitizers: Seq[Update] = Seq(
    agentReferenceNumberSanitizer,
    contactDetailsSanitizer,
    agencyDetailsSanitizer,
    organisationOrIndividualSanitizer)

  implicit val formats: Format[AgentRecord] = Json.format[AgentRecord]
  object Common {
    val phoneNumberPattern = """^[A-Z0-9 )/(*#-]+$"""
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
    val dateOfBirthPattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val organisationTypePattern = """^[A-Z a-z 0-9]{1,4}$"""
    val countryCodeEnum1 = Seq("GB")
    val safeIdPattern = """^X[A-Z]000[0-9]{10}$"""
    val agentReferenceNumberPattern = """^[A-Z](ARN)[0-9]{7}$"""
  }
}
