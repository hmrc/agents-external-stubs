package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsArray, JsObject}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class KnownFactsControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with ExampleDesPayloads {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "KnownFactsController" when {

    "GET /agents-external-stubs/known-facts/:enrolmentKey" should {
      "respond 200 with a known facts details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
        Users.create(UserGenerator.individual("foo1").withPrincipalEnrolment(enrolmentKey))
        Users.create(UserGenerator.agent("foo2").withDelegatedEnrolment(enrolmentKey))

        val result = KnownFacts.getKnownFacts(enrolmentKey)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("enrolmentKey", be(enrolmentKey)) and
            haveProperty[Seq[JsObject]](
              "verifiers",
              eachElement(haveProperty[String]("key") and haveProperty[String]("value"))) and
            haveProperty[JsObject]("user", haveProperty[String]("userId", be("foo1"))) and
            haveProperty[Seq[JsObject]]("agents", have(size(1))) and
            haveProperty[JsArray]("_links")
        )
      }
    }
  }
}
