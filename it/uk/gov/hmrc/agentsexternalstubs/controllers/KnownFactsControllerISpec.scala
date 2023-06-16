package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, Enrolment, EnrolmentKey, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.support._

class KnownFactsControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "KnownFactsController" when {

    "GET /agents-external-stubs/known-facts/:enrolmentKey" should {
      //TODO - fix flaky test?
      "respond 200 with a known facts details" in {
        // <_< issue is here on sign in, but why just here, it's used all over the place
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
        val enrolment = Enrolment.from(EnrolmentKey(enrolmentKey))
        Seq(
          Users.create(
            UserGenerator.individual("foo1").withAssignedPrincipalEnrolment(enrolment.toEnrolmentKey.get),
            Some(AG.Individual)
          ),
          Users.create(
            UserGenerator.agent("foo2").withAssignedDelegatedEnrolment(enrolment.toEnrolmentKey.get),
            Some(AG.Agent)
          )
        ).map(_ should haveStatus(201))

        val result = KnownFacts.getKnownFacts(enrolmentKey)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("enrolmentKey", be(enrolmentKey)) and
            haveProperty[Seq[JsObject]](
              "verifiers",
              eachElement(haveProperty[String]("key") and haveProperty[String]("value"))
            ) and
            haveProperty[JsObject]("user", haveProperty[String]("userId", be("foo1"))) and
            haveProperty[Seq[JsObject]]("agents", have(size(1)))
        )
      }
    }

    "POST /agents-external-stubs/known-facts" should {
      "create a sanitized known fact and return 201" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"

        val result = KnownFacts.createKnownFacts(Json.parse(s"""
          |{ "enrolmentKey": "$enrolmentKey",
          | "verifiers": [
          |   {
          |     "key": "NINO",
          |     "value": ""
          |   }
          |  ]
          |} """.stripMargin))
        result should haveStatus(201)

        val feedback = KnownFacts.getKnownFacts(enrolmentKey)
        feedback should haveStatus(200)
        feedback should haveValidJsonBody(
          haveProperty[String]("enrolmentKey", be(enrolmentKey)) and
            haveProperty[Seq[JsObject]](
              "verifiers",
              eachElement(
                haveProperty[String]("key") and
                  haveProperty[String]("value")
              )
            )
        )
      }
    }

    "PUT /agents-external-stubs/known-facts/:enrolmentKey" should {
      "sanitize and update known fact and return 202" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
        Users.create(
          UserGenerator.individual("foo1").withAssignedPrincipalEnrolment(EnrolmentKey(enrolmentKey)),
          Some(AG.Individual)
        )

        val result = KnownFacts.upsertKnownFacts(
          enrolmentKey,
          Json.parse(s"""
            |{ "enrolmentKey": "$enrolmentKey",
            | "verifiers": [
            |   {
            |     "key": "NINO",
            |     "value": "AB087054B"
            |   }
            |  ]
            |} """.stripMargin)
        )
        result should haveStatus(202)

        val feedback = KnownFacts.getKnownFacts(enrolmentKey)
        feedback should haveStatus(200)
        feedback should haveValidJsonBody(
          haveProperty[String]("enrolmentKey", be(enrolmentKey)) and
            haveProperty[Seq[JsObject]](
              "verifiers",
              eachElement(
                haveProperty[String]("key") and
                  haveProperty[String]("value")
              )
            )
        )
        feedback.json.as[uk.gov.hmrc.agentsexternalstubs.models.KnownFacts].getVerifierValue("NINO") shouldBe Some(
          "AB087054B"
        )
      }
    }

    "PUT /agents-external-stubs/known-facts/:enrolmentKey/verifier" should {
      "update single known fact verifier and return 202" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
        Users.create(
          UserGenerator.individual("foo1").withAssignedPrincipalEnrolment(EnrolmentKey(enrolmentKey)),
          Some(AG.Individual)
        )

        val result = KnownFacts.upsertKnownFactVerifier(
          enrolmentKey,
          Json.parse(s"""{"key": "NINO", "value": "AB087054B"}""")
        )
        result should haveStatus(202)

        val feedback = KnownFacts.getKnownFacts(enrolmentKey)
        feedback should haveStatus(200)
        feedback should haveValidJsonBody(
          haveProperty[String]("enrolmentKey", be(enrolmentKey)) and
            haveProperty[Seq[JsObject]](
              "verifiers",
              eachElement(
                haveProperty[String]("key") and
                  haveProperty[String]("value")
              )
            )
        )
        feedback.json.as[uk.gov.hmrc.agentsexternalstubs.models.KnownFacts].getVerifierValue("NINO") shouldBe Some(
          "AB087054B"
        )
      }
    }

    "DELETE /agents-external-stubs/known-facts/:enrolmentKey" should {
      "remove known fact and return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
        Users.create(
          UserGenerator.individual("foo1").withAssignedPrincipalEnrolment(EnrolmentKey(enrolmentKey)),
          Some(AG.Individual)
        )

        val result = KnownFacts.deleteKnownFacts(enrolmentKey)
        result should haveStatus(204)

        val feedback = KnownFacts.getKnownFacts(enrolmentKey)
        feedback should haveStatus(404)
      }
    }

    "POST /agents-external-stubs/known-facts/regime/PAYE/:agentId" should {
      "return 204 NoContent" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val agentId = "MARN5311437"

        val result = KnownFacts.createPAYEKnownFacts(agentId)
        result should haveStatus(204)

      }
    }
  }
}
