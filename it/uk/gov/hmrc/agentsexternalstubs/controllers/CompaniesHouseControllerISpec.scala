package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support._

class CompaniesHouseControllerISpec extends ServerBaseISpec with MongoDB with TestRequests {

  val url = s"http://localhost:$port"
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "Calling" when {

    "GET /companies-house-api-proxy/company/:companynumber" should {
      "respond 200 with company details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val companyNumber = "01234567"

        val result = get(s"/companies-house-api-proxy/company/$companyNumber")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("company_number", be(companyNumber))
            and haveProperty[String]("company_status", be("active"))
            and haveProperty[String]("company_name")
        )
      }
    }

    "GET /companies-house-api-proxy/company/:companynumber/officers" should {
      "respond 200 with company details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val companyNumber = "01234567"
        val surname = "Norton"

        val result = get(s"/companies-house-api-proxy/company/$companyNumber/officers?surname=$surname")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Seq[JsObject]](
            "items",
            eachElement(haveProperty[String]("name", be(s"${surname.toUpperCase}, Jane")))
          ) and haveProperty[JsObject](
            "links",
            haveProperty[String]("self", be(s"/company/$companyNumber/appointments"))
          )
        )
      }
    }

  }
}
