package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.SubscribeAgentServicesPayload._

/**
  * ----------------------------------------------------------------------------
  * This SubscribeAgentServicesPayload code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
case class SubscribeAgentServicesPayload(
  safeId: Option[String] = None,
  agencyName: String,
  agencyAddress: AgencyAddress,
  telephoneNumber: Option[String] = None,
  agencyEmail: Option[String] = None)

object SubscribeAgentServicesPayload {

  import Validator._

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
      extends AgencyAddress

  object ForeignAddress {

    val validate: Validator[ForeignAddress] = Validator(
      check(
        _.addressLine1.matches(Common.addressLinePattern),
        s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine2.matches(Common.addressLinePattern),
        s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine3.matches(Common.addressLinePattern),
        s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine4.matches(Common.addressLinePattern),
        s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.postalCode.matches(Common.postalCodePattern1),
        s"""Invalid postalCode, does not matches regex ${Common.postalCodePattern1}"""),
      check(_.countryCode.isOneOf(Common.countryCodeEnum0), "Invalid countryCode, does not match allowed values")
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
      extends AgencyAddress

  object UkAddress {

    val validate: Validator[UkAddress] = Validator(
      check(
        _.addressLine1.matches(Common.addressLinePattern),
        s"""Invalid addressLine1, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine2.matches(Common.addressLinePattern),
        s"""Invalid addressLine2, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine3.matches(Common.addressLinePattern),
        s"""Invalid addressLine3, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.addressLine4.matches(Common.addressLinePattern),
        s"""Invalid addressLine4, does not matches regex ${Common.addressLinePattern}"""),
      check(
        _.postalCode.matches(Common.postalCodePattern0),
        s"""Invalid postalCode, does not matches regex ${Common.postalCodePattern0}"""),
      check(_.countryCode.isOneOf(Common.countryCodeEnum1), "Invalid countryCode, does not match allowed values")
    )

    implicit val formats: Format[UkAddress] = Json.format[UkAddress]

  }

  val validate: Validator[SubscribeAgentServicesPayload] = Validator(
    check(
      _.safeId.matches(Common.safeIdPattern),
      s"""Invalid safeId, does not matches regex ${Common.safeIdPattern}"""),
    check(
      _.agencyName.matches(Common.agencyNamePattern),
      s"""Invalid agencyName, does not matches regex ${Common.agencyNamePattern}"""),
    checkObject(_.agencyAddress, AgencyAddress.validate),
    check(
      _.telephoneNumber.matches(Common.telephoneNumberPattern),
      s"""Invalid telephoneNumber, does not matches regex ${Common.telephoneNumberPattern}"""),
    check(
      _.agencyEmail.lengthMinMaxInclusive(1, 132),
      "Invalid length of agencyEmail, should be between 1 and 132 inclusive")
  )

  implicit val formats: Format[SubscribeAgentServicesPayload] = Json.format[SubscribeAgentServicesPayload]
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
