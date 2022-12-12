package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support._

class DataStreamStubControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient = app.injector.instanceOf[WSClient]

  "DataStreamStubController" when {

    "POST /write/audit" should {
      "return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        DataStreamStubs.writeAudit("{}") should haveStatus(204)
      }
    }

    "POST /write/audit/merged" should {
      "return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        DataStreamStubs.writeAuditMerged("{}") should haveStatus(204)
      }
    }
  }
}
