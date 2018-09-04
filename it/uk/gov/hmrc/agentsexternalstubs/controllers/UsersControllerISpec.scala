package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.Status
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}
import uk.gov.hmrc.domain.Nino

class UsersControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val usersService = app.injector.instanceOf[UsersService]

  "UsersController" when {

    "GET /agents-external-stubs/users/:userId" should {
      "return 404 NotFound for non existent user id" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.get("1261a762761")
        result should haveStatus(404)
      }

      "return 404 NotFound if user exists but on a different planet" in {
        SignIn.signInAndGetSession("foo")
        implicit val authSession2: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        val result = Users.get("foo")
        result should haveStatus(404)
      }

      "return an existing user" in {
        val session1 = SignIn.signInAndGetSession("712717287")
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = session1.planetId)
        val result = Users.get("712717287")
        result should haveStatus(200)
        val user = result.json.as[User]
        user.userId shouldBe "712717287"
        user.planetId shouldBe Some(authSession.planetId)
      }
    }

    "POST /agents-external-stubs/users/" should {
      "store a new user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.create(User("yuwyquhh"))
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
      }

      "fail if trying to store user with duplicated userId on the same planet" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result1 = Users.create(User("yuwyquhh"))
        result1 should haveStatus(201)
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
        val result2 = Users.create(User("yuwyquhh"))
        result2.status shouldBe Status.CONFLICT
      }

      "sanitize invalid user and succeed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.create(User("yuwyquhh", nino = Some(Nino("HW827856C"))))
        result should haveStatus(201)
      }

      "make user Admin if none exist in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.update(UserGenerator.agent("foo", credentialRole = User.CR.User))
        result should haveStatus(202)
        Users.get("foo").json.as[User].credentialRole shouldBe Some(User.CR.Admin)
      }

      "return 400 if Admin exists already in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result1 = Users.update(UserGenerator.agent("foo", groupId = "group1", credentialRole = User.CR.User))
        result1 should haveStatus(202)
        Users.get("foo").json.as[User].credentialRole shouldBe Some(User.CR.Admin)
        val result2 = Users.create(UserGenerator.agent("bar", groupId = "group1", credentialRole = User.CR.Admin))
        result2 should haveStatus(400)
      }
    }

    "PUT /agents-external-stubs/users/:userId" should {
      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result should haveStatus(404)
      }

      "update an existing user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", principalEnrolments = Seq(Enrolment("foo"))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].principalEnrolments should contain(Enrolment("foo"))
      }
    }

    "DELETE /agents-external-stubs/users/:userId" should {
      "return 204 if user can be removed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.delete("foo")
        result should haveStatus(204)
      }

      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.delete("ABC123")
        result should haveStatus(404)
      }

      "return 400 if user can't be removed without breaking group constraint" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(User("foo", groupId = Some("group1")))
        Users.create(UserGenerator.individual(userId = "bar", groupId = "group1", credentialRole = "User"))
        val result = Users.delete("foo")
        result should haveStatus(400)
      }
    }

    "GET /agents-external-stubs/users" should {
      "return 200 with the list of all users on the current planet only" in {
        val otherPlanetAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        Users.create(UserGenerator.individual("boo1"))(otherPlanetAuthSession)
        Users.create(UserGenerator.organisation("boo2"))(otherPlanetAuthSession)

        implicit val currentAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.individual("foo1"))
        Users.create(UserGenerator.organisation("foo2"))
        Users.create(UserGenerator.agent("foo3"))

        val result1 = Users.getAll()
        result1 should haveStatus(200)
        val users1 = result1.json.as[Users].users
        users1.size shouldBe 4
        users1.map(_.userId) should contain.only("foo", "foo1", "foo2", "foo3")
        users1.flatMap(_.affinityGroup) should contain.only("Individual", "Agent", "Organisation")

        val result2 = Users.getAll()(otherPlanetAuthSession)
        result2 should haveStatus(200)
        val users2 = result2.json.as[Users].users
        users2.size shouldBe 3
        users2.map(_.userId) should contain.only("boo", "boo1", "boo2")
        users2.flatMap(_.affinityGroup) should contain.only("Individual", "Organisation")
      }

      "return 200 with the list of users having given affinity" in {
        implicit val currentAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.individual("foo1"))
        Users.create(UserGenerator.organisation("foo2"))
        Users.create(UserGenerator.agent("foo3"))

        val result1 = Users.getAll(affinityGroup = Some("Agent"))
        result1 should haveStatus(200)
        val users1 = result1.json.as[Users].users
        users1.size shouldBe 1
        users1.map(_.userId) should contain.only("foo3")
        users1.flatMap(_.affinityGroup) should contain.only("Agent")

        val result2 = Users.getAll(affinityGroup = Some("Individual"))
        result2 should haveStatus(200)
        val users2 = result2.json.as[Users].users
        users2.size shouldBe 1
        users2.map(_.userId) should contain.only("foo1")
        users2.flatMap(_.affinityGroup) should contain.only("Individual")
      }
    }
  }
}
