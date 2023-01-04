package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID
import uk.gov.hmrc.agentsexternalstubs.connectors.{MicroserviceAuthConnector, TestAppConfig}
import uk.gov.hmrc.agentsexternalstubs.controllers.BearerToken
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.AuthStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Authorization, SessionId}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import play.api.test.Helpers._

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ExternalAuthorisationServiceISpec extends ServerBaseISpec with WireMockSupport with AuthStubs {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val groupsService = app.injector.instanceOf[GroupsService]
  lazy val authenticationService = app.injector.instanceOf[AuthenticationService]
  lazy val httpPost = app.injector.instanceOf[HttpPost]
  lazy val appConfig = TestAppConfig(wireMockBaseUrlAsString, wireMockPort)
  lazy val authConnector = new MicroserviceAuthConnector(appConfig, httpPost)
  lazy val underTest =
    new ExternalAuthorisationService(usersService, groupsService, httpPost, appConfig)

  val authoriseRequest = AuthoriseRequest(
    Seq.empty,
    Seq(
      "credentials",
      "allEnrolments",
      "affinityGroup",
      "confidenceLevel",
      "credentialStrength",
      "credentialRole",
      "nino",
      "groupIdentifier",
      "name",
      "dateOfBirth",
      "agentInformation"
    )
  )

  "ExternalAuthorisationService" should {
    "consult external auth service, and if session exists recreate session and agent user locally" in {
      val planetId = UUID.randomUUID().toString
      val authToken = "Bearer " + UUID.randomUUID().toString
      val sessionId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(authToken)), sessionId = Some(SessionId(sessionId)))

      givenAuthorisedFor(
        authoriseRequest,
        AuthoriseResponse(
          credentials = Some(Credentials("AgentFoo", "GovernmentGateway")),
          allEnrolments =
            Some(Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001")))))),
          affinityGroup = Some("Agent"),
          confidenceLevel = None,
          credentialStrength = None,
          credentialRole = Some("User"),
          nino = None,
          groupIdentifier = Some("foo-group-1"),
          name = Some(Name(Some("Foo"), Some("Bar"))),
          dateOfBirth = Some(LocalDate.parse("1993-09-21")),
          agentInformation = Some(AgentInformation(Some("a"), Some("b"), Some("c")))
        )
      )
      implicit val ec: ExecutionContext = ExecutionContext.global
      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe defined
      val session = sessionOpt.get
      session.authToken shouldBe BearerToken.unapply(authToken).get
      session.planetId shouldBe planetId
      session.userId shouldBe "AgentFoo"
      session.sessionId shouldBe sessionId

      val (userOpt, groupOpt) = await(usersService.findUserAndGroup("AgentFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      groupOpt shouldBe defined
      val user = userOpt.get
      val group = groupOpt.get
      user.userId shouldBe "AgentFoo"
      group.principalEnrolments should contain.only(
        Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001"))))
      )
      group.affinityGroup shouldBe AG.Agent
      user.confidenceLevel shouldBe None
      user.credentialStrength shouldBe None
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe Some(Nino("AB 08 00 48 B"))
      user.groupId shouldBe Some("foo-group-1")
      user.name shouldBe Some("Foo Bar")
      user.dateOfBirth shouldBe defined
      group.agentCode shouldBe Some("a")
      group.agentFriendlyName shouldBe Some("b")
      group.agentId shouldBe Some("c")
    }

    "consult external auth service, and if session exists recreate session and individual user locally" in {
      val planetId = UUID.randomUUID().toString
      val authToken = "Bearer " + UUID.randomUUID().toString
      val sessionId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(authToken)), sessionId = Some(SessionId(sessionId)))
      implicit val ec: ExecutionContext = ExecutionContext.global

      givenAuthorisedFor(
        authoriseRequest,
        AuthoriseResponse(
          credentials = Some(Credentials("UserFoo", "GovernmentGateway")),
          allEnrolments = Some(Seq(Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))))),
          affinityGroup = Some("Individual"),
          confidenceLevel = Some(250),
          credentialStrength = Some("strong"),
          credentialRole = Some("User"),
          nino = Some(Nino("HW827856C")),
          groupIdentifier = Some("foo-group-2"),
          name = Some(Name(Some("Foo"), Some("Bar"))),
          dateOfBirth = Some(LocalDate.parse("1993-09-21")),
          agentInformation = None
        )
      )

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe defined
      val session = sessionOpt.get
      session.authToken shouldBe BearerToken.unapply(authToken).get
      session.planetId shouldBe planetId
      session.userId shouldBe "UserFoo"
      session.sessionId shouldBe sessionId

      val (userOpt, groupOpt) = await(usersService.findUserAndGroup("UserFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      groupOpt shouldBe defined
      val user = userOpt.get
      val group = groupOpt.get
      user.userId shouldBe "UserFoo"
      group.principalEnrolments should contain.only(
        Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876"))))
      )
      group.affinityGroup shouldBe AG.Individual
      user.confidenceLevel shouldBe Some(250)
      user.credentialStrength shouldBe Some("strong")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe Some(Nino("HW827856C"))
      user.groupId shouldBe Some("foo-group-2")
      user.name shouldBe Some("Foo Bar")
      user.dateOfBirth shouldBe Some(LocalDate.parse("1993-09-21"))
      group.agentCode shouldBe None
      group.agentFriendlyName shouldBe None
      group.agentId shouldBe None
    }

    "consult external auth service, and if session missing do nothing" in {
      val planetId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(UUID.randomUUID().toString)))
      implicit val ec: ExecutionContext = ExecutionContext.global

      givenUnauthorised

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe None
    }

    "consult external auth service, and if session exists recreate session and merge individual user" in {
      val planetId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization("Bearer " + UUID.randomUUID().toString)))
      implicit val ec: ExecutionContext = ExecutionContext.global

      val existingUser = await(
        usersService.createUser(
          UserGenerator
            .individual("UserFoo", 50, "User")
            .withAssignedPrincipalEnrolment(
              Enrolment("HMRC-MTD-VAT", Some(Seq(Identifier("VRN", "405985922")))).toEnrolmentKey.get
            ),
          planetId,
          affinityGroup = Some(AG.Individual)
        )
      )

      givenAuthorisedFor(
        authoriseRequest,
        AuthoriseResponse(
          credentials = Some(Credentials("UserFoo", "GovernmentGateway")),
          allEnrolments = Some(Seq(Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))))),
          affinityGroup = Some("Individual"),
          confidenceLevel = Some(250),
          credentialStrength = Some("strong"),
          credentialRole = Some("User"),
          nino = Some(Nino("HW827856C")),
          groupIdentifier = Some("foo-group-2"),
          name = Some(Name(Some("Foo"), Some("Bar"))),
          dateOfBirth = existingUser.dateOfBirth,
          agentInformation = None
        )
      )

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe defined

      val (userOpt, groupOpt) = await(usersService.findUserAndGroup("UserFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      groupOpt shouldBe defined
      val user = userOpt.get
      val group = groupOpt.get
      user.userId shouldBe "UserFoo"
      group.principalEnrolments should contain.only(
        Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))),
        Enrolment("HMRC-MTD-VAT", Some(Seq(Identifier("VRN", "405985922"))))
      )
      group.affinityGroup shouldBe AG.Individual
      user.confidenceLevel shouldBe Some(50)
      user.credentialStrength shouldBe Some("strong")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe existingUser.nino
      user.groupId shouldBe existingUser.groupId
      user.name shouldBe existingUser.name
      user.dateOfBirth shouldBe existingUser.dateOfBirth
      group.agentCode shouldBe None
      group.agentFriendlyName shouldBe None
      group.agentId shouldBe None
    }

    "consult external auth service and parse response" in {
      val planetId = UUID.randomUUID().toString
      val authToken = "Bearer " + UUID.randomUUID().toString
      val sessionId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(authToken)), sessionId = Some(SessionId(sessionId)))
      implicit val ec: ExecutionContext = ExecutionContext.global

      givenAuthorisedFor(
        s"""
          |{
          |  "credentials": {
          |    "providerId": "1551815928588520",
          |    "providerType": "GovernmentGateway"
          |  },
          |  "allEnrolments": [
          |    {
          |      "key": "HMRC-NI",
          |      "identifiers": [
          |        {
          |          "key": "NINO",
          |          "value": "AB123456A"
          |        }
          |      ],
          |      "state": "Activated",
          |      "confidenceLevel": 250
          |    }
          |  ],
          |  "affinityGroup": "Individual",
          |  "confidenceLevel": 250,
          |  "credentialStrength": "weak",
          |  "credentialRole": "User",
          |  "nino": "AB123456A",
          |  "groupIdentifier": "testGroupId-b1062cdf-c73f-4a3f-b949-d43354399729",
          |  "name": {
          |    "name": "Foo Bar"
          |  },
          |  "agentInformation": {}
          |}
         """.stripMargin
      )

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe defined
      val session = sessionOpt.get
      session.authToken shouldBe BearerToken.unapply(authToken).get
      session.planetId shouldBe planetId
      session.userId shouldBe "1551815928588520"
      session.sessionId shouldBe sessionId

      val (userOpt, groupOpt) = await(usersService.findUserAndGroup("1551815928588520", sessionOpt.get.planetId))
      userOpt shouldBe defined
      groupOpt shouldBe defined
      val user = userOpt.get
      val group = groupOpt.get
      user.userId shouldBe "1551815928588520"
      group.principalEnrolments shouldBe empty
      group.affinityGroup shouldBe AG.Individual
      user.confidenceLevel shouldBe Some(250)
      user.credentialStrength shouldBe Some("weak")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe Some(Nino("AB123456A"))
      user.groupId shouldBe Some("testGroupId-b1062cdf-c73f-4a3f-b949-d43354399729")
      user.name shouldBe Some("Foo Bar")
      group.agentCode shouldBe None
      group.agentFriendlyName shouldBe None
      group.agentId shouldBe None
    }
  }
}
