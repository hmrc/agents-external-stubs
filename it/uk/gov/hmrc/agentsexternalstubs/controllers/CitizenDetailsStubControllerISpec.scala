package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class CitizenDetailsStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "AuthStubController" when {

    "GET /citizen-details/nino/:nino" should {
      "return citizen data" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val result = CitizenDetailsStub.getCitizen("nino", "foo")
        result.status shouldBe 404
      }
    }
  }
}
