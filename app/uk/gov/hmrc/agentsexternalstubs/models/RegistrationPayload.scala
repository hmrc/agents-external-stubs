package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.RegistrationPayload._

/**
  * ----------------------------------------------------------------------------
  * This RegistrationPayload code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
case class RegistrationPayload(
  regime: String,
  requiresNameMatch: Boolean,
  isAnAgent: Boolean,
  individual: Option[Individual] = None,
  organisation: Option[Organisation] = None)

object RegistrationPayload {

  import Validator._

  case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String] = None)

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

  case class Organisation(organisationName: String, organisationType: String)

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

  val validate: Validator[RegistrationPayload] = Validator(
    check(
      _.regime.matches(Common.regimePattern),
      s"""Invalid regime, does not matches regex ${Common.regimePattern}"""),
    checkObjectIfSome(_.individual, Individual.validate),
    checkObjectIfSome(_.organisation, Organisation.validate),
    checkIfAtLeastOneIsDefined(Seq(_.organisation, _.individual))
  )

  implicit val formats: Format[RegistrationPayload] = Json.format[RegistrationPayload]
  object Common {
    val firstNamePattern = """^[a-zA-Z &`\-\'^]{1,35}$"""
    val dateOfBirthPattern = """^\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"""
    val organisationNamePattern = """^[a-zA-Z0-9 '&\/]{1,105}$"""
    val organisationTypeEnum = Seq("Partnership", "LLP", "Corporate Body", "Unincorporated Body")
    val regimePattern = """^[A-Z]{3,10}$"""
  }
}
