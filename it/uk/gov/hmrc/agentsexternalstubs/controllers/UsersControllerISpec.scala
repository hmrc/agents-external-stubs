package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Enrolment, User}
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}
import uk.gov.hmrc.domain.Nino

class UsersControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val wsClient = app.injector.instanceOf[WSClient]
  val usersService = app.injector.instanceOf[UsersService]

  "UsersController" when {

    "GET /agents-external-stubs/users/:userId" should {
      "return 404 NotFound for non existent user id" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.get("1261a762761")
        result.status shouldBe 404
      }

      "return 404 NotFound if user exists but on a different planet" in {
        SignIn.signInAndGetSession("foo", planetId = "saturn")
        implicit val authSession2: AuthenticatedSession = SignIn.signInAndGetSession("boo", planetId = "x123")
        val result = Users.get("foo")
        result.status shouldBe 404
      }

      "return an existing user" in {
        SignIn.signInAndGetSession("712717287", planetId = "saturn")
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "saturn")
        val result = Users.get("712717287")
        result.status shouldBe 200
        result.json.as[User] shouldBe User("712717287", planetId = Some("saturn"))
      }
    }

    "POST /agents-external-stubs/users/" should {
      "create a new user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.create(User("yuwyquhh"))
        result.status shouldBe 201
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
      }

      "fail if trying to create user with duplicated userId on the same planet" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result1 = Users.create(User("yuwyquhh"))
        result1.status shouldBe 201
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
        val result2 = Users.create(User("yuwyquhh"))
        result2.status shouldBe Status.CONFLICT
      }

      "fail if trying to create invalid user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.create(User("yuwyquhh", nino = Some(Nino("HW827856C"))))
        result.status shouldBe 400
      }
    }

    "PUT /agents-external-stubs/users/:userId" should {
      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result.status shouldBe 404
      }

      "update an existing user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result.status shouldBe 202
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].principalEnrolments should contain(Enrolment("foo"))
      }
    }
  }
}
