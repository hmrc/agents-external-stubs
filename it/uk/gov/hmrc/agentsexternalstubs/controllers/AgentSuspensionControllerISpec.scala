package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Enrolment, Identifier, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.UsersRepository
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class AgentSuspensionControllerISpec extends ServerBaseISpec with TestRequests {

  override def url: String = s"http://localhost:$port"
  override def wsClient: WSClient = app.injector.instanceOf[WSClient]
  val repo = app.injector.instanceOf[UsersRepository]

  "AgentSuspensionControllerISpec" when {
    "GET /agent-suspension/status/arn " should {
      "return active status when agent is not suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent()
          .copy(
            userId = session.userId,
            principalEnrolments =
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001"))))))
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 200
        result.json shouldBe Json.parse("""{"services": []}""")
      }

      "return suspended status and list of suspended regimes when agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent()
          .copy(
            userId = session.userId,
            suspendedRegimes = Some(Set("ITSA", "VATC")),
            principalEnrolments =
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", "TARN0000001")))))
          )
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 200
        result.json shouldBe Json.parse(
          """{"services": ["ITSA", "VATC"]}""")
      }

      "return not found when user record cannot be found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        await(repo.delete(session.userId, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 404
        result.json shouldBe Json.parse("""{"code":"USER_NOT_FOUND"}""")
      }
    }
  }
}
