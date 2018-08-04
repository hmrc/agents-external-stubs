package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{NotAuthorized, ServerBaseISpec, TestRequests}

class CitizenDetailsStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "AuthStubController" when {

    "GET /citizen-details/nino/:nino" should {
      "respond 200 with citizen data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(User.individual("foo", 50, "HW 82 78 56 C"))

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result.status shouldBe 200
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(User.individual("foo", 50, "JH 59 92 01 D"))

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result.status shouldBe 404
      }

      "respond 400 if nino not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = CitizenDetailsStub.getCitizen("nino", "W82785C")

        result.status shouldBe 400
      }

      "respond 400 if tax identifier type not supported" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")

        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")

        result.status shouldBe 400
      }

      "respond 401 if not authenticated" in {
        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")(NotAuthorized)

        result.status shouldBe 401
      }
    }
  }
}
