/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.Status._
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{RecordsService, RelationshipRecordsService}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.LocalDate

class HipStubControllerISpec
    extends ServerBaseISpec with TestRequests with TestStubs with ExampleDesPayloads with WireMockSupport {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val repo: RecordsRepository = app.injector.instanceOf[RecordsRepository]
  lazy val recordsService: RecordsService = app.injector.instanceOf[RecordsService]
  lazy val relationshipRecordsService: RelationshipRecordsService =
    app.injector.instanceOf[RelationshipRecordsService]

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
        val result = wsClient.url(s"$url/etmp/RESTAdapter/rosm/agent-relationship").get().futureValue

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
    "deauthorise with matching authProfile" should {
      "return Created and end the relationship" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val createResult = HipStub.updateAgentRelationship()
        createResult should haveStatus(CREATED)

        val before = await(
          relationshipRecordsService.findByKey(
            RelationshipRecord.fullKey("ITSA", "AARN1234567", "MTDBSA", "1234"),
            session.planetId
          )
        )
        before.size shouldBe 1
        before.head.active shouldBe true

        val deAuthResult = HipStub.updateAgentRelationship(action = "0002", authProfile = Some("ALL00001"))

        deAuthResult should haveStatus(CREATED)
        deAuthResult.json.toString() should include("""{"processingDate":""")

        val after = await(
          relationshipRecordsService.findByKey(
            RelationshipRecord.fullKey("ITSA", "AARN1234567", "MTDBSA", "1234"),
            session.planetId
          )
        )
        after.size shouldBe 1
        after.head.active shouldBe false
        after.head.endDate shouldBe defined
      }
    }
    "deauthorise with mismatched authProfile" should {
      "return 014 and leave the relationship active" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "AARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val createResult = HipStub.updateAgentRelationship()
        createResult should haveStatus(CREATED)

        val before = await(
          relationshipRecordsService.findByKey(
            RelationshipRecord.fullKey("ITSA", "AARN1234567", "MTDBSA", "1234"),
            session.planetId
          )
        )
        before.size shouldBe 1
        before.head.active shouldBe true

        // Attempt to deauthorise using a different authProfile to the one used when creating
        val deAuthResult =
          HipStub.updateAgentRelationship(action = "0002", authProfile = Some("ITSAS001"))

        deAuthResult should haveStatus(UNPROCESSABLE_ENTITY)
        deAuthResult.json.toString() should include("""code":"014","text":"No active relationship found"""")

        val after = await(
          relationshipRecordsService.findByKey(
            RelationshipRecord.fullKey("ITSA", "AARN1234567", "MTDBSA", "1234"),
            session.planetId
          )
        )
        after.size shouldBe 1
        after.head.active shouldBe true
        after.head.endDate should not be defined
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
          .toString() should include("""code":"014","text":"No active relationship found""")
      }
    }
  }

  "HipStubController.itsaTaxPayerBusinessDetails" when {
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
            BusinessDetailsRecord(
              safeId = "safe-1",
              nino = "AB732851A",
              mtdId = "WOHV90190595538"
            ),
            session.planetId
          )
        )

        val result = HipStub.itsaTaxPayerBusinessDetails()

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """"nino":"AB732851A","mtdId":"WOHV90190595538",""".stripMargin
        )
      }
    }

    "results are found for a suffixless nino request" should {
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
            BusinessDetailsRecord(
              safeId = "safe-1",
              nino = "AB732851A",
              mtdId = "WOHV90190595538"
            ),
            session.planetId
          )
        )

        val result = HipStub.itsaTaxPayerBusinessDetails(nino = Some("AB732851"))

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """"nino":"AB732851A","mtdId":"WOHV90190595538",""".stripMargin
        )
      }
    }

    "results are found for a suffixless nino in database" should {
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
            BusinessDetailsRecord(
              safeId = "safe-1",
              nino = "AB732851",
              mtdId = "WOHV90190595538"
            ),
            session.planetId
          )
        )

        val result = HipStub.itsaTaxPayerBusinessDetails(nino = Some("AB732851A"))

        result should haveStatus(OK)
        result.json
          .toString() should include(
          """"nino":"AB732851","mtdId":"WOHV90190595538",""".stripMargin
        )
      }
    }

    "results are not found due to suffix mismatch" should {
      "return 422" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        await(
          repo.store(
            BusinessDetailsRecord(
              safeId = "safe-1",
              nino = "AB732851A",
              mtdId = "WOHV90190595538"
            ),
            session.planetId
          )
        )

        val result = HipStub.itsaTaxPayerBusinessDetails(nino = Some("AB732851B"))

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"006","text":"Subscription data not found"""
        )
      }
    }

    "no ITSA taxpayer record is found" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo")
          .copy(assignedPrincipalEnrolments =
            Seq(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567").toEnrolmentKey.get)
          )
        await(userService.createUser(user, session.planetId, Some(AG.Agent)))

        val result = HipStub.itsaTaxPayerBusinessDetails()

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"006","text":"Subscription data not found"""
        )
      }
    }

    "there is an issue with the headers" should {
      "return a 422 error" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = HipStub.itsaTaxPayerBusinessDetails(transmittingSystemHeader = None)

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

        val result = HipStub.itsaTaxPayerBusinessDetails(nino = None, mtdReference = None)

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json
          .toString() should include(
          """code":"006","text":"Request could not be processed"""
        )
      }
    }

    "there is no session" should {
      "return an unauthorized error" in {
        val result = wsClient.url(s"$url/etmp/RESTAdapter/itsa/taxpayer/business-details").get().futureValue

        result should haveStatus(UNAUTHORIZED)
        result.json.toString should include("""{"code":"UNAUTHORIZED","reason":"SessionRecordNotFound"}""")
      }
    }

  }

  "HipStubController.createAgentSubscription" when {
    "all request parameters are valid" should {}
  }
}
