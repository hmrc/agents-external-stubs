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
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgetReferenceNumber", "TARN0000001"))))))
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 200
        result.json shouldBe Json.parse("""{"status": "NotSuspended"}""")
      }

      "return suspended status and list of suspended services when agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent()
          .copy(
            userId = session.userId,
            suspendedServices = Some(Set("HMRC-MTD-IT", "HMRC-MTD-VAT")),
            principalEnrolments =
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgetReferenceNumber", "TARN0000001")))))
          )
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 200
        result.json shouldBe Json.parse(
          """{"status": "Suspended", "suspendedServices": ["HMRC-MTD-IT", "HMRC-MTD-VAT"]}""")
      }

      "return Unauthorized when the arn does not match that of the logged in user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent()
          .copy(
            userId = session.userId,
            suspendedServices = Some(Set("HMRC-MTD-IT", "HMRC-MTD-VAT")),
            principalEnrolments =
              Seq(Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgetReferenceNumber", "TARN0000001")))))
          )
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("ZARN0000001"))

        result.status shouldBe 401
        result.body shouldBe "arn does not match that of logged in user"
      }

      "return Unauthorized when the logged in user is not an agent" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator.individual(userId = session.userId)
        await(repo.update(user, session.planetId))
        val result = AgentSuspensionStub.getSuspensionStatus(Arn("TARN0000001"))

        result.status shouldBe 401
        result.body shouldBe "user is not an agent"
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
