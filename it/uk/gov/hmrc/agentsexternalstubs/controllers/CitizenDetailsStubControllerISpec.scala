package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, NotAuthorized, ServerBaseISpec, TestRequests}

class CitizenDetailsStubControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "CitizenDetailsStubController" when {

    "GET /citizen-details/nino/:nino" should {
      "respond 200 with citizen data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(UserGenerator
          .individual(userId = "foo", nino = "HW 82 78 56 C", name = "Alan Brian Foo-Foe", dateOfBirth = "1975-12-18"))

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result.status shouldBe 200
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "dateOfBirth").as[String] shouldBe "18121975"
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(UserGenerator
          .individual(userId = "foo", nino = "JH 59 92 01 D", name = "Alan Brian Foo-Foe", dateOfBirth = "1975-12-18"))

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
