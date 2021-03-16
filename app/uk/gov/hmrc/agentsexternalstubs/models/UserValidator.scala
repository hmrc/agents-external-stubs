/*
 * Copyright 2021 HM Revenue & Customs
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
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import uk.gov.hmrc.agentsexternalstubs.models.User.AG._

object UserValidator {

  import Validator._

  type UserConstraint = User => Validated[String, Unit]

  val validateAffinityGroup: UserConstraint = user =>
    user.affinityGroup match {
      case Some(Individual) | Some(Organisation) | Some(Agent) | None => Valid(())
      case _ =>
        Invalid("affinityGroup must be none, or one of [Individual, Organisation, Agent]")
    }

  val validateConfidenceLevel: UserConstraint = user =>
    user.confidenceLevel match {
      case Some(50) | Some(100) | Some(200) | Some(300)
          if user.affinityGroup.contains(Individual) && user.nino.isDefined =>
        Valid(())
      case None => Valid(())
      case _ =>
        Invalid("confidenceLevel can only be set for Individuals and has to be one of [50, 100, 200, 300]")
    }

  val validateCredentialStrength: UserConstraint = user =>
    user.credentialStrength match {
      case Some("weak") | Some("strong") | None => Valid(())
      case _ =>
        Invalid("credentialStrength must be none, or one of [weak, strong]")
    }

  val validateCredentialRole: UserConstraint = user =>
    user.affinityGroup match {
      case Some(Individual | Agent) =>
        if (user.credentialRole.isEmpty || user.credentialRole.exists(User.CR.all)) Valid(())
        else
          Invalid("credentialRole must be none, or one of [Admin, User, Assistant] for Individual or Agent")
      case Some(Organisation) =>
        if (user.credentialRole.contains(User.CR.Admin) || user.credentialRole.contains(User.CR.User)) Valid(())
        else Invalid("credentialRole must be Admin or User for Organisation")
      case _ => Valid(())
    }

  val validateNino: UserConstraint = user =>
    user.nino match {
      case Some(_) if user.affinityGroup.contains(Individual) || user.affinityGroup.contains(Agent) => Valid(())
      case None                                                                                     => Valid(())
      case _                                                                                        => Invalid("NINO can be only set for Individual or Agent")
    }

  val validateConfidenceLevelAndNino: UserConstraint = user =>
    (user.affinityGroup, user.nino, user.confidenceLevel) match {
      case (Some(Individual), Some(_), Some(_)) => Valid(())
      case (Some(Individual), None, Some(_)) =>
        Invalid("confidenceLevel must be accompanied by NINO")
      case (Some(Individual), Some(_), None) =>
        Invalid("NINO must be accompanied by confidenceLevel")
      case _ => Valid(())
    }

  val validateDateOfBirth: UserConstraint = user =>
    user.dateOfBirth match {
      case Some(_) if user.affinityGroup.contains(Individual) || user.affinityGroup.contains(Agent) => Valid(())
      case None                                                                                     => Valid(())
      case _                                                                                        => Invalid("dateOfBirth can be only set for Individual or Agent")
    }

  val validateAgentCode: UserConstraint = user =>
    user.agentCode match {
      case Some(_) if user.affinityGroup.contains(Agent) => Valid(())
      case None if user.affinityGroup.contains(Agent) =>
        Invalid("agentCode is required for Agent")
      case _ => Valid(())
    }

  val addressLineOptValidator: Validator[Option[String]] =
    check(_.lengthMinMaxInclusive(1, 35), "Invalid length of address Line, should be between 1 and 35 inclusive")
  val postalCodeValidator: Validator[Option[String]] =
    check(_.matches(Common.postcodeRegex), "Invalid postcode, should be valid UK postcode")
  val countryCodeValidator: Validator[Option[String]] =
    check(_.isOneOf(Common.countryCodes), "Invalid countryCode, does not match allowed values")

  val validateAddressFields: Validator[User.Address] = Validator(
    checkProperty(_.line1, addressLineOptValidator),
    checkProperty(_.line2, addressLineOptValidator),
    checkProperty(_.line3, addressLineOptValidator),
    checkProperty(_.line4, addressLineOptValidator),
    checkProperty(_.postcode, postalCodeValidator),
    checkProperty(_.countryCode, countryCodeValidator)
  )

  val validateAddress: UserConstraint = user =>
    user.address match {
      case Some(address) => validateAddressFields(address).leftMap(_.mkString(", "))
      case _             => Valid(())
    }

  val validateSuspendedRegimes: UserConstraint = user => {
    val validRegimes = Set("ALL", "ITSA", "VATC", "TRS", "CGT")
    user.suspendedRegimes match {
      case None => Valid(())
      case Some(regimes) =>
        import Validator.Implicits._
        regimes
          .map(regime =>
            if (validRegimes.contains(regime)) Valid(())
            else Invalid(s"suspended regime $regime not valid")
          )
          .reduce(_ combine _)
    }
  }

  val validateEachPrincipalEnrolment: UserConstraint = user =>
    if (user.principalEnrolments.isEmpty) Valid(())
    else {
      import Validator.Implicits._
      user.principalEnrolments
        .map(e =>
          Validated
            .cond(
              user.affinityGroup
                .forall(ag =>
                  Services(e.key)
                    .map(_.affinityGroups)
                    .forall(_.contains(ag))
                ),
              (),
              s"Service ${e.key} is not available for this user's affinity group"
            )
            .andThen(_ => Enrolment.validate(e))
        )
        .reduce(_ combine _)
    }

  val validatePrincipalEnrolmentsAreDistinct: UserConstraint = user =>
    if (user.principalEnrolments.isEmpty) Valid(())
    else {
      val keys = user.principalEnrolments.map(_.key)
      if (keys.size == keys.distinct.size) Valid(())
      else {
        val repeated: Iterable[String] = keys.groupBy(identity).filter { case (_, k) => k.size > 1 }.map(_._2.head)
        val redundant = repeated.map(r => (r, Services.apply(r))).collect {
          case (_, Some(s)) if !s.flags.multipleEnrolment => s.name
          case (r, None)                                  => r
        }
        if (redundant.isEmpty) Valid(()) else Invalid(s"Repeated principal enrolments: ${redundant.mkString(", ")}")
      }
    }

  val validateEachDelegatedEnrolment: UserConstraint = user =>
    user.delegatedEnrolments match {
      case s if s.isEmpty => Valid(())
      case _ if user.affinityGroup.contains(Agent) =>
        import Validator.Implicits._
        user.delegatedEnrolments
          .map(e =>
            Validated
              .cond(
                Services(e.key)
                  .map(_.affinityGroups)
                  .forall(ag => ag.contains(Individual) || ag.contains(Organisation)),
                (),
                s"Enrolment for ${e.key} may not be delegated to an Agent."
              )
              .andThen(_ => Enrolment.validate(e))
          )
          .reduce(_ combine _)
      case _ => Invalid("Only Agents can have delegated enrolments")
    }

  val validateDelegatedEnrolmentsValuesAreDistinct: UserConstraint = user =>
    if (user.delegatedEnrolments.isEmpty) Valid(())
    else {
      import Validator.Implicits._
      val results = user.delegatedEnrolments
        .groupBy(_.key)
        .collect { case (key, es) if es.size > 1 => (key, es) }
        .map { case (key, es) =>
          val keys = es.map(e => e.toEnrolmentKeyTag.getOrElse(e.key))
          if (keys.size == keys.distinct.size) Valid(())
          else Invalid(s", $key")
        }
      if (results.isEmpty) Valid(())
      else
        results
          .reduce(_ combine _)
          .leftMap(keys => s"Delegated enrolment values must be distinct$keys")
    }

  private val constraints: Seq[UserConstraint] = Seq(
    validateAffinityGroup,
    validateConfidenceLevel,
    validateCredentialStrength,
    validateCredentialRole,
    validateNino,
    validateConfidenceLevelAndNino,
    validateDateOfBirth,
    validateAgentCode,
    validateEachPrincipalEnrolment,
    validatePrincipalEnrolmentsAreDistinct,
    validateEachDelegatedEnrolment,
    validateDelegatedEnrolmentsValuesAreDistinct,
    validateAddress,
    validateSuspendedRegimes
  )

  val validate: User => Validated[List[String], Unit] = Validator.validate(constraints: _*)

  object Common {

    final val postcodeRegex =
      "^([A-Za-z][A-Za-z]\\d\\d|[A-Za-z][A-Za-z]\\d|[A-Za-z]\\d|[A-Za-z]\\d\\d|[A-Za-z]\\d[A-Za-z]|[A-Za-z]{2}\\d[A-Za-z]) {0,1}\\d[A-Za-z]{2}$"

    final val countryCodes = Set(
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
  }

}
