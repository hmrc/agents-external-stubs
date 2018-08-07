package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{Nino => NinoPredicate, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

class AuthStubControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

  "AuthStubController" when {

    "POST /auth/authorise" should {
      "throw MissingBearerToken if token is missing" in {
        an[MissingBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(HeaderCarrier(), concurrent.ExecutionContext.Implicits.global))
        }
      }

      "throw InvalidBearerToken if token is invalid" in {
        an[InvalidBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier().withExtraHeaders(HeaderNames.AUTHORIZATION -> "foo"),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "throw SessionRecordNotFound if session could not be found" in {
        an[SessionRecordNotFound] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization("Bearer foo"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "return 400 BadRequest if authorise field missing" in {
        val authToken = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"foo":[{"enrolment":"FOO"}],"retrieve":[]}""")(AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body shouldBe """/authorise -> [error.path.missing]"""
      }

      "return 400 BadRequest if predicate not supported" in {
        val authToken = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"authorise":[{"foo":"FOO"}],"retrieve":[]}""")(AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body should include("""/authorise(0) -> [Unsupported predicate {"foo":"FOO"}, should be one of [""")
      }

      "return 200 OK if predicate empty" in {
        val authToken = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"authorise":[],"retrieve":[]}""")(AuthContext.withToken(authToken))
        result.status shouldBe 200
      }

      "retrieve credentials" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        val creds = await(
          authConnector
            .authorise[Credentials](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        creds.providerId shouldBe id
        creds.providerType shouldBe "GovernmentGateway"
      }

      "authorise if user authenticated with the expected provider" in {
        val authToken = givenAnAuthenticatedUser(User(randomId), providerType = "OneTimeLogin")
        await(
          authConnector
            .authorise[Unit](AuthProviders(AuthProvider.OneTimeLogin), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
      }

      "throw UnsupportedAuthProvider if user authenticated with another provider" in {
        val authToken = givenAnAuthenticatedUser(User(randomId), providerType = "someOtherProvider")
        an[UnsupportedAuthProvider] shouldBe thrownBy {
          await(
            authConnector
              .authorise(AuthProviders(AuthProvider.GovernmentGateway), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve authProviderId" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        val creds = await(
          authConnector
            .authorise[LegacyCredentials](EmptyPredicate, Retrievals.authProviderId)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        creds shouldBe GGCredId(id)
      }

      "throw InsufficientEnrolments if user not enrolled" in {
        val authToken = givenAnAuthenticatedUser(User(randomId))
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("serviceA"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "throw InsufficientEnrolments if user not enrolled with expected identifier key" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        givenUserEnrolledFor(id, "juniper", "serviceA", "idOfA", "2362168736781263")
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("serviceA").withIdentifier("foo", "123"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "throw InsufficientEnrolments if user not enrolled with expected identifier value" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        givenUserEnrolledFor(id, "juniper", "serviceA", "idOfA", "2362168736781263")
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("serviceA").withIdentifier("idOfA", "2362168736"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve authorisedEnrolments" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        givenUserEnrolledFor(id, "juniper", "serviceA", "idOfA", "2362168736781263")
        givenUserEnrolledFor(id, "juniper", "serviceB", "idOfB", "4783748738748778")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](Enrolment("serviceA"), Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        enrolments.getEnrolment("serviceA") shouldBe Some(
          Enrolment("serviceA", Seq(EnrolmentIdentifier("idOfA", "2362168736781263")), "Activated"))
        enrolments.getEnrolment("serviceB") shouldBe None
      }

      "retrieve allEnrolments" in {
        val id = randomId
        val authToken = givenAnAuthenticatedUser(User(id))
        givenUserEnrolledFor(id, "juniper", "serviceA", "idOfA", "2362168736781263")
        givenUserEnrolledFor(id, "juniper", "serviceB", "idOfB", "4783748738748778")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("serviceA") shouldBe Some(
          Enrolment("serviceA", Seq(EnrolmentIdentifier("idOfA", "2362168736781263")), "Activated"))
        enrolments.getEnrolment("serviceB") shouldBe Some(
          Enrolment("serviceB", Seq(EnrolmentIdentifier("idOfB", "4783748738748778")), "Activated"))
      }

      "authorize if confidenceLevel matches" in {
        val authToken =
          givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 300))

        await(
          authConnector
            .authorise[Unit](ConfidenceLevel.L300, EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
      }

      "throw IncorrectCredentialStrength if confidenceLevel does not match" in {
        val authToken =
          givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 100))

        an[InsufficientConfidenceLevel] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](ConfidenceLevel.L200, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve confidenceLevel" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 200))

        val confidence = await(
          authConnector
            .authorise[ConfidenceLevel](EmptyPredicate, Retrievals.confidenceLevel)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        confidence shouldBe ConfidenceLevel.L200
      }

      "authorize if credentialStrength matches" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        await(
          authConnector
            .authorise[Unit](CredentialStrength(CredentialStrength.strong), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
      }

      "throw IncorrectCredentialStrength if credentialStrength does not match" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        an[IncorrectCredentialStrength] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](CredentialStrength(CredentialStrength.weak), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve credentialStrength" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        val strength = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.credentialStrength)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        strength shouldBe Some(CredentialStrength.strong)
      }

      "authorize if affinityGroup matches" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Agent)))

        await(
          authConnector
            .authorise[Unit](AffinityGroup.Agent, EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
      }

      "throw UnsupportedAffinityGroup if affinityGroup does not match" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Individual)))

        an[UnsupportedAffinityGroup] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](AffinityGroup.Agent, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve affinityGroup" in {
        val authToken = givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Agent)))

        val affinityGroupOpt = await(
          authConnector
            .authorise[Option[AffinityGroup]](EmptyPredicate, Retrievals.affinityGroup)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        affinityGroupOpt shouldBe Some(AffinityGroup.Agent)
      }

      "authorize if user has nino" in {
        val authToken =
          givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        await(
          authConnector
            .authorise[Unit](NinoPredicate(hasNino = true, Some("HW827856C")), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
      }

      "throw exception if nino does not match" in {
        val authToken =
          givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        an[InternalError] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](NinoPredicate(hasNino = true, Some("AB827856A")), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve nino" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        val ninoOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.nino)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        ninoOpt shouldBe Some("HW827856C")
      }

      "throw UnsupportedCredentialRole if credentialRole does not match" in {
        val authToken =
          givenAnAuthenticatedUser(User(randomId, credentialRole = Some("Foo"), isNonCompliant = Some(true)))

        an[UnsupportedCredentialRole] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](Admin, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "retrieve credentialRole" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.individual(credentialRole = "Assistant"))

        val credentialRoleOpt = await(
          authConnector
            .authorise[Option[CredentialRole]](EmptyPredicate, Retrievals.credentialRole)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        credentialRoleOpt shouldBe Some(Assistant)
      }

      "retrieve groupIdentifier" in {
        val authToken = givenAnAuthenticatedUser(User(randomId, groupId = Some("AAA-999-XXX")))

        val groupIdentifierOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.groupIdentifier)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        groupIdentifierOpt shouldBe Some("AAA-999-XXX")
      }

      "retrieve name" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.individual(name = "Foo Boo"))

        val nameOpt = await(
          authConnector
            .authorise[Name](EmptyPredicate, Retrievals.name)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        nameOpt shouldBe Name(Some("Foo"), Some("Boo"))
      }

      "retrieve dateOfBirth" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.individual(dateOfBirth = "1985-09-17"))

        val dateOfBirthOpt = await(
          authConnector
            .authorise[Option[LocalDate]](EmptyPredicate, Retrievals.dateOfBirth)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        dateOfBirthOpt shouldBe Some(LocalDate.parse("1985-09-17"))
      }

      "retrieve agentCode" in {
        val authToken = givenAnAuthenticatedUser(UserGenerator.agent(agentCode = "AAABBB1234567"))

        val agentCodeOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.agentCode)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        agentCodeOpt shouldBe Some("AAABBB1234567")
      }

      "retrieve agentInformation" in {
        val authToken =
          givenAnAuthenticatedUser(UserGenerator.agent(agentCode = "AAABBB1234567", agentFriendlyName = "Fox & Co"))

        val agentInfo = await(
          authConnector
            .authorise[AgentInformation](EmptyPredicate, Retrievals.agentInformation)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        agentInfo.agentCode shouldBe Some("AAABBB1234567")
        agentInfo.agentFriendlyName shouldBe Some("Fox & Co")
        agentInfo.agentId.isDefined shouldBe true
      }
    }
  }
}
