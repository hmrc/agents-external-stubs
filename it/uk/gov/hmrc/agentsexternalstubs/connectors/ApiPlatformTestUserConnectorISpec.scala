package uk.gov.hmrc.agentsexternalstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests, WireMockSupport}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

class ApiPlatformTestUserConnectorISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with WireMockSupport {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpGet = app.injector.instanceOf[HttpGet]
  lazy val connector = new ApiPlatformTestUserConnector(wireMockBaseUrl, httpGet)

  "ApiPlatformTestUserConnector" when {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    "looking for an individual with nino" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/nino/PE938808A"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testIndividualResponse)))

        val result: Option[TestUser] = await(connector.getIndividualUserByNino("PE938808A"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.individualDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.organisationDetails) shouldBe None

        val user = result.map(TestUser.asUser).get
        user.userId shouldBe "945350439195"
        user.firstName shouldBe Some("Ida")
        user.lastName shouldBe Some("Newton")
        user.nino shouldBe Some(Nino("PE938808A"))
        user.principalEnrolments.find(_.key == "HMRC-MTD-VAT").flatMap(_.identifierValueOf("VRN")) shouldBe Some(
          "666119668")
        user.principalEnrolments.find(_.key == "HMRC-MTD-IT").flatMap(_.identifierValueOf("MTDITID")) shouldBe Some(
          "XJIT00000328268")
        user.affinityGroup shouldBe Some("Individual")
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/nino/PE938808A"))
            .willReturn(aResponse()
              .withStatus(404)))

        val result = await(connector.getIndividualUserByNino("PE938808A"))

        result shouldBe None
      }
    }

    "looking for an individual with vrn" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/vrn/666119668"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testIndividualResponse)))

        val result: Option[TestUser] = await(connector.getIndividualUserByVrn("666119668"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.individualDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.organisationDetails) shouldBe None

        val user = result.map(TestUser.asUser).get
        user.userId shouldBe "945350439195"
        user.firstName shouldBe Some("Ida")
        user.lastName shouldBe Some("Newton")
        user.nino shouldBe Some(Nino("PE938808A"))
        user.principalEnrolments.find(_.key == "HMRC-MTD-VAT").flatMap(_.identifierValueOf("VRN")) shouldBe Some(
          "666119668")
        user.principalEnrolments.find(_.key == "HMRC-MTD-IT").flatMap(_.identifierValueOf("MTDITID")) shouldBe Some(
          "XJIT00000328268")
        user.affinityGroup shouldBe Some("Individual")
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/vrn/666119668"))
            .willReturn(aResponse()
              .withStatus(404)))

        val result = await(connector.getIndividualUserByVrn("666119668"))

        result shouldBe None
      }
    }

    "looking for an organisation with vrn" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/organisations/vrn/666119668"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(testOrganisationResponse)))

        val result: Option[TestUser] = await(connector.getOrganisationUserByVrn("666119668"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.organisationDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.individualDetails) shouldBe None

        val user = result.map(TestUser.asUser).get
        user.userId shouldBe "085603622877"
        user.name shouldBe Some("Company ABF123")
        user.affinityGroup shouldBe Some("Organisation")
        user.principalEnrolments.find(_.key == "HMRC-MTD-VAT").flatMap(_.identifierValueOf("VRN")) shouldBe Some(
          "666119668")
        user.principalEnrolments.find(_.key == "HMRC-MTD-IT").flatMap(_.identifierValueOf("MTDITID")) shouldBe None
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/organisations/vrn/666119668"))
            .willReturn(aResponse()
              .withStatus(404)))

        val result = await(connector.getOrganisationUserByVrn("666119668"))

        result shouldBe None
      }
    }
  }

  val testIndividualResponse =
    s"""
       |{
       |  "userId":"945350439195",
       |  "password":"bLohysg8utsa",
       |  "userFullName": "Ida Newton",
       |  "emailAddress": "ida.newton@example.com",
       |  "individualDetails": {
       |    "firstName": "Ida",
       |    "lastName": "Newton",
       |    "dateOfBirth": "1960-06-01",
       |    "address": {
       |      "line1": "45 Springfield Rise",
       |      "line2": "Glasgow",
       |      "postcode": "TS1 1PA"
       |    }
       |  },
       |  "saUtr":"1000057161",
       |  "nino":"PE938808A",
       |  "mtdItId":"XJIT00000328268",
       |  "vrn":"666119668",
       |  "vatRegistrationDate":"2001-11-02",
       |  "eoriNumber":"GB1234567890"
       |}
                 """.stripMargin

  val testOrganisationResponse =
    s"""
       |{
       |  "userId":"085603622877",
       |  "password":"nyezgdfrlsmc",
       |  "userFullName": "Ida Newton",
       |  "emailAddress": "ida.newton@example.com",
       |  "organisationDetails": {
       |    "name":"Company ABF123",
       |    "address": {
       |      "line1":"1 Abbey Road",
       |      "line2":"Aberdeen",
       |      "postcode": "TS1 1PA"
       |    }
       |  },
       |  "saUtr":"8000083480",
       |  "empRef":"538/EMKXYNSVTH",
       |  "ctUtr":"4000082459",
       |  "vrn":"666119668",
       |  "vatRegistrationDate":"2001-11-02",
       |  "lisaManagerReferenceNumber":"Z123456",
       |  "secureElectronicTransferReferenceNumber":"123456789012",
       |  "pensionSchemeAdministratorIdentifier":"A1234567",
       |  "eoriNumber":"GB1234567890"
       |}
     """.stripMargin
}
