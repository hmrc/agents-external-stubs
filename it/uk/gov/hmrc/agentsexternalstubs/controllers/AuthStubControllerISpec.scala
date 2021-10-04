package uk.gov.hmrc.agentsexternalstubs.controllers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, urlEqualTo}
import org.joda.time.LocalDate
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, EmptyRetrieval, GGCredId, LegacyCredentials, Name}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{Nino => NinoPredicate, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.Authorization

import scala.concurrent.Future

class AuthStubControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with WireMockSupport {

  val url = s"http://localhost:$port"
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val authConnector: AuthConnector = app.injector.instanceOf[MicroserviceAuthConnector]

  class TestFixture extends AuthorisedFunctions {
    def authConnector: AuthConnector = app.injector.instanceOf[MicroserviceAuthConnector]
  }

  "AuthStubController" when {

    "POST /auth/authorise" should {
      "throw MissingBearerToken if token is missing" in {
        an[MissingBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(HeaderCarrier(), concurrent.ExecutionContext.Implicits.global)
          )
        }
      }

      "throw InvalidBearerToken if token is invalid" in {
        an[InvalidBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier().withExtraHeaders(HeaderNames.AUTHORIZATION -> "foo"),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "throw SessionRecordNotFound if session could not be found" in {
        an[SessionRecordNotFound] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization("Bearer foo"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "return 400 BadRequest if authorise field missing" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"foo":[{"enrolment":"FOO"}],"retrieve":[]}""")(AuthContext.fromToken(authToken))
        result should haveStatus(400)
        result.body shouldBe """/authorise -> [error.path.missing]"""
      }

      "return 400 BadRequest if predicate not supported" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"authorise":[{"foo":"FOO"}],"retrieve":[]}""")(AuthContext.fromToken(authToken))
        result should haveStatus(400)
        result.body should include("""/authorise(0) -> [Unsupported predicate {"foo":"FOO"}, should be one of [""")
      }

      "return 200 OK if predicate empty" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId))
        val result =
          AuthStub.authorise(s"""{"authorise":[],"retrieve":[]}""")(AuthContext.fromToken(authToken))
        result should haveStatus(200)
      }

      "retrieve credentials" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id))
        val creds = await(
          authConnector
            .authorise[Option[Credentials]](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        creds.get.providerId shouldBe id
        creds.get.providerType shouldBe "GovernmentGateway"
      }

      "retrieve credentials if PrivilegedApplication" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), providerType = "PrivilegedApplication")
        val creds = await(
          authConnector
            .authorise[Option[Credentials]](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        creds.get.providerId shouldBe id
        creds.get.providerType shouldBe "PrivilegedApplication"
      }

      "retrieve optionalCredentials (v2) if GovernmentGateway" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id))
        val creds = await(
          authConnector
            .authorise[Option[Credentials]](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        creds.map(_.providerId) shouldBe Some(id)
        creds.map(_.providerType) shouldBe Some("GovernmentGateway")
      }

      "retrieve optionalCredentials (v2) if PrivilegedApplication" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), providerType = "PrivilegedApplication")
        val creds = await(
          authConnector
            .authorise[Option[Credentials]](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        creds.map(_.providerId) shouldBe Some(id)
        creds.map(_.providerType) shouldBe Some("PrivilegedApplication")
      }

      "authorise if user authenticated with the OneTimeLogin provider" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId), providerType = "OneTimeLogin")
        await(
          authConnector
            .authorise[Unit](AuthProviders(AuthProvider.OneTimeLogin), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "authorise if user authenticated with the PrivilegedApplication provider" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId), providerType = "PrivilegedApplication")
        await(
          authConnector
            .authorise[Unit](AuthProviders(AuthProvider.PrivilegedApplication), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "authorise if user has a STRIDE enrolment" in {
        val authToken: String = givenAnAuthenticatedUser(
          UserGenerator.individual(randomId).withStrideRole(role = "FOO"),
          providerType = "PrivilegedApplication"
        )
        await(
          authConnector
            .authorise[Unit](AuthProviders(AuthProvider.PrivilegedApplication) and Enrolment("FOO"), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "throw UnsupportedAuthProvider if user authenticated with another provider" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId), providerType = "someOtherProvider")
        an[UnsupportedAuthProvider] shouldBe thrownBy {
          await(
            authConnector
              .authorise(AuthProviders(AuthProvider.GovernmentGateway), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve authProviderId" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id))
        val creds = await(
          authConnector
            .authorise[LegacyCredentials](EmptyPredicate, Retrievals.authProviderId)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        creds shouldBe GGCredId(id)
      }

      "throw InsufficientEnrolments if user not enrolled" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId))
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("HMRC-MTD-IT"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "throw InsufficientEnrolments if user not enrolled with expected identifier key" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), planetId = id)
        givenUserEnrolledFor(
          id,
          planetId = id,
          service = "HMRC-MTD-IT",
          identifierKey = "MTDITID",
          identifierValue = "236216873678126"
        )
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", "123"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "throw InsufficientEnrolments if user not enrolled with expected identifier value" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), planetId = id)
        givenUserEnrolledFor(id, planetId = id, "HMRC-MTD-IT", "MTDITID", "236216873678126")
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", "2362168736"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "throw InsufficientEnrolments if user does not have NINO" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), planetId = id)
        an[InsufficientEnrolments] shouldBe thrownBy {
          await(
            authConnector
              .authorise(Enrolment("HMRC-NI").withIdentifier("NINO", "HW827856C"), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "authorise if user has an synthetic HMRC-NI enrolment" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(randomId))
        await(
          authConnector
            .authorise[Unit](Enrolment("HMRC-NI"), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "authorise if user has an synthetic HMRC-NI enrolment and NINO matches" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(randomId, nino = "HW827856C"))
        await(
          authConnector
            .authorise[Unit](Enrolment("HMRC-NI").withIdentifier("NINO", "HW827856C"), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "retrieve authorisedEnrolments" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), planetId = id)
        givenUserEnrolledFor(id, planetId = id, "HMRC-MTD-IT", "MTDITID", "236216873678126")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](Enrolment("HMRC-MTD-IT"), Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe None
      }

      "retrieve authorisedEnrolments for member of a group" in {
        val userId = randomId
        val userOrganisationId = randomId
        val groupId = randomId

        givenAnAuthenticatedUser(User(userOrganisationId, groupId = Some(groupId)), planetId = userId)
        givenUserEnrolledFor(userOrganisationId, planetId = userId, "HMRC-MTD-IT", "MTDITID", "236216873678126")

        val authToken: String = givenAnAuthenticatedUser(User(userId, groupId = Some(groupId)), planetId = userId)
        givenUserEnrolledFor(userId, planetId = userId, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](Enrolment("HMRC-MTD-IT"), Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe None
      }

      "fail retrieving authorisedEnrolments for member of an other group" in {
        val userId = randomId
        val userOrganisationId = randomId

        givenAnAuthenticatedUser(User(userOrganisationId, groupId = Some(randomId)), planetId = userId)
        givenUserEnrolledFor(userOrganisationId, planetId = userId, "HMRC-MTD-IT", "MTDITID", "236216873678126")

        val authToken: String = givenAnAuthenticatedUser(User(userId, groupId = Some(randomId)), planetId = userId)
        givenUserEnrolledFor(userId, planetId = userId, "IR-SA", "UTR", "1234567890")

        an[InsufficientEnrolments] shouldBe thrownBy(
          await(
            authConnector
              .authorise[Enrolments](Enrolment("HMRC-MTD-IT"), Retrievals.authorisedEnrolments)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        )
      }

      "retrieve authorisedEnrolments if PrivilegedApplication" in {
        val id = randomId
        val authToken: String =
          givenAnAuthenticatedUser(User(id), planetId = id, providerType = "PrivilegedApplication")
        givenUserWithStrideRole(id, planetId = id, "FOO_ROLE")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](Enrolment("FOO_ROLE"), Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("FOO_ROLE") shouldBe Some(Enrolment("FOO_ROLE", Seq.empty, "Activated"))
        enrolments.getEnrolment("IR-SA") shouldBe None
      }

      "retrieve authorisedEnrolments with HMRC-NI" in {
        val id = randomId
        val user = UserGenerator.individual(id)
        val authToken: String = givenAnAuthenticatedUser(user, planetId = id)
        givenUserEnrolledFor(id, planetId = id, "HMRC-MTD-IT", "MTDITID", "236216873678126")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](Enrolment("HMRC-NI"), Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe None
        enrolments.getEnrolment("IR-SA") shouldBe None
        enrolments.getEnrolment("HMRC-NI") shouldBe Some(
          Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", user.nino.get.value)), "Activated")
        )
      }

      "retrieve allEnrolments" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id), planetId = id)
        givenUserEnrolledFor(id, planetId = id, "HMRC-MTD-IT", "MTDITID", "236216873678126")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe Some(
          Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")
        )
      }

      "retrieve allEnrolments of a member of a group" in {
        val userId = randomId
        val userOrganisationId = randomId
        val groupId = randomId

        givenAnAuthenticatedUser(User(userOrganisationId, groupId = Some(groupId)), planetId = userId)
        givenUserEnrolledFor(userOrganisationId, planetId = userId, "IR-SA", "UTR", "1234567890")

        val authToken: String = givenAnAuthenticatedUser(User(userId, groupId = Some(groupId)), planetId = userId)
        givenUserEnrolledFor(userId, planetId = userId, "HMRC-MTD-IT", "MTDITID", "236216873678126")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe Some(
          Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")
        )
      }

      "retrieve allEnrolments of a member of an other group" in {
        val userId = randomId
        val userOrganisationId = randomId

        givenAnAuthenticatedUser(User(userOrganisationId, groupId = Some(randomId)), planetId = userId)
        givenUserEnrolledFor(userOrganisationId, planetId = userId, "IR-SA", "UTR", "1234567890")

        val authToken: String = givenAnAuthenticatedUser(User(userId, groupId = Some(randomId)), planetId = userId)
        givenUserEnrolledFor(userId, planetId = userId, "HMRC-MTD-IT", "MTDITID", "236216873678126")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe None
      }

      "retrieve allEnrolments if PrivilegedApplication" in {
        val id = randomId
        val authToken: String =
          givenAnAuthenticatedUser(User(id), planetId = id, providerType = "PrivilegedApplication")
        givenUserWithStrideRole(id, planetId = id, "FOO_ROLE")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("FOO_ROLE") shouldBe Some(Enrolment("FOO_ROLE", Seq.empty, "Activated"))
        enrolments.getEnrolment("IR-SA") shouldBe None
      }

      "retrieve allEnrolments with HMRC-NI" in {
        val id = randomId
        val user = UserGenerator.individual(id)
        val authToken: String = givenAnAuthenticatedUser(user, planetId = id)
        givenUserEnrolledFor(id, planetId = id, "HMRC-MTD-IT", "MTDITID", "236216873678126")
        givenUserEnrolledFor(id, planetId = id, "IR-SA", "UTR", "1234567890")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.allEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("HMRC-MTD-IT") shouldBe Some(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "236216873678126")), "Activated")
        )
        enrolments.getEnrolment("IR-SA") shouldBe Some(
          Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")
        )
        enrolments.getEnrolment("HMRC-NI") shouldBe Some(
          Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", user.nino.get.value)), "Activated")
        )
      }

      "authorize if confidenceLevel matches" in {
        val authToken: String =
          givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 200))

        await(
          authConnector
            .authorise[Unit](ConfidenceLevel.L200, EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "throw IncorrectCredentialStrength if confidenceLevel does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 100))

        an[InsufficientConfidenceLevel] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](ConfidenceLevel.L200, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve confidenceLevel" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(confidenceLevel = 200))

        val confidence = await(
          authConnector
            .authorise[ConfidenceLevel](EmptyPredicate, Retrievals.confidenceLevel)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        confidence shouldBe ConfidenceLevel.L200
      }

      "authorize if credentialStrength matches" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        await(
          authConnector
            .authorise[Unit](CredentialStrength(CredentialStrength.strong), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "throw IncorrectCredentialStrength if credentialStrength does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        an[IncorrectCredentialStrength] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](CredentialStrength(CredentialStrength.weak), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve credentialStrength" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, credentialStrength = Some("strong")))

        val strength = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.credentialStrength)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        strength shouldBe Some(CredentialStrength.strong)
      }

      "authorize if affinityGroup matches" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Agent)))

        await(
          authConnector
            .authorise[Unit](AffinityGroup.Agent, EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "throw UnsupportedAffinityGroup if affinityGroup does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Individual)))

        an[UnsupportedAffinityGroup] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](AffinityGroup.Agent, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "throw UnsupportedAffinityGroup if none of alternative affinityGroup does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Agent)))

        an[UnsupportedAffinityGroup] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](AffinityGroup.Organisation or AffinityGroup.Individual, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve affinityGroup" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId, affinityGroup = Some(User.AG.Agent)))

        val affinityGroupOpt = await(
          authConnector
            .authorise[Option[AffinityGroup]](EmptyPredicate, Retrievals.affinityGroup)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        affinityGroupOpt shouldBe Some(AffinityGroup.Agent)
      }

      "authorize if user has nino" in {
        val authToken: String =
          givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        await(
          authConnector
            .authorise[Unit](NinoPredicate(hasNino = true, Some("HW827856C")), EmptyRetrieval)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
      }

      "throw exception if nino does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        an[InternalError] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](NinoPredicate(hasNino = true, Some("AB827856A")), EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve nino" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(nino = "HW827856C"))

        val ninoOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.nino)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        ninoOpt shouldBe Some("HW827856C")
      }

      "throw UnsupportedCredentialRole if credentialRole does not match" in {
        val authToken: String =
          givenAnAuthenticatedUser(User(randomId, credentialRole = Some("Foo"), isNonCompliant = Some(true)))

        an[UnsupportedCredentialRole] shouldBe thrownBy {
          await(
            authConnector
              .authorise[Unit](Admin, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
                concurrent.ExecutionContext.Implicits.global
              )
          )
        }
      }

      "retrieve credentialRole" in {
        givenAnAuthenticatedUser(
          UserGenerator.individual(groupId = "group1", credentialRole = "User"),
          planetId = "saturn"
        )

        val authToken: String =
          givenAnAuthenticatedUser(
            UserGenerator.individual(groupId = "group1", credentialRole = "Assistant"),
            planetId = "saturn"
          )

        val credentialRoleOpt = await(
          authConnector
            .authorise[Option[CredentialRole]](EmptyPredicate, Retrievals.credentialRole)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        credentialRoleOpt shouldBe Some(Assistant)
      }

      "retrieve groupIdentifier" in {
        val authToken: String = givenAnAuthenticatedUser(User(randomId, groupId = Some("AAA-999-XXX")))

        val groupIdentifierOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.groupIdentifier)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        groupIdentifierOpt shouldBe Some("AAA-999-XXX")
      }

      "retrieve name" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(name = "Foo Boo"))

        val nameOpt = await(
          authConnector
            .authorise[Option[Name]](EmptyPredicate, Retrievals.name)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        nameOpt.get shouldBe Name(Some("Foo"), Some("Boo"))
      }

      "retrieve optionalName (v2)" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(name = "Foo Boo"))

        val nameOpt = await(
          authConnector
            .authorise[Option[Name]](EmptyPredicate, Retrievals.name)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        nameOpt shouldBe Some(Name(Some("Foo"), Some("Boo")))
      }

      "retrieve dateOfBirth" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.individual(dateOfBirth = "1985-09-17"))

        val dateOfBirthOpt = await(
          authConnector
            .authorise[Option[LocalDate]](EmptyPredicate, Retrievals.dateOfBirth)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        dateOfBirthOpt shouldBe Some(LocalDate.parse("1985-09-17"))
      }

      "retrieve agentCode" in {
        val authToken: String = givenAnAuthenticatedUser(UserGenerator.agent(agentCode = "AAABBB1234567"))

        val agentCodeOpt = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.agentCode)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        agentCodeOpt shouldBe Some("AAABBB1234567")
      }

      "retrieve agentInformation" in {
        val authToken: String =
          givenAnAuthenticatedUser(UserGenerator.agent(agentCode = "AAABBB1234567", agentFriendlyName = "Fox & Co"))

        val agentInfo = await(
          authConnector
            .authorise[AgentInformation](EmptyPredicate, Retrievals.agentInformation)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        agentInfo.agentCode shouldBe Some("AAABBB1234567")
        agentInfo.agentFriendlyName shouldBe Some("Fox & Co")
        agentInfo.agentId.isDefined shouldBe true
      }

      "retrieve email address" in {
        val id = randomId
        val authToken: String = givenAnAuthenticatedUser(User(id))
        val email = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.email)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        email shouldBe Some("event-agents-external-aaaadghuc4fueomsg3kpkvdmry@hmrcdigital.slack.com")
      }

      "retrieve internalId" in {
        val id = randomId
        val planetId = randomId
        val authToken: String = givenAnAuthenticatedUser(user = User(id), planetId = planetId)
        val internalId = await(
          authConnector
            .authorise[Option[String]](EmptyPredicate, Retrievals.internalId)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global
            )
        )
        internalId shouldBe Some(s"$id@$planetId")
      }

      "authorize if any of enrolment matches" in new TestFixture {
        val authToken: String =
          givenAnAuthenticatedUser(
            UserGenerator
              .individual(randomId)
              .withPrincipalEnrolment("HMRC-MTD-VAT~VRN~936707596")
          )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            (Enrolment("HMRC-MTD-IT") or Enrolment("HMRC-NI") or Enrolment("HMRC-MTD-VAT"))
              and AuthProviders(GovernmentGateway)
          ) {
            Future.successful("success")
          }
        )
      }

      "authorize if mtd-it delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/mtd-it-auth/agent/${agent.agentCode.get}/client/236216873678126"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            Enrolment("HMRC-MTD-IT")
              .withIdentifier("MTDITID", "236216873678126")
              .withDelegatedAuthRule("mtd-it-auth")
          ) {
            Future.successful("success")
          }
        ) shouldBe "success"
      }

      "do not authorize if mtd-it delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/mtd-it-auth/agent/${agent.agentCode.get}/client/236216873678126"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-MTD-IT")
                .withIdentifier("MTDITID", "236216873678126")
                .withDelegatedAuthRule("mtd-it-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "do not authorize for mtd-it delegated auth rule when identifier type differs" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/mtd-it-auth/agent/${agent.agentCode.get}/client/236216873678126"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-MTD-VAT")
                .withIdentifier("VRN", "236216873678126")
                .withDelegatedAuthRule("mtd-it-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "authorize if mtd-vat delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/mtd-vat-auth/agent/${agent.agentCode.get}/client/936707596"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            Enrolment("HMRC-MTD-VAT")
              .withIdentifier("VRN", "936707596")
              .withDelegatedAuthRule("mtd-vat-auth")
          ) {
            Future.successful("success")
          }
        ) shouldBe "success"
      }

      "do not authorize if mtd-vat delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/mtd-vat-auth/agent/${agent.agentCode.get}/client/936707596"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-MTD-VAT")
                .withIdentifier("VRN", "936707596")
                .withDelegatedAuthRule("mtd-vat-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "authorize if afi delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/afi-auth/agent/${agent.agentCode.get}/client/HW827856C"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            Enrolment("HMRC-NI")
              .withIdentifier("NINO", "HW827856C")
              .withDelegatedAuthRule("afi-auth")
          ) {
            Future.successful("success")
          }
        ) shouldBe "success"
      }

      "do not authorize if afi delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/afi-auth/agent/${agent.agentCode.get}/client/HW827856C"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-NI")
                .withIdentifier("NINO", "HW827856C")
                .withDelegatedAuthRule("afi-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "authorize if sa delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/sa-auth/agent/${agent.agentCode.get}/client/1234556"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            Enrolment("IR-SA")
              .withIdentifier("UTR", "1234556")
              .withDelegatedAuthRule("sa-auth")
          ) {
            Future.successful("success")
          }
        ) shouldBe "success"
      }

      "do not authorize if sa delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/sa-auth/agent/${agent.agentCode.get}/client/1234556"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("IR-SA")
                .withIdentifier("UTR", "1234556")
                .withDelegatedAuthRule("sa-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "authorize if trust delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/trust-auth/agent/${agent.agentCode.get}/client/1234556"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        val result: String =
          await(
            authorised(
              Enrolment("HMRC-TERS-ORG")
                .withIdentifier("UTR", "1234556")
                .withDelegatedAuthRule("trust-auth")
            ) {
              Future.successful("success")
            }
          )
        println(result)
      }

      "do not authorize if trust delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/trust-auth/agent/${agent.agentCode.get}/client/1234556"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-TERS-ORG")
                .withIdentifier("UTR", "1234556")
                .withDelegatedAuthRule("trust-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }

      "authorize if cgt delegated auth rule returns true" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/cgt-auth/agent/${agent.agentCode.get}/client/XMCGTP123456789"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        await(
          authorised(
            Enrolment("HMRC-CGT-PD")
              .withIdentifier("CGTPDRef", "XMCGTP123456789")
              .withDelegatedAuthRule("cgt-auth")
          ) {
            Future.successful("success")
          }
        ) shouldBe "success"
      }

      "do not authorize if cgt delegated auth rule returns false" in new TestFixture {
        val agent: User = UserGenerator.agent(randomId)
        val authToken: String =
          givenAnAuthenticatedUser(agent)

        WireMock.stubFor(
          WireMock
            .get(urlEqualTo(s"/agent-access-control/cgt-auth/agent/${agent.agentCode.get}/client/XMCGTP123456789"))
            .willReturn(
              aResponse()
                .withStatus(401)
            )
        )

        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken")))

        an[InternalError] shouldBe thrownBy {
          await(
            authorised(
              Enrolment("HMRC-CGT-PD")
                .withIdentifier("CGTPDRef", "XMCGTP123456789")
                .withDelegatedAuthRule("cgt-auth")
            ) {
              Future.successful("success")
            }
          )
        }
      }
    }

    "GET /auth/authority" should {
      "return current individual user authority record" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .individual(authSession.userId)
          .withPrincipalEnrolment("IR-SA~UTR~123456")
        Users.update(user)
        val result = AuthStub.getAuthority()
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("uri"),
          haveProperty[Int]("confidenceLevel", be(user.confidenceLevel.getOrElse(50))),
          haveProperty[String]("credentialStrength", be(user.credentialStrength.getOrElse("weak"))),
          haveProperty[String]("userDetailsLink", be(s"/user-details/id/${user.userId}")),
          haveProperty[String]("legacyOid"),
          haveProperty[String]("ids", be(s"/auth/_ids")),
          haveProperty[String]("lastUpdated"),
          haveProperty[String]("loggedInAt"),
          haveProperty[String]("enrolments", be(s"/auth/_enrolments")),
          haveProperty[String]("affinityGroup", be(user.affinityGroup.getOrElse("none"))),
          haveProperty[String]("correlationId"),
          haveProperty[String]("credId", be(user.userId)),
          haveProperty[JsObject](
            "accounts",
            haveProperty[JsObject]("paye", haveProperty[String]("nino", be(user.nino.get.value.replace(" ", "")))),
            haveProperty[JsObject]("sa", haveProperty[String]("utr", be("123456")))
          )
        )
      }

      "return current agent user authority record" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val user =
          UserGenerator.agent(authSession.userId).withPrincipalEnrolment("IR-PAYE-AGENT~IRAgentReference~123456")
        Users.update(user)
        val result = AuthStub.getAuthority()
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("uri"),
          haveProperty[Int]("confidenceLevel", be(user.confidenceLevel.getOrElse(50))),
          haveProperty[String]("credentialStrength", be(user.credentialStrength.getOrElse("weak"))),
          haveProperty[String]("userDetailsLink", be(s"/user-details/id/${user.userId}")),
          haveProperty[String]("legacyOid"),
          haveProperty[String]("ids", be(s"/auth/_ids")),
          haveProperty[String]("lastUpdated"),
          haveProperty[String]("loggedInAt"),
          haveProperty[String]("enrolments", be(s"/auth/_enrolments")),
          haveProperty[String]("affinityGroup", be(user.affinityGroup.getOrElse("none"))),
          haveProperty[String]("correlationId"),
          haveProperty[String]("credId", be(user.userId)),
          haveProperty[JsObject](
            "accounts",
            haveProperty[JsObject](
              "agent",
              haveProperty[String]("agentCode", be(user.agentCode.get)),
              haveProperty[String]("agentUserRole", be("admin")),
              haveProperty[String]("payeReference", be("123456"))
            )
          )
        )
      }

      "return 401 if auth token missing" in {
        val result = AuthStub.getAuthority()(NotAuthorized)
        result should haveStatus(401)
      }
    }

    "GET /auth/_ids" should {
      "return current user's internal and external ids" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = AuthStub.getIds()
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("internalId", be(authSession.userId)),
          haveProperty[String]("externalId", be(authSession.userId))
        )
      }

      "return 401 if auth token missing" in {
        val result = AuthStub.getIds()(NotAuthorized)
        result should haveStatus(401)
      }
    }

    "GET /auth/_enrolments" should {
      "return current user's enrolments" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator.individual(authSession.userId)
        Users.update(user)
        val result = AuthStub.getEnrolments()
        result should haveStatus(200)
        result should haveValidJsonArrayBody(
          eachArrayElement[JsObject](
            haveProperty[String]("key"),
            haveProperty[Seq[JsObject]](
              "identifiers",
              eachElement[JsObject](haveProperty[String]("key"), haveProperty[String]("value"))
            ),
            haveProperty[String]("state")
          )
        )
      }

      "return 401 if auth token missing" in {
        val result = AuthStub.getEnrolments()(NotAuthorized)
        result should haveStatus(401)
      }
    }
  }
}
