package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1170.json app/uk/gov/hmrc/agentsexternalstubs/models/BusinessPartnerRecord.scala BusinessPartnerRecord "
  * ----------------------------------------------------------------------------
  *
  *  BusinessPartnerRecord
  *  -  AddressDetails
  *  -  AgencyDetails
  *  -  -  AgencyAddress
  *  -  ContactDetails
  *  -  ForeignAddress
  *  -  Individual
  *  -  Organisation
  *  -  UkAddress
  */
case class BusinessPartnerRecord(
  businessPartnerExists: Boolean = false,
  safeId: String,
  agentReferenceNumber: Option[String] = None,
  utr: Option[String] = None,
  nino: Option[String] = None,
  eori: Option[String] = None,
  isAnAgent: Boolean = false,
  isAnASAgent: Boolean = false,
  isAnIndividual: Boolean = false,
  individual: Option[Individual] = None,
  isAnOrganisation: Boolean = false,
  organisation: Option[Organisation] = None,
  addressDetails: AddressDetails,
  contactDetails: Option[ContactDetails] = None,
  agencyDetails: Option[AgencyDetails] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(safeId).map(BusinessPartnerRecord.uniqueKey)
  override def lookupKeys: Seq[String] =
    Seq(
      agentReferenceNumber.map(BusinessPartnerRecord.agentReferenceNumberKey),
      utr.map(BusinessPartnerRecord.utrKey),
      nino.map(BusinessPartnerRecord.ninoKey),
      eori.map(BusinessPartnerRecord.eoriKey)
    ).collect { case Some(x) => x }
  override def withId(id: Option[String]): BusinessPartnerRecord = copy(id = id)

  def withBusinessPartnerExists(businessPartnerExists: Boolean): BusinessPartnerRecord =
    copy(businessPartnerExists = businessPartnerExists)
  def modifyBusinessPartnerExists(pf: PartialFunction[Boolean, Boolean]): BusinessPartnerRecord =
    if (pf.isDefinedAt(businessPartnerExists)) copy(businessPartnerExists = pf(businessPartnerExists)) else this
  def withSafeId(safeId: String): BusinessPartnerRecord = copy(safeId = safeId)
  def modifySafeId(pf: PartialFunction[String, String]): BusinessPartnerRecord =
    if (pf.isDefinedAt(safeId)) copy(safeId = pf(safeId)) else this
  def withAgentReferenceNumber(agentReferenceNumber: Option[String]): BusinessPartnerRecord =
    copy(agentReferenceNumber = agentReferenceNumber)
  def modifyAgentReferenceNumber(pf: PartialFunction[Option[String], Option[String]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(agentReferenceNumber)) copy(agentReferenceNumber = pf(agentReferenceNumber)) else this
  def withUtr(utr: Option[String]): BusinessPartnerRecord = copy(utr = utr)
  def modifyUtr(pf: PartialFunction[Option[String], Option[String]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(utr)) copy(utr = pf(utr)) else this
  def withNino(nino: Option[String]): BusinessPartnerRecord = copy(nino = nino)
  def modifyNino(pf: PartialFunction[Option[String], Option[String]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(nino)) copy(nino = pf(nino)) else this
  def withEori(eori: Option[String]): BusinessPartnerRecord = copy(eori = eori)
  def modifyEori(pf: PartialFunction[Option[String], Option[String]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(eori)) copy(eori = pf(eori)) else this
  def withIsAnAgent(isAnAgent: Boolean): BusinessPartnerRecord = copy(isAnAgent = isAnAgent)
  def modifyIsAnAgent(pf: PartialFunction[Boolean, Boolean]): BusinessPartnerRecord =
    if (pf.isDefinedAt(isAnAgent)) copy(isAnAgent = pf(isAnAgent)) else this
  def withIsAnASAgent(isAnASAgent: Boolean): BusinessPartnerRecord = copy(isAnASAgent = isAnASAgent)
  def modifyIsAnASAgent(pf: PartialFunction[Boolean, Boolean]): BusinessPartnerRecord =
    if (pf.isDefinedAt(isAnASAgent)) copy(isAnASAgent = pf(isAnASAgent)) else this
  def withIsAnIndividual(isAnIndividual: Boolean): BusinessPartnerRecord = copy(isAnIndividual = isAnIndividual)
  def modifyIsAnIndividual(pf: PartialFunction[Boolean, Boolean]): BusinessPartnerRecord =
    if (pf.isDefinedAt(isAnIndividual)) copy(isAnIndividual = pf(isAnIndividual)) else this
  def withIndividual(individual: Option[Individual]): BusinessPartnerRecord = copy(individual = individual)
  def modifyIndividual(pf: PartialFunction[Option[Individual], Option[Individual]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
  def withIsAnOrganisation(isAnOrganisation: Boolean): BusinessPartnerRecord = copy(isAnOrganisation = isAnOrganisation)
  def modifyIsAnOrganisation(pf: PartialFunction[Boolean, Boolean]): BusinessPartnerRecord =
    if (pf.isDefinedAt(isAnOrganisation)) copy(isAnOrganisation = pf(isAnOrganisation)) else this
  def withOrganisation(organisation: Option[Organisation]): BusinessPartnerRecord = copy(organisation = organisation)
  def modifyOrganisation(pf: PartialFunction[Option[Organisation], Option[Organisation]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(organisation)) copy(organisation = pf(organisation)) else this
  def withAddressDetails(addressDetails: AddressDetails): BusinessPartnerRecord = copy(addressDetails = addressDetails)
  def modifyAddressDetails(pf: PartialFunction[AddressDetails, AddressDetails]): BusinessPartnerRecord =
    if (pf.isDefinedAt(addressDetails)) copy(addressDetails = pf(addressDetails)) else this
  def withContactDetails(contactDetails: Option[ContactDetails]): BusinessPartnerRecord =
    copy(contactDetails = contactDetails)
  def modifyContactDetails(pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
  def withAgencyDetails(agencyDetails: Option[AgencyDetails]): BusinessPartnerRecord =
    copy(agencyDetails = agencyDetails)
  def modifyAgencyDetails(pf: PartialFunction[Option[AgencyDetails], Option[AgencyDetails]]): BusinessPartnerRecord =
    if (pf.isDefinedAt(agencyDetails)) copy(agencyDetails = pf(agencyDetails)) else this
}

object BusinessPartnerRecord extends RecordUtils[BusinessPartnerRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[BusinessPartnerRecord] = RecordMetaData[BusinessPartnerRecord](this)

  def uniqueKey(key: String): String = s"""safeId:${key.toUpperCase}"""
  def agentReferenceNumberKey(key: String): String = s"""agentReferenceNumber:${key.toUpperCase}"""
  def utrKey(key: String): String = s"""utr:${key.toUpperCase}"""
  def ninoKey(key: String): String = s"""nino:${key.toUpperCase}"""
  def eoriKey(key: String): String = s"""eori:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  val safeIdValidator: Validator[String] =
    check(_.matches(Common.safeIdPattern), s"""Invalid safeId, does not matches regex ${Common.safeIdPattern}""")
  val agentReferenceNumberValidator: Validator[Option[String]] = check(
    _.matches(Common.agentReferenceNumberPattern),
    s"""Invalid agentReferenceNumber, does not matches regex ${Common.agentReferenceNumberPattern}""")
  val utrValidator: Validator[Option[String]] =
    check(_.matches(Common.utrPattern), s"""Invalid utr, does not matches regex ${Common.utrPattern}""")
  val ninoValidator: Validator[Option[String]] =
    check(_.matches(Common.ninoPattern), s"""Invalid nino, does not matches regex ${Common.ninoPattern}""")
  val eoriValidator: Validator[Option[String]] =
    check(_.matches(Common.eoriPattern), s"""Invalid eori, does not matches regex ${Common.eoriPattern}""")
  val individualValidator: Validator[Option[Individual]] = checkIfSome(identity, Individual.validate)
  val organisationValidator: Validator[Option[Organisation]] = checkIfSome(identity, Organisation.validate)
  val addressDetailsValidator: Validator[AddressDetails] = checkProperty(identity, AddressDetails.validate)
  val contactDetailsValidator: Validator[Option[ContactDetails]] = checkIfSome(identity, ContactDetails.validate)
  val agencyDetailsValidator: Validator[Option[AgencyDetails]] = checkIfSome(identity, AgencyDetails.validate)

  override val validate: Validator[BusinessPartnerRecord] = Validator(
    checkProperty(_.safeId, safeIdValidator),
    checkProperty(_.agentReferenceNumber, agentReferenceNumberValidator),
    checkProperty(_.utr, utrValidator),
    checkProperty(_.nino, ninoValidator),
    checkProperty(_.eori, eoriValidator),
    checkProperty(_.individual, individualValidator),
    checkProperty(_.organisation, organisationValidator),
    checkProperty(_.addressDetails, addressDetailsValidator),
    checkProperty(_.contactDetails, contactDetailsValidator),
    checkProperty(_.agencyDetails, agencyDetailsValidator),
    checkIfOnlyOneSetIsDefined(
      Seq(Set(_.isAnIndividual.asOption, _.individual), Set(_.isAnOrganisation.asOption, _.organisation)),
      "[{isAnIndividual,individual},{isAnOrganisation,organisation}]"
    )
  )

  override val gen: Gen[BusinessPartnerRecord] = for {
    businessPartnerExists <- Generator.booleanGen
    safeId                <- Generator.safeIdGen
    isAnAgent             <- Generator.booleanGen
    isAnASAgent           <- Generator.booleanGen
    isAnIndividual        <- Generator.booleanGen
    isAnOrganisation      <- Generator.booleanGen
    addressDetails        <- AddressDetails.gen
  } yield
    BusinessPartnerRecord(
      businessPartnerExists = businessPartnerExists,
      safeId = safeId,
      isAnAgent = isAnAgent,
      isAnASAgent = isAnASAgent,
      isAnIndividual = isAnIndividual,
      isAnOrganisation = isAnOrganisation,
      addressDetails = addressDetails
    )

  val agentReferenceNumberSanitizer: Update = seed =>
    entity =>
      entity.copy(
        agentReferenceNumber = agentReferenceNumberValidator(entity.agentReferenceNumber)
          .fold(_ => None, _ => entity.agentReferenceNumber)
          .orElse(Generator.get(Generator.arnGen)(seed)))

  val utrSanitizer: Update = seed =>
    entity =>
      entity.copy(
        utr = utrValidator(entity.utr)
          .fold(_ => None, _ => entity.utr)
          .orElse(Generator.get(Generator.utrGen)(seed)))

  val ninoSanitizer: Update = seed =>
    entity =>
      entity.copy(
        nino = ninoValidator(entity.nino)
          .fold(_ => None, _ => entity.nino)
          .orElse(Generator.get(Generator.ninoNoSpacesGen)(seed)))

  val eoriSanitizer: Update = seed =>
    entity =>
      entity.copy(
        eori = eoriValidator(entity.eori)
          .fold(_ => None, _ => entity.eori)
          .orElse(Generator.get(Generator.regex(Common.eoriPattern))(seed)))

  val individualSanitizer: Update = seed =>
    entity =>
      entity.copy(
        individual = entity.individual
          .orElse(Generator.get(Individual.gen)(seed))
          .map(Individual.sanitize(seed)))

  val organisationSanitizer: Update = seed =>
    entity =>
      entity.copy(
        organisation = entity.organisation
          .orElse(Generator.get(Organisation.gen)(seed))
          .map(Organisation.sanitize(seed)))

  val contactDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(
        contactDetails = entity.contactDetails
          .orElse(Generator.get(ContactDetails.gen)(seed))
          .map(ContactDetails.sanitize(seed)))

  val agencyDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(
        agencyDetails = entity.agencyDetails
          .orElse(Generator.get(AgencyDetails.gen)(seed))
          .map(AgencyDetails.sanitize(seed)))

  val isAnIndividualAndIndividualCompoundSanitizer: Update = seed =>
    entity =>
      entity.copy(
        isAnIndividual = true,
        individual = entity.individual.orElse(Generator.get(Individual.gen)(seed)).map(Individual.sanitize(seed)),
        isAnOrganisation = false,
        organisation = None
  )

  val isAnOrganisationAndOrganisationCompoundSanitizer: Update = seed =>
    entity =>
      entity.copy(
        isAnOrganisation = true,
        organisation =
          entity.organisation.orElse(Generator.get(Organisation.gen)(seed)).map(Organisation.sanitize(seed)),
        isAnIndividual = false,
        individual = None
  )

  val isAnIndividualOrIsAnOrganisationAlternativeSanitizer: Update = seed =>
    entity =>
      if (entity.isAnIndividual.isDefined) isAnIndividualAndIndividualCompoundSanitizer(seed)(entity)
      else if (entity.isAnOrganisation.isDefined) isAnOrganisationAndOrganisationCompoundSanitizer(seed)(entity)
      else
        Generator.get(Gen.chooseNum(0, 1))(seed) match {
          case Some(0) => isAnIndividualAndIndividualCompoundSanitizer(seed)(entity)
          case _       => isAnOrganisationAndOrganisationCompoundSanitizer(seed)(entity)
    }

  override val sanitizers: Seq[Update] = Seq(
    agentReferenceNumberSanitizer,
    utrSanitizer,
    ninoSanitizer,
    eoriSanitizer,
    contactDetailsSanitizer,
    agencyDetailsSanitizer,
    isAnIndividualOrIsAnOrganisationAlternativeSanitizer
  )

  implicit val formats: Format[BusinessPartnerRecord] = Json.format[BusinessPartnerRecord]

  sealed trait AddressDetails {
    def addressLine2: Option[String] = None
    def addressLine3: Option[String] = None
    def addressLine1: String
    def countryCode: String
    def addressLine4: Option[String] = None
  }

  object AddressDetails extends RecordUtils[AddressDetails] {

    override val validate: Validator[AddressDetails] = {
      case x: UkAddress      => UkAddress.validate(x)
      case x: ForeignAddress => ForeignAddress.validate(x)
    }

    override val gen: Gen[AddressDetails] = Gen.oneOf[AddressDetails](
      UkAddress.gen.map(_.asInstanceOf[AddressDetails]),
      ForeignAddress.gen.map(_.asInstanceOf[AddressDetails]))

    val sanitizer: Update = seed => {
      case x: UkAddress      => UkAddress.sanitize(seed)(x)
      case x: ForeignAddress => ForeignAddress.sanitize(seed)(x)
    }
    override val sanitizers: Seq[Update] = Seq(sanitizer)

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
    agencyEmail: Option[String] = None) {

    def withAgencyName(agencyName: Option[String]): AgencyDetails = copy(agencyName = agencyName)
    def modifyAgencyName(pf: PartialFunction[Option[String], Option[String]]): AgencyDetails =
      if (pf.isDefinedAt(agencyName)) copy(agencyName = pf(agencyName)) else this
    def withAgencyAddress(agencyAddress: Option[AgencyDetails.AgencyAddress]): AgencyDetails =
      copy(agencyAddress = agencyAddress)
    def modifyAgencyAddress(
      pf: PartialFunction[Option[AgencyDetails.AgencyAddress], Option[AgencyDetails.AgencyAddress]]): AgencyDetails =
      if (pf.isDefinedAt(agencyAddress)) copy(agencyAddress = pf(agencyAddress)) else this
    def withAgencyEmail(agencyEmail: Option[String]): AgencyDetails = copy(agencyEmail = agencyEmail)
    def modifyAgencyEmail(pf: PartialFunction[Option[String], Option[String]]): AgencyDetails =
      if (pf.isDefinedAt(agencyEmail)) copy(agencyEmail = pf(agencyEmail)) else this
  }

  object AgencyDetails extends RecordUtils[AgencyDetails] {

    val agencyNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 40), "Invalid length of agencyName, should be between 1 and 40 inclusive")
    val agencyAddressValidator: Validator[Option[AgencyAddress]] = checkIfSome(identity, AgencyAddress.validate)
    val agencyEmailValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 132), "Invalid length of agencyEmail, should be between 1 and 132 inclusive")

    override val validate: Validator[AgencyDetails] = Validator(
      checkProperty(_.agencyName, agencyNameValidator),
      checkProperty(_.agencyAddress, agencyAddressValidator),
      checkProperty(_.agencyEmail, agencyEmailValidator)
    )

    override val gen: Gen[AgencyDetails] = Gen const AgencyDetails(
      )

    val agencyNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          agencyName = agencyNameValidator(entity.agencyName)
            .fold(_ => None, _ => entity.agencyName)
            .orElse(Generator.get(
              UserGenerator.agencyNameGen.map(_.take(40)).suchThat(_.length >= 1).suchThat(_.length <= 40))(seed)))

    val agencyAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(
          agencyAddress = agencyAddressValidator(entity.agencyAddress)
            .fold(_ => None, _ => entity.agencyAddress)
            .orElse(Generator.get(AgencyAddress.gen)(seed)))

    val agencyEmailSanitizer: Update = seed =>
      entity =>
        entity.copy(
          agencyEmail = agencyEmailValidator(entity.agencyEmail)
            .fold(_ => None, _ => entity.agencyEmail)
            .orElse(Generator.get(
              Generator.emailGen.variant("agency").suchThat(_.length >= 1).suchThat(_.length <= 132))(seed)))

    override val sanitizers: Seq[Update] = Seq(agencyNameSanitizer, agencyAddressSanitizer, agencyEmailSanitizer)

    implicit val formats: Format[AgencyDetails] = Json.format[AgencyDetails]

    sealed trait AgencyAddress {
      def addressLine2: Option[String] = None
      def addressLine3: Option[String] = None
      def addressLine1: String
      def countryCode: String
      def addressLine4: Option[String] = None
    }

    object AgencyAddress extends RecordUtils[AgencyAddress] {

      override val validate: Validator[AgencyAddress] = {
        case x: ForeignAddress => ForeignAddress.validate(x)
        case x: UkAddress      => UkAddress.validate(x)
      }

      override val gen: Gen[AgencyAddress] = Gen.oneOf[AgencyAddress](
        ForeignAddress.gen.map(_.asInstanceOf[AgencyAddress]),
        UkAddress.gen.map(_.asInstanceOf[AgencyAddress]))

      val sanitizer: Update = seed => {
        case x: ForeignAddress => ForeignAddress.sanitize(seed)(x)
        case x: UkAddress      => UkAddress.sanitize(seed)(x)
      }
      override val sanitizers: Seq[Update] = Seq(sanitizer)

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

  object ContactDetails extends RecordUtils[ContactDetails] {

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
      check(_.lengthMinMaxInclusive(1, 132), "Invalid length of emailAddress, should be between 1 and 132 inclusive")

    override val validate: Validator[ContactDetails] = Validator(
      checkProperty(_.phoneNumber, phoneNumberValidator),
      checkProperty(_.mobileNumber, mobileNumberValidator),
      checkProperty(_.faxNumber, faxNumberValidator),
      checkProperty(_.emailAddress, emailAddressValidator)
    )

    override val gen: Gen[ContactDetails] = Gen const ContactDetails(
      )

    val phoneNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          phoneNumber = phoneNumberValidator(entity.phoneNumber)
            .fold(_ => None, _ => entity.phoneNumber)
            .orElse(Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val mobileNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          mobileNumber = mobileNumberValidator(entity.mobileNumber)
            .fold(_ => None, _ => entity.mobileNumber)
            .orElse(Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val faxNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          faxNumber = faxNumberValidator(entity.faxNumber)
            .fold(_ => None, _ => entity.faxNumber)
            .orElse(Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed)))

    val emailAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(
          emailAddress = emailAddressValidator(entity.emailAddress)
            .fold(_ => None, _ => entity.emailAddress)
            .orElse(Generator.get(Generator.emailGen.suchThat(_.length >= 1).suchThat(_.length <= 132))(seed)))

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
      extends AddressDetails with AgencyDetails.AgencyAddress {

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

    val addressLine1Validator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine1, should be between 1 and 35 inclusive")
    val addressLine2Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine2, should be between 1 and 35 inclusive")
    val addressLine3Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine3, should be between 1 and 35 inclusive")
    val addressLine4Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine4, should be between 1 and 35 inclusive")
    val postalCodeValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 10), "Invalid length of postalCode, should be between 1 and 10 inclusive")
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")

    override val validate: Validator[ForeignAddress] = Validator(
      checkProperty(_.addressLine1, addressLine1Validator),
      checkProperty(_.addressLine2, addressLine2Validator),
      checkProperty(_.addressLine3, addressLine3Validator),
      checkProperty(_.addressLine4, addressLine4Validator),
      checkProperty(_.postalCode, postalCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
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
          addressLine2 = addressLine2Validator(entity.addressLine2)
            .fold(_ => None, _ => entity.addressLine2)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line2).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = addressLine3Validator(entity.addressLine3)
            .fold(_ => None, _ => entity.addressLine3)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line3).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine4 = addressLine4Validator(entity.addressLine4)
            .fold(_ => None, _ => entity.addressLine4)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line4).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val postalCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          postalCode = postalCodeValidator(entity.postalCode)
            .fold(_ => None, _ => entity.postalCode)
            .orElse(Generator.get(Generator.postcode.suchThat(_.length >= 1).suchThat(_.length <= 10))(seed)))

    override val sanitizers: Seq[Update] =
      Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer, postalCodeSanitizer)

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class Individual(firstName: String, middleName: Option[String] = None, lastName: String, dateOfBirth: String) {

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
    def modifyDateOfBirth(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(dateOfBirth)) copy(dateOfBirth = pf(dateOfBirth)) else this
  }

  object Individual extends RecordUtils[Individual] {

    val firstNameValidator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of firstName, should be between 1 and 35 inclusive")
    val middleNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of middleName, should be between 1 and 35 inclusive")
    val lastNameValidator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of lastName, should be between 1 and 35 inclusive")
    val dateOfBirthValidator: Validator[String] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}""")

    override val validate: Validator[Individual] = Validator(
      checkProperty(_.firstName, firstNameValidator),
      checkProperty(_.middleName, middleNameValidator),
      checkProperty(_.lastName, lastNameValidator),
      checkProperty(_.dateOfBirth, dateOfBirthValidator)
    )

    override val gen: Gen[Individual] = for {
      firstName   <- Generator.forename().suchThat(_.length >= 1).suchThat(_.length <= 35)
      lastName    <- Generator.surname.suchThat(_.length >= 1).suchThat(_.length <= 35)
      dateOfBirth <- Generator.dateYYYYMMDDGen.variant("ofbirth")
    } yield
      Individual(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth
      )

    val middleNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          middleName = middleNameValidator(entity.middleName)
            .fold(_ => None, _ => entity.middleName)
            .orElse(Generator.get(
              Generator.forename().variant("middle").suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    override val sanitizers: Seq[Update] = Seq(middleNameSanitizer)

    implicit val formats: Format[Individual] = Json.format[Individual]

  }

  case class Organisation(organisationName: String, isAGroup: Boolean = false, organisationType: String) {

    def withOrganisationName(organisationName: String): Organisation = copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[String, String]): Organisation =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withIsAGroup(isAGroup: Boolean): Organisation = copy(isAGroup = isAGroup)
    def modifyIsAGroup(pf: PartialFunction[Boolean, Boolean]): Organisation =
      if (pf.isDefinedAt(isAGroup)) copy(isAGroup = pf(isAGroup)) else this
    def withOrganisationType(organisationType: String): Organisation = copy(organisationType = organisationType)
    def modifyOrganisationType(pf: PartialFunction[String, String]): Organisation =
      if (pf.isDefinedAt(organisationType)) copy(organisationType = pf(organisationType)) else this
  }

  object Organisation extends RecordUtils[Organisation] {

    val organisationNameValidator: Validator[String] = check(
      _.lengthMinMaxInclusive(1, 105),
      "Invalid length of organisationName, should be between 1 and 105 inclusive")
    val organisationTypeValidator: Validator[String] = check(
      _.matches(Common.organisationTypePattern),
      s"""Invalid organisationType, does not matches regex ${Common.organisationTypePattern}""")

    override val validate: Validator[Organisation] = Validator(
      checkProperty(_.organisationName, organisationNameValidator),
      checkProperty(_.organisationType, organisationTypeValidator))

    override val gen: Gen[Organisation] = for {
      organisationName <- Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 105)
      isAGroup         <- Generator.booleanGen
      organisationType <- Generator.regex(Common.organisationTypePattern)
    } yield
      Organisation(
        organisationName = organisationName,
        isAGroup = isAGroup,
        organisationType = organisationType
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
      extends AddressDetails with AgencyDetails.AgencyAddress {

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

    val addressLine1Validator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine1, should be between 1 and 35 inclusive")
    val addressLine2Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine2, should be between 1 and 35 inclusive")
    val addressLine3Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine3, should be between 1 and 35 inclusive")
    val addressLine4Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine4, should be between 1 and 35 inclusive")
    val postalCodeValidator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 10), "Invalid length of postalCode, should be between 1 and 10 inclusive")
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")

    override val validate: Validator[UkAddress] = Validator(
      checkProperty(_.addressLine1, addressLine1Validator),
      checkProperty(_.addressLine2, addressLine2Validator),
      checkProperty(_.addressLine3, addressLine3Validator),
      checkProperty(_.addressLine4, addressLine4Validator),
      checkProperty(_.postalCode, postalCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
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
          addressLine2 = addressLine2Validator(entity.addressLine2)
            .fold(_ => None, _ => entity.addressLine2)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line2).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine3 = addressLine3Validator(entity.addressLine3)
            .fold(_ => None, _ => entity.addressLine3)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line3).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          addressLine4 = addressLine4Validator(entity.addressLine4)
            .fold(_ => None, _ => entity.addressLine4)
            .orElse(Generator.get(
              Generator.address4Lines35Gen.map(_.line4).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)))

    override val sanitizers: Seq[Update] = Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer)

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  object Common {
    val ninoPattern = """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$"""
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
    val utrPattern = """^[0-9]{1,10}$"""
    val agentReferenceNumberPattern = """^[A-Z](ARN)[0-9]{7}$"""
    val eoriPattern = """^[A-Z]{2}[0-9]{12}$"""
  }
}
