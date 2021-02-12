package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.{Organisation, UkAddress}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, BusinessPartnerRecord, Generator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}

class NiExemptionRegistrationStubControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "NiExemptionRegistrationStub" should {
    "return 200 with subscribed NI business details with eori" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val seed = "foo"
      val utr = Generator.utr(seed)
      val eori = Generator.eori(seed)
      val business = Organisation.seed(seed)
      val address = UkAddress.seed(seed)

      val createResult = Records.createBusinessPartnerRecord(
        BusinessPartnerRecord
          .seed(seed)
          .withUtr(Some(utr))
          .withEori(Some(eori))
          .withAddressDetails(address)
          .withIsAnIndividual(false)
          .withIsAnOrganisation(true)
          .withOrganisation(Some(business))
      )
      createResult should haveStatus(201)

      val result =
        NiExemptionRegistrationStubs.niBusinesses(utr, Json.parse(s"""{"postcode":"${address.postalCode}"}"""))

      result should haveStatus(200)
      result should haveValidJsonBody(
        haveProperty[String]("name", be(business.organisationName)) and haveProperty[JsObject](
          "subscription",
          haveProperty[String]("status", be("NI_SUBSCRIBED")) and haveProperty[String]("eori", be(eori))
        )
      )
    }

    "return 200 with not_subscribed NI business details without eori" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val seed = "foo"
      val utr = Generator.utr(seed)
      val business = Organisation.seed(seed)
      val address = UkAddress.seed(seed)

      val createResult = Records.createBusinessPartnerRecord(
        BusinessPartnerRecord
          .seed(seed)
          .withUtr(Some(utr))
          .withEori(None)
          .withAddressDetails(address)
          .withIsAnIndividual(false)
          .withIsAnOrganisation(true)
          .withOrganisation(Some(business)),
        autoFill = false
      )
      createResult should haveStatus(201)

      val result =
        NiExemptionRegistrationStubs.niBusinesses(utr, Json.parse(s"""{"postcode":"${address.postalCode}"}"""))

      result should haveStatus(200)
      result should haveValidJsonBody(
        haveProperty[String]("name", be(business.organisationName)) and haveProperty[JsObject](
          "subscription",
          notHaveProperty("eori") and haveProperty[String]("status", be("NI_NOT_SUBSCRIBED"))
        )
      )
    }

    "return 409 when postcode does not match" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val seed = "foo"
      val utr = Generator.utr(seed)
      val business = Organisation.seed(seed)
      val address = UkAddress.seed(seed)

      val createResult = Records.createBusinessPartnerRecord(
        BusinessPartnerRecord
          .seed(seed)
          .withUtr(Some(utr))
          .withEori(None)
          .withAddressDetails(address)
          .withIsAnIndividual(false)
          .withIsAnOrganisation(true)
          .withOrganisation(Some(business)),
        autoFill = false
      )
      createResult should haveStatus(201)

      val result =
        NiExemptionRegistrationStubs.niBusinesses(utr, Json.parse(s"""{"postcode":"BN14 7BU"}"""))

      result should haveStatus(409)
    }

    "return 409 when business partner record does not exist" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val seed = "foo"
      val utr = Generator.utr(seed)

      val result =
        NiExemptionRegistrationStubs.niBusinesses(utr, Json.parse(s"""{"postcode":"BN14 7BU"}"""))

      result should haveStatus(409)
    }

    "return 409 when utr is invalid" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val utr = "ab12345678"

      val result =
        NiExemptionRegistrationStubs.niBusinesses(utr, Json.parse(s"""{"postcode":"BN14 7BU"}"""))

      result should haveStatus(409)
    }
  }
}
