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
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.AgencyDetails
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.identifiers.SuspensionDetails
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{RecordsService, RelationshipRecordsService}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.{LocalDate, LocalDateTime}

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

  "HipStubController.getAgentSubscription" when {
    "all request parameters are valid" should {
      "return the agent subscription response with status 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val (
          updateDetailsStatus,
          amlSupervisionUpdateStatus,
          directorPartnerUpdateStatus,
          acceptNewTermsStatus,
          reriskStatus
        ) = (
          UpdateDetailsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AmlSupervisionUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          DirectorPartnerUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AcceptNewTermsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          ReriskStatus(AgencyDetailsStatusValue.fromString("ACCEPTED"))
        )

        val safeId = "XA0000123456789"

        val arn = Generator.arn(safeId)

        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = "XA0000123456789",
          utr = Some("1234567890"),
          agentReferenceNumber = Some(arn.value),
          isAnAgent = true,
          isAnASAgent = true,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option("ABC Agency"))
              .withAgencyAddress(
                Some(
                  BusinessPartnerRecord.UkAddress(
                    "1 Agency Street",
                    None,
                    None,
                    None,
                    "NE1 1DE",
                    "GB"
                  )
                )
              )
              .withAgencyEmail(Some("abc@test.com"))
              .withAgencyTelephoneNumber(Some("01911234567"))
              .withSupervisoryBody(Some("HMRC"))
              .withMembershipNumber(Some("1234567890"))
              .withEvidenceObjectReference(Some("1234e4567-e89b-12d3-a456-426614174000"))
              .withUpdateDetailsStatus(Some(updateDetailsStatus))
              .withAmlSupervisionUpdateStatus(Some(amlSupervisionUpdateStatus))
              .withDirectorPartnerUpdateStatus(Some(directorPartnerUpdateStatus))
              .withAcceptNewTermsStatus(Some(acceptNewTermsStatus))
              .withReriskStatus(Some(reriskStatus))
          ),
          suspensionDetails = Some(SuspensionDetails(false, None)),
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result = HipStub.getSubscription(
          arn = arn.value
        )

        result should haveStatus(OK)

        result should haveValidJsonBody(haveProperty[JsObject]("success", not be empty))

        val json = result.json

        (json \ "success" \ "processingDate").as[LocalDateTime] should be >= LocalDateTime.now().minusMinutes(1)
        (json \ "success" \ "utr").as[String] should be("1234567890")
        (json \ "success" \ "name").as[String] should be("ABC Agency")
        (json \ "success" \ "addr1").as[String] should be("10 New Street")
        (json \ "success" \ "addr2").toOption should be(empty)
        (json \ "success" \ "addr3").toOption should be(empty)
        (json \ "success" \ "addr4").toOption should be(empty)
        (json \ "success" \ "postcode").as[String] should be("AA11AA")
        (json \ "success" \ "country").as[String] should be("GB")
        (json \ "success" \ "phone").as[String] should be("01911234567")
        (json \ "success" \ "email").as[String] should be("abc@test.com")
        (json \ "success" \ "suspensionStatus").as[String] should be("F")
//        (json \ "success" \ "regime").toOption should be(empty)
        (json \ "success" \ "supervisoryBody").as[String] should be("HMRC")
        (json \ "success" \ "membershipNumber").as[String] should be("1234567890")
        (json \ "success" \ "evidenceObjectReference").as[String] should be("1234e4567-e89b-12d3-a456-426614174000")
        (json \ "success" \ "updateDetailsStatus").as[String] should be("ACCEPTED")
//        (json \ "success" \ "updateDetailsLastUpdated").as[String] should be("")
//        (json \ "success" \ "updateDetailsLastSuccessfullyComplete").as[String] should be("")
        (json \ "success" \ "amlSupervisionUpdateStatus").as[String] should be("ACCEPTED")
//        (json \ "success" \ "amlSupervisionUpdateLastUpdated").as[String] should be("")
//        (json \ "success" \ "amlSupervisionUpdateLastSuccessfullyComplete").as[String] should be("")
        (json \ "success" \ "directorPartnerUpdateStatus").as[String] should be("ACCEPTED")
//        (json \ "success" \ "directorPartnerUpdateLastUpdated").as[String] should be("")
//        (json \ "success" \ "directorPartnerUpdateLastSuccessfullyComplete").as[String] should be("")
        (json \ "success" \ "acceptNewTermsStatus").as[String] should be("ACCEPTED")
//        (json \ "success" \ "acceptNewTermsLastUpdated").as[String] should be("")
//        (json \ "success" \ "acceptNewTermsLastSuccessfullyComplete").as[String] should be("")
        (json \ "success" \ "reriskStatus").as[String] should be("ACCEPTED")
//        (json \ "success" \ "reriskLastUpdated").as[String] should be("")
//        (json \ "success" \ "reriskLastSuccessfullyComplete").as[String] should be("")

        println(json)
      }
    }

    "base headers are invalid" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val (
          updateDetailsStatus,
          amlSupervisionUpdateStatus,
          directorPartnerUpdateStatus,
          acceptNewTermsStatus,
          reriskStatus
        ) = (
          UpdateDetailsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AmlSupervisionUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          DirectorPartnerUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AcceptNewTermsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          ReriskStatus(AgencyDetailsStatusValue.fromString("ACCEPTED"))
        )

        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = "XA0000123456789",
          agentReferenceNumber = Some("ZARN1234567"),
          isAnAgent = true,
          isAnASAgent = true,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option("ABC Agency"))
              .withAgencyAddress(
                Some(
                  BusinessPartnerRecord.UkAddress(
                    "1 Agency Street",
                    None,
                    None,
                    None,
                    "NE1 1DE",
                    "GB"
                  )
                )
              )
              .withAgencyEmail(Some("abc@test.com"))
              .withAgencyTelephoneNumber(Some("01911234567"))
              .withSupervisoryBody(Some("HMRC"))
              .withMembershipNumber(Some("1234567890"))
              .withEvidenceObjectReference(Some("1234e4567-e89b-12d3-a456-426614174000"))
              .withUpdateDetailsStatus(Some(updateDetailsStatus))
              .withAmlSupervisionUpdateStatus(Some(amlSupervisionUpdateStatus))
              .withDirectorPartnerUpdateStatus(Some(directorPartnerUpdateStatus))
              .withAcceptNewTermsStatus(Some(acceptNewTermsStatus))
              .withReriskStatus(Some(reriskStatus))
          ),
          suspensionDetails = Some(SuspensionDetails(false, None)),
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result =
          HipStub.getSubscription(
            arn = "ZARN1234567",
            transmittingSystemHeader = Some("NOT_HIP")
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"003","text":"Request could not be processed""")
      }
    }

    "arn is invalid" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result =
          HipStub.getSubscription(
            arn = "NO_ARN!"
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"003","text":"Request could not be processed""")
      }
    }

    "business partner record does not exist" should {
      "return 422 Subscription Data Not Found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"

        val arn = Generator.arn(safeId)

        val result =
          HipStub.getSubscription(
            arn = arn.value
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"006","text":"Subscription Data Not Found""")
      }
    }

    "if it's not an ASA agent" should {
      "return 422 unprocessable entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val (
          updateDetailsStatus,
          amlSupervisionUpdateStatus,
          directorPartnerUpdateStatus,
          acceptNewTermsStatus,
          reriskStatus
        ) = (
          UpdateDetailsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AmlSupervisionUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          DirectorPartnerUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AcceptNewTermsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          ReriskStatus(AgencyDetailsStatusValue.fromString("ACCEPTED"))
        )

        val safeId = "XA0000123456789"

        val arn = Generator.arn(safeId)

        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = "XA0000123456789",
          utr = Some("1234567890"),
          agentReferenceNumber = Some(arn.value),
          isAnAgent = true,
          isAnASAgent = false,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option("ABC Agency"))
              .withAgencyAddress(
                Some(
                  BusinessPartnerRecord.UkAddress(
                    "1 Agency Street",
                    None,
                    None,
                    None,
                    "NE1 1DE",
                    "GB"
                  )
                )
              )
              .withAgencyEmail(Some("abc@test.com"))
              .withAgencyTelephoneNumber(Some("01911234567"))
              .withSupervisoryBody(Some("HMRC"))
              .withMembershipNumber(Some("1234567890"))
              .withEvidenceObjectReference(Some("1234e4567-e89b-12d3-a456-426614174000"))
              .withUpdateDetailsStatus(Some(updateDetailsStatus))
              .withAmlSupervisionUpdateStatus(Some(amlSupervisionUpdateStatus))
              .withDirectorPartnerUpdateStatus(Some(directorPartnerUpdateStatus))
              .withAcceptNewTermsStatus(Some(acceptNewTermsStatus))
              .withReriskStatus(Some(reriskStatus))
          ),
          suspensionDetails = Some(SuspensionDetails(false, None)),
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result =
          HipStub.getSubscription(
            arn = arn.value
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"006","text":"Subscription Data Not Found""")
      }
    }

    "agent is suspended" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val (
          updateDetailsStatus,
          amlSupervisionUpdateStatus,
          directorPartnerUpdateStatus,
          acceptNewTermsStatus,
          reriskStatus
        ) = (
          UpdateDetailsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AmlSupervisionUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          DirectorPartnerUpdateStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          AcceptNewTermsStatus(AgencyDetailsStatusValue.fromString("ACCEPTED")),
          ReriskStatus(AgencyDetailsStatusValue.fromString("ACCEPTED"))
        )

        val safeId = "XA0000123456789"

        val arn = Generator.arn(safeId)

        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = "XA0000123456789",
          utr = Some("1234567890"),
          agentReferenceNumber = Some(arn.value),
          isAnAgent = true,
          isAnASAgent = true,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option("ABC Agency"))
              .withAgencyAddress(
                Some(
                  BusinessPartnerRecord.UkAddress(
                    "1 Agency Street",
                    None,
                    None,
                    None,
                    "NE1 1DE",
                    "GB"
                  )
                )
              )
              .withAgencyEmail(Some("abc@test.com"))
              .withAgencyTelephoneNumber(Some("01911234567"))
              .withSupervisoryBody(Some("HMRC"))
              .withMembershipNumber(Some("1234567890"))
              .withEvidenceObjectReference(Some("1234e4567-e89b-12d3-a456-426614174000"))
              .withUpdateDetailsStatus(Some(updateDetailsStatus))
              .withAmlSupervisionUpdateStatus(Some(amlSupervisionUpdateStatus))
              .withDirectorPartnerUpdateStatus(Some(directorPartnerUpdateStatus))
              .withAcceptNewTermsStatus(Some(acceptNewTermsStatus))
              .withReriskStatus(Some(reriskStatus))
          ),
          suspensionDetails = Some(SuspensionDetails(true, None)),
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result = HipStub.getSubscription(
          arn = arn.value
        )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"058","text":"Agent is terminated""")

      }
    }

    "there is no session" should {
      "return an unauthorized error" in {
        val arn = "ZARN1234567"

        val result =
          wsClient
            .url(s"$url/etmp/RESTAdapter/generic/agent/subscription/$arn")
            .get()
            .futureValue

        result should haveStatus(UNAUTHORIZED)
        result.json.toString should include("""{"code":"UNAUTHORIZED","reason":"SessionRecordNotFound"}""")
      }
    }

  }

  "HipStubController.createAgentSubscription" when {

    "all request parameters are valid" should {
      "return Created and update the business partner record" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"
        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = safeId,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = None,
          suspensionDetails = None,
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result = HipStub.createAgentSubscription(
          safeId = safeId,
          name = "Alex Rider",
          addr1 = "River House",
          addr2 = Some("London"),
          addr3 = None,
          addr4 = None,
          postcode = Some("W11AA"),
          country = "GB",
          phone = Some("01911234567"),
          email = "test@example.com",
          supervisoryBody = Some("Mi6"),
          membershipNumber = Some("MEM123"),
          evidenceObjectReference = Some("123e4567-e89b-12d3-a456-426614174000"),
          updateDetailsStatus = "ACCEPTED",
          amlSupervisionUpdateStatus = "PENDING",
          directorPartnerUpdateStatus = "REQUIRED",
          acceptNewTermsStatus = "ACCEPTED",
          reriskStatus = "REJECTED"
        )

        result should haveStatus(CREATED)
        result should haveValidJsonBody(
          haveProperty[String]("arn", not be empty) and
            haveProperty[String]("processingDate", not be empty)
        )

        val updated =
          await(recordsService.getRecordMaybeExt[BusinessPartnerRecord, SafeId](SafeId(safeId), session.planetId))
        updated shouldBe defined

        val record = updated.getOrElse(fail)
        record.isAnAgent shouldBe true
        record.isAnASAgent shouldBe true

        record.addressDetails match {
          case a: BusinessPartnerRecord.UkAddress =>
            a.addressLine1 shouldBe "River House"
            a.addressLine2 shouldBe Some("London")
            a.postalCode shouldBe "W11AA"
            a.countryCode shouldBe "GB"
          case other =>
            fail(s"Expected UkAddress but got: ${other.getClass.getSimpleName}")
        }

        val agentDetails = record.agencyDetails.getOrElse(fail)

        agentDetails.agencyAddress match {
          case Some(a: BusinessPartnerRecord.UkAddress) =>
            a.addressLine1 shouldBe "River House"
            a.addressLine2 shouldBe Some("London")
            a.postalCode shouldBe "W11AA"
            a.countryCode shouldBe "GB"
          case Some(other) =>
            fail(s"Expected UkAddress but got: ${other.getClass.getSimpleName}")
          case None =>
            fail("Expected agencyAddress to be defined")
        }

        agentDetails.agencyEmail shouldBe Some("test@example.com")
        agentDetails.agencyTelephone shouldBe Some("01911234567")
        agentDetails.supervisoryBody shouldBe Some("Mi6")
        agentDetails.membershipNumber shouldBe Some("MEM123")
        agentDetails.evidenceObjectReference shouldBe Some("123e4567-e89b-12d3-a456-426614174000")

        agentDetails.updateDetailsStatus match {
          case Some(UpdateDetailsStatus(status, lastUpdated, lastSuccessfulUpdate)) =>
            status shouldBe AgencyDetailsStatusValue.Accepted

            val now = LocalDateTime.now()
            lastUpdated should (be >= now.minusMinutes(1) and be <= now)
            lastSuccessfulUpdate should (be >= now.minusMinutes(1) and be <= now)

          case other =>
            fail(s"Expected UpdateDetailsStatus but got: $other")
        }
      }
    }

    "base headers are invalid" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"
        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = safeId,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = None,
          suspensionDetails = None,
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result =
          HipStub.createAgentSubscription(
            safeId = safeId,
            transmittingSystemHeader = Some("NOT_HIP")
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"003","text":"Request could not be processed""")
      }
    }

    "safeId is invalid" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result =
          HipStub.createAgentSubscription(
            safeId = "NOT_SAFE!"
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"003","text":"Request could not be processed""")
      }
    }

    "body contains invalid data" should {
      "return 422 Unprocessable Entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"
        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = safeId,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = None,
          suspensionDetails = None,
          id = None
        )

        await(repo.store(existingRecord, session.planetId))

        val result =
          HipStub.createAgentSubscription(
            safeId = safeId,
            name = ""
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("Request could not be processed")
      }
    }

    "business partner record does not exist" should {
      "return 422 SAFE ID Not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"

        val result =
          HipStub.createAgentSubscription(
            safeId = safeId
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"006","text":"SAFE ID Not found""")
      }
    }

    "business partner is already subscribed" should {
      "return 422 with already subscribed message" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val safeId = "XA0000123456789"
        val existingRecord = BusinessPartnerRecord(
          businessPartnerExists = true,
          safeId = safeId,
          isAnIndividual = true,
          individual = Some(BusinessPartnerRecord.Individual("Bill", None, "Jones", "1990-01-01")),
          organisation = None,
          addressDetails = BusinessPartnerRecord.UkAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = "AA11AA",
            countryCode = "GB"
          ),
          contactDetails = Some(BusinessPartnerRecord.ContactDetails()),
          agencyDetails = Some(
            AgencyDetails(
              agencyName = Some("existing"),
              agencyAddress = None,
              agencyEmail = None,
              agencyTelephone = None,
              supervisoryBody = None,
              membershipNumber = None,
              evidenceObjectReference = None,
              updateDetailsStatus = Some(
                UpdateDetailsStatus(
                  AgencyDetailsStatusValue.Accepted,
                  LocalDateTime.now(),
                  LocalDateTime.now()
                )
              ),
              amlSupervisionUpdateStatus = None,
              directorPartnerUpdateStatus = None,
              acceptNewTermsStatus = None,
              reriskStatus = None
            )
          ),
          suspensionDetails = None,
          id = None
        ).copy(
          isAnAgent = true,
          isAnASAgent = true,
          agentReferenceNumber = Some("ZARN1234567")
        )

        await(repo.store(existingRecord, session.planetId))

        val result =
          HipStub.createAgentSubscription(
            safeId = safeId,
            name = "Alex Rider",
            addr1 = "River House",
            addr2 = Some("London"),
            postcode = Some("W11AA"),
            country = "GB",
            phone = Some("01911234567"),
            email = "test@example.com"
          )

        result should haveStatus(UNPROCESSABLE_ENTITY)
        result.json.toString should include("""code":"061"""")
        result.json.toString should include("""BP has already a valid Agent Subscription ZARN1234567""")
      }
    }

    "there is no session" should {
      "return an unauthorized error" in {
        val safeId = "XA0000123456789"

        val result =
          wsClient
            .url(s"$url/etmp/RESTAdapter/generic/agent/subscription/$safeId")
            .post(play.api.libs.json.Json.obj("name" -> "x"))
            .futureValue

        result should haveStatus(UNAUTHORIZED)
        result.json.toString should include("""{"code":"UNAUTHORIZED","reason":"SessionRecordNotFound"}""")
      }
    }
  }

}
