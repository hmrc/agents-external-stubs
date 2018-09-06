package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.RegistrationPayload._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1163-64.json app/uk/gov/hmrc/agentsexternalstubs/models/RegistrationPayload.scala RegistrationPayload output:payload"
  * ----------------------------------------------------------------------------
  *
  *  RegistrationPayload
  *  -  Individual
  *  -  Organisation
  */
case class RegistrationPayload(
  regime: String,
  requiresNameMatch: Boolean,
  isAnAgent: Boolean,
  individual: Option[Individual] = None,
  organisation: Option[Organisation] = None) {

  def withRegime(regime: String): RegistrationPayload = copy(regime = regime)
  def modifyRegime(pf: PartialFunction[String, String]): RegistrationPayload =
    if (pf.isDefinedAt(regime)) copy(regime = pf(regime)) else this
  def withRequiresNameMatch(requiresNameMatch: Boolean): RegistrationPayload =
    copy(requiresNameMatch = requiresNameMatch)
  def modifyRequiresNameMatch(pf: PartialFunction[Boolean, Boolean]): RegistrationPayload =
    if (pf.isDefinedAt(requiresNameMatch)) copy(requiresNameMatch = pf(requiresNameMatch)) else this
  def withIsAnAgent(isAnAgent: Boolean): RegistrationPayload = copy(isAnAgent = isAnAgent)
  def modifyIsAnAgent(pf: PartialFunction[Boolean, Boolean]): RegistrationPayload =
    if (pf.isDefinedAt(isAnAgent)) copy(isAnAgent = pf(isAnAgent)) else this
  def withIndividual(individual: Option[Individual]): RegistrationPayload = copy(individual = individual)
  def modifyIndividual(pf: PartialFunction[Option[Individual], Option[Individual]]): RegistrationPayload =
    if (pf.isDefinedAt(individual)) copy(individual = pf(individual)) else this
  def withOrganisation(organisation: Option[Organisation]): RegistrationPayload = copy(organisation = organisation)
  def modifyOrganisation(pf: PartialFunction[Option[Organisation], Option[Organisation]]): RegistrationPayload =
    if (pf.isDefinedAt(organisation)) copy(organisation = pf(organisation)) else this
}

object RegistrationPayload {

  import Validator._

  val validate: Validator[RegistrationPayload] = Validator(
    check(
      _.regime.matches(Common.regimePattern),
      s"""Invalid regime, does not matches regex ${Common.regimePattern}"""),
    checkObjectIfSome(_.individual, Individual.validate),
    checkObjectIfSome(_.organisation, Organisation.validate),
    checkIfAtLeastOneIsDefined(Seq(_.organisation, _.individual))
  )

  implicit val formats: Format[RegistrationPayload] = Json.format[RegistrationPayload]

  case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String] = None) {

    def withFirstName(firstName: String): Individual = copy(firstName = firstName)
    def modifyFirstName(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(firstName)) copy(firstName = pf(firstName)) else this
    def withLastName(lastName: String): Individual = copy(lastName = lastName)
    def modifyLastName(pf: PartialFunction[String, String]): Individual =
      if (pf.isDefinedAt(lastName)) copy(lastName = pf(lastName)) else this
    def withDateOfBirth(dateOfBirth: Option[String]): Individual = copy(dateOfBirth = dateOfBirth)
    def modifyDateOfBirth(pf: PartialFunction[Option[String], Option[String]]): Individual =
      if (pf.isDefinedAt(dateOfBirth)) copy(dateOfBirth = pf(dateOfBirth)) else this
  }

  object Individual {

    val validate: Validator[Individual] = Validator(
      check(
        _.firstName.matches(Common.firstNamePattern),
        s"""Invalid firstName, does not matches regex ${Common.firstNamePattern}"""),
      check(
        _.lastName.matches(Common.firstNamePattern),
        s"""Invalid lastName, does not matches regex ${Common.firstNamePattern}"""),
      check(
        _.dateOfBirth.matches(Common.dateOfBirthPattern),
        s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}""")
    )

    implicit val formats: Format[Individual] = Json.format[Individual]

  }

  case class Organisation(organisationName: String, organisationType: String) {

    def withOrganisationName(organisationName: String): Organisation = copy(organisationName = organisationName)
    def modifyOrganisationName(pf: PartialFunction[String, String]): Organisation =
      if (pf.isDefinedAt(organisationName)) copy(organisationName = pf(organisationName)) else this
    def withOrganisationType(organisationType: String): Organisation = copy(organisationType = organisationType)
    def modifyOrganisationType(pf: PartialFunction[String, String]): Organisation =
      if (pf.isDefinedAt(organisationType)) copy(organisationType = pf(organisationType)) else this
  }

  object Organisation {

    val validate: Validator[Organisation] = Validator(
      check(
        _.organisationName.matches(Common.organisationNamePattern),
        s"""Invalid organisationName, does not matches regex ${Common.organisationNamePattern}"""),
      check(
        _.organisationType.isOneOf(Common.organisationTypeEnum),
        "Invalid organisationType, does not match allowed values")
    )

    implicit val formats: Format[Organisation] = Json.format[Organisation]

  }

  object Common {
    val firstNamePattern = """^[a-zA-Z &`\-\'^]{1,35}$"""
    val dateOfBirthPattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val organisationNamePattern = """^[a-zA-Z0-9 '&\/]{1,105}$"""
    val organisationTypeEnum = Seq("Partnership", "LLP", "Corporate Body", "Unincorporated Body")
    val regimePattern = """^[A-Z]{3,10}$"""
  }
}
