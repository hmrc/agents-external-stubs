/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentmtdidentifiers.model.PlrId
import uk.gov.hmrc.agentsexternalstubs.models.Pillar2Record.Common
import uk.gov.hmrc.agentsexternalstubs.models.Pillar2Record.{AccountStatus, AccountingPeriod, ContactDetails, FilingMemberDetails, UpeCorrespAddressDetails, UpeDetails}

/** ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema schema.json app/uk/gov/hmrc/agentsexternalstubs/models/Pillar2Record.scala Pillar2Record "
  * ----------------------------------------------------------------------------
  *
  * A number of things were adjusted by hand:
  *  - manually added plrReference for indexing and easy identification
  *  - added 'unique key' and 'lookup keys'
  *  - replaced the phone number generator to a standard UK phone number to avoid huge strings being generated
  *
  *  Pillar2Record
  *  -  FormBundleNumber
  *  -  UpeDetails
  *  -  AccountingPeriod
  *  -  UpeCorrespAddressDetails
  *  -  PrimaryContactDetails
  */

case class Pillar2Record(
  plrReference: String,
  formBundleNumber: String,
  upeDetails: UpeDetails,
  accountingPeriod: AccountingPeriod,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails: ContactDetails,
  secondaryContactDetails: Option[ContactDetails] = None,
  filingMemberDetails: Option[FilingMemberDetails] = None,
  accountStatus: Option[AccountStatus] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Some(Pillar2Record.plrReferenceKey(plrReference))
  override def lookupKeys: Seq[String] = Seq(Pillar2Record.plrReferenceKey(plrReference))
  override def withId(id: Option[String]): Pillar2Record = copy(id = id)
  def withPlrReference(plrReference: String): Pillar2Record = copy(plrReference = plrReference)
  def modifyPlrReference(pf: PartialFunction[String, String]): Pillar2Record =
    if (pf.isDefinedAt(plrReference)) copy(plrReference = pf(plrReference)) else this
  def withUpeDetails(upeDetails: UpeDetails): Pillar2Record = copy(upeDetails = upeDetails)
  def modifyUpeDetails(pf: PartialFunction[UpeDetails, UpeDetails]): Pillar2Record =
    if (pf.isDefinedAt(upeDetails)) copy(upeDetails = pf(upeDetails)) else this
  def withAccountingPeriod(accountingPeriod: AccountingPeriod): Pillar2Record =
    copy(accountingPeriod = accountingPeriod)
  def modifyAccountingPeriod(pf: PartialFunction[AccountingPeriod, AccountingPeriod]): Pillar2Record =
    if (pf.isDefinedAt(accountingPeriod)) copy(accountingPeriod = pf(accountingPeriod)) else this
  def withUpeCorrespAddressDetails(upeCorrespAddressDetails: UpeCorrespAddressDetails): Pillar2Record =
    copy(upeCorrespAddressDetails = upeCorrespAddressDetails)
  def modifyUpeCorrespAddressDetails(
    pf: PartialFunction[UpeCorrespAddressDetails, UpeCorrespAddressDetails]
  ): Pillar2Record =
    if (pf.isDefinedAt(upeCorrespAddressDetails)) copy(upeCorrespAddressDetails = pf(upeCorrespAddressDetails))
    else this
  def withPrimaryContactDetails(primaryContactDetails: ContactDetails): Pillar2Record =
    copy(primaryContactDetails = primaryContactDetails)
  def modifyPrimaryContactDetails(pf: PartialFunction[ContactDetails, ContactDetails]): Pillar2Record =
    if (pf.isDefinedAt(primaryContactDetails)) copy(primaryContactDetails = pf(primaryContactDetails)) else this
  def withSecondaryContactDetails(secondaryContactDetails: Option[ContactDetails]): Pillar2Record =
    copy(secondaryContactDetails = secondaryContactDetails)
  def modifySecondaryContactDetails(
    pf: PartialFunction[Option[ContactDetails], Option[ContactDetails]]
  ): Pillar2Record =
    if (pf.isDefinedAt(secondaryContactDetails)) copy(secondaryContactDetails = pf(secondaryContactDetails)) else this
  def withFilingMemberDetails(filingMemberDetails: Option[FilingMemberDetails]): Pillar2Record =
    copy(filingMemberDetails = filingMemberDetails)
  def modifyFilingMemberDetails(
    pf: PartialFunction[Option[FilingMemberDetails], Option[FilingMemberDetails]]
  ): Pillar2Record =
    if (pf.isDefinedAt(filingMemberDetails)) copy(filingMemberDetails = pf(filingMemberDetails)) else this
  def withAccountStatus(accountStatus: Option[AccountStatus]): Pillar2Record = copy(accountStatus = accountStatus)
  def modifyAccountStatus(pf: PartialFunction[Option[AccountStatus], Option[AccountStatus]]): Pillar2Record =
    if (pf.isDefinedAt(accountStatus)) copy(accountStatus = pf(accountStatus)) else this
}

object Pillar2Record extends RecordUtils[Pillar2Record] {

  implicit val recordUtils: RecordUtils[Pillar2Record] = this
  implicit val takesPlrIdKey: TakesKey[Pillar2Record, PlrId] = TakesKey(plrId => plrReferenceKey(plrId.value))

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[Pillar2Record] = RecordMetaData[Pillar2Record]

  import Generator.GenOps._
  import Validator._

  def plrReferenceKey(key: String): String = s"""plrReference:${key.toUpperCase}"""

  val plrReferenceValidator: Validator[String] = check(
    _.matches(Common.plrReferencePattern),
    s"""Invalid plrReference, does not matches regex ${Common.plrReferencePattern}"""
  )
  val formBundleNumberValidator: Validator[String] = check(
    _.matches(Common.formBundleNumberPattern),
    s"""Invalid form bundle number, does not matches regex ${Common.formBundleNumberPattern}"""
  )
  val upeDetailsValidator: Validator[UpeDetails] = checkProperty(identity, UpeDetails.validate)
  val accountingPeriodValidator: Validator[AccountingPeriod] = checkProperty(identity, AccountingPeriod.validate)
  val upeCorrespAddressDetailsValidator: Validator[UpeCorrespAddressDetails] =
    checkProperty(identity, UpeCorrespAddressDetails.validate)
  val primaryContactDetailsValidator: Validator[ContactDetails] = checkProperty(identity, ContactDetails.validate)
  val secondaryContactDetailsValidator: Validator[Option[ContactDetails]] =
    checkIfSome(identity, ContactDetails.validate)
  val filingMemberDetailsValidator: Validator[Option[FilingMemberDetails]] =
    checkIfSome(identity, FilingMemberDetails.validate)
  val accountStatusValidator: Validator[Option[AccountStatus]] = checkIfSome(identity, AccountStatus.validate)

  override val validate: Validator[Pillar2Record] = Validator(
    checkProperty(_.plrReference, plrReferenceValidator),
    checkProperty(_.formBundleNumber, formBundleNumberValidator),
    checkProperty(_.upeDetails, upeDetailsValidator),
    checkProperty(_.accountingPeriod, accountingPeriodValidator),
    checkProperty(_.upeCorrespAddressDetails, upeCorrespAddressDetailsValidator),
    checkProperty(_.primaryContactDetails, primaryContactDetailsValidator),
    checkProperty(_.secondaryContactDetails, secondaryContactDetailsValidator),
    checkProperty(_.filingMemberDetails, filingMemberDetailsValidator),
    checkProperty(_.accountStatus, accountStatusValidator)
  )

  override val gen: Gen[Pillar2Record] = for {
    plrReference             <- Generator.regex(Common.plrReferencePattern)
    formBundleNumber         <- Generator.regex(Common.formBundleNumberPattern)
    upeDetails               <- UpeDetails.gen
    accountingPeriod         <- AccountingPeriod.gen
    upeCorrespAddressDetails <- UpeCorrespAddressDetails.gen
    primaryContactDetails    <- ContactDetails.gen
  } yield Pillar2Record(
    plrReference = plrReference,
    formBundleNumber = formBundleNumber,
    upeDetails = upeDetails,
    accountingPeriod = accountingPeriod,
    upeCorrespAddressDetails = upeCorrespAddressDetails,
    primaryContactDetails = primaryContactDetails
  )

  val upeDetailsSanitizer: Update = seed =>
    entity => entity.copy(upeDetails = UpeDetails.sanitize(seed)(entity.upeDetails))

  val accountingPeriodSanitizer: Update = seed =>
    entity => entity.copy(accountingPeriod = AccountingPeriod.sanitize(seed)(entity.accountingPeriod))

  val upeCorrespAddressDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(upeCorrespAddressDetails = UpeCorrespAddressDetails.sanitize(seed)(entity.upeCorrespAddressDetails))

  val primaryContactDetailsSanitizer: Update = seed =>
    entity => entity.copy(primaryContactDetails = ContactDetails.sanitize(seed)(entity.primaryContactDetails))

  val secondaryContactDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(secondaryContactDetails =
        entity.secondaryContactDetails
          .orElse(Generator.get(ContactDetails.gen)(seed))
          .map(ContactDetails.sanitize(seed))
      )

  val filingMemberDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(filingMemberDetails =
        entity.filingMemberDetails
          .orElse(Generator.get(FilingMemberDetails.gen)(seed))
          .map(FilingMemberDetails.sanitize(seed))
      )

  val accountStatusSanitizer: Update = seed =>
    entity =>
      entity.copy(accountStatus =
        entity.accountStatus
          .orElse(Generator.get(AccountStatus.gen)(seed))
          .map(AccountStatus.sanitize(seed))
      )

  override val sanitizers: Seq[Update] = Seq(
    upeDetailsSanitizer,
    accountingPeriodSanitizer,
    upeCorrespAddressDetailsSanitizer,
    primaryContactDetailsSanitizer,
    secondaryContactDetailsSanitizer,
    filingMemberDetailsSanitizer,
    accountStatusSanitizer
  )

  implicit val formats: Format[Pillar2Record] = Json.format[Pillar2Record]

  case class AccountStatus(inactive: Boolean = false) {

    def withInactive(inactive: Boolean): AccountStatus = copy(inactive = inactive)
    def modifyInactive(pf: PartialFunction[Boolean, Boolean]): AccountStatus =
      if (pf.isDefinedAt(inactive)) copy(inactive = pf(inactive)) else this
  }

  object AccountStatus extends RecordUtils[AccountStatus] {

    override val validate: Validator[AccountStatus] = Validator()

    override val gen: Gen[AccountStatus] = for {
      inactive <- Gen.const(false)
    } yield AccountStatus(
      inactive = inactive
    )

    override val sanitizers: Seq[Update] = Seq()

    implicit val formats: Format[AccountStatus] = Json.format[AccountStatus]

  }

  case class AccountingPeriod(startDate: String, endDate: String, dueDate: Option[String] = None) {

    def withStartDate(startDate: String): AccountingPeriod = copy(startDate = startDate)
    def modifyStartDate(pf: PartialFunction[String, String]): AccountingPeriod =
      if (pf.isDefinedAt(startDate)) copy(startDate = pf(startDate)) else this
    def withEndDate(endDate: String): AccountingPeriod = copy(endDate = endDate)
    def modifyEndDate(pf: PartialFunction[String, String]): AccountingPeriod =
      if (pf.isDefinedAt(endDate)) copy(endDate = pf(endDate)) else this
    def withDueDate(dueDate: Option[String]): AccountingPeriod = copy(dueDate = dueDate)
    def modifyDueDate(pf: PartialFunction[Option[String], Option[String]]): AccountingPeriod =
      if (pf.isDefinedAt(dueDate)) copy(dueDate = pf(dueDate)) else this
  }

  object AccountingPeriod extends RecordUtils[AccountingPeriod] {

    override val validate: Validator[AccountingPeriod] = Validator()

    override val gen: Gen[AccountingPeriod] = for {
      startDate <- Generator.dateYYYYMMDDGen.variant("start")
      endDate   <- Generator.dateYYYYMMDDGen.variant("end")
    } yield AccountingPeriod(
      startDate = startDate,
      endDate = endDate
    )

    val dueDateSanitizer: Update = seed =>
      entity => entity.copy(dueDate = Generator.get(Generator.dateYYYYMMDDGen.variant("due"))(seed))

    override val sanitizers: Seq[Update] = Seq(dueDateSanitizer)

    implicit val formats: Format[AccountingPeriod] = Json.format[AccountingPeriod]

  }

  case class ContactDetails(name: String, telephone: Option[String] = None, emailAddress: String) {

    def withName(name: String): ContactDetails = copy(name = name)
    def modifyName(pf: PartialFunction[String, String]): ContactDetails =
      if (pf.isDefinedAt(name)) copy(name = pf(name)) else this
    def withTelephone(telephone: Option[String]): ContactDetails = copy(telephone = telephone)
    def modifyTelephone(pf: PartialFunction[Option[String], Option[String]]): ContactDetails =
      if (pf.isDefinedAt(telephone)) copy(telephone = pf(telephone)) else this
    def withEmailAddress(emailAddress: String): ContactDetails = copy(emailAddress = emailAddress)
    def modifyEmailAddress(pf: PartialFunction[String, String]): ContactDetails =
      if (pf.isDefinedAt(emailAddress)) copy(emailAddress = pf(emailAddress)) else this
  }

  object ContactDetails extends RecordUtils[ContactDetails] {

    val nameValidator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 160), "Invalid length of name, should be between 1 and 160 inclusive")
    val telephoneValidator: Validator[Option[String]] = check(
      _.matches(Common.telephonePattern),
      s"""Invalid telephone, does not matches regex ${Common.telephonePattern}"""
    )

    override val validate: Validator[ContactDetails] =
      Validator(checkProperty(_.name, nameValidator), checkProperty(_.telephone, telephoneValidator))

    override val gen: Gen[ContactDetails] = for {
      name         <- Generator.stringMinMaxN(1, 160).suchThat(_.length >= 1).suchThat(_.length <= 160)
      emailAddress <- Generator.emailGen
    } yield ContactDetails(
      name = name,
      emailAddress = emailAddress
    )

    val telephoneSanitizer: Update = seed =>
      entity =>
        entity.copy(telephone =
          telephoneValidator(entity.telephone)
            .fold(_ => None, _ => entity.telephone)
            .orElse(Generator.get(Generator.ukPhoneNumber)(seed))
        )

    override val sanitizers: Seq[Update] = Seq(telephoneSanitizer)

    implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]

  }

  case class FilingMemberDetails(
    safeId: String,
    customerIdentification1: Option[String] = None,
    customerIdentification2: Option[String] = None,
    organisationName: String
  ) {

    def withSafeId(safeId: String): FilingMemberDetails = copy(safeId = safeId)
    def modifySafeId(pf: PartialFunction[String, String]): FilingMemberDetails =
      if (pf.isDefinedAt(safeId)) copy(safeId = pf(safeId)) else this
    def withCustomerIdentification1(customerIdentification1: Option[String]): FilingMemberDetails =
      copy(customerIdentification1 = customerIdentification1)
    def modifyCustomerIdentification1(pf: PartialFunction[Option[String], Option[String]]): FilingMemberDetails =
      if (pf.isDefinedAt(customerIdentification1)) copy(customerIdentification1 = pf(customerIdentification1)) else this
    def withCustomerIdentification2(customerIdentification2: Option[String]): FilingMemberDetails =
      copy(customerIdentification2 = customerIdentification2)
    def modifyCustomerIdentification2(pf: PartialFunction[Option[String], Option[String]]): FilingMemberDetails =
      if (pf.isDefinedAt(customerIdentification2)) copy(customerIdentification2 = pf(customerIdentification2)) else this
    def withOrganisationName(organisationName: String): FilingMemberDetails = copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[String, String]): FilingMemberDetails =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
  }

  object FilingMemberDetails extends RecordUtils[FilingMemberDetails] {

    val safeIdValidator: Validator[String] =
      check(_.matches(Common.safeIdPattern), s"""Invalid safeId, does not matches regex ${Common.safeIdPattern}""")
    val customerIdentification1Validator: Validator[Option[String]] = check(
      _.matches(Common.customerIdentificationPattern),
      s"""Invalid customerIdentification1, does not matches regex ${Common.customerIdentificationPattern}"""
    )
    val customerIdentification2Validator: Validator[Option[String]] = check(
      _.matches(Common.customerIdentificationPattern),
      s"""Invalid customerIdentification2, does not matches regex ${Common.customerIdentificationPattern}"""
    )
    val organisationNameValidator: Validator[String] = check(
      _.lengthMinMaxInclusive(1, 160),
      "Invalid length of organisationName, should be between 1 and 160 inclusive"
    )

    override val validate: Validator[FilingMemberDetails] = Validator(
      checkProperty(_.safeId, safeIdValidator),
      checkProperty(_.customerIdentification1, customerIdentification1Validator),
      checkProperty(_.customerIdentification2, customerIdentification2Validator),
      checkProperty(_.organisationName, organisationNameValidator)
    )

    override val gen: Gen[FilingMemberDetails] = for {
      safeId           <- Generator.safeIdGen
      organisationName <- Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 160)
    } yield FilingMemberDetails(
      safeId = safeId,
      organisationName = organisationName
    )

    val customerIdentification1Sanitizer: Update = seed =>
      entity =>
        entity.copy(customerIdentification1 =
          customerIdentification1Validator(entity.customerIdentification1)
            .fold(_ => None, _ => entity.customerIdentification1)
            .orElse(Generator.get(Generator.regex(Common.customerIdentificationPattern))(seed))
        )

    val customerIdentification2Sanitizer: Update = seed =>
      entity =>
        entity.copy(customerIdentification2 =
          customerIdentification2Validator(entity.customerIdentification2)
            .fold(_ => None, _ => entity.customerIdentification2)
            .orElse(Generator.get(Generator.regex(Common.customerIdentificationPattern))(seed))
        )

    override val sanitizers: Seq[Update] = Seq(customerIdentification1Sanitizer, customerIdentification2Sanitizer)

    implicit val formats: Format[FilingMemberDetails] = Json.format[FilingMemberDetails]

  }

  case class UpeCorrespAddressDetails(
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postCode: Option[String] = None,
    countryCode: String
  ) {

    def withAddressLine1(addressLine1: String): UpeCorrespAddressDetails = copy(addressLine1 = addressLine1)
    def modifyAddressLine1(pf: PartialFunction[String, String]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(addressLine1)) copy(addressLine1 = pf(addressLine1)) else this
    def withAddressLine2(addressLine2: Option[String]): UpeCorrespAddressDetails = copy(addressLine2 = addressLine2)
    def modifyAddressLine2(pf: PartialFunction[Option[String], Option[String]]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(addressLine2)) copy(addressLine2 = pf(addressLine2)) else this
    def withAddressLine3(addressLine3: Option[String]): UpeCorrespAddressDetails = copy(addressLine3 = addressLine3)
    def modifyAddressLine3(pf: PartialFunction[Option[String], Option[String]]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(addressLine3)) copy(addressLine3 = pf(addressLine3)) else this
    def withAddressLine4(addressLine4: Option[String]): UpeCorrespAddressDetails = copy(addressLine4 = addressLine4)
    def modifyAddressLine4(pf: PartialFunction[Option[String], Option[String]]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(addressLine4)) copy(addressLine4 = pf(addressLine4)) else this
    def withPostCode(postCode: Option[String]): UpeCorrespAddressDetails = copy(postCode = postCode)
    def modifyPostCode(pf: PartialFunction[Option[String], Option[String]]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(postCode)) copy(postCode = pf(postCode)) else this
    def withCountryCode(countryCode: String): UpeCorrespAddressDetails = copy(countryCode = countryCode)
    def modifyCountryCode(pf: PartialFunction[String, String]): UpeCorrespAddressDetails =
      if (pf.isDefinedAt(countryCode)) copy(countryCode = pf(countryCode)) else this
  }

  object UpeCorrespAddressDetails extends RecordUtils[UpeCorrespAddressDetails] {

    val addressLine1Validator: Validator[String] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine1, should be between 1 and 35 inclusive")
    val addressLine2Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine2, should be between 1 and 35 inclusive")
    val addressLine3Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine3, should be between 1 and 35 inclusive")
    val addressLine4Validator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 35), "Invalid length of addressLine4, should be between 1 and 35 inclusive")
    val postCodeValidator: Validator[Option[String]] =
      check(_.lengthMinMaxInclusive(1, 10), "Invalid length of postCode, should be between 1 and 10 inclusive")
    val countryCodeValidator: Validator[String] =
      check(_.isOneOf(Common.countryCodeEnum), "Invalid countryCode, does not match allowed values")

    override val validate: Validator[UpeCorrespAddressDetails] = Validator(
      checkProperty(_.addressLine1, addressLine1Validator),
      checkProperty(_.addressLine2, addressLine2Validator),
      checkProperty(_.addressLine3, addressLine3Validator),
      checkProperty(_.addressLine4, addressLine4Validator),
      checkProperty(_.postCode, postCodeValidator),
      checkProperty(_.countryCode, countryCodeValidator)
    )

    override val gen: Gen[UpeCorrespAddressDetails] = for {
      addressLine1 <- Generator.address4Lines35Gen.map(_.line1).suchThat(_.length >= 1).suchThat(_.length <= 35)
      countryCode  <- Gen.oneOf(Common.countryCodeEnum)
    } yield UpeCorrespAddressDetails(
      addressLine1 = addressLine1,
      countryCode = countryCode
    )

    val addressLine2Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine2 =
          addressLine2Validator(entity.addressLine2)
            .fold(_ => None, _ => entity.addressLine2)
            .orElse(
              Generator
                .get(Generator.address4Lines35Gen.map(_.line2).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)
            )
        )

    val addressLine3Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine3 =
          addressLine3Validator(entity.addressLine3)
            .fold(_ => None, _ => entity.addressLine3)
            .orElse(
              Generator
                .get(Generator.address4Lines35Gen.map(_.line3).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)
            )
        )

    val addressLine4Sanitizer: Update = seed =>
      entity =>
        entity.copy(addressLine4 =
          addressLine4Validator(entity.addressLine4)
            .fold(_ => None, _ => entity.addressLine4)
            .orElse(
              Generator
                .get(Generator.address4Lines35Gen.map(_.line4).suchThat(_.length >= 1).suchThat(_.length <= 35))(seed)
            )
        )

    val postCodeSanitizer: Update = seed =>
      entity =>
        entity.copy(postCode =
          postCodeValidator(entity.postCode)
            .fold(_ => None, _ => entity.postCode)
            .orElse(Generator.get(Generator.postcode.suchThat(_.length >= 1).suchThat(_.length <= 10))(seed))
        )

    override val sanitizers: Seq[Update] =
      Seq(addressLine2Sanitizer, addressLine3Sanitizer, addressLine4Sanitizer, postCodeSanitizer)

    implicit val formats: Format[UpeCorrespAddressDetails] = Json.format[UpeCorrespAddressDetails]

  }

  case class UpeDetails(
    customerIdentification1: Option[String] = None,
    customerIdentification2: Option[String] = None,
    organisationName: String,
    registrationDate: String,
    domesticOnly: Boolean = false,
    filingMember: Boolean = false
  ) {

    def withCustomerIdentification1(customerIdentification1: Option[String]): UpeDetails =
      copy(customerIdentification1 = customerIdentification1)
    def modifyCustomerIdentification1(pf: PartialFunction[Option[String], Option[String]]): UpeDetails =
      if (pf.isDefinedAt(customerIdentification1)) copy(customerIdentification1 = pf(customerIdentification1)) else this
    def withCustomerIdentification2(customerIdentification2: Option[String]): UpeDetails =
      copy(customerIdentification2 = customerIdentification2)
    def modifyCustomerIdentification2(pf: PartialFunction[Option[String], Option[String]]): UpeDetails =
      if (pf.isDefinedAt(customerIdentification2)) copy(customerIdentification2 = pf(customerIdentification2)) else this
    def withOrganisationName(organisationName: String): UpeDetails = copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[String, String]): UpeDetails =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withRegistrationDate(registrationDate: String): UpeDetails = copy(registrationDate = registrationDate)
    def modifyRegistrationDate(pf: PartialFunction[String, String]): UpeDetails =
      if (pf.isDefinedAt(registrationDate)) copy(registrationDate = pf(registrationDate)) else this
    def withDomesticOnly(domesticOnly: Boolean): UpeDetails = copy(domesticOnly = domesticOnly)
    def modifyDomesticOnly(pf: PartialFunction[Boolean, Boolean]): UpeDetails =
      if (pf.isDefinedAt(domesticOnly)) copy(domesticOnly = pf(domesticOnly)) else this
    def withFilingMember(filingMember: Boolean): UpeDetails = copy(filingMember = filingMember)
    def modifyFilingMember(pf: PartialFunction[Boolean, Boolean]): UpeDetails =
      if (pf.isDefinedAt(filingMember)) copy(filingMember = pf(filingMember)) else this
  }

  object UpeDetails extends RecordUtils[UpeDetails] {

    val customerIdentification1Validator: Validator[Option[String]] = check(
      _.matches(Common.customerIdentificationPattern),
      s"""Invalid customerIdentification1, does not matches regex ${Common.customerIdentificationPattern}"""
    )
    val customerIdentification2Validator: Validator[Option[String]] = check(
      _.matches(Common.customerIdentificationPattern),
      s"""Invalid customerIdentification2, does not matches regex ${Common.customerIdentificationPattern}"""
    )
    val organisationNameValidator: Validator[String] = check(
      _.lengthMinMaxInclusive(1, 160),
      "Invalid length of organisationName, should be between 1 and 160 inclusive"
    )

    override val validate: Validator[UpeDetails] = Validator(
      checkProperty(_.customerIdentification1, customerIdentification1Validator),
      checkProperty(_.customerIdentification2, customerIdentification2Validator),
      checkProperty(_.organisationName, organisationNameValidator)
    )

    override val gen: Gen[UpeDetails] = for {
      organisationName <- Generator.company.suchThat(_.length >= 1).suchThat(_.length <= 160)
      registrationDate <- Generator.dateYYYYMMDDGen.variant("registration")
      domesticOnly     <- Generator.booleanGen
      filingMember     <- Generator.booleanGen
    } yield UpeDetails(
      organisationName = organisationName,
      registrationDate = registrationDate,
      domesticOnly = domesticOnly,
      filingMember = filingMember
    )

    val customerIdentification1Sanitizer: Update = seed =>
      entity =>
        entity.copy(customerIdentification1 =
          customerIdentification1Validator(entity.customerIdentification1)
            .fold(_ => None, _ => entity.customerIdentification1)
            .orElse(Generator.get(Generator.regex(Common.customerIdentificationPattern))(seed))
        )

    val customerIdentification2Sanitizer: Update = seed =>
      entity =>
        entity.copy(customerIdentification2 =
          customerIdentification2Validator(entity.customerIdentification2)
            .fold(_ => None, _ => entity.customerIdentification2)
            .orElse(Generator.get(Generator.regex(Common.customerIdentificationPattern))(seed))
        )

    override val sanitizers: Seq[Update] = Seq(customerIdentification1Sanitizer, customerIdentification2Sanitizer)

    implicit val formats: Format[UpeDetails] = Json.format[UpeDetails]

  }

  object Common {
    val formBundleNumberPattern = """^[0-9]{12}$"""
    val plrReferencePattern = """^X[A-Z]{1}PLR[0-9]{10}$"""
    val safeIdPattern = """^X[A-Z]{1}[0-9]{13}$"""
    val telephonePattern =
      """^(?:(?:\(?(?:00|\+)([1-4]\d\d|[1-9]\d?)\)?)?[\-\.\ \/]?)?((?:\(?\d{1,}\)?[\-\.\ \/]?){0,})(?:[\-\.\ \/]?(?:#|ext\.?|extension|x)[\-\.\ \/]?(\d+))?$"""
    val countryCodeEnum = Seq(
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
      "FC",
      "FI",
      "FJ",
      "FK",
      "FM",
      "FO",
      "FR",
      "GA",
      "GB",
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
    val customerIdentificationPattern = """^[A-Za-z0-9]{1,15}$"""
  }
}
