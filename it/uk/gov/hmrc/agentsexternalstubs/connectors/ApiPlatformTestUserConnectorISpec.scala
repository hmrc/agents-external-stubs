package uk.gov.hmrc.agentsexternalstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.models.Generator
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests, WireMockSupport}
import uk.gov.hmrc.domain.{Nino, Vrn}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

class ApiPlatformTestUserConnectorISpec
    extends ServerBaseISpec with TestRequests with TestStubs with WireMockSupport
    with ExampleApiPlatformTestUserResponses {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpGet = app.injector.instanceOf[HttpGet]
  lazy val connector = new ApiPlatformTestUserConnector(TestAppConfig(wireMockBaseUrlAsString, wireMockPort), httpGet)

  "ApiPlatformTestUserConnector" when {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    "looking for an individual with nino" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/nino/PE938808A"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(testIndividualResponse(nino = Nino("PE938808A"), vrn = Vrn("666119668")))
            )
        )

        val result: Option[TestUser] = await(connector.getIndividualUserByNino("PE938808A"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.individualDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.organisationDetails) shouldBe None

        val (user, _) = result.map(TestUser.asUserAndGroup).get
        user.firstName shouldBe Some("Ida")
        user.lastName shouldBe Some("Newton")
        user.nino shouldBe Some(Nino("PE938808A"))
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/nino/PE938808A"))
            .willReturn(
              aResponse()
                .withStatus(404)
            )
        )

        val result = await(connector.getIndividualUserByNino("PE938808A"))

        result shouldBe None
      }
    }

    "looking for an individual with vrn" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/vrn/666119668"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(testIndividualResponse(nino = Nino("PE938808A"), vrn = Vrn("666119668")))
            )
        )

        val result: Option[TestUser] = await(connector.getIndividualUserByVrn("666119668"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.individualDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.organisationDetails) shouldBe None

        val (user, _) = result.map(TestUser.asUserAndGroup).get
        user.firstName shouldBe Some("Ida")
        user.lastName shouldBe Some("Newton")
        user.nino shouldBe Some(Nino("PE938808A"))
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/individuals/vrn/666119668"))
            .willReturn(
              aResponse()
                .withStatus(404)
            )
        )

        val result = await(connector.getIndividualUserByVrn("666119668"))

        result shouldBe None
      }
    }

    "looking for an organisation with vrn" should {
      "returns some test user if exists" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/organisations/vrn/666119668"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(testOrganisationResponse(Vrn("666119668")))
            )
        )

        val result: Option[TestUser] = await(connector.getOrganisationUserByVrn("666119668"))

        result shouldBe defined
        result.map(_.userFullName) shouldBe Some("Ida Newton")
        result.flatMap(_.vrn) shouldBe Some("666119668")
        result.flatMap(_.organisationDetails.map(_.address.postcode)) shouldBe Some("TS1 1PA")
        result.flatMap(_.individualDetails) shouldBe None

        val (user, _) = result.map(TestUser.asUserAndGroup).get
        user.name shouldBe Some("Company ABF123")
      }

      "return none if missing" in {
        WireMock.stubFor(
          WireMock
            .get(urlEqualTo("/organisations/vrn/666119668"))
            .willReturn(
              aResponse()
                .withStatus(404)
            )
        )

        val result = await(connector.getOrganisationUserByVrn("666119668"))

        result shouldBe None
      }
    }
  }
}

trait ExampleApiPlatformTestUserResponses {

  def testIndividualResponse(
    nino: Nino = Generator.ninoNoSpacesGen.sample.map(Nino.apply).get,
    utr: Utr = Generator.utrGen.sample.map(Utr.apply).get,
    vrn: Vrn = Generator.vrnGen.sample.map(Vrn.apply).get
  ): String =
    s"""
      |{
      |  "userId":"${Generator.userID.sample.get}",
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
      |  "saUtr":"${utr.value}",
      |  "nino":"${nino.value}",
      |  "mtdItId":"${Generator.mtdbsaGen.sample.get}",
      |  "vrn":"${vrn.value}",
      |  "vatRegistrationDate":"2001-11-02",
      |  "eoriNumber":"${Generator.eoriGen.sample.get}"
      |}
                 """.stripMargin

  def testOrganisationResponse(
    vrn: Vrn = Generator.vrnGen.sample.map(Vrn.apply).get,
    utr: Utr = Generator.utrGen.sample.map(Utr.apply).get
  ): String =
    s"""
      |{
      |  "userId":"${Generator.userID.sample.get}",
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
      |  "saUtr":"${utr.value}",
      |  "empRef":"538/EMKXYNSVTH",
      |  "ctUtr":"${utr.value}",
      |  "vrn":"${vrn.value}",
      |  "vatRegistrationDate":"2001-11-02",
      |  "lisaManagerReferenceNumber":"Z123456",
      |  "secureElectronicTransferReferenceNumber":"123456789012",
      |  "pensionSchemeAdministratorIdentifier":"A1234567",
      |  "eoriNumber":"${Generator.eoriGen.sample.get}"
      |}
     """.stripMargin
}
