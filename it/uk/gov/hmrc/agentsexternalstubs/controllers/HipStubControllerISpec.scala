package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.Status.{CREATED, NOT_FOUND, OK, UNAUTHORIZED, UNPROCESSABLE_ENTITY}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
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
      "return a 422 error" in {
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

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"009","text":"No Relationships with activity"""
        )
      }
    }

    "no relationship records are found for a non-agent" should {
      "return a 422 error" in {
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

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"009","text":"No Relationships with activity"""
        )
      }
    }

    "no business partner record is found" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.displayAgentRelationship()

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString() should include("""code":"009","text":"No Relationships with activity""")
      }
    }

    "there is an issue with the headers" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.displayAgentRelationship(transmittingSystemHeader = None)

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"006","text":"Request could not be processed"""
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

  "HipStubController.updateAgentRelationship" when {
    "all request parameters are valid" should {
      "return Created" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val result = HipStub.updateAgentRelationship()

        result should haveStatus(CREATED)
        result.json
          .toString() should include(
          """{"processingDate":"""
        )
      }

    }
    "there is an issue with the headers" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.updateAgentRelationship(transmittingSystemHeader = None)

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"006","text":"Request could not be processed"""
        )
      }
    }

    "body contains invalid data" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.updateAgentRelationship(idType = Some("CTUTR"))

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"013","text":"ID Type is invalid or missing"""
        )
      }
    }

    "no business partner record found" should {
      "return 404 Not Found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.updateAgentRelationship()

        result should haveStatus(NOT_FOUND)
        result.body should include(
          """no business partner record found"""
        )
      }
    }

    "agent is suspended" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        val storedUser: User = await(userService.createUser(user, session.planetId, Some(AG.Agent)))
        await(
          groupsService.updateGroup(storedUser.groupId.get, session.planetId, _.copy(suspendedRegimes = Set("ITSA")))
        )

        val result = HipStub.updateAgentRelationship()

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include("""code":"059","text":"AARN1234567 is currently suspended""")
      }
    }

    "vat customer is insolvent" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        val storedUser: User = await(userService.createUser(user, session.planetId, Some(AG.Agent)))
        await(
          groupsService.updateGroup(storedUser.groupId.get, session.planetId, _.copy(suspendedRegimes = Set("ITSA")))
        )

        await(
          repo.store(
            VatCustomerInformationRecord(
              vrn = "1234567890",
              approvedInformation = Some(
                ApprovedInformation(
                  customerDetails = CustomerDetails(isInsolvent = Some(true), mandationStatus = "???"),
                  PPOB = PPOB.gen.sample.get
                )
              )
            ),
            session.planetId
          )
        )

        val result = HipStub.updateAgentRelationship(regime = "VATC", idType = Some("VRN"), refNumber = "1234567890")

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include("""code":"094","text":"Insolvent Trader - request could not be completed""")
      }
    }

    "deauthorise and no relationship exists" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val result = HipStub.updateAgentRelationship(action = "0002")

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include("""code":"014","text":"No active relationship""")
      }
    }
  }
}
