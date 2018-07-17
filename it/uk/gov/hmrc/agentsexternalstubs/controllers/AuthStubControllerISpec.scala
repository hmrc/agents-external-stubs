package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, ServerBaseISpec, TestRequests}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

class AuthStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

  "AuthStubController" when {

    "POST /auth/authorise" should {
      "return 401 Unauthorized if token is missing" in {
        an[MissingBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(HeaderCarrier(), concurrent.ExecutionContext.Implicits.global))
        }
      }

      "return 401 Unauthorized if token is invalid" in {
        an[InvalidBearerToken] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier().withExtraHeaders(HeaderNames.AUTHORIZATION -> "foo"),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "return 401 Unauthorized if session could not be found" in {
        an[SessionRecordNotFound] shouldBe thrownBy {
          await(
            authConnector
              .authorise(EmptyPredicate, EmptyRetrieval)(
                HeaderCarrier(authorization = Some(Authorization("Bearer foo"))),
                concurrent.ExecutionContext.Implicits.global))
        }
      }

      "return 400 BadRequest if authorise field missing" in {
        val authToken = givenAnAuthenticatedUser("foo")
        val result =
          AuthStub.authorise(s"""{"foo":[{"enrolment":"FOO"}],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body shouldBe """/authorise -> [error.path.missing]"""
      }

      "return 400 BadRequest if predicate not supported" in {
        val authToken = givenAnAuthenticatedUser("foo")
        val result =
          AuthStub.authorise(s"""{"authorise":[{"foo":"FOO"}],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body shouldBe """/authorise(0) -> [Unsupported predicate {"foo":"FOO"}, should be one of [enrolment,authProviders]]"""
      }

      "return 200 OK if predicate empty" in {
        val authToken = givenAnAuthenticatedUser("foo")
        val result =
          AuthStub.authorise(s"""{"authorise":[],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 200
      }

      "retrieve credentials" in {
        val authToken = givenAnAuthenticatedUser("foo")
        val creds = await(
          authConnector
            .authorise[Credentials](EmptyPredicate, Retrievals.credentials)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        creds.providerId shouldBe "foo"
        creds.providerType shouldBe "GovernmentGateway"
      }

      "retrieve authProviderId" in {
        val authToken = givenAnAuthenticatedUser("foo")
        val creds = await(
          authConnector
            .authorise[LegacyCredentials](EmptyPredicate, Retrievals.authProviderId)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        creds shouldBe GGCredId("foo")
      }

      "retrieve authorisedEnrolments" in {
        val authToken = givenAnAuthenticatedUser("foo123")
        givenUserEnrolledFor("foo123", "serviceA", "idOfA", "2362168736781263")

        val enrolments = await(
          authConnector
            .authorise[Enrolments](EmptyPredicate, Retrievals.authorisedEnrolments)(
              HeaderCarrier(authorization = Some(Authorization(s"Bearer $authToken"))),
              concurrent.ExecutionContext.Implicits.global))
        enrolments.getEnrolment("foo") shouldBe None
        enrolments.getEnrolment("serviceA") shouldBe Some(
          Enrolment("serviceA", Seq(EnrolmentIdentifier("idOfA", "2362168736781263")), "Activated"))
      }
    }
  }
}
