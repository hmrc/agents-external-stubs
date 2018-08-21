package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Enrolment, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, ServerBaseISpec, TestRequests}

import scala.concurrent.duration._

class DesStubControllerISpec extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  override implicit val defaultTimeout = 60.seconds

  "DesController" when {

    "POST /registration/relationship" should {
      "respond 200 when authorising for ITSA" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",  
                       |   "agentReferenceNumber": "ZARN1234567",  
                       |   "regime": "ITSA",
                       |   "authorisation": {    
                       |     "action": "Authorise",    
                       |     "isExclusiveAgent": true  
                       |   }
                       |}
          """.stripMargin))
        result.status shouldBe 202
      }
    }

    "GET /registration/relationship" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))
        result.status shouldBe 200
      }
    }
  }
}
