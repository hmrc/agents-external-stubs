package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED, UNPROCESSABLE_ENTITY}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, Enrolment, RelationshipRecord, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services.RecordsService
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.LocalDate

class HipStubControllerISpec
    extends ServerBaseISpec with TestRequests with TestStubs with ExampleDesPayloads with WireMockSupport {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val repo: RecordsRepository = app.injector.instanceOf[RecordsRepository]
  lazy val recordsService: RecordsService = app.injector.instanceOf[RecordsService]

  "HipStubController.displayAgentRelationship" when {

    "results are found" should {
      "return 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        await(
          repo.store(
            RelationshipRecord(
              regime = "VATC",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "987654321",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))
            ),
            session.planetId
          )
        )

        val result = HipStub.displayAgentRelationship(
          regime = Some("ITSA"),
          isAnAgent = Some(true),
          activeOnly = Some(true),
          arn = Some("ZARN1234567")
        )

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """relationshipDisplayResponse":[{"refNumber":"012345678901234","arn":"ZARN1234567","individual":{"firstName":"Caden","lastName":"Foran"},"dateFrom":"2012-01-01","dateTo":"9999-12-31","contractAccountCategory":"33"}]}"""
        )
      }
    }

    "no relationship records are found for an agent" should {
      "return an empty array" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val result = HipStub.displayAgentRelationship(
          regime = Some("ITSA"),
          isAnAgent = Some(true),
          activeOnly = Some(true),
          arn = Some("ZARN1234567")
        )

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """relationshipDisplayResponse":[]"""
        )
      }
    }

    "no relationship records are found for a non-agent" should {
      "return an empty array" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val result = HipStub.displayAgentRelationship(
          regime = Some("ITSA"),
          isAnAgent = Some(false),
          activeOnly = Some(true),
          refNumber = Some("123456789012345")
        )

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """relationshipDisplayResponse":[]"""
        )
      }
    }

    "no business partner record is found" should {
      "return 404" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.displayAgentRelationship()

        result should haveStatus(NOT_FOUND)
        result.json.toString() should include("""code":"INVALID_SUBMISSION","reason":"No BusinessPartnerRecord found""")
      }
    }

    "there is an issue with the headers" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.displayAgentRelationship(transmittingSystemHeader = None)

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"TBC","text":"transmittingSystem header missing or invalid"""
        )
      }
    }

    "there is an issue with the query parameters" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.displayAgentRelationship(regime = None)

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"001","text":"Missing SAP Number or Regime"""
        )
      }
    }

    "the agent is suspended" should {
      "return Agent Suspended error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        val storedUser: User = await(userService.createUser(user, session.planetId, Some(AG.Agent)))
        await(
          groupsService.updateGroup(storedUser.groupId.get, session.planetId, _.copy(suspendedRegimes = Set("ITSA")))
        )

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        val result =
          HipStub.displayAgentRelationship(
            regime = Some("ITSA"),
            isAnAgent = Option(true),
            activeOnly = Option(true),
            arn = Some("ZARN1234567")
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include("""code":"059","text":"ZARN1234567 is currently suspended""")
      }
    }

    "there is no session" should {
      "return an unauthorized error" in {
        val result = wsClient.url(s"$url/RESTAdapter/rosm/agent-relationship").get().futureValue

        result should haveStatus(UNAUTHORIZED)
        result.json.toString should include("""{"code":"UNAUTHORIZED","reason":"SessionRecordNotFound"}""")
      }
    }

  }
}
