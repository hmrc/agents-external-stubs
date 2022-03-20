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
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.ChangeOfCircumstanceDetails.DeregistrationDetails
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.LegalEntityDetails.CustomerDetails
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord._
import uk.gov.hmrc.agentsexternalstubs.models.User.AG
import uk.gov.hmrc.smartstub.{Female, Male, Names}

import java.text.SimpleDateFormat
import java.time.{LocalDate, ZoneId}
import java.util.Date

/** ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/IF1712.json app/uk/gov/hmrc/agentsexternalstubs/models/PPTSubscriptionDisplayRecord.scala PPTSubscriptionDisplayRecord "
  * ----------------------------------------------------------------------------
  *
  *  PPTSubscriptionDisplayRecord
  *  -  ChangeOfCircumstanceDetails
  *  -  -  DeregistrationDetails
  *  -  IndividualDetails
  *  -  LegalEntityDetails
  *  -  -  CustomerDetails
  *  -  OrganisationDetails
  */

case class PPTSubscriptionDisplayRecord(
  pptReference: String,
  legalEntityDetails: LegalEntityDetails,
  changeOfCircumstanceDetails: ChangeOfCircumstanceDetails,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(pptReference).map(PPTSubscriptionDisplayRecord.uniqueKey)
  override def lookupKeys: Seq[String] =
    Seq(Option(pptReference).map(PPTSubscriptionDisplayRecord.pptReferenceKey)).collect { case Some(x) => x }
  override def withId(id: Option[String]): PPTSubscriptionDisplayRecord = copy(id = id)

  def withPptReference(pptReference: String): PPTSubscriptionDisplayRecord = copy(pptReference = pptReference)
  def modifyPptReference(pf: PartialFunction[String, String]): PPTSubscriptionDisplayRecord =
    if (pf.isDefinedAt(pptReference)) copy(pptReference = pf(pptReference)) else this
  def withLegalEntityDetails(legalEntityDetails: LegalEntityDetails): PPTSubscriptionDisplayRecord =
    copy(legalEntityDetails = legalEntityDetails)
  def modifyLegalEntityDetails(
    pf: PartialFunction[LegalEntityDetails, LegalEntityDetails]
  ): PPTSubscriptionDisplayRecord =
    if (pf.isDefinedAt(legalEntityDetails)) copy(legalEntityDetails = pf(legalEntityDetails)) else this
  def withChangeOfCircumstanceDetails(
    changeOfCircumstanceDetails: ChangeOfCircumstanceDetails
  ): PPTSubscriptionDisplayRecord = copy(changeOfCircumstanceDetails = changeOfCircumstanceDetails)
  def modifyChangeOfCircumstanceDetails(
    pf: PartialFunction[ChangeOfCircumstanceDetails, ChangeOfCircumstanceDetails]
  ): PPTSubscriptionDisplayRecord =
    if (pf.isDefinedAt(changeOfCircumstanceDetails)) copy(changeOfCircumstanceDetails = pf(changeOfCircumstanceDetails))
    else this
}

object PPTSubscriptionDisplayRecord extends RecordUtils[PPTSubscriptionDisplayRecord] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[PPTSubscriptionDisplayRecord] =
    RecordMetaData[PPTSubscriptionDisplayRecord](this)

  def uniqueKey(key: String): String = s"""pptReference:${key.toUpperCase}"""
  def pptReferenceKey(key: String): String = s"""pptReference:${key.toUpperCase}"""

  import Validator._
  import Generator.GenOps._

  val pptReferenceValidator: Validator[String] = check(
    _.matches(Common.pptReferencePattern),
    s"""Invalid pptReference, does not matches regex ${Common.pptReferencePattern}"""
  )
  val legalEntityDetailsValidator: Validator[LegalEntityDetails] = checkProperty(identity, LegalEntityDetails.validate)
  val changeOfCircumstanceDetailsValidator: Validator[ChangeOfCircumstanceDetails] =
    checkProperty(identity, ChangeOfCircumstanceDetails.validate)

  override val validate: Validator[PPTSubscriptionDisplayRecord] = Validator(
    checkProperty(_.pptReference, pptReferenceValidator),
    checkProperty(_.legalEntityDetails, legalEntityDetailsValidator),
    checkProperty(_.changeOfCircumstanceDetails, changeOfCircumstanceDetailsValidator)
  )

  override val gen: Gen[PPTSubscriptionDisplayRecord] = for {
    pptReference                <- Generator.regex(Common.pptReferencePattern)
    legalEntityDetails          <- LegalEntityDetails.gen
    changeOfCircumstanceDetails <- ChangeOfCircumstanceDetails.gen
  } yield PPTSubscriptionDisplayRecord(
    pptReference = pptReference,
    legalEntityDetails = legalEntityDetails,
    changeOfCircumstanceDetails = changeOfCircumstanceDetails
  )

  def generateWith(
    affinityGroup: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    pptRegistrationDate: Option[String],
    pptReference: String
  ): PPTSubscriptionDisplayRecord =
    PPTSubscriptionDisplayRecord
      .generate(PPTSubscriptionDisplayRecord.getClass.getSimpleName)
      .withChangeOfCircumstanceDetails(
        ChangeOfCircumstanceDetails(
          DeregistrationDetails(deregistrationDate = DateSupport.inTheNextYear.sample.get)
        )
      )
      .withLegalEntityDetails(
        LegalEntityDetails(
          dateOfApplication = pptRegistrationDate orElse DateSupport.inThePastYear.sample,
          customerDetails = CustomerDetails.generateWith(affinityGroup, firstName, lastName)
        )
      )
      .withPptReference(pptReference)

  val legalEntityDetailsSanitizer: Update = seed =>
    entity => entity.copy(legalEntityDetails = LegalEntityDetails.sanitize(seed)(entity.legalEntityDetails))

  val changeOfCircumstanceDetailsSanitizer: Update = seed =>
    entity =>
      entity.copy(changeOfCircumstanceDetails =
        ChangeOfCircumstanceDetails.sanitize(seed)(entity.changeOfCircumstanceDetails)
      )

  override val sanitizers: Seq[Update] = Seq(legalEntityDetailsSanitizer, changeOfCircumstanceDetailsSanitizer)

  implicit val formats: Format[PPTSubscriptionDisplayRecord] = Json.format[PPTSubscriptionDisplayRecord]

  case class ChangeOfCircumstanceDetails(deregistrationDetails: ChangeOfCircumstanceDetails.DeregistrationDetails) {

    def withDeregistrationDetails(
      deregistrationDetails: ChangeOfCircumstanceDetails.DeregistrationDetails
    ): ChangeOfCircumstanceDetails = copy(deregistrationDetails = deregistrationDetails)
    def modifyDeregistrationDetails(
      pf: PartialFunction[
        ChangeOfCircumstanceDetails.DeregistrationDetails,
        ChangeOfCircumstanceDetails.DeregistrationDetails
      ]
    ): ChangeOfCircumstanceDetails =
      if (pf.isDefinedAt(deregistrationDetails)) copy(deregistrationDetails = pf(deregistrationDetails)) else this
  }

  object ChangeOfCircumstanceDetails extends RecordUtils[ChangeOfCircumstanceDetails] {

    val deregistrationDetailsValidator: Validator[DeregistrationDetails] =
      checkProperty(identity, DeregistrationDetails.validate)

    override val validate: Validator[ChangeOfCircumstanceDetails] = Validator(
      checkProperty(_.deregistrationDetails, deregistrationDetailsValidator)
    )

    override val gen: Gen[ChangeOfCircumstanceDetails] = for {
      deregistrationDetails <- DeregistrationDetails.gen
    } yield ChangeOfCircumstanceDetails(
      deregistrationDetails = deregistrationDetails
    )

    val deregistrationDetailsSanitizer: Update = seed =>
      entity => entity.copy(deregistrationDetails = DeregistrationDetails.sanitize(seed)(entity.deregistrationDetails))

    override val sanitizers: Seq[Update] = Seq(deregistrationDetailsSanitizer)

    implicit val formats: Format[ChangeOfCircumstanceDetails] = Json.format[ChangeOfCircumstanceDetails]

    case class DeregistrationDetails(deregistrationDate: String) {

      def withDeregistrationDate(deregistrationDate: String): DeregistrationDetails =
        copy(deregistrationDate = deregistrationDate)
      def modifyDeregistrationDate(pf: PartialFunction[String, String]): DeregistrationDetails =
        if (pf.isDefinedAt(deregistrationDate)) copy(deregistrationDate = pf(deregistrationDate)) else this
    }

    object DeregistrationDetails extends RecordUtils[DeregistrationDetails] {

      val deregistrationDateValidator: Validator[String] = check(
        _.matches(Common.dateOfApplicationPattern),
        s"""Invalid deregistrationDate, does not matches regex ${Common.dateOfApplicationPattern}"""
      )

      override val validate: Validator[DeregistrationDetails] = Validator(
        checkProperty(_.deregistrationDate, deregistrationDateValidator)
      )

      override val gen: Gen[DeregistrationDetails] = for {
        deregistrationDate <- DateSupport.inTheNextYear
      } yield DeregistrationDetails(
        deregistrationDate = deregistrationDate
      )

      override val sanitizers: Seq[Update] = Seq()

      implicit val formats: Format[DeregistrationDetails] = Json.format[DeregistrationDetails]
    }
  }

  case class IndividualDetails(firstName: Option[String] = None, lastName: Option[String] = None) {

    def withFirstName(firstName: Option[String]): IndividualDetails = copy(firstName = firstName)
    def modifyFirstName(pf: PartialFunction[Option[String], Option[String]]): IndividualDetails =
      if (pf.isDefinedAt(firstName)) copy(firstName = pf(firstName)) else this
    def withLastName(lastName: Option[String]): IndividualDetails = copy(lastName = lastName)
    def modifyLastName(pf: PartialFunction[Option[String], Option[String]]): IndividualDetails =
      if (pf.isDefinedAt(lastName)) copy(lastName = pf(lastName)) else this
  }

  object IndividualDetails extends RecordUtils[IndividualDetails] {

    val firstNameValidator: Validator[Option[String]] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid firstName, does not matches regex ${Common.firstNamePattern}"""
    )
    val lastNameValidator: Validator[Option[String]] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid lastName, does not matches regex ${Common.firstNamePattern}"""
    )

    override val validate: Validator[IndividualDetails] =
      Validator(checkProperty(_.firstName, firstNameValidator), checkProperty(_.lastName, lastNameValidator))

    override val gen: Gen[IndividualDetails] = Gen const IndividualDetails(
    )

    val firstNameSanitizer: Update = seed =>
      entity =>
        entity.copy(firstName =
          firstNameValidator(entity.firstName)
            .fold(_ => None, _ => entity.firstName)
            .orElse(Generator.get(Generator.forename())(seed))
        )

    val lastNameSanitizer: Update = seed =>
      entity =>
        entity.copy(lastName =
          lastNameValidator(entity.lastName)
            .fold(_ => None, _ => entity.lastName)
            .orElse(Generator.get(Generator.surname)(seed))
        )

    override val sanitizers: Seq[Update] = Seq(firstNameSanitizer, lastNameSanitizer)

    implicit val formats: Format[IndividualDetails] = Json.format[IndividualDetails]

  }

  case class LegalEntityDetails(
    dateOfApplication: Option[String] = None,
    customerDetails: Option[LegalEntityDetails.CustomerDetails] = None
  ) {

    def withDateOfApplication(dateOfApplication: Option[String]): LegalEntityDetails =
      copy(dateOfApplication = dateOfApplication)
    def modifyDateOfApplication(pf: PartialFunction[Option[String], Option[String]]): LegalEntityDetails =
      if (pf.isDefinedAt(dateOfApplication)) copy(dateOfApplication = pf(dateOfApplication)) else this
    def withCustomerDetails(customerDetails: Option[LegalEntityDetails.CustomerDetails]): LegalEntityDetails =
      copy(customerDetails = customerDetails)
    def modifyCustomerDetails(
      pf: PartialFunction[Option[LegalEntityDetails.CustomerDetails], Option[LegalEntityDetails.CustomerDetails]]
    ): LegalEntityDetails =
      if (pf.isDefinedAt(customerDetails)) copy(customerDetails = pf(customerDetails)) else this
  }

  object LegalEntityDetails extends RecordUtils[LegalEntityDetails] {

    val dateOfApplicationValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfApplicationPattern),
      s"""Invalid dateOfApplication, does not matches regex ${Common.dateOfApplicationPattern}"""
    )
    val customerDetailsValidator: Validator[Option[CustomerDetails]] = checkIfSome(identity, CustomerDetails.validate)

    override val validate: Validator[LegalEntityDetails] = Validator(
      checkProperty(_.dateOfApplication, dateOfApplicationValidator),
      checkProperty(_.customerDetails, customerDetailsValidator)
    )

    override val gen: Gen[LegalEntityDetails] = Gen const LegalEntityDetails(
    )

    val dateOfApplicationSanitizer: Update = seed =>
      entity =>
        entity.copy(dateOfApplication =
          dateOfApplicationValidator(entity.dateOfApplication)
            .fold(_ => None, _ => entity.dateOfApplication)
            .orElse(Generator.get(Generator.dateYYYYMMDDGen.variant("ofapplication"))(seed))
        )

    val customerDetailsSanitizer: Update = seed =>
      entity =>
        entity.copy(customerDetails =
          entity.customerDetails
            .orElse(Generator.get(CustomerDetails.gen)(seed))
            .map(CustomerDetails.sanitize(seed))
        )

    override val sanitizers: Seq[Update] = Seq(dateOfApplicationSanitizer, customerDetailsSanitizer)

    implicit val formats: Format[LegalEntityDetails] = Json.format[LegalEntityDetails]

    case class CustomerDetails(
      customerType: String,
      individualDetails: Option[IndividualDetails] = None,
      organisationDetails: Option[OrganisationDetails] = None
    ) {

      def withCustomerType(customerType: String): CustomerDetails = copy(customerType = customerType)
      def modifyCustomerType(pf: PartialFunction[String, String]): CustomerDetails =
        if (pf.isDefinedAt(customerType)) copy(customerType = pf(customerType)) else this
      def withIndividualDetails(individualDetails: Option[IndividualDetails]): CustomerDetails =
        copy(individualDetails = individualDetails)
      def modifyIndividualDetails(
        pf: PartialFunction[Option[IndividualDetails], Option[IndividualDetails]]
      ): CustomerDetails =
        if (pf.isDefinedAt(individualDetails)) copy(individualDetails = pf(individualDetails)) else this
      def withOrganisationDetails(organisationDetails: Option[OrganisationDetails]): CustomerDetails =
        copy(organisationDetails = organisationDetails)
      def modifyOrganisationDetails(
        pf: PartialFunction[Option[OrganisationDetails], Option[OrganisationDetails]]
      ): CustomerDetails =
        if (pf.isDefinedAt(organisationDetails)) copy(organisationDetails = pf(organisationDetails)) else this
    }

    object CustomerDetails extends RecordUtils[CustomerDetails] {

      val customerTypeValidator: Validator[String] =
        check(_.isOneOf(Common.customerTypeEnum), "Invalid customerType, does not match allowed values")
      val individualDetailsValidator: Validator[Option[IndividualDetails]] =
        checkIfSome(identity, IndividualDetails.validate)
      val organisationDetailsValidator: Validator[Option[OrganisationDetails]] =
        checkIfSome(identity, OrganisationDetails.validate)

      override val validate: Validator[CustomerDetails] = Validator(
        checkProperty(_.customerType, customerTypeValidator),
        checkProperty(_.individualDetails, individualDetailsValidator),
        checkProperty(_.organisationDetails, organisationDetailsValidator),
        checkIfOnlyOneSetIsDefined(
          Seq(Set(_.individualDetails), Set(_.organisationDetails)),
          "[{individualDetails},{organisationDetails}]"
        )
      )

      override val gen: Gen[CustomerDetails] = for {
        customerType <- Gen.oneOf(Common.customerTypeEnum)
      } yield CustomerDetails(
        customerType = customerType
      )

      def generateWith(
        affinityGroup: Option[String],
        firstName: Option[String],
        lastName: Option[String]
      ): Option[CustomerDetails] =
        affinityGroup flatMap {
          case AG.Individual =>
            Some(
              LegalEntityDetails.CustomerDetails
                .generate(AG.Individual)
                .withIndividualDetails(
                  Some(
                    IndividualDetails(
                      firstName orElse Names._forenames(Female).sample,
                      lastName orElse Names.surname.sample
                    )
                  )
                )
                .withOrganisationDetails(None)
                .withCustomerType(AG.Individual)
            )
          case AG.Organisation =>
            Some(
              LegalEntityDetails.CustomerDetails
                .generate(AG.Organisation)
                .withCustomerType(AG.Organisation)
                .withIndividualDetails(None)
                .withOrganisationDetails(
                  Some(
                    OrganisationDetails(
                      firstName orElse (for (
                        forename <- Names._forenames(Male).sample; lastname <- Names.surname.sample
                      )
                        yield s"$forename $lastname")
                    )
                  )
                )
            )
          case _ => None
        }

      val individualDetailsSanitizer: Update = seed =>
        entity =>
          entity.copy(individualDetails =
            entity.individualDetails
              .orElse(Generator.get(IndividualDetails.gen)(seed))
              .map(IndividualDetails.sanitize(seed))
          )

      val organisationDetailsSanitizer: Update = seed =>
        entity =>
          entity.copy(organisationDetails =
            entity.organisationDetails
              .orElse(Generator.get(OrganisationDetails.gen)(seed))
              .map(OrganisationDetails.sanitize(seed))
          )

      val individualDetailsCompoundSanitizer: Update = seed =>
        entity =>
          entity.copy(
            individualDetails = entity.individualDetails
              .orElse(Generator.get(IndividualDetails.gen)(seed))
              .map(IndividualDetails.sanitize(seed)),
            organisationDetails = None
          )

      val organisationDetailsCompoundSanitizer: Update = seed =>
        entity =>
          entity.copy(
            organisationDetails = entity.organisationDetails
              .orElse(Generator.get(OrganisationDetails.gen)(seed))
              .map(OrganisationDetails.sanitize(seed)),
            individualDetails = None
          )

      val individualDetailsOrOrganisationDetailsAlternativeSanitizer: Update = seed =>
        entity =>
          if (entity.customerType == AG.Individual && entity.organisationDetails.isEmpty)
            individualDetailsCompoundSanitizer(seed)(entity)
          else if (entity.customerType == AG.Organisation && entity.individualDetails.isEmpty)
            organisationDetailsCompoundSanitizer(seed)(entity)
          else if (entity.individualDetails.isDefined) individualDetailsCompoundSanitizer(seed)(entity)
          else if (entity.organisationDetails.isDefined) organisationDetailsCompoundSanitizer(seed)(entity)
          else
            Generator.get(Gen.chooseNum(0, 1))(seed) match {
              case Some(0) => individualDetailsCompoundSanitizer(seed)(entity)
              case _       => organisationDetailsCompoundSanitizer(seed)(entity)
            }

      override val sanitizers: Seq[Update] = Seq(individualDetailsOrOrganisationDetailsAlternativeSanitizer)

      implicit val formats: Format[CustomerDetails] = Json.format[CustomerDetails]

    }

  }

  case class OrganisationDetails(organisationName: Option[String] = None) {

    def withOrganisationName(organisationName: Option[String]): OrganisationDetails =
      copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[Option[String], Option[String]]): OrganisationDetails =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
  }

  object OrganisationDetails extends RecordUtils[OrganisationDetails] {

    val organisationNameValidator: Validator[Option[String]] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid organisationName, does not matches regex ${Common.firstNamePattern}"""
    )

    override val validate: Validator[OrganisationDetails] = Validator(
      checkProperty(_.organisationName, organisationNameValidator)
    )

    override val gen: Gen[OrganisationDetails] = Gen const OrganisationDetails(
    )

    val organisationNameSanitizer: Update = seed =>
      entity =>
        entity.copy(organisationName =
          organisationNameValidator(entity.organisationName)
            .fold(_ => None, _ => entity.organisationName)
            .orElse(Generator.get(Generator.company)(seed))
        )

    override val sanitizers: Seq[Update] = Seq(organisationNameSanitizer)

    implicit val formats: Format[OrganisationDetails] = Json.format[OrganisationDetails]

  }

  object Common {
    val firstNamePattern = """^[a-zA-Z &`\-\'^]{1,35}$"""
    val dateOfApplicationPattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val pptReferencePattern = """^X[A-Z]PPT000[0-9]{7}$"""
    val customerTypeEnum = Seq("Individual", "Organisation")
  }

  object DateSupport {

    def inTheNextYear: Gen[String] = {
      val today = LocalDate.now().toEpochDay
      aDateBetween(today + 1, today + 365)
    }

    def inThePastYear: Gen[String] = {
      val today = LocalDate.now().toEpochDay
      aDateBetween(today - 365, today - 1)
    }

    private def aDateBetween(daysFrom: Long, daysTo: Long) = {
      Gen.choose(daysFrom, daysTo).map(day => LocalDate.ofEpochDay(day))
    } map (d => Date.from(d.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant)) map (new SimpleDateFormat(
      "yyyy-MM-dd"
    ).format(_))

  }
}
