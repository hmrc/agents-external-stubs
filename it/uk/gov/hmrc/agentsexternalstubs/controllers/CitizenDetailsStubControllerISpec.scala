package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, NotAuthorized, ServerBaseISpec, TestRequests}

class CitizenDetailsStubControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "CitizenDetailsStubController" when {

    "GET /citizen-details/nino/:nino" should {
      "respond 200 with citizen data if found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(
              userId = session.userId,
              nino = "HW 82 78 56 C",
              name = "Alan Brian Foo-Foe",
              dateOfBirth = "1975-12-18"))

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result should haveStatus(200)
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "dateOfBirth").as[String] shouldBe "18121975"
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(
              userId = session.userId,
              nino = "JH 59 92 01 D",
              name = "Alan Brian Foo-Foe",
              dateOfBirth = "1975-12-18"))

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result should haveStatus(404)
      }

      "respond 400 if nino not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("nino", "W82785C")

        result should haveStatus(400)
      }

      "respond 400 if tax identifier type not supported" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")

        result should haveStatus(400)
      }

      "respond 401 if not authenticated" in {
        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")(NotAuthorized)

        result should haveStatus(401)
      }
    }

    "GET /citizen-details/:nino/designatory-details" should {
      "return user designatory details for Individuals" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val user = UserGenerator.individual(userId = session.userId)
        Users.update(user)

        val result = CitizenDetailsStub.getDesignatoryDetails(user.nino.get.value)

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[JsObject](
          "person",
          haveProperty[String]("firstName", be(user.firstName.get)) and haveProperty[String](
            "lastName",
            be(user.lastName.get)) and haveProperty[String]("nino", be(user.nino.get.value)) and haveProperty[String](
            "sex") and haveProperty[Boolean]("deceased")
        ) and haveProperty[JsObject](
          "address",
          haveProperty[String]("line1") and haveProperty[String]("postcode") and haveProperty[String]("country")
        ))
      }

      "return user designatory details for Agents" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val user = UserGenerator.agent(userId = session.userId)
        Users.update(user)

        val result = CitizenDetailsStub.getDesignatoryDetails(user.nino.get.value)

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[JsObject](
          "person",
          haveProperty[String]("firstName", be(user.firstName.get)) and haveProperty[String](
            "lastName",
            be(user.lastName.get)) and haveProperty[String]("nino", be(user.nino.get.value)) and haveProperty[String](
            "sex") and haveProperty[Boolean]("deceased")
        ) and haveProperty[JsObject](
          "address",
          haveProperty[String]("line1") and haveProperty[String]("postcode") and haveProperty[String]("country")
        ))
      }
    }

    "GET /citizen-details/:nino/designatory-details/basic" should {
      "return basic user details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val user = UserGenerator.individual(userId = session.userId)
        Users.update(user)

        val result = CitizenDetailsStub.getDesignatoryDetailsBasic(user.nino.get.value)

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[String](
          "firstName",
          be(user.firstName.get)) and haveProperty[String]("lastName", be(user.lastName.get)) and haveProperty[String](
          "nino",
          be(user.nino.get.value)) and haveProperty[Boolean]("deceased"))
      }
    }
  }
}
