/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.tools

import org.jsoup.Jsoup
import play.api.libs.json.{JsObject, JsValue, Json}

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets

object EtProfileTestSupport:

  final case class Check(name: String, passed: Boolean)

  final case class V3Scenario(
    slug: String,
    label: String,
    service: String,
    clientId: String,
    knownFact: Option[String]
  ):
    def requestBody: JsObject =
      Json.obj("service" -> service, "clientId" -> clientId) ++ knownFact.fold(Json.obj())(value =>
        Json.obj("knownFact" -> value)
      )

  enum V2InvitationTrigger(val argument: String, val label: String):
    case MtdVat extends V2InvitationTrigger("mtd-vat", "MTD-VAT")
    case MtdIt extends V2InvitationTrigger("mtd-it", "MTD-IT")

  object V2InvitationTrigger:
    def from(argument: String): Option[V2InvitationTrigger] = values.find(_.argument == argument)

  val RequiredProfiles: Seq[String] = Seq(
    "capital-gains-tax-uk-property",
    "plastic-packaging-tax",
    "pillar-2",
    "trusts",
    "country-by-country-reporting"
  )

  val RequiredServices: Seq[String] = "mtd-vat" +: RequiredProfiles

  val OrganisationServices: Seq[String] = Seq("mtd-vat", "mtd-income-tax") ++ RequiredProfiles

  val RequiredPrincipalServices: Seq[String] = Seq(
    "HMCE-VATDEC-ORG",
    "HMRC-MTD-VAT",
    "HMRC-MTD-IT",
    "HMRC-CGT-PD",
    "HMRC-PPT-ORG",
    "HMRC-PILLAR2-ORG",
    "HMRC-CBC-ORG",
    "HMRC-TERS-ORG",
    "HMRC-TERSNT-ORG"
  )

  val RequiredAgentScopes: Seq[String] = Seq(
    "write:sent-invitations",
    "read:sent-invitations",
    "write:cancel-invitations",
    "read:check-relationship",
    "write:relationships"
  )

  private val requiredOrganisationFields: Seq[(Seq[String], String => Boolean)] = Seq(
    Seq("userId")                                     -> nonEmpty,
    Seq("password")                                   -> nonEmpty,
    Seq("organisationDetails", "address", "postcode") -> nonEmpty,
    Seq("vrn")                                        -> matches("^[0-9]{9}$"),
    Seq("vatRegistrationDate")                        -> matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"),
    Seq("nino")                                       -> nonEmpty,
    Seq("mtdItId")                                    -> nonEmpty,
    Seq("capitalGainsTaxOnUKPropertyReference")       -> matches("^X[A-Z]CGTP[0-9]{9}$"),
    Seq("plasticPackagingTaxRegistrationNumber")      -> matches("^X[A-Z]PPT000[0-9]{7}$"),
    Seq("pillar2Id")                                  -> matches("^X[A-Z]PLR[0-9]{10}$"),
    Seq("countryByCountryReportingId")                -> matches("^X[A-Z]CBC[0-9]{10}$"),
    Seq("trustsUniqueTaxpayerReference")              -> matches("^[0-9]{10}$"),
    Seq("trustsUniqueReferenceNumber")                -> matches("^[A-Z]{2}TRUST[0-9]{8}$")
  )

  def validateServices(responseBody: String): Seq[Check] =
    RequiredServices.map(profile => Check(profile, responseBody.contains(profile)))

  def validateV3Disabled(v3Status: Int, v3Body: JsValue, v2Status: Int): Seq[Check] =
    Seq(
      Check("V3-only relationship endpoint returns HTTP 404", v3Status == 404),
      Check(
        "V3-only relationship endpoint returns StandardNotFound",
        v3Body == Json.obj("code" -> "NOT_FOUND", "message" -> "Resource was not found.")
      ),
      Check("V2 invitations endpoint remains available", v2Status == 200)
    )

  def invitationListed(response: JsValue, invitationId: String): Boolean =
    response
      .asOpt[Seq[JsValue]]
      .exists(
        _.exists(entry =>
          (entry \ "href")
            .asOpt[String]
            .flatMap(href => scala.util.Try(URI.create(href).getPath.split('/').last).toOption)
            .contains(invitationId)
        )
      )

  def validateOrganisation(document: JsValue): Seq[Check] =
    requiredOrganisationFields.map: (path, predicate) =>
      val value = valueAt(document, path).flatMap(_.asOpt[String])
      Check(path.mkString("."), value.exists(predicate))

  def organisationFromDeveloperHubHtml(html: String): JsObject =
    val document = Jsoup.parse(html)

    def field(key: String): String =
      Option(document.selectFirst(s"[data-${key.toLowerCase}]"))
        .map(_.text().trim)
        .filter(_.nonEmpty)
        .getOrElse(throw IllegalArgumentException(s"Developer Hub HTML is missing required field: $key"))

    val userId = field("userId")
    val password = field("password")
    val organisationDetails =
      try Json.parse(field("organisationDetails"))
      catch case _: Exception => throw IllegalArgumentException("Developer Hub Organisation Details is not valid JSON")

    Json.obj(
      "userId"                                -> userId,
      "password"                              -> password,
      "organisationDetails"                   -> organisationDetails,
      "vrn"                                   -> field("vrn"),
      "vatRegistrationDate"                   -> field("vatRegistrationDate"),
      "nino"                                  -> field("nino"),
      "mtdItId"                               -> field("mtdItId"),
      "capitalGainsTaxOnUKPropertyReference"  -> field("capitalGainsTaxOnUKPropertyReference"),
      "plasticPackagingTaxRegistrationNumber" -> field("plasticPackagingTaxRegistrationNumber"),
      "pillar2Id"                             -> field("pillar2Id"),
      "countryByCountryReportingId"           -> field("countryByCountryReportingId"),
      "trustsUniqueTaxpayerReference"         -> field("trustsUniqueTaxpayerReference"),
      "trustsUniqueReferenceNumber"           -> field("trustsUniqueReferenceNumber")
    )

  def v2InvitationBody(document: JsValue, trigger: V2InvitationTrigger): JsObject =
    trigger match
      case V2InvitationTrigger.MtdVat =>
        Json.obj(
          "service"      -> Json.arr("MTD-VAT"),
          "clientType"   -> "business",
          "clientIdType" -> "vrn",
          "clientId"     -> requiredString(document, Seq("vrn")),
          "knownFact"    -> requiredString(document, Seq("vatRegistrationDate"))
        )
      case V2InvitationTrigger.MtdIt =>
        Json.obj(
          "service"      -> Json.arr("MTD-IT"),
          "clientType"   -> "personal",
          "clientIdType" -> "ni",
          "clientId"     -> requiredString(document, Seq("nino")).replace(" ", ""),
          "knownFact"    -> requiredString(document, Seq("organisationDetails", "address", "postcode")),
          "agentType"    -> "main"
        )

  def v3TrustsInvitationBody(document: JsValue): JsObject =
    Json.obj(
      "service"  -> "TRUSTS",
      "clientId" -> requiredString(document, Seq("trustsUniqueTaxpayerReference"))
    )

  def mongoEvidenceFromConsole(console: String): JsValue =
    val marker = "APB12584_RESULT="
    console.linesIterator
      .map(_.trim)
      .find(_.contains(marker))
      .map(line => line.substring(line.indexOf(marker) + marker.length))
      .map(Json.parse)
      .getOrElse(throw IllegalArgumentException(s"Jenkins output does not contain the $marker result line"))

  def validateMongoEvidence(document: JsValue): Seq[Check] =
    val enrolments = (document \ "user" \ "assignedPrincipalEnrolments").asOpt[Seq[String]].getOrElse(Seq.empty)
    val services = enrolments.flatMap(_.split("~").headOption).toSet
    Seq(
      Check("AES user", (document \ "user").asOpt[JsObject].isDefined),
      Check("AES group", (document \ "group").asOpt[JsObject].isDefined),
      Check(
        "group.affinityGroup=Organisation",
        (document \ "group" \ "affinityGroup").asOpt[String].contains("Organisation")
      )
    ) ++ RequiredPrincipalServices.map(service => Check(service, services.contains(service))) ++ Seq(
      Check(
        "PPT known fact",
        recordString(document, "pptReference", "legalEntityDetails", "dateOfApplication").isDefined
      ),
      Check("CBC known fact", recordString(document, "cbcId", "primaryContact", "0", "email").isDefined),
      Check("Pillar 2 known fact", recordString(document, "plrReference", "upeDetails", "registrationDate").isDefined)
    )

  def v3OrganisationScenarios(organisation: JsValue, mongoEvidence: JsValue): Seq[V3Scenario] =
    val pptId = requiredString(organisation, Seq("plasticPackagingTaxRegistrationNumber"))
    val cbcId = requiredString(organisation, Seq("countryByCountryReportingId"))
    val pillar2Id = requiredString(organisation, Seq("pillar2Id"))
    Seq(
      V3Scenario(
        "mtd-vat",
        "MTD-VAT",
        "MTD-VAT",
        requiredString(organisation, Seq("vrn")),
        Some(requiredString(organisation, Seq("vatRegistrationDate")))
      ),
      V3Scenario(
        "trusts-taxable",
        "Trusts (taxable)",
        "TRUSTS",
        requiredString(organisation, Seq("trustsUniqueTaxpayerReference")),
        None
      ),
      V3Scenario(
        "trusts-nontaxable",
        "Trusts (non-taxable)",
        "TRUSTS",
        requiredString(organisation, Seq("trustsUniqueReferenceNumber")),
        None
      ),
      V3Scenario(
        "cgt-pd",
        "Capital Gains Tax",
        "CGT-PD",
        requiredString(organisation, Seq("capitalGainsTaxOnUKPropertyReference")),
        Some(requiredString(organisation, Seq("organisationDetails", "address", "postcode")))
      ),
      V3Scenario(
        "ppt",
        "Plastic Packaging Tax",
        "PPT",
        pptId,
        Some(requiredRecordString(mongoEvidence, "pptReference", pptId, "legalEntityDetails", "dateOfApplication"))
      ),
      V3Scenario(
        "cbc-uk",
        "Country-by-Country Reporting (UK)",
        "CBC",
        cbcId,
        Some(requiredRecordString(mongoEvidence, "cbcId", cbcId, "primaryContact", "0", "email"))
      ),
      V3Scenario(
        "pillar2",
        "Pillar 2",
        "PILLAR2",
        pillar2Id,
        Some(requiredRecordString(mongoEvidence, "plrReference", pillar2Id, "upeDetails", "registrationDate"))
      )
    )

  def oauthAuthoriseUrl(clientId: String, redirectUri: String): URI =
    val query = Seq(
      "response_type" -> "code",
      "client_id"     -> clientId,
      "scope"         -> RequiredAgentScopes.mkString(" "),
      "state"         -> "APB-12584",
      "redirect_uri"  -> redirectUri
    ).map((key, value) => s"${encode(key)}=${encode(value)}").mkString("&")

    URI.create(s"https://test-www.tax.service.gov.uk/oauth/authorize?$query")

  def requiredString(document: JsValue, path: Seq[String]): String =
    valueAt(document, path)
      .flatMap(_.asOpt[String])
      .filter(_.nonEmpty)
      .getOrElse(throw IllegalArgumentException(s"Missing required JSON field: ${path.mkString(".")}"))

  private def valueAt(document: JsValue, path: Seq[String]): Option[JsValue] =
    path.foldLeft(Option(document)): (current, field) =>
      current.flatMap: json =>
        field.toIntOption match
          case Some(index) => json.asOpt[Seq[JsValue]].flatMap(_.lift(index))
          case None        => (json \ field).toOption

  private def requiredRecordString(
    document: JsValue,
    identifierField: String,
    identifier: String,
    valuePath: String*
  ): String =
    val record = records(document)
      .find(record => (record \ identifierField).asOpt[String].contains(identifier))
      .getOrElse(throw IllegalArgumentException(s"Mongo evidence is missing the $identifierField record"))
    valueAt(record, valuePath)
      .flatMap(_.asOpt[String])
      .filter(_.nonEmpty)
      .getOrElse(
        throw IllegalArgumentException(s"Mongo evidence is missing ${valuePath.mkString(".")} for $identifierField")
      )

  private def recordString(document: JsValue, identifierField: String, valuePath: String*): Option[String] =
    records(document)
      .find(record => (record \ identifierField).asOpt[String].exists(_.nonEmpty))
      .flatMap(record => valueAt(record, valuePath))
      .flatMap(_.asOpt[String])
      .filter(_.nonEmpty)

  private def records(document: JsValue): Seq[JsValue] =
    (document \ "records").asOpt[Seq[JsValue]].getOrElse(Seq.empty)

  private def nonEmpty(value: String): Boolean = value.nonEmpty

  private def matches(pattern: String)(value: String): Boolean = value.matches(pattern)

  private def encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
