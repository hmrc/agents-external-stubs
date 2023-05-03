package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class UsersControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val usersService = app.injector.instanceOf[UsersService]

  "UsersController" when {

    "GET /agents-external-stubs/users/:userId" should {
      "return 404 NotFound for non existent user id" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.get("1261a762761")
        result should haveStatus(404)
      }

      "return 404 NotFound if user exists but on a different planet" in {
        SignIn.signInAndGetSession()
        implicit val authSession2: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        val result = Users.get("foo")
        result should haveStatus(404)
      }

      "return an existing user" in {
        val session1 = SignIn.signInAndGetSession("712717287")
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession(planetId = session1.planetId)
        val result = Users.get("712717287")
        result should haveStatus(200)
        val user = result.json.as[User]
        user.userId shouldBe "712717287"
        user.planetId shouldBe Some(authSession.planetId)
      }
    }

    "POST /agents-external-stubs/users/" should {
      "store a new user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.create(User("yuwyquhh"), Some(AG.Individual))
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
      }

      "store a new STRIDE user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.create(User("stride").withStrideRole("FOO"), affinityGroup = None)
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/stride")
      }

      "fail if trying to store user with duplicated userId on the same planet" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result1 = Users.create(User("yuwyquhh"), Some(AG.Individual))
        result1 should haveStatus(201)
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
        val result2 = Users.create(User("yuwyquhh"), Some(AG.Individual))
        result2.status shouldBe Status.CONFLICT
      }

      "sanitize invalid user and succeed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.create(User("yuwyquhh", nino = Some(Nino("HW827856C"))), Some(AG.Individual))
        result should haveStatus(201)
      }

      "make user Admin if none exist in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = Users.create(
          UserGenerator.agent("bar", credentialRole = User.CR.User),
          affinityGroup = Some(AG.Agent)
        )
        result should haveStatus(201)
        Users.get(authSession.userId).json.as[User].credentialRole shouldBe Some(User.CR.Admin)
      }

      "return 400 if Admin exists already in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result1 = Users.create(
          UserGenerator.agent("bar", groupId = "testGroup", credentialRole = User.CR.Admin),
          affinityGroup = Some(AG.Agent)
        )
        result1 should haveStatus(201)
        val result2 = Users.create(
          UserGenerator.agent("baz", groupId = "testGroup", credentialRole = User.CR.Admin),
          affinityGroup = Some(AG.Agent)
        )
        result2 should haveStatus(400)
      }
    }

    "PUT /agents-external-stubs/users" should {
      "update current user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val currentUser = userService.findByUserId(authSession.userId, planetId = authSession.planetId).futureValue.get
        val result =
          Users.updateCurrent(currentUser.copy(name = Some("New Name")))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].name should contain("New Name")
      }
    }

    "POST /agents-external-stubs/users" should {
      "create a new user on the same planet as the logged-in user (if planetId is unspecified)" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val currentUser = userService.findByUserId(authSession.userId, planetId = authSession.planetId).futureValue.get
        val result = wsClient
          .url(s"$url/agents-external-stubs/users?affinityGroup=Individual")
          .withHttpHeaders(implicitly[AuthContext].headers: _*)
          .post(Json.toJson(User("foo")))
          .futureValue
        result should haveStatus(201)
        userService.findByUserId("foo", authSession.planetId).futureValue shouldBe defined
      }

      "create a new user on a different planet if one is specified" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = wsClient
          .url(s"$url/agents-external-stubs/users?affinityGroup=Individual&planetId=bar")
          .withHttpHeaders(implicitly[AuthContext].headers: _*)
          .post(Json.toJson(User("foo")))
          .futureValue
        result should haveStatus(201)
        userService.findByUserId("foo", authSession.planetId).futureValue shouldBe empty
        userService.findByUserId("foo", planetId = "bar").futureValue shouldBe defined
      }

      "create a new user on a specified planet even if the caller is unauthenticated" in {
        val result = wsClient
          .url(s"$url/agents-external-stubs/users?affinityGroup=Individual&planetId=bar")
          .withHttpHeaders(Seq.empty: _*) // no headers: unauthenticated
          .post(Json.toJson(User("foo")))
          .futureValue
        result should haveStatus(201)
        userService.findByUserId("foo", planetId = "bar").futureValue shouldBe defined
      }

      "not create a user if the caller is unauthenticated and planetId is not specified" in {
        val result = wsClient
          .url(s"$url/agents-external-stubs/users?affinityGroup=Individual")
          .withHttpHeaders(Seq.empty: _*) // no headers: unauthenticated
          .post(Json.toJson(User("foo")))
          .futureValue
        result should haveStatus(400)
      }
    }

    "PUT /agents-external-stubs/users/:userId" should {
      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.update(User("7728378273", name = Some("New Name")))
        result should haveStatus(404)
      }

      "update an existing user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val currentUser = userService.findByUserId(authSession.userId, planetId = authSession.planetId).futureValue.get
        val result = Users.update(currentUser.copy(name = Some("New Name")))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].name should contain("New Name")
      }
    }

    "DELETE /agents-external-stubs/users/:userId" should {
      "return 204 if user can be removed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.delete(authSession.userId)
        result should haveStatus(204)
      }

      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.delete("ABC123")
        result should haveStatus(404)
      }

      "return 400 if user can't be removed without breaking group constraint" in {
        userService
          .createUser(
            User("foo", groupId = Some("group1")),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.individual(userId = "bar", groupId = "group1", credentialRole = "User"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = Users.delete(authSession.userId)
        result should haveStatus(400)
      }
    }

    "GET /agents-external-stubs/users" should {
      "return 200 with the list of all users on the current planet only" in {
        val otherPlanetAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        Users.create(UserGenerator.individual("boo1"), Some(AG.Individual))(otherPlanetAuthSession)
        Users.create(UserGenerator.organisation("boo2"), Some(AG.Organisation))(otherPlanetAuthSession)

        implicit val currentAuthSession: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.create(UserGenerator.individual("foo1"), Some(AG.Individual))
        Users.create(UserGenerator.organisation("foo2"), Some(AG.Organisation))
        Users.create(UserGenerator.agent("foo3"), Some(AG.Agent))

        val result1 = Users.getAll()
        result1 should haveStatus(200)
        val users1 = result1.json.as[Users].users
        users1.size shouldBe 4
        users1.map(_.userId) should contain.only(currentAuthSession.userId, "foo1", "foo2", "foo3")

        val result2 = Users.getAll()(otherPlanetAuthSession)
        result2 should haveStatus(200)
        val users2 = result2.json.as[Users].users
        users2.size shouldBe 3
        users2.map(_.userId) should contain.only("boo", "boo1", "boo2")
      }

      "return 200 with the list of users having given affinity" in {
        val planetId = "testPlanet"

        userService.createUser(UserGenerator.individual("foo1"), planetId = planetId, Some(AG.Individual)).futureValue
        userService.createUser(UserGenerator.individual("foo2"), planetId = planetId, Some(AG.Organisation)).futureValue
        userService.createUser(UserGenerator.individual("foo3"), planetId = planetId, Some(AG.Agent)).futureValue

        implicit val currentAuthSession: AuthenticatedSession =
          SignIn.signInAndGetSession(userId = "foo1", planetId = planetId)

        val result1 = Users.getAll(affinityGroup = Some("Agent"))
        result1 should haveStatus(200)
        val users1 = result1.json.as[Users].users
        users1.size shouldBe 1
        users1.map(_.userId) should contain.only("foo3")

        val result2 = Users.getAll(affinityGroup = Some("Individual"))
        result2 should haveStatus(200)
        val users2 = result2.json.as[Users].users
        users2.size shouldBe 1
        users2.map(_.userId) should contain.only("foo1")
      }
    }

    "POST /agents-external-stubs/users/api-platform" should {
      "store a new individual user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val userId = "apitestuser"
        val utr = Generator.utr(userId)
        val nino = Generator.ninoNoSpaces(userId)
        val payload = Json.parse(s"""{
          |   "userId": "$userId",
          |   "userFullName": "API Test User",
          |   "emailAddress": "user@api.gov.uk",
          |   "affinityGroup": "Individual",
          |   "saUtr": "$utr",
          |   "nino": "$nino",
          |   "individualDetails": {
          |      "firstName": "Test",
          |      "lastName": "User",
          |      "dateOfBirth": "1972-12-23",
          |      "address": {
          |        "line1": "11 High Lane",
          |        "line2": "Croydon",
          |        "postcode": "CR12 3XZ"
          |      }
          |   },
          |   "services": [
          |      "self-assessment"
          |   ]
          |}
      """.stripMargin)

        val result = Users.createApiPlatformTestUser(payload)

        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/apitestuser")
        val user = Users.get(userId).json.as[User]
        val group = Groups.get(user.groupId.get).json.as[Group]
        user.nino shouldBe Some(nino)
        user.dateOfBirth shouldBe Some(LocalDate.parse("1972-12-23"))
        user.name shouldBe Some("Test User")
        group.affinityGroup shouldBe AG.Individual
        group.principalEnrolments should contain(Enrolment("IR-SA", "UTR", utr))
        user.facts("businesspostcode") shouldBe Some("CR12 3XZ")
      }
    }

    "store a new organisation user" in {
      implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
      val userId = "apitestuser"
      val utr = Generator.utr(userId)
      val eori = Generator.eori(userId)
      val vrn = Generator.vrn(userId).value
      val payload = Json.parse(s"""{
        |   "userId": "$userId",
        |   "userFullName": "API Test User",
        |   "emailAddress": "user@api.gov.uk",
        |   "affinityGroup": "Organisation",
        |   "ctUtr": "$utr",
        |   "eoriNumber": "$eori",
        |   "vrn": "$vrn",
        |   "organisationDetails": {
        |      "name": "Test Organisation User",
        |      "address": {
        |        "line1": "11 High Lane",
        |        "line2": "Croydon",
        |        "postcode": "CR12 3XZ"
        |      }
        |   },
        |   "services": [
        |      "corporation-tax",
        |      "customs-services",
        |      "mtd-vat",
        |      "submit-vat-returns"
        |   ]
        |}
      """.stripMargin)

      val result = Users.createApiPlatformTestUser(payload)

      result should haveStatus(201)
      result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/apitestuser")
      val (Some(user), Some(group)) = userService.findUserAndGroup(userId, planetId = authSession.planetId).futureValue
      group.affinityGroup shouldBe AG.Organisation
      user.nino shouldBe None
      user.dateOfBirth shouldBe None
      user.name shouldBe Some("Test Organisation User")
      group.principalEnrolments should contain(Enrolment("IR-CT", "UTR", utr))
      group.principalEnrolments should contain(Enrolment("HMRC-CUS-ORG", "EORINumber", eori))
      group.principalEnrolments should contain(Enrolment("HMRC-MTD-VAT", "VRN", vrn))
      group.principalEnrolments should contain(Enrolment("HMCE-VATDEC-ORG", "VATRegNo", vrn))
      user.facts("Postcode") shouldBe Some("CR12 3XZ")
    }

    "store a new agent user" in {
      val userId = "apitestuser"
      val arn = Generator.arn(userId).value
      val payload = Json.parse(s"""{
        |   "userId": "$userId",
        |   "userFullName": "API Test User",
        |   "emailAddress": "user@api.gov.uk",
        |   "affinityGroup": "Agent",
        |   "arn": "$arn",
        |   "services": [
        |      "agent-services"
        |   ]
        |}
      """.stripMargin)

      {
        implicit val authContext: AuthContext = NotAuthorized.withHeader("X-Client-ID", "fooClientId")
        val result = Users.createApiPlatformTestUser(payload)

        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/apitestuser")
      }
      {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession(planetId = Planet.DEFAULT)
        val (Some(user), Some(group)) =
          userService.findUserAndGroup(userId, planetId = authSession.planetId).futureValue
        group.affinityGroup shouldBe AG.Agent
        user.nino shouldBe Some(Nino("WZ 58 73 41 D"))
        user.dateOfBirth shouldBe defined
        user.name shouldBe Some("API Test User")
        group.principalEnrolments should contain.only(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn))
      }
    }
  }

  "POST /agents-external-stubs/users/re-index" should {
    "re-index all existing users" in {
      implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
      val result = Users.reindexAllUsers
      result.status shouldBe 200
      result.body should include("true")
    }
  }
}
