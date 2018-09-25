package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import org.joda.time.LocalDate
import uk.gov.hmrc.agentsexternalstubs.TcpProxiesConfig
import uk.gov.hmrc.agentsexternalstubs.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.agentsexternalstubs.controllers.BearerToken
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.AuthStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.http.logging.{Authorization, SessionId}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class ExternalAuthorisationServiceISpec extends ServerBaseISpec with WireMockSupport with MongoDB with AuthStubs {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val authenticationService = app.injector.instanceOf[AuthenticationService]
  lazy val httpPost = app.injector.instanceOf[HttpPost]
  lazy val authConnector = new MicroserviceAuthConnector(wireMockBaseUrl, httpPost)
  lazy val tcpProxiesConfig = TcpProxiesConfig("false", 0, 0, 0, 0, 0, 0, "0")
  lazy val underTest =
    new ExternalAuthorisationService(usersService, tcpProxiesConfig, httpPost, wireMockBaseUrl)

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
      val ec = ExecutionContext.Implicits.global

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

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe defined
      val session = sessionOpt.get
      session.authToken shouldBe BearerToken.unapply(authToken).get
      session.planetId shouldBe planetId
      session.userId shouldBe "AgentFoo"
      session.sessionId shouldBe sessionId

      val userOpt = await(usersService.findByUserId("AgentFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      val user = userOpt.get
      user.userId shouldBe "AgentFoo"
      user.principalEnrolments should contain.only(
        Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001")))))
      user.affinityGroup shouldBe Some("Agent")
      user.confidenceLevel shouldBe None
      user.credentialStrength shouldBe None
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe None
      user.groupId shouldBe Some("foo-group-1")
      user.name shouldBe Some("Foo Bar")
      user.dateOfBirth shouldBe None
      user.agentCode shouldBe Some("a")
      user.agentFriendlyName shouldBe Some("b")
      user.agentId shouldBe Some("c")
    }

    "consult external auth service, and if session exists recreate session and individual user locally" in {
      val planetId = UUID.randomUUID().toString
      val authToken = "Bearer " + UUID.randomUUID().toString
      val sessionId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(authToken)), sessionId = Some(SessionId(sessionId)))
      val ec = ExecutionContext.Implicits.global

      givenAuthorisedFor(
        authoriseRequest,
        AuthoriseResponse(
          credentials = Some(Credentials("UserFoo", "GovernmentGateway")),
          allEnrolments = Some(Seq(Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))))),
          affinityGroup = Some("Individual"),
          confidenceLevel = Some(200),
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

      val userOpt = await(usersService.findByUserId("UserFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      val user = userOpt.get
      user.userId shouldBe "UserFoo"
      user.principalEnrolments should contain.only(
        Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))))
      user.affinityGroup shouldBe Some("Individual")
      user.confidenceLevel shouldBe Some(200)
      user.credentialStrength shouldBe Some("strong")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe Some(Nino("HW827856C"))
      user.groupId shouldBe Some("foo-group-2")
      user.name shouldBe Some("Foo Bar")
      user.dateOfBirth shouldBe Some(LocalDate.parse("1993-09-21"))
      user.agentCode shouldBe None
      user.agentFriendlyName shouldBe None
      user.agentId shouldBe None
    }

    "consult external auth service, and if session missing do nothing" in {
      val planetId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(UUID.randomUUID().toString)))
      val ec = ExecutionContext.Implicits.global

      givenUnauthorised

      val sessionOpt =
        await(underTest.maybeExternalSession(planetId, authenticationService.authenticate)(ec, hc))
      sessionOpt shouldBe None
    }

    "consult external auth service, and if session exists recreate session and merge individual user" in {
      val planetId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization("Bearer " + UUID.randomUUID().toString)))
      val ec = ExecutionContext.Implicits.global

      val existingUser = await(
        usersService.createUser(
          UserGenerator
            .individual("UserFoo", 50, "User")
            .withPrincipalEnrolment(Enrolment("HMRC-MTD-VAT", Some(Seq(Identifier("VRN", "405985922"))))),
          planetId
        ))

      givenAuthorisedFor(
        authoriseRequest,
        AuthoriseResponse(
          credentials = Some(Credentials("UserFoo", "GovernmentGateway")),
          allEnrolments = Some(Seq(Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))))),
          affinityGroup = Some("Individual"),
          confidenceLevel = Some(200),
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

      val userOpt = await(usersService.findByUserId("UserFoo", sessionOpt.get.planetId))
      userOpt shouldBe defined
      val user = userOpt.get
      user.userId shouldBe "UserFoo"
      user.principalEnrolments should contain.only(
        Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "X12345678909876")))),
        Enrolment("HMRC-MTD-VAT", Some(Seq(Identifier("VRN", "405985922"))))
      )
      user.affinityGroup shouldBe Some("Individual")
      user.confidenceLevel shouldBe Some(50)
      user.credentialStrength shouldBe Some("strong")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe existingUser.nino
      user.groupId shouldBe existingUser.groupId
      user.name shouldBe existingUser.name
      user.dateOfBirth shouldBe existingUser.dateOfBirth
      user.agentCode shouldBe None
      user.agentFriendlyName shouldBe None
      user.agentId shouldBe None
    }

    "consult external auth service and parse response" in {
      val planetId = UUID.randomUUID().toString
      val authToken = "Bearer " + UUID.randomUUID().toString
      val sessionId = UUID.randomUUID().toString
      val hc = HeaderCarrier(authorization = Some(Authorization(authToken)), sessionId = Some(SessionId(sessionId)))
      val ec = ExecutionContext.Implicits.global

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
           |      "confidenceLevel": 200
           |    }
           |  ],
           |  "affinityGroup": "Individual",
           |  "confidenceLevel": 200,
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

      val userOpt = await(usersService.findByUserId("1551815928588520", sessionOpt.get.planetId))
      userOpt shouldBe defined
      val user = userOpt.get
      user.userId shouldBe "1551815928588520"
      user.principalEnrolments shouldBe empty
      user.affinityGroup shouldBe Some("Individual")
      user.confidenceLevel shouldBe Some(200)
      user.credentialStrength shouldBe Some("weak")
      user.credentialRole shouldBe Some("Admin")
      user.nino shouldBe Some(Nino("AB123456A"))
      user.groupId shouldBe Some("testGroupId-b1062cdf-c73f-4a3f-b949-d43354399729")
      user.name shouldBe Some("Foo Bar")
      user.agentCode shouldBe None
      user.agentFriendlyName shouldBe None
      user.agentId shouldBe None
    }
  }
}
