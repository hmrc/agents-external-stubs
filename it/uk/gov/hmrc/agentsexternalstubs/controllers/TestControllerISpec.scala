package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, NotAuthorized, ServerBaseISpec, TestRequests}

class TestControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "TestController" when {

    "GET /agents-external-stubs/test/auth/agent-mtd" should {
      "return 401 Unauthorized if user not authenticated" in {
        val result = TestMe.testAuthAgentMtd(NotAuthorized)
        result.status shouldBe 401
      }

      "return 401 Unauthorized if user authenticated but has no enrolments" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = TestMe.testAuthAgentMtd
        result.status shouldBe 401
      }

      "respond with some data if user exists and has expected enrolment" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(
          User(
            authSession.userId,
            principalEnrolments =
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001")))))))

        val result = TestMe.testAuthAgentMtd

        result.status shouldBe 200
        result.json.as[String] shouldBe "TARN0000001"
      }
    }

    "GET /agents-external-stubs/test/auth/client-mtd-it" should {
      "return 401 Unauthorized if user not authenticated" in {
        val result = TestMe.testAuthAgentMtd(NotAuthorized)
        result.status shouldBe 401
      }

      "return 401 Unauthorized if user authenticated but has no enrolments" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = TestMe.testAuthClientMtdIt
        result.status shouldBe 401
      }

      "respond with some data if user exists and has expected enrolment" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(
          User(
            authSession.userId,
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", "ABC1234567")))))))

        val result = TestMe.testAuthClientMtdIt

        result.status shouldBe 200
        result.json.as[String] shouldBe "ABC1234567"
      }
    }
  }
}
