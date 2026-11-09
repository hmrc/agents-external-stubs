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

import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class EtProfileTestSupportSpec extends BaseUnitSpec:

  private val validOrganisation = Json.obj(
    "userId"              -> "123456789012",
    "password"            -> "test-password",
    "organisationDetails" -> Json.obj(
      "address" -> Json.obj("postcode" -> "AA1 1AA")
    ),
    "vrn"                                   -> "123456789",
    "vatRegistrationDate"                   -> "2001-11-02",
    "nino"                                  -> "AB123456A",
    "mtdItId"                               -> "XAIT00000000001",
    "capitalGainsTaxOnUKPropertyReference"  -> "XACGTP123456789",
    "plasticPackagingTaxRegistrationNumber" -> "XAPPT0001234567",
    "pillar2Id"                             -> "XAPLR1234567890",
    "countryByCountryReportingId"           -> "XACBC1234567890",
    "trustsUniqueTaxpayerReference"         -> "1234567890",
    "trustsUniqueReferenceNumber"           -> "AATRUST12345678"
  )

  private val validMongoEvidence = Json.obj(
    "user" -> Json.obj(
      "assignedPrincipalEnrolments" -> EtProfileTestSupport.RequiredPrincipalServices.map(service =>
        s"$service~id~value"
      )
    ),
    "group"   -> Json.obj("affinityGroup" -> "Organisation"),
    "records" -> Json.arr(
      Json.obj(
        "pptReference"       -> "XAPPT0001234567",
        "legalEntityDetails" -> Json.obj("dateOfApplication" -> "2020-01-02")
      ),
      Json.obj(
        "cbcId"          -> "XACBC1234567890",
        "primaryContact" -> Json.arr(Json.obj("email" -> "client@example.com"))
      ),
      Json.obj(
        "plrReference" -> "XAPLR1234567890",
        "upeDetails"   -> Json.obj("registrationDate" -> "2021-02-03")
      )
    )
  )

  "validateServices" should:
    "report every required profile when the services response contains them" in:
      val response = EtProfileTestSupport.RequiredServices.mkString(" ")

      EtProfileTestSupport.validateServices(response).forall(_.passed) shouldBe true

    "identify a missing profile" in:
      val response = EtProfileTestSupport.RequiredServices.filterNot(_ == "pillar-2").mkString(" ")

      EtProfileTestSupport.validateServices(response).find(_.name == "pillar-2").value.passed shouldBe false

  "validateV3Disabled" should:
    val standardNotFound = Json.obj("code" -> "NOT_FOUND", "message" -> "Resource was not found.")

    "accept the exact disabled V3 response while V2 remains available" in:
      EtProfileTestSupport.validateV3Disabled(404, standardNotFound, 200).forall(_.passed) shouldBe true

    "reject an enabled V3 relationship response" in:
      val checks = EtProfileTestSupport.validateV3Disabled(200, Json.obj("agentAuthorised" -> false), 200)

      checks.find(_.name.contains("HTTP 404")).value.passed shouldBe false
      checks.find(_.name.contains("StandardNotFound")).value.passed shouldBe false

    "reject a V2 regression" in:
      EtProfileTestSupport.validateV3Disabled(404, standardNotFound, 500).last.passed shouldBe false

  "invitationListed" should:
    "recognise the invitation ID at the end of a V3 list href" in:
      val response = Json.arr(
        Json.obj(
          "href" -> "https://www.tax.service.gov.uk/agent-client-relationships/agent/AARN1234567/invitations/C1234567890",
          "service" -> "MTD-VAT",
          "status"  -> "Pending"
        )
      )

      EtProfileTestSupport.invitationListed(response, "C1234567890") shouldBe true

    "reject a different invitation ID" in:
      val response = Json.arr(Json.obj("href" -> "/agent/AARN1234567/invitations/C1234567890"))

      EtProfileTestSupport.invitationListed(response, "D9876543210") shouldBe false

  "validateOrganisation" should:
    "accept a complete organisation with correctly formatted identifiers" in:
      EtProfileTestSupport.validateOrganisation(validOrganisation).forall(_.passed) shouldBe true

    "identify missing and malformed fields" in:
      val invalid = (validOrganisation - "mtdItId") ++ Json.obj("pillar2Id" -> "not-a-pillar-2-id")
      val checks = EtProfileTestSupport.validateOrganisation(invalid)

      checks.find(_.name == "mtdItId").value.passed shouldBe false
      checks.find(_.name == "pillar2Id").value.passed shouldBe false

  "organisationFromDeveloperHubHtml" should:
    "extract and decode the organisation fields rendered by Developer Hub" in:
      val html =
        """<html><body>
          |  <p data-userid>123456789012</p>
          |  <p data-password>test&amp;password</p>
          |  <p data-organisationdetails>{&quot;name&quot;:&quot;Test Org&quot;,&quot;address&quot;:{&quot;postcode&quot;:&quot;AA1 1AA&quot;}}</p>
          |  <p data-vrn>123456789</p>
          |  <p data-vatregistrationdate>2001-11-02</p>
          |  <p data-nino>AB123456A</p>
          |  <p data-mtditid>XAIT00000000001</p>
          |  <p data-capitalgainstaxonukpropertyreference>XACGTP123456789</p>
          |  <p data-plasticpackagingtaxregistrationnumber>XAPPT0001234567</p>
          |  <p data-pillar2id>XAPLR1234567890</p>
          |  <p data-countrybycountryreportingid>XACBC1234567890</p>
          |  <p data-trustsuniquetaxpayerreference>1234567890</p>
          |  <p data-trustsuniquereferencenumber>AATRUST12345678</p>
          |</body></html>""".stripMargin

      val expected = validOrganisation ++ Json.obj(
        "password"            -> "test&password",
        "organisationDetails" -> Json.obj("name" -> "Test Org", "address" -> Json.obj("postcode" -> "AA1 1AA"))
      )

      EtProfileTestSupport.organisationFromDeveloperHubHtml(html) shouldBe expected

    "reject a page missing a required field" in:
      val error = the[IllegalArgumentException] thrownBy EtProfileTestSupport.organisationFromDeveloperHubHtml(
        "<html><body><p data-userid>123456789012</p></body></html>"
      )

      error.getMessage should include("password")

  "v2InvitationBody" should:
    "build the supported MTD-VAT organisation trigger request" in:
      EtProfileTestSupport.v2InvitationBody(
        validOrganisation,
        EtProfileTestSupport.V2InvitationTrigger.MtdVat
      ) shouldBe Json.obj(
        "service"      -> Json.arr("MTD-VAT"),
        "clientType"   -> "business",
        "clientIdType" -> "vrn",
        "clientId"     -> "123456789",
        "knownFact"    -> "2001-11-02"
      )

    "build the legacy MTD-IT trigger request from an organisation" in:
      EtProfileTestSupport.v2InvitationBody(
        validOrganisation,
        EtProfileTestSupport.V2InvitationTrigger.MtdIt
      ) shouldBe Json.obj(
        "service"      -> Json.arr("MTD-IT"),
        "clientType"   -> "personal",
        "clientIdType" -> "ni",
        "clientId"     -> "AB123456A",
        "knownFact"    -> "AA1 1AA",
        "agentType"    -> "main"
      )

  "v3TrustsInvitationBody" should:
    "build the V3 taxable Trusts request without a known fact" in:
      EtProfileTestSupport.v3TrustsInvitationBody(validOrganisation) shouldBe Json.obj(
        "service"  -> "TRUSTS",
        "clientId" -> "1234567890"
      )

  "mongoEvidenceFromConsole" should:
    "extract the protected JSON marker from a Jenkins console log" in:
      val console =
        s"""switched to db admin
          |1
          |[query-mongo] APB12584_RESULT=${Json.stringify(validMongoEvidence)}
          |bye
          |""".stripMargin

      EtProfileTestSupport.mongoEvidenceFromConsole(console) shouldBe validMongoEvidence

    "reject a console log without the result marker" in:
      val error =
        the[IllegalArgumentException] thrownBy EtProfileTestSupport.mongoEvidenceFromConsole("Finished: SUCCESS")

      error.getMessage should include("APB12584_RESULT=")

  "validateMongoEvidence" should:
    "confirm the organisation, required enrolments and V3 known facts" in:
      EtProfileTestSupport.validateMongoEvidence(validMongoEvidence).forall(_.passed) shouldBe true

    "identify a missing required enrolment and known fact" in:
      val invalid = validMongoEvidence ++ Json.obj(
        "user"    -> Json.obj("assignedPrincipalEnrolments" -> Seq("HMRC-MTD-VAT~VRN~123456789")),
        "records" -> Json.arr()
      )
      val checks = EtProfileTestSupport.validateMongoEvidence(invalid)

      checks.find(_.name == "HMRC-CBC-ORG").value.passed shouldBe false
      checks.find(_.name == "CBC known fact").value.passed shouldBe false

  "v3OrganisationScenarios" should:
    "build every Organisation request with the exact private known fact" in:
      val scenarios = EtProfileTestSupport.v3OrganisationScenarios(validOrganisation, validMongoEvidence)

      scenarios.map(_.slug) shouldBe Seq(
        "mtd-vat",
        "trusts-taxable",
        "trusts-nontaxable",
        "cgt-pd",
        "ppt",
        "cbc-uk",
        "pillar2"
      )
      scenarios.find(_.slug == "mtd-vat").value.requestBody shouldBe Json.obj(
        "service"   -> "MTD-VAT",
        "clientId"  -> "123456789",
        "knownFact" -> "2001-11-02"
      )
      scenarios.find(_.slug == "trusts-taxable").value.requestBody shouldBe Json.obj(
        "service"  -> "TRUSTS",
        "clientId" -> "1234567890"
      )
      scenarios.find(_.slug == "trusts-nontaxable").value.requestBody shouldBe Json.obj(
        "service"  -> "TRUSTS",
        "clientId" -> "AATRUST12345678"
      )
      scenarios.find(_.slug == "cgt-pd").value.requestBody shouldBe Json.obj(
        "service"   -> "CGT-PD",
        "clientId"  -> "XACGTP123456789",
        "knownFact" -> "AA1 1AA"
      )
      scenarios.find(_.slug == "ppt").value.requestBody shouldBe Json.obj(
        "service"   -> "PPT",
        "clientId"  -> "XAPPT0001234567",
        "knownFact" -> "2020-01-02"
      )
      scenarios.find(_.slug == "cbc-uk").value.requestBody shouldBe Json.obj(
        "service"   -> "CBC",
        "clientId"  -> "XACBC1234567890",
        "knownFact" -> "client@example.com"
      )
      scenarios.find(_.slug == "pillar2").value.requestBody shouldBe Json.obj(
        "service"   -> "PILLAR2",
        "clientId"  -> "XAPLR1234567890",
        "knownFact" -> "2021-02-03"
      )

    "reject Mongo evidence belonging to a different identifier" in:
      val wrongEvidence = validMongoEvidence ++ Json.obj(
        "records" -> Json.arr(
          Json.obj(
            "pptReference"       -> "XAPPT0007654321",
            "legalEntityDetails" -> Json.obj("dateOfApplication" -> "2020-01-02")
          )
        )
      )

      val error = the[IllegalArgumentException] thrownBy EtProfileTestSupport.v3OrganisationScenarios(
        validOrganisation,
        wrongEvidence
      )
      error.getMessage should include("pptReference")

  "oauthAuthoriseUrl" should:
    "include every V2 and V3 scope and the configured redirect URI" in:
      val url = EtProfileTestSupport.oauthAuthoriseUrl("client-id", "https://www.example.com/auth-redirect").toString

      url should include("client_id=client-id")
      url should include(
        "scope=write%3Asent-invitations+read%3Asent-invitations+write%3Acancel-invitations+read%3Acheck-relationship+write%3Arelationships"
      )
      url should include("redirect_uri=https%3A%2F%2Fwww.example.com%2Fauth-redirect")
