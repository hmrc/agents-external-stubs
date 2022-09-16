package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino

class UsersControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"

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
        val result = Users.create(User("yuwyquhh"))
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
      }

      "store a new STRIDE user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.create(User("stride").withStrideRole("FOO"))
        result should haveStatus(201)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/stride")
      }

      "fail if trying to store user with duplicated userId on the same planet" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result1 = Users.create(User("yuwyquhh"))
        result1 should haveStatus(201)
        result1.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/yuwyquhh")
        val result2 = Users.create(User("yuwyquhh"))
        result2.status shouldBe Status.CONFLICT
      }

      "sanitize invalid user and succeed" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.create(User("yuwyquhh", nino = Some(Nino("HW827856C"))))
        result should haveStatus(201)
      }

      "make user Admin if none exist in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.update(UserGenerator.agent(authSession.userId, credentialRole = User.CR.User))
        result should haveStatus(202)
        Users.get(authSession.userId).json.as[User].credentialRole shouldBe Some(User.CR.Admin)
      }

      "return 400 if Admin exists already in the group" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result1 =
          Users.update(UserGenerator.agent(authSession.userId, groupId = "group1", credentialRole = User.CR.User))
        result1 should haveStatus(202)
        Users.get(authSession.userId).json.as[User].credentialRole shouldBe Some(User.CR.Admin)
        val result2 = Users.create(UserGenerator.agent("bar", groupId = "group1", credentialRole = User.CR.Admin))
        result2 should haveStatus(400)
      }
    }

    "PUT /agents-external-stubs/users" should {
      "update current user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result =
          Users.updateCurrent(User("7728378273", enrolments = User.Enrolments(principal = Seq(Enrolment("foo")))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].enrolments.principal should contain(Enrolment("foo"))
      }

      "update current user with a legacy principalEnrolments" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378276")
        val userJson = Json.obj(
          "userId" -> "7728378276",
          "principalEnrolments" -> Json.arr(
            Json.obj("key" -> "foo1")
          )
        )
        val result = Users.updateCurrentLegacy(userJson)
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378276")
        val result2 = Users.get("7728378276")
        result2.json.as[User].enrolments.principal should contain(Enrolment("foo1"))
      }
    }

    "PUT /agents-external-stubs/users/:userId" should {
      "return 404 if userId not found" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = Users.update(User("7728378273", enrolments = User.Enrolments(principal = Seq(Enrolment("foo")))))
        result should haveStatus(404)
      }

      "update an existing user" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("7728378273")
        val result = Users.update(User("7728378273", enrolments = User.Enrolments(principal = Seq(Enrolment("foo")))))
        result should haveStatus(202)
        result.header(HeaderNames.LOCATION) shouldBe Some("/agents-external-stubs/users/7728378273")
        val result2 = Users.get("7728378273")
        result2.json.as[User].enrolments.principal should contain(Enrolment("foo"))
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
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(User(authSession.userId, groupId = Some("group1")))
        Users.create(UserGenerator.individual(userId = "bar", groupId = "group1", credentialRole = "User"))
        val result = Users.delete(authSession.userId)
        result should haveStatus(400)
      }
    }

    "GET /agents-external-stubs/users" should {
      "return 200 with the list of all users on the current planet only" in {
        val otherPlanetAuthSession: AuthenticatedSession = SignIn.signInAndGetSession("boo")
        Users.create(UserGenerator.individual("boo1"))(otherPlanetAuthSession)
        Users.create(UserGenerator.organisation("boo2"))(otherPlanetAuthSession)

        implicit val currentAuthSession: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.create(UserGenerator.individual("foo1"))
        Users.create(UserGenerator.organisation("foo2"))
        Users.create(UserGenerator.agent("foo3"))

        val result1 = Users.getAll()
        result1 should haveStatus(200)
        val users1 = result1.json.as[Users].users
        users1.size shouldBe 4
        users1.map(_.userId) should contain.only(currentAuthSession.userId, "foo1", "foo2", "foo3")
        users1.flatMap(_.affinityGroup) should contain.only("Individual", "Agent", "Organisation")

        val result2 = Users.getAll()(otherPlanetAuthSession)
        result2 should haveStatus(200)
        val users2 = result2.json.as[Users].users
        users2.size shouldBe 3
        users2.map(_.userId) should contain.only("boo", "boo1", "boo2")
        users2.flatMap(_.affinityGroup) should contain.only("Individual", "Organisation")
      }

      "return 200 with the list of users having given affinity" in {
        implicit val currentAuthSession: AuthenticatedSession = SignIn.signInAndGetSession()
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
        user.affinityGroup shouldBe Some(User.AG.Individual)
        user.nino shouldBe Some(nino)
        user.dateOfBirth shouldBe Some(LocalDate.parse("1972-12-23"))
        user.name shouldBe Some("Test User")
        user.enrolments.principal should contain(Enrolment("IR-SA", "UTR", utr))
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
      val user = Users.get(userId).json.as[User]
      user.affinityGroup shouldBe Some(User.AG.Organisation)
      user.nino shouldBe None
      user.dateOfBirth shouldBe None
      user.name shouldBe Some("Test Organisation User")
      user.enrolments.principal should contain(Enrolment("IR-CT", "UTR", utr))
      user.enrolments.principal should contain(Enrolment("HMRC-CUS-ORG", "EORINumber", eori))
      user.enrolments.principal should contain(Enrolment("HMRC-MTD-VAT", "VRN", vrn))
      user.enrolments.principal should contain(Enrolment("HMCE-VATDEC-ORG", "VATRegNo", vrn))
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
        val user = Users.get(userId).json.as[User]
        user.affinityGroup shouldBe Some(User.AG.Agent)
        user.nino shouldBe Some(Nino("WZ 58 73 41 D"))
        user.dateOfBirth shouldBe defined
        user.name shouldBe Some("API Test User")
        user.enrolments.principal should contain.only(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn))
      }
    }
  }

  "POST /agents-external-stubs/users/re-index" should {
    "re-index all existing users" in {
      implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
      val result = Users.reindexAllUsers
      result.status shouldBe 200
      result.body should include("users has been re-indexed")
    }
  }
}
