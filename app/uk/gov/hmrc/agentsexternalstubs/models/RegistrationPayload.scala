/*
 * Copyright 2020 HM Revenue & Customs
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
  requiresNameMatch: Boolean = false,
  isAnAgent: Boolean = false,
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

  val regimeValidator: Validator[String] =
    check(_.matches(Common.regimePattern), s"""Invalid regime, does not matches regex ${Common.regimePattern}""")
  val individualValidator: Validator[Option[Individual]] = checkIfSome(identity, Individual.validate)
  val organisationValidator: Validator[Option[Organisation]] = checkIfSome(identity, Organisation.validate)

  val validate: Validator[RegistrationPayload] = Validator(
    checkProperty(_.regime, regimeValidator),
    checkProperty(_.individual, individualValidator),
    checkProperty(_.organisation, organisationValidator),
    checkIfOnlyOneSetIsDefined(Seq(Set(_.individual), Set(_.organisation)), "[{individual},{organisation}]")
  )

  implicit val formats: Format[RegistrationPayload] = Json.format[RegistrationPayload]

  case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String] = None)

  object Individual {

    val firstNameValidator: Validator[String] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid firstName, does not matches regex ${Common.firstNamePattern}""")
    val lastNameValidator: Validator[String] = check(
      _.matches(Common.firstNamePattern),
      s"""Invalid lastName, does not matches regex ${Common.firstNamePattern}""")
    val dateOfBirthValidator: Validator[Option[String]] = check(
      _.matches(Common.dateOfBirthPattern),
      s"""Invalid dateOfBirth, does not matches regex ${Common.dateOfBirthPattern}""")

    val validate: Validator[Individual] = Validator(
      checkProperty(_.firstName, firstNameValidator),
      checkProperty(_.lastName, lastNameValidator),
      checkProperty(_.dateOfBirth, dateOfBirthValidator)
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

    val organisationNameValidator: Validator[String] = check(
      _.matches(Common.organisationNamePattern),
      s"""Invalid organisationName, does not matches regex ${Common.organisationNamePattern}""")
    val organisationTypeValidator: Validator[String] =
      check(_.isOneOf(Common.organisationTypeEnum), "Invalid organisationType, does not match allowed values")

    val validate: Validator[Organisation] = Validator(
      checkProperty(_.organisationName, organisationNameValidator),
      checkProperty(_.organisationType, organisationTypeValidator))

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
