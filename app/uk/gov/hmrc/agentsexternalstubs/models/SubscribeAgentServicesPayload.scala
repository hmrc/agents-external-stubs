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

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.SubscribeAgentServicesPayload._

/**
  * ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1173.json app/uk/gov/hmrc/agentsexternalstubs/models/SubscribeAgentServicesPayload.scala SubscribeAgentServicesPayload output:payload"
  * ----------------------------------------------------------------------------
  *
  *  SubscribeAgentServicesPayload
  *  -  AgencyAddress
  *  -  ForeignAddress
  *  -  UkAddress
  */
case class SubscribeAgentServicesPayload(
  safeId: Option[String] = None,
  agencyName: String,
  agencyAddress: AgencyAddress,
  telephoneNumber: Option[String] = None,
  agencyEmail: Option[String] = None) {

  def withSafeId(safeId: Option[String]): SubscribeAgentServicesPayload = copy(safeId = safeId)
  def modifySafeId(pf: PartialFunction[Option[String], Option[String]]): SubscribeAgentServicesPayload =
    if (pf.isDefinedAt(safeId)) copy(safeId = pf(safeId)) else this
  def withAgencyName(agencyName: String): SubscribeAgentServicesPayload = copy(agencyName = agencyName)
  def modifyAgencyName(pf: PartialFunction[String, String]): SubscribeAgentServicesPayload =
    if (pf.isDefinedAt(agencyName)) copy(agencyName = pf(agencyName)) else this
  def withAgencyAddress(agencyAddress: AgencyAddress): SubscribeAgentServicesPayload =
    copy(agencyAddress = agencyAddress)
  def modifyAgencyAddress(pf: PartialFunction[AgencyAddress, AgencyAddress]): SubscribeAgentServicesPayload =
    if (pf.isDefinedAt(agencyAddress)) copy(agencyAddress = pf(agencyAddress)) else this
  def withTelephoneNumber(telephoneNumber: Option[String]): SubscribeAgentServicesPayload =
    copy(telephoneNumber = telephoneNumber)
  def modifyTelephoneNumber(pf: PartialFunction[Option[String], Option[String]]): SubscribeAgentServicesPayload =
    if (pf.isDefinedAt(telephoneNumber)) copy(telephoneNumber = pf(telephoneNumber)) else this
  def withAgencyEmail(agencyEmail: Option[String]): SubscribeAgentServicesPayload = copy(agencyEmail = agencyEmail)
  def modifyAgencyEmail(pf: PartialFunction[Option[String], Option[String]]): SubscribeAgentServicesPayload =
    if (pf.isDefinedAt(agencyEmail)) copy(agencyEmail = pf(agencyEmail)) else this
}

object SubscribeAgentServicesPayload {

  import Validator._

  val safeIdValidator: Validator[Option[String]] =
    check(_.matches(Common.safeIdPattern), s"""Invalid safeId, does not matches regex ${Common.safeIdPattern}""")
  val agencyNameValidator: Validator[String] = check(
    _.matches(Common.agencyNamePattern),
    s"""Invalid agencyName, does not matches regex ${Common.agencyNamePattern}""")
  val agencyAddressValidator: Validator[AgencyAddress] = checkProperty(identity, AgencyAddress.validate)
  val telephoneNumberValidator: Validator[Option[String]] = check(
    _.matches(Common.telephoneNumberPattern),
    s"""Invalid telephoneNumber, does not matches regex ${Common.telephoneNumberPattern}""")
  val agencyEmailValidator: Validator[Option[String]] =
    check(_.lengthMinMaxInclusive(1, 132), "Invalid length of agencyEmail, should be between 1 and 132 inclusive")

  val validate: Validator[SubscribeAgentServicesPayload] = Validator(
    checkProperty(_.safeId, safeIdValidator),
    checkProperty(_.agencyName, agencyNameValidator),
    checkProperty(_.agencyAddress, agencyAddressValidator),
    checkProperty(_.telephoneNumber, telephoneNumberValidator),
    checkProperty(_.agencyEmail, agencyEmailValidator)
  )

  implicit val formats: Format[SubscribeAgentServicesPayload] = Json.format[SubscribeAgentServicesPayload]

  sealed trait AgencyAddress {
    def addressLine2: Option[String] = None
    def addressLine3: Option[String] = None
    def addressLine1: String
    def countryCode: String
    def addressLine4: Option[String] = None
  }

  object AgencyAddress {

    val validate: Validator[AgencyAddress] = {
      case x: UkAddress      => UkAddress.validate(x)
      case x: ForeignAddress => ForeignAddress.validate(x)
    }

    implicit val reads: Reads[AgencyAddress] = new Reads[AgencyAddress] {
      override def reads(json: JsValue): JsResult[AgencyAddress] = {
        val r0 =
          UkAddress.formats.reads(json).flatMap(e => UkAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          ForeignAddress.formats
            .reads(json)
            .flatMap(e => ForeignAddress.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of AgencyAddress, i.e. UkAddress, ForeignAddress"),
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
        case x: UkAddress      => UkAddress.formats.writes(x)
        case x: ForeignAddress => ForeignAddress.formats.writes(x)
      }
    }

  }

  case class ForeignAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    override val countryCode: String)
      extends AgencyAddress {

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

  object ForeignAddress {

    val addressLine1Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}""")
    val addressLine2Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}""")
    val addressLine3Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}""")
    val addressLine4Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}""")
    val postalCodeValidator: Validator[Option[String]] = check(
      _.matches(Common.postalCodePattern1),
      s"""Invalid postalCode, does not matches regex ${Common.postalCodePattern1}""")
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

  case class UkAddress(
    override val addressLine1: String,
    override val addressLine2: Option[String] = None,
    override val addressLine3: Option[String] = None,
    override val addressLine4: Option[String] = None,
    postalCode: String,
    override val countryCode: String)
      extends AgencyAddress {

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

  object UkAddress {

    val addressLine1Validator: Validator[String] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}""")
    val addressLine2Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}""")
    val addressLine3Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}""")
    val addressLine4Validator: Validator[Option[String]] = check(
      _.matches(Common.addressLinePattern),
      s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}""")
    val postalCodeValidator: Validator[String] = check(
      _.matches(Common.postalCodePattern0),
      s"""Invalid postalCode, does not matches regex ${Common.postalCodePattern0}""")
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
    val telephoneNumberPattern = """^[0-9-+()#x ]{1,24}$"""
    val addressLinePattern = """^[A-Za-z0-9 \-,.&'\/]{1,35}$"""
    val safeIdPattern = """^[A-Za-z0-9 \-,.&'\/]{1,15}$"""
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
    val postalCodePattern0 = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}$|BFPO\s?[0-9]{1,5}$"""
    val countryCodeEnum1 = Seq("GB")
    val postalCodePattern1 = """^[A-Za-z0-9 \-,.&'\/]{1,10}$"""
    val agencyNamePattern = """^[A-Za-z0-9 \-,.&'\/]{1,40}$"""
  }
}
