package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.services.SignInService
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, MongoApp, ServerBaseISpec, TestRequests}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, InvalidBearerToken, MissingBearerToken, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.ExecutionContext.Implicits.global

class AuthStubControllerISpec extends ServerBaseISpec with MongoApp with TestRequests {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]
  val signInService: SignInService = app.injector.instanceOf[SignInService]

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

      "return 200 OK if predicate empty" in {
        val authToken = givenUserAuthenticated("foo")
        val result =
          AuthStub.authorise(s"""{"authorise":[],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 200
      }

      "return 400 BadRequest if authorise field missing" in {
        val authToken = givenUserAuthenticated("foo")
        val result =
          AuthStub.authorise(s"""{"foo":[{"enrolment":"FOO"}],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body shouldBe """/authorise -> [error.path.missing]"""
      }

      "return 400 BadRequest if predicate not supported" in {
        val authToken = givenUserAuthenticated("foo")
        val result =
          AuthStub.authorise(s"""{"authorise":[{"foo":"FOO"}],"retrieve":[]}""", AuthContext.withToken(authToken))
        result.status shouldBe 400
        result.body shouldBe """/authorise(0) -> [Unsupported predicate {"foo":"FOO"}, should be one of [enrolment,authProviders]]"""
      }
    }
  }

  def givenUserAuthenticated(userId: String): String =
    await(signInService.createNewAuthentication(userId, "any"))
      .getOrElse(throw new Exception("Could not sing in user"))
      .authToken
}
