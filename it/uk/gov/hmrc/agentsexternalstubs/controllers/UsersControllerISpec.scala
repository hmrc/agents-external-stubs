package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, User}
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class UsersControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  val usersService = app.injector.instanceOf[UsersService]

  "UsersController" when {

    "GET /agents-external-stubs/users/:userId" should {
      "return 404 NotFound for non existent user id" in {
        val result = Users.get("1261a762761")
        result.status shouldBe 404
      }

      "return an existing user" in {
        givenAnAuthenticatedUser("712717287")
        val result = Users.get("712717287")
        result.status shouldBe 200
        result.json.as[User] shouldBe User("712717287")
      }
    }

    "POST /agents-external-stubs/users/" should {
      "create a new user" in {
        val result = Users.create(User("yuwyquhh"))
        result.status shouldBe 201
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
      }

      "fail if trying to create duplicated user" in {
        val result1 = Users.create(User("yuwyquhh"))
        result1.status shouldBe 201
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
        val result2 = Users.create(User("yuwyquhh"))
        result2.status shouldBe Status.CONFLICT
      }
    }

    "PUT /agents-external-stubs/users/:userId" should {
      "return 404 if userId not found" in {
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result.status shouldBe 404
      }

      "update an existing user" in {
        givenAnAuthenticatedUser("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result.status shouldBe 202
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].principalEnrolments should contain(Enrolment("foo"))
      }
    }
  }
}
