/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord._

/** ----------------------------------------------------------------------------
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
  *  -  ContactDetails
  *  -  CorrespondenceContactDetails
  *  -  CustomerDetails
  *  -  Deregistration
  *  -  FlatRateScheme
  *  -  ForeignAddress
  *  -  GroupOrPartner
  *  -  IndividualName
  *  -  NonStdTaxPeriods
  *  -  PPOB
  *  -  Period
  *  -  UkAddress
  */
case class VatCustomerInformationRecord(
  vrn: String,
  approvedInformation: Option[ApprovedInformation] = None,
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
    pf: PartialFunction[Option[ApprovedInformation], Option[ApprovedInformation]]
  ): VatCustomerInformationRecord =
    if (pf.isDefinedAt(approvedInformation)) copy(approvedInformation = pf(approvedInformation)) else this
}

object VatCustomerInformationRecord extends RecordUtils[VatCustomerInformationRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[VatCustomerInformationRecord] =
    RecordMetaData[VatCustomerInformationRecord](this)

  def uniqueKey(key: String): String = s"""vrn:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  val vrnValidator: Validator[String] =
    check(_.matches(Common.vrnPattern), s"""Invalid vrn, does not matches regex ${Common.vrnPattern}""")
  val approvedInformationValidator: Validator[Option[ApprovedInformation]] =
    checkIfSome(identity, ApprovedInformation.validate)

  override val validate: Validator[VatCustomerInformationRecord] =
    Validator(checkProperty(_.vrn, vrnValidator), checkProperty(_.approvedInformation, approvedInformationValidator))

  override val gen: Gen[VatCustomerInformationRecord] = for {
    vrn <- Generator.vrnGen
  } yield VatCustomerInformationRecord(
    vrn = vrn
  )

  val approvedInformationSanitizer: Update = seed =>
    entity =>
      entity.copy(
        approvedInformation = entity.approvedInformation
          .orElse(Generator.get(ApprovedInformation.gen)(seed))
          .map(ApprovedInformation.sanitize(seed))
      )

  override val sanitizers: Seq[Update] = Seq(approvedInformationSanitizer)

  implicit val formats: Format[VatCustomerInformationRecord] = Json.format[VatCustomerInformationRecord]

  sealed trait Address {
    def line4: Option[String] = None
    def line1: String
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
            .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        )
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of Address, i.e. UkAddress, ForeignAddress"),
            r0,
            r1
          )
        )
      }

      private def aggregateErrors[T](errors: JsResult[T]*): JsError =
        errors.foldLeft(JsError())((a, r) =>
          r match {
            case e: JsError => JsError(a.errors ++ e.errors)
            case _          => a
          }
        )
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
    groupOrPartnerMbrs: Option[Seq[GroupOrPartner]] = None
  ) {

    def withCustomerDetails(customerDetails: CustomerDetails): ApprovedInformation =
      copy(customerDetails = customerDetails)
    def modifyCustomerDetails(pf: PartialFunction[CustomerDetails, CustomerDetails]): ApprovedInformation =
      if (pf.isDefinedAt(customerDetails)) copy(customerDetails = pf(customerDetails)) else this
    def withPPOB(PPOB: PPOB): ApprovedInformation = copy(PPOB = PPOB)
    def modifyPPOB(pf: PartialFunction[PPOB, PPOB]): ApprovedInformation =
      if (pf.isDefinedAt(PPOB)) copy(PPOB = pf(PPOB)) else this
    def withCorrespondenceContactDetails(
      correspondenceContactDetails: Option[CorrespondenceContactDetails]
    ): ApprovedInformation =
      copy(correspondenceContactDetails = correspondenceContactDetails)
    def modifyCorrespondenceContactDetails(
      pf: PartialFunction[Option[CorrespondenceContactDetails], Option[CorrespondenceContactDetails]]
    ): ApprovedInformation =
      if (pf.isDefinedAt(correspondenceContactDetails))
        copy(correspondenceContactDetails = pf(correspondenceContactDetails))
      else this
    def withBankDetails(bankDetails: Option[BankDetails]): ApprovedInformation = copy(bankDetails = bankDetails)
    def modifyBankDetails(pf: PartialFunction[Option[BankDetails], Option[BankDetails]]): ApprovedInformation =
      if (pf.isDefinedAt(bankDetails)) copy(bankDetails = pf(bankDetails)) else this
    def withBusinessActivities(businessActivities: Option[BusinessActivities]): ApprovedInformation =
      copy(businessActivities = businessActivities)
    def modifyBusinessActivities(
      pf: PartialFunction[Option[BusinessActivities], Option[BusinessActivities]]
    ): ApprovedInformation =
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
      pf: PartialFunction[Option[Seq[GroupOrPartner]], Option[Seq[GroupOrPartner]]]
    ): ApprovedInformation =
      if (pf.isDefinedAt(groupOrPartnerMbrs)) copy(groupOrPartnerMbrs = pf(groupOrPartnerMbrs)) else this
  }

  object ApprovedInformation extends RecordUtils[ApprovedInformation] {

    val customerDetailsValidator: Validator[CustomerDetails] = checkProperty(identity, CustomerDetails.validate)
    val PPOBValidator: Validator[PPOB] = checkProperty(identity, PPOB.validate)
    val correspondenceContactDetailsValidator: Validator[Option[CorrespondenceContactDetails]] =
      checkIfSome(identity, CorrespondenceContactDetails.validate)
    val bankDetailsValidator: Validator[Option[BankDetails]] = checkIfSome(identity, BankDetails.validate)
    val businessActivitiesValidator: Validator[Option[BusinessActivities]] =
      checkIfSome(identity, BusinessActivities.validate)
    val flatRateSchemeValidator: Validator[Option[FlatRateScheme]] = checkIfSome(identity, FlatRateScheme.validate)
    val deregistrationValidator: Validator[Option[Deregistration]] = checkIfSome(identity, Deregistration.validate)
    val returnPeriodValidator: Validator[Option[Period]] = checkIfSome(identity, Period.validate)
    val groupOrPartnerMbrsValidator: Validator[Option[Seq[GroupOrPartner]]] = Validator(
      checkEachIfSome(identity, GroupOrPartner.validate),
      check(_.size >= 1, "Invalid array size, must be greater than or equal to 1")
    )

    override val validate: Validator[ApprovedInformation] = Validator(
      checkProperty(_.customerDetails, customerDetailsValidator),
      checkProperty(_.PPOB, PPOBValidator),
      checkProperty(_.correspondenceContactDetails, correspondenceContactDetailsValidator),
      checkProperty(_.bankDetails, bankDetailsValidator),
      checkProperty(_.businessActivities, businessActivitiesValidator),
      checkProperty(_.flatRateScheme, flatRateSchemeValidator),
      checkProperty(_.deregistration, deregistrationValidator),
      checkProperty(_.returnPeriod, returnPeriodValidator),
      checkProperty(_.groupOrPartnerMbrs, groupOrPartnerMbrsValidator)
    )

    override val gen: Gen[ApprovedInformation] = for {
      customerDetails <- CustomerDetails.gen
      ppob            <- PPOB.gen
    } yield ApprovedInformation(
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
            .map(CorrespondenceContactDetails.sanitize(seed))
        )

    val bankDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankDetails = entity.bankDetails
            .orElse(Generator.get(BankDetails.gen)(seed))
            .map(BankDetails.sanitize(seed))
        )

    val businessActivitiesSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessActivities = entity.businessActivities
            .orElse(Generator.get(BusinessActivities.gen)(seed))
            .map(BusinessActivities.sanitize(seed))
        )

    val flatRateSchemeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          flatRateScheme = entity.flatRateScheme
            .orElse(Generator.get(FlatRateScheme.gen)(seed))
            .map(FlatRateScheme.sanitize(seed))
        )

    val deregistrationSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregistration = entity.deregistration
            .orElse(Generator.get(Deregistration.gen)(seed))
            .map(Deregistration.sanitize(seed))
        )

    val returnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          returnPeriod = entity.returnPeriod
            .orElse(Generator.get(Period.gen)(seed))
            .map(Period.sanitize(seed))
        )

    val groupOrPartnerMbrsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          groupOrPartnerMbrs = entity.groupOrPartnerMbrs
            .orElse(Generator.get(Generator.nonEmptyListOfMaxN(1, GroupOrPartner.gen))(seed))
            .map(_.map(GroupOrPartner.sanitize(seed)))
        )

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
    bankBuildSocietyName: Option[String] = None
  ) {

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

    val IBANValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 34), "Invalid length of IBAN, should be between 1 and 34 inclusive")
    val BICValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 11), "Invalid length of BIC, should be between 1 and 11 inclusive")
    val accountHolderNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 60), "Invalid length of accountHolderName, should be between 1 and 60 inclusive")
    val bankAccountNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.bankAccountNumberPattern),
      s"""Invalid bankAccountNumber, does not matches regex ${Common.bankAccountNumberPattern}"""
    )
    val sortCodeValidator: Validator[Option[String]] = check(
      _.matches(Common.sortCodePattern),
      s"""Invalid sortCode, does not matches regex ${Common.sortCodePattern}"""
    )
    val buildingSocietyNumberValidator: Validator[Option[String]] = check(
      _.lengthMinMaxInclusive(1, 20),
      "Invalid length of buildingSocietyNumber, should be between 1 and 20 inclusive"
    )
    val bankBuildSocietyNameValidator: Validator[Option[String]] = check(
      _.lengthMinMaxInclusive(1, 40),
      "Invalid length of bankBuildSocietyName, should be between 1 and 40 inclusive"
    )

    override val validate: Validator[BankDetails] = Validator(
      checkProperty(_.IBAN, IBANValidator),
      checkProperty(_.BIC, BICValidator),
      checkProperty(_.accountHolderName, accountHolderNameValidator),
      checkProperty(_.bankAccountNumber, bankAccountNumberValidator),
      checkProperty(_.sortCode, sortCodeValidator),
      checkProperty(_.buildingSocietyNumber, buildingSocietyNumberValidator),
      checkProperty(_.bankBuildSocietyName, bankBuildSocietyNameValidator)
    )

    override val gen: Gen[BankDetails] = Gen const BankDetails(
    )

    val IBANSanitizer: Update = seed =>
      entity =>
        entity.copy(IBAN =
          IBANValidator(entity.IBAN)
            .fold(_ => None, _ => entity.IBAN)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 34).suchThat(_.length >= 1).suchThat(_.length <= 34))(seed)
            )
        )

    val BICSanitizer: Update = seed =>
      entity =>
        entity.copy(BIC =
          BICValidator(entity.BIC)
            .fold(_ => None, _ => entity.BIC)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 11).suchThat(_.length >= 1).suchThat(_.length <= 11))(seed)
            )
        )

    val accountHolderNameSanitizer: Update = seed =>
      entity =>
        entity.copy(accountHolderName =
          accountHolderNameValidator(entity.accountHolderName)
            .fold(_ => None, _ => entity.accountHolderName)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 60).suchThat(_.length >= 1).suchThat(_.length <= 60))(seed)
            )
        )

    val bankAccountNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          bankAccountNumber = bankAccountNumberValidator(entity.bankAccountNumber)
            .fold(_ => None, _ => entity.bankAccountNumber)
            .orElse(Generator.get(Generator.regex(Common.bankAccountNumberPattern))(seed))
        )

    val sortCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          sortCode = sortCodeValidator(entity.sortCode)
            .fold(_ => None, _ => entity.sortCode)
            .orElse(Generator.get(Generator.regex(Common.sortCodePattern))(seed))
        )

    val buildingSocietyNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(buildingSocietyNumber =
          buildingSocietyNumberValidator(entity.buildingSocietyNumber)
            .fold(_ => None, _ => entity.buildingSocietyNumber)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 20).suchThat(_.length >= 1).suchThat(_.length <= 20))(seed)
            )
        )

    val bankBuildSocietyNameSanitizer: Update = seed =>
      entity =>
        entity.copy(bankBuildSocietyName =
          bankBuildSocietyNameValidator(entity.bankBuildSocietyName)
            .fold(_ => None, _ => entity.bankBuildSocietyName)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 40).suchThat(_.length >= 1).suchThat(_.length <= 40))(seed)
            )
        )

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
    mainCode4: Option[String] = None
  ) {

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

    val primaryMainCodeValidator: Validator[String] = check(
      _.matches(Common.primaryMainCodePattern),
      s"""Invalid primaryMainCode, does not matches regex ${Common.primaryMainCodePattern}"""
    )
    val mainCode2Validator: Validator[Option[String]] = check(
      _.matches(Common.primaryMainCodePattern),
      s"""Invalid mainCode2, does not matches regex ${Common.primaryMainCodePattern}"""
    )
    val mainCode3Validator: Validator[Option[String]] = check(
      _.matches(Common.primaryMainCodePattern),
      s"""Invalid mainCode3, does not matches regex ${Common.primaryMainCodePattern}"""
    )
    val mainCode4Validator: Validator[Option[String]] = check(
      _.matches(Common.primaryMainCodePattern),
      s"""Invalid mainCode4, does not matches regex ${Common.primaryMainCodePattern}"""
    )

    override val validate: Validator[BusinessActivities] = Validator(
      checkProperty(_.primaryMainCode, primaryMainCodeValidator),
      checkProperty(_.mainCode2, mainCode2Validator),
      checkProperty(_.mainCode3, mainCode3Validator),
      checkProperty(_.mainCode4, mainCode4Validator)
    )

    override val gen: Gen[BusinessActivities] = for {
      primaryMainCode <- Generator.regex(Common.primaryMainCodePattern)
    } yield BusinessActivities(
      primaryMainCode = primaryMainCode
    )

    val mainCode2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode2 = mainCode2Validator(entity.mainCode2)
            .fold(_ => None, _ => entity.mainCode2)
            .orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed))
        )

    val mainCode3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode3 = mainCode3Validator(entity.mainCode3)
            .fold(_ => None, _ => entity.mainCode3)
            .orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed))
        )

    val mainCode4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          mainCode4 = mainCode4Validator(entity.mainCode4)
            .fold(_ => None, _ => entity.mainCode4)
            .orElse(Generator.get(Generator.regex(Common.primaryMainCodePattern))(seed))
        )

    override val sanitizers: Seq[Update] = Seq(mainCode2Sanitizer, mainCode3Sanitizer, mainCode4Sanitizer)

    implicit val formats: Format[BusinessActivities] = Json.format[BusinessActivities]

  }

  case class ContactDetails(
    primaryPhoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None
  ) {

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

    val primaryPhoneNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.primaryPhoneNumberPattern),
      s"""Invalid primaryPhoneNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""
    )
    val mobileNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.primaryPhoneNumberPattern),
      s"""Invalid mobileNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""
    )
    val faxNumberValidator: Validator[Option[String]] = check(
      _.matches(Common.primaryPhoneNumberPattern),
      s"""Invalid faxNumber, does not matches regex ${Common.primaryPhoneNumberPattern}"""
    )
    val emailAddressValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(3, 132), "Invalid length of emailAddress, should be between 3 and 132 inclusive")

    override val validate: Validator[ContactDetails] = Validator(
      checkProperty(_.primaryPhoneNumber, primaryPhoneNumberValidator),
      checkProperty(_.mobileNumber, mobileNumberValidator),
      checkProperty(_.faxNumber, faxNumberValidator),
      checkProperty(_.emailAddress, emailAddressValidator)
    )

    override val gen: Gen[ContactDetails] = Gen const ContactDetails(
    )

    val primaryPhoneNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          primaryPhoneNumber = primaryPhoneNumberValidator(entity.primaryPhoneNumber)
            .fold(_ => None, _ => entity.primaryPhoneNumber)
            .orElse(
              Generator.get(
                Generator.ukPhoneNumber.variant("primary").suchThat(_.length >= 1).suchThat(_.length <= 24)
              )(seed)
            )
        )

    val mobileNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          mobileNumber = mobileNumberValidator(entity.mobileNumber)
            .fold(_ => None, _ => entity.mobileNumber)
            .orElse(Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed))
        )

    val faxNumberSanitizer: Update = seed =>
      entity =>
        entity.copy(
          faxNumber = faxNumberValidator(entity.faxNumber)
            .fold(_ => None, _ => entity.faxNumber)
            .orElse(Generator.get(Generator.ukPhoneNumber.suchThat(_.length >= 1).suchThat(_.length <= 24))(seed))
        )

    val emailAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(
          emailAddress = emailAddressValidator(entity.emailAddress)
            .fold(_ => None, _ => entity.emailAddress)
            .orElse(Generator.get(Generator.emailGen.suchThat(_.length >= 3).suchThat(_.length <= 132))(seed))
        )

    override val sanitizers: Seq[Update] =
      Seq(primaryPhoneNumberSanitizer, mobileNumberSanitizer, faxNumberSanitizer, emailAddressSanitizer)

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]

  }

  case class CorrespondenceContactDetails(
    address: Address,
    RLS: Option[String] = None,
    contactDetails: Option[ContactDetails] = None
  ) {

    def withAddress(address: Address): CorrespondenceContactDetails = copy(address = address)
    def modifyAddress(pf: PartialFunction[Address, Address]): CorrespondenceContactDetails =
      if (pf.isDefinedAt(address)) copy(address = pf(address)) else this
    def withRLS(RLS: Option[String]): CorrespondenceContactDetails = copy(RLS = RLS)
    def modifyRLS(pf: PartialFunction[Option[String], Option[String]]): CorrespondenceContactDetails =
      if (pf.isDefinedAt(RLS)) copy(RLS = pf(RLS)) else this
    def withContactDetails(contactDetails: Option[ContactDetails]): CorrespondenceContactDetails =
      copy(contactDetails = contactDetails)
    def modifyContactDetails(
      pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]
    ): CorrespondenceContactDetails =
      if (pf.isDefinedAt(contactDetails)) copy(contactDetails = pf(contactDetails)) else this
  }

  object CorrespondenceContactDetails extends RecordUtils[CorrespondenceContactDetails] {

    val addressValidator: Validator[Address] = checkProperty(identity, Address.validate)
    val RLSValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.RLSEnum), "Invalid RLS, does not match allowed values")
    val contactDetailsValidator: Validator[Option[ContactDetails]] = checkIfSome(identity, ContactDetails.validate)

    override val validate: Validator[CorrespondenceContactDetails] = Validator(
      checkProperty(_.address, addressValidator),
      checkProperty(_.RLS, RLSValidator),
      checkProperty(_.contactDetails, contactDetailsValidator)
    )

    override val gen: Gen[CorrespondenceContactDetails] = for {
      address <- Address.gen
    } yield CorrespondenceContactDetails(
      address = address
    )

    val RLSSanitizer: Update = seed =>
      entity =>
        entity.copy(
          RLS = RLSValidator(entity.RLS)
            .fold(_ => None, _ => entity.RLS)
            .orElse(Generator.get(Gen.oneOf(Common.RLSEnum))(seed))
        )

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails = entity.contactDetails
            .orElse(Generator.get(ContactDetails.gen)(seed))
            .map(ContactDetails.sanitize(seed))
        )

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
    businessStartDate: Option[String] = None,
    isInsolvent: Option[Boolean] = Some(false)
  ) {

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
    def withInsolvencyFlag(insolvent: Boolean = false): CustomerDetails = copy(isInsolvent = Some(insolvent))
  }

  object CustomerDetails extends RecordUtils[CustomerDetails] {

    val organisationNameValidator: Validator[Option[String]] = check(
      _.lengthMinMaxInclusive(1, 105),
      "Invalid length of organisationName, should be between 1 and 105 inclusive"
    )
    val individualValidator: Validator[Option[IndividualName]] = checkIfSome(identity, IndividualName.validate)
    val dateOfBirthValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val tradingNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 160), "Invalid length of tradingName, should be between 1 and 160 inclusive")
    val mandationStatusValidator: Validator[String] =
      check(_.isOneOf(Common.mandationStatusEnum), "Invalid mandationStatus, does not match allowed values")
    val registrationReasonValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.registrationReasonEnum), "Invalid registrationReason, does not match allowed values")
    val effectiveRegistrationDateValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid effectiveRegistrationDate, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val businessStartDateValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid businessStartDate, does not matches regex ${Common.dateOfBirthPattern}"""
    )

    override val validate: Validator[CustomerDetails] = Validator(
      checkProperty(_.organisationName, organisationNameValidator),
      checkProperty(_.individual, individualValidator),
      checkProperty(_.dateOfBirth, dateOfBirthValidator),
      checkProperty(_.tradingName, tradingNameValidator),
      checkProperty(_.mandationStatus, mandationStatusValidator),
      checkProperty(_.registrationReason, registrationReasonValidator),
      checkProperty(_.effectiveRegistrationDate, effectiveRegistrationDateValidator),
      checkProperty(_.businessStartDate, businessStartDateValidator),
      checkIfOnlyOneSetIsDefined(
        Seq(Set(_.individual, _.dateOfBirth), Set(_.organisationName)),
        "[{individual,dateOfBirth},{organisationName}]"
      )
    )

    override val gen: Gen[CustomerDetails] = for {
      mandationStatus <- Gen.oneOf(Common.mandationStatusEnum)
    } yield CustomerDetails(
      mandationStatus = mandationStatus
    )

    val organisationNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          organisationName = organisationNameValidator(entity.organisationName)
            .fold(_ => None, _ => entity.organisationName)
            .orElse(Generator.get(Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 105))(seed))
        )

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual = entity.individual
            .orElse(Generator.get(IndividualName.gen)(seed))
            .map(IndividualName.sanitize(seed))
        )

    val dateOfBirthSanitizer: Update = seed =>
      entity =>
        entity.copy(
          dateOfBirth = dateOfBirthValidator(entity.dateOfBirth)
            .fold(_ => None, _ => entity.dateOfBirth)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("ofbirth"))(seed))
        )

    val tradingNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          tradingName = tradingNameValidator(entity.tradingName)
            .fold(_ => None, _ => entity.tradingName)
            .orElse(Generator.get(Generator.tradingNameGen.suchThat(_.length >= 1).suchThat(_.length <= 160))(seed))
        )

    val registrationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          registrationReason = registrationReasonValidator(entity.registrationReason)
            .fold(_ => None, _ => entity.registrationReason)
            .orElse(Generator.get(Gen.oneOf(Common.registrationReasonEnum))(seed))
        )

    val effectiveRegistrationDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          effectiveRegistrationDate = effectiveRegistrationDateValidator(entity.effectiveRegistrationDate)
            .fold(_ => None, _ => entity.effectiveRegistrationDate)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("effectiveregistration"))(seed))
        )

    val businessStartDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          businessStartDate = businessStartDateValidator(entity.businessStartDate)
            .fold(_ => None, _ => entity.businessStartDate)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("businessstart"))(seed))
        )

    val individualAndDateOfBirthCompoundSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual =
            entity.individual.orElse(Generator.get(IndividualName.gen)(seed)).map(IndividualName.sanitize(seed)),
          dateOfBirth = entity.dateOfBirth.orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("ofbirth"))(seed)),
          organisationName = None
        )

    val organisationNameCompoundSanitizer: Update = seed =>
      entity =>
        entity.copy(
          organisationName = entity.organisationName.orElse(
            Generator.get(Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 105))(seed)
          ),
          individual = None,
          dateOfBirth = None
        )

    val individualOrOrganisationNameAlternativeSanitizer: Update = seed =>
      entity =>
        if (entity.individual.isDefined) individualAndDateOfBirthCompoundSanitizer(seed)(entity)
        else if (entity.organisationName.isDefined) organisationNameCompoundSanitizer(seed)(entity)
        else
          Generator.get(Gen.chooseNum(0, 1))(seed) match {
            case Some(0) => individualAndDateOfBirthCompoundSanitizer(seed)(entity)
            case _       => organisationNameCompoundSanitizer(seed)(entity)
          }

    override val sanitizers: Seq[Update] = Seq(
      tradingNameSanitizer,
      registrationReasonSanitizer,
      effectiveRegistrationDateSanitizer,
      businessStartDateSanitizer,
      individualOrOrganisationNameAlternativeSanitizer
    )

    implicit val formats: Format[CustomerDetails] = Json.format[CustomerDetails]

  }

  case class Deregistration(
    deregistrationReason: Option[String] = None,
    effectDateOfCancellation: Option[String] = None,
    lastReturnDueDate: Option[String] = None
  ) {

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

    val deregistrationReasonValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.deregistrationReasonEnum), "Invalid deregistrationReason, does not match allowed values")
    val effectDateOfCancellationValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid effectDateOfCancellation, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val lastReturnDueDateValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid lastReturnDueDate, does not matches regex ${Common.dateOfBirthPattern}"""
    )

    override val validate: Validator[Deregistration] = Validator(
      checkProperty(_.deregistrationReason, deregistrationReasonValidator),
      checkProperty(_.effectDateOfCancellation, effectDateOfCancellationValidator),
      checkProperty(_.lastReturnDueDate, lastReturnDueDateValidator)
    )

    override val gen: Gen[Deregistration] = Gen const Deregistration(
    )

    val deregistrationReasonSanitizer: Update = seed =>
      entity =>
        entity.copy(
          deregistrationReason = deregistrationReasonValidator(entity.deregistrationReason)
            .fold(_ => None, _ => entity.deregistrationReason)
            .orElse(Generator.get(Gen.oneOf(Common.deregistrationReasonEnum))(seed))
        )

    val effectDateOfCancellationSanitizer: Update = seed =>
      entity =>
        entity.copy(
          effectDateOfCancellation = effectDateOfCancellationValidator(entity.effectDateOfCancellation)
            .fold(_ => None, _ => entity.effectDateOfCancellation)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("effect-ofcancellation"))(seed))
        )

    val lastReturnDueDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          lastReturnDueDate = lastReturnDueDateValidator(entity.lastReturnDueDate)
            .fold(_ => None, _ => entity.lastReturnDueDate)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("lastreturndue"))(seed))
        )

    override val sanitizers: Seq[Update] =
      Seq(deregistrationReasonSanitizer, effectDateOfCancellationSanitizer, lastReturnDueDateSanitizer)

    implicit val formats: Format[Deregistration] = Json.format[Deregistration]

  }

  case class FlatRateScheme(
    FRSCategory: Option[String] = None,
    FRSPercentage: Option[BigDecimal] = None,
    startDate: Option[String] = None,
    limitedCostTrader: Boolean = false
  ) {

    def withFRSCategory(FRSCategory: Option[String]): FlatRateScheme = copy(FRSCategory = FRSCategory)
    def modifyFRSCategory(pf: PartialFunction[Option[String], Option[String]]): FlatRateScheme =
      if (pf.isDefinedAt(FRSCategory)) copy(FRSCategory = pf(FRSCategory)) else this
    def withFRSPercentage(FRSPercentage: Option[BigDecimal]): FlatRateScheme = copy(FRSPercentage = FRSPercentage)
    def modifyFRSPercentage(pf: PartialFunction[Option[BigDecimal], Option[BigDecimal]]): FlatRateScheme =
      if (pf.isDefinedAt(FRSPercentage)) copy(FRSPercentage = pf(FRSPercentage)) else this
    def withStartDate(startDate: Option[String]): FlatRateScheme = copy(startDate = startDate)
    def modifyStartDate(pf: PartialFunction[Option[String], Option[String]]): FlatRateScheme =
      if (pf.isDefinedAt(startDate)) copy(startDate = pf(startDate)) else this
    def withLimitedCostTrader(limitedCostTrader: Boolean): FlatRateScheme = copy(limitedCostTrader = limitedCostTrader)
    def modifyLimitedCostTrader(pf: PartialFunction[Boolean, Boolean]): FlatRateScheme =
      if (pf.isDefinedAt(limitedCostTrader)) copy(limitedCostTrader = pf(limitedCostTrader)) else this
  }

  object FlatRateScheme extends RecordUtils[FlatRateScheme] {

    val FRSCategoryValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.FRSCategoryEnum), "Invalid FRSCategory, does not match allowed values")
    val FRSPercentageValidator: Validator[Option[BigDecimal]] = check(
      _.inRange(BigDecimal(0), BigDecimal(999.99), Some(BigDecimal(0.01))),
      "Invalid number FRSPercentage, must be in range <0,999.99>"
    )
    val startDateValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid startDate, does not matches regex ${Common.dateOfBirthPattern}"""
    )

    override val validate: Validator[FlatRateScheme] = Validator(
      checkProperty(_.FRSCategory, FRSCategoryValidator),
      checkProperty(_.FRSPercentage, FRSPercentageValidator),
      checkProperty(_.startDate, startDateValidator)
    )

    override val gen: Gen[FlatRateScheme] = for {
      limitedCostTrader <- Generator.booleanGen
    } yield FlatRateScheme(
      limitedCostTrader = limitedCostTrader
    )

    val FRSCategorySanitizer: Update = seed =>
      entity =>
        entity.copy(
          FRSCategory = FRSCategoryValidator(entity.FRSCategory)
            .fold(_ => None, _ => entity.FRSCategory)
            .orElse(Generator.get(Gen.oneOf(Common.FRSCategoryEnum))(seed))
        )

    val FRSPercentageSanitizer: Update = seed =>
      entity =>
        entity.copy(
          FRSPercentage = FRSPercentageValidator(entity.FRSPercentage)
            .fold(_ => None, _ => entity.FRSPercentage)
            .orElse(Generator.get(Generator.chooseBigDecimal(0, 999.99, Some(0.01)))(seed))
        )

    val startDateSanitizer: Update = seed =>
      entity =>
        entity.copy(
          startDate = startDateValidator(entity.startDate)
            .fold(_ => None, _ => entity.startDate)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("start"))(seed))
        )

    override val sanitizers: Seq[Update] = Seq(FRSCategorySanitizer, FRSPercentageSanitizer, startDateSanitizer)

    implicit val formats: Format[FlatRateScheme] = Json.format[FlatRateScheme]

  }

  case class ForeignAddress(
    override val line1: String,
    override val line2: String,
    override val line3: Option[String] = None,
    override val line4: Option[String] = None,
    postCode: Option[String] = None,
    override val countryCode: String
  ) extends Address {

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

    val line1Validator: Validator[String] =
      check(_.matches(Common.linePattern), s"""Invalid line1, does not matches regex ${Common.linePattern}""")
    val line2Validator: Validator[String] =
      check(_.matches(Common.linePattern), s"""Invalid line2, does not matches regex ${Common.linePattern}""")
    val line3Validator: Validator[Option[String]] =
      check(_.matches(Common.linePattern), s"""Invalid line3, does not matches regex ${Common.linePattern}""")
    val line4Validator: Validator[Option[String]] =
      check(_.matches(Common.linePattern), s"""Invalid line4, does not matches regex ${Common.linePattern}""")
    val postCodeValidator: Validator[Option[String]] = check(
      _.matches(Common.postCodePattern),
      s"""Invalid postCode, does not matches regex ${Common.postCodePattern}"""
    )
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")

    override val validate: Validator[ForeignAddress] = Validator(
      checkProperty(_.line1, line1Validator),
      checkProperty(_.line2, line2Validator),
      checkProperty(_.line3, line3Validator),
      checkProperty(_.line4, line4Validator),
      checkProperty(_.postCode, postCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
    )

    override val gen: Gen[ForeignAddress] = for {
      line1       <- Generator.address4Lines35Gen.map(_.line1)
      line2       <- Generator.address4Lines35Gen.map(_.line2)
      countryCode <- Gen.oneOf(Common.countryCodeEnum0)
    } yield ForeignAddress(
      line1 = line1,
      line2 = line2,
      countryCode = countryCode
    )

    val line3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          line3 = line3Validator(entity.line3)
            .fold(_ => None, _ => entity.line3)
            .orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed))
        )

    val line4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          line4 = line4Validator(entity.line4)
            .fold(_ => None, _ => entity.line4)
            .orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed))
        )

    val postCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(
          postCode = postCodeValidator(entity.postCode)
            .fold(_ => None, _ => entity.postCode)
            .orElse(Generator.get(Generator.postcode)(seed))
        )

    override val sanitizers: Seq[Update] = Seq(line3Sanitizer, line4Sanitizer, postCodeSanitizer)

    implicit val formats: Format[ForeignAddress] = Json.format[ForeignAddress]

  }

  case class GroupOrPartner(
    typeOfRelationship: String,
    organisationName: Option[String] = None,
    individual: Option[IndividualName] = None,
    SAP_Number: String
  ) {

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

    val typeOfRelationshipValidator: Validator[String] =
      check(_.isOneOf(Common.typeOfRelationshipEnum), "Invalid typeOfRelationship, does not match allowed values")
    val organisationNameValidator: Validator[Option[String]] = check(
      _.lengthMinMaxInclusive(1, 105),
      "Invalid length of organisationName, should be between 1 and 105 inclusive"
    )
    val individualValidator: Validator[Option[IndividualName]] = checkIfSome(identity, IndividualName.validate)
    val SAP_NumberValidator: Validator[String] = check(
      _.matches(Common.SAP_NumberPattern),
      s"""Invalid SAP_Number, does not matches regex ${Common.SAP_NumberPattern}"""
    )

    override val validate: Validator[GroupOrPartner] = Validator(
      checkProperty(_.typeOfRelationship, typeOfRelationshipValidator),
      checkProperty(_.organisationName, organisationNameValidator),
      checkProperty(_.individual, individualValidator),
      checkProperty(_.SAP_Number, SAP_NumberValidator)
    )

    override val gen: Gen[GroupOrPartner] = for {
      typeOfRelationship <- Gen.oneOf(Common.typeOfRelationshipEnum)
      sap_number         <- Generator.regex(Common.SAP_NumberPattern)
    } yield GroupOrPartner(
      typeOfRelationship = typeOfRelationship,
      SAP_Number = sap_number
    )

    val organisationNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          organisationName = organisationNameValidator(entity.organisationName)
            .fold(_ => None, _ => entity.organisationName)
            .orElse(Generator.get(Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 105))(seed))
        )

    val individualSanitizer: Update = seed =>
      entity =>
        entity.copy(
          individual = entity.individual
            .orElse(Generator.get(IndividualName.gen)(seed))
            .map(IndividualName.sanitize(seed))
        )

    override val sanitizers: Seq[Update] = Seq(organisationNameSanitizer, individualSanitizer)

    implicit val formats: Format[GroupOrPartner] = Json.format[GroupOrPartner]

  }

  case class IndividualName(
    title: Option[String] = None,
    firstName: Option[String] = None,
    middleName: Option[String] = None,
    lastName: Option[String] = None
  ) {

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

    val titleValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.titleEnum), "Invalid title, does not match allowed values")
    val firstNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of firstName, should be between 1 and 35 inclusive")
    val middleNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of middleName, should be between 1 and 35 inclusive")
    val lastNameValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of lastName, should be between 1 and 35 inclusive")

    override val validate: Validator[IndividualName] = Validator(
      checkProperty(_.title, titleValidator),
      checkProperty(_.firstName, firstNameValidator),
      checkProperty(_.middleName, middleNameValidator),
      checkProperty(_.lastName, lastNameValidator)
    )

    override val gen: Gen[IndividualName] = Gen const IndividualName(
    )

    val titleSanitizer: Update = seed =>
      entity =>
        entity.copy(
          title = titleValidator(entity.title)
            .fold(_ => None, _ => entity.title)
            .orElse(Generator.get(Gen.oneOf(Common.titleEnum))(seed))
        )

    val firstNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          firstName = firstNameValidator(entity.firstName)
            .fold(_ => None, _ => entity.firstName)
            .orElse(Generator.get(Generator.forename().suchThat(_.length >= 1).suchThat(_.length <= 35))(seed))
        )

    val middleNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          middleName = middleNameValidator(entity.middleName)
            .fold(_ => None, _ => entity.middleName)
            .orElse(
              Generator
                .get(Generator.forename().variant("middle").suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)
            )
        )

    val lastNameSanitizer: Update = seed =>
      entity =>
        entity.copy(
          lastName = lastNameValidator(entity.lastName)
            .fold(_ => None, _ => entity.lastName)
            .orElse(Generator.get(Generator.surname.suchThat(_.length >= 1).suchThat(_.length <= 35))(seed))
        )

    override val sanitizers: Seq[Update] =
      Seq(titleSanitizer, firstNameSanitizer, middleNameSanitizer, lastNameSanitizer)

    implicit val formats: Format[IndividualName] = Json.format[IndividualName]

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
    period22: Option[String] = None
  ) {

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

    val period01Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period01, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period02Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period02, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period03Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period03, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period04Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period04, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period05Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period05, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period06Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period06, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period07Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period07, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period08Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period08, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period09Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period09, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period10Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period10, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period11Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period11, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period12Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period12, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period13Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period13, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period14Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period14, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period15Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period15, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period16Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period16, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period17Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period17, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period18Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period18, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period19Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period19, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period20Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period20, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period21Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period21, does not matches regex ${Common.dateOfBirthPattern}"""
    )
    val period22Validator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid period22, does not matches regex ${Common.dateOfBirthPattern}"""
    )

    override val validate: Validator[NonStdTaxPeriods] = Validator(
      checkProperty(_.period01, period01Validator),
      checkProperty(_.period02, period02Validator),
      checkProperty(_.period03, period03Validator),
      checkProperty(_.period04, period04Validator),
      checkProperty(_.period05, period05Validator),
      checkProperty(_.period06, period06Validator),
      checkProperty(_.period07, period07Validator),
      checkProperty(_.period08, period08Validator),
      checkProperty(_.period09, period09Validator),
      checkProperty(_.period10, period10Validator),
      checkProperty(_.period11, period11Validator),
      checkProperty(_.period12, period12Validator),
      checkProperty(_.period13, period13Validator),
      checkProperty(_.period14, period14Validator),
      checkProperty(_.period15, period15Validator),
      checkProperty(_.period16, period16Validator),
      checkProperty(_.period17, period17Validator),
      checkProperty(_.period18, period18Validator),
      checkProperty(_.period19, period19Validator),
      checkProperty(_.period20, period20Validator),
      checkProperty(_.period21, period21Validator),
      checkProperty(_.period22, period22Validator)
    )

    override val gen: Gen[NonStdTaxPeriods] = Gen const NonStdTaxPeriods(
    )

    val period01Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period01 = period01Validator(entity.period01)
            .fold(_ => None, _ => entity.period01)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period02Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period02 = period02Validator(entity.period02)
            .fold(_ => None, _ => entity.period02)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period03Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period03 = period03Validator(entity.period03)
            .fold(_ => None, _ => entity.period03)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period04Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period04 = period04Validator(entity.period04)
            .fold(_ => None, _ => entity.period04)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period05Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period05 = period05Validator(entity.period05)
            .fold(_ => None, _ => entity.period05)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period06Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period06 = period06Validator(entity.period06)
            .fold(_ => None, _ => entity.period06)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period07Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period07 = period07Validator(entity.period07)
            .fold(_ => None, _ => entity.period07)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period08Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period08 = period08Validator(entity.period08)
            .fold(_ => None, _ => entity.period08)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period09Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period09 = period09Validator(entity.period09)
            .fold(_ => None, _ => entity.period09)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period10Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period10 = period10Validator(entity.period10)
            .fold(_ => None, _ => entity.period10)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period11Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period11 = period11Validator(entity.period11)
            .fold(_ => None, _ => entity.period11)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period12Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period12 = period12Validator(entity.period12)
            .fold(_ => None, _ => entity.period12)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period13Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period13 = period13Validator(entity.period13)
            .fold(_ => None, _ => entity.period13)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period14Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period14 = period14Validator(entity.period14)
            .fold(_ => None, _ => entity.period14)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period15Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period15 = period15Validator(entity.period15)
            .fold(_ => None, _ => entity.period15)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period16Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period16 = period16Validator(entity.period16)
            .fold(_ => None, _ => entity.period16)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period17Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period17 = period17Validator(entity.period17)
            .fold(_ => None, _ => entity.period17)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period18Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period18 = period18Validator(entity.period18)
            .fold(_ => None, _ => entity.period18)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period19Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period19 = period19Validator(entity.period19)
            .fold(_ => None, _ => entity.period19)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period20Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period20 = period20Validator(entity.period20)
            .fold(_ => None, _ => entity.period20)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period21Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period21 = period21Validator(entity.period21)
            .fold(_ => None, _ => entity.period21)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

    val period22Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          period22 = period22Validator(entity.period22)
            .fold(_ => None, _ => entity.period22)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen)(seed))
        )

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
    websiteAddress: Option[String] = None
  ) {

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

    val addressValidator: Validator[Address] = checkProperty(identity, Address.validate)
    val RLSValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.RLSEnum), "Invalid RLS, does not match allowed values")
    val contactDetailsValidator: Validator[Option[ContactDetails]] = checkIfSome(identity, ContactDetails.validate)
    val websiteAddressValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 132), "Invalid length of websiteAddress, should be between 1 and 132 inclusive")

    override val validate: Validator[PPOB] = Validator(
      checkProperty(_.address, addressValidator),
      checkProperty(_.RLS, RLSValidator),
      checkProperty(_.contactDetails, contactDetailsValidator),
      checkProperty(_.websiteAddress, websiteAddressValidator)
    )

    override val gen: Gen[PPOB] = for {
      address <- Address.gen
    } yield PPOB(
      address = address
    )

    val RLSSanitizer: Update = seed =>
      entity =>
        entity.copy(
          RLS = RLSValidator(entity.RLS)
            .fold(_ => None, _ => entity.RLS)
            .orElse(Generator.get(Gen.oneOf(Common.RLSEnum))(seed))
        )

    val contactDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          contactDetails = entity.contactDetails
            .orElse(Generator.get(ContactDetails.gen)(seed))
            .map(ContactDetails.sanitize(seed))
        )

    val websiteAddressSanitizer: Update = seed =>
      entity =>
        entity.copy(
          websiteAddress = websiteAddressValidator(entity.websiteAddress)
            .fold(_ => None, _ => entity.websiteAddress)
            .orElse(
              Generator.get(Generator.stringMinMaxN(1, 132).suchThat(_.length >= 1).suchThat(_.length <= 132))(seed)
            )
        )

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

    val stdReturnPeriodValidator: Validator[Option[String]] =
      check(_.isOneOf(Common.stdReturnPeriodEnum), "Invalid stdReturnPeriod, does not match allowed values")
    val nonStdTaxPeriodsValidator: Validator[Option[NonStdTaxPeriods]] =
      checkIfSome(identity, NonStdTaxPeriods.validate)

    override val validate: Validator[Period] = Validator(
      checkProperty(_.stdReturnPeriod, stdReturnPeriodValidator),
      checkProperty(_.nonStdTaxPeriods, nonStdTaxPeriodsValidator)
    )

    override val gen: Gen[Period] = Gen const Period(
    )

    val stdReturnPeriodSanitizer: Update = seed =>
      entity =>
        entity.copy(
          stdReturnPeriod = stdReturnPeriodValidator(entity.stdReturnPeriod)
            .fold(_ => None, _ => entity.stdReturnPeriod)
            .orElse(Generator.get(Gen.oneOf(Common.stdReturnPeriodEnum))(seed))
        )

    val nonStdTaxPeriodsSanitizer: Update = seed =>
      entity =>
        entity.copy(
          nonStdTaxPeriods = entity.nonStdTaxPeriods
            .orElse(Generator.get(NonStdTaxPeriods.gen)(seed))
            .map(NonStdTaxPeriods.sanitize(seed))
        )

    override val sanitizers: Seq[Update] = Seq(stdReturnPeriodSanitizer, nonStdTaxPeriodsSanitizer)

    implicit val formats: Format[Period] = Json.format[Period]

  }

  case class UkAddress(
    override val line1: String,
    override val line2: String,
    override val line3: Option[String] = None,
    override val line4: Option[String] = None,
    postCode: String,
    override val countryCode: String
  ) extends Address {

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

    val line1Validator: Validator[String] =
      check(_.matches(Common.linePattern), s"""Invalid line1, does not matches regex ${Common.linePattern}""")
    val line2Validator: Validator[String] =
      check(_.matches(Common.linePattern), s"""Invalid line2, does not matches regex ${Common.linePattern}""")
    val line3Validator: Validator[Option[String]] =
      check(_.matches(Common.linePattern), s"""Invalid line3, does not matches regex ${Common.linePattern}""")
    val line4Validator: Validator[Option[String]] =
      check(_.matches(Common.linePattern), s"""Invalid line4, does not matches regex ${Common.linePattern}""")
    val postCodeValidator: Validator[String] = check(
      _.matches(Common.postCodePattern),
      s"""Invalid postCode, does not matches regex ${Common.postCodePattern}"""
    )
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")

    override val validate: Validator[UkAddress] = Validator(
      checkProperty(_.line1, line1Validator),
      checkProperty(_.line2, line2Validator),
      checkProperty(_.line3, line3Validator),
      checkProperty(_.line4, line4Validator),
      checkProperty(_.postCode, postCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
    )

    override val gen: Gen[UkAddress] = for {
      line1       <- Generator.address4Lines35Gen.map(_.line1)
      line2       <- Generator.address4Lines35Gen.map(_.line2)
      postCode    <- Generator.postcode
      countryCode <- Gen.const("GB")
    } yield UkAddress(
      line1 = line1,
      line2 = line2,
      postCode = postCode,
      countryCode = countryCode
    )

    val line3Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          line3 = line3Validator(entity.line3)
            .fold(_ => None, _ => entity.line3)
            .orElse(Generator.get(Generator.address4Lines35Gen.map(_.line3))(seed))
        )

    val line4Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          line4 = line4Validator(entity.line4)
            .fold(_ => None, _ => entity.line4)
            .orElse(Generator.get(Generator.address4Lines35Gen.map(_.line4))(seed))
        )

    override val sanitizers: Seq[Update] = Seq(line3Sanitizer, line4Sanitizer)

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  object Common {
    val RLSEnum = Seq("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")
    val mandationStatusEnum = Seq("1", "2", "3", "4")
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
      "0014"
    )
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
    val sortCodePattern = """^[0-9]{6}$"""
    val SAP_NumberPattern = """^[0-9]{42}$"""
  }
}
