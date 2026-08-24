/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.http.Status.{BAD_REQUEST, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.*
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.*

class DesIfStubControllerISpec
    extends ServerBaseISpec with TestRequests with TestStubs with WireMockSupport with ExampleDesPayloads {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "DesIfStubController.getLegacyRelationshipsByNino" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getLegacyRelationshipsByNino("AA000001A") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getLegacyRelationshipsByNino("AA000001A") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid nino" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getLegacyRelationshipsByNino("BADNINO") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getLegacyRelationshipsByUtr" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getLegacyRelationshipsByUtr("1234567890") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getLegacyRelationshipsByUtr("1234567890") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid utr" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getLegacyRelationshipsByUtr("BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getVatCustomerInformation" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload)) should haveStatus(201)
      DesStub.getVatCustomerInformation("123456789") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getVatCustomerInformation("101747696") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid vrn" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getVatCustomerInformation("BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getBusinessPartnerRecord" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createBusinessPartnerRecord(
        Json.parse(validBusinessPartnerRecordPayload),
        autoFill = false
      ) should haveStatus(201)
      DesStub.getBusinessPartnerRecord("utr", "0123456789") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getBusinessPartnerRecord("utr", "1234567890") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid identifier type" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getBusinessPartnerRecord("badType", "12345") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getVatKnownFacts" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload)) should haveStatus(201)
      DesStub.getVatKnownFacts("123456789") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getVatKnownFacts("101747696") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid vrn" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getVatKnownFacts("BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.subscribeAgentServicesWithUtr" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createBusinessPartnerRecord(
        Json.parse(validBusinessPartnerRecordPayload),
        autoFill = false
      ) should haveStatus(201)
      DesStub.subscribeToAgentServicesWithUtr("0123456789", Json.parse(validAgentSubmission)) should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj("agencyName" -> "Agency", "agencyAddress" -> Json.obj("addressLine1" -> "1 Any Street"))
      DesStub.subscribeToAgentServicesWithUtr("1234567890", payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid utr" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.subscribeToAgentServicesWithUtr("BAD", Json.obj()) should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.subscribeAgentServicesWithSafeId" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createBusinessPartnerRecord(
        Json.parse(validBusinessPartnerRecordPayload),
        autoFill = false
      ) should haveStatus(201)
      DesStub.subscribeToAgentServicesWithSafeId("XE0001234567890", Json.parse(validAgentSubmission)) should haveStatus(
        200
      )
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj("agencyName" -> "Agency", "agencyAddress" -> Json.obj("addressLine1" -> "1 Any Street"))
      DesStub.subscribeToAgentServicesWithSafeId("XE0001234567890", payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid safeId" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.subscribeToAgentServicesWithSafeId("BAD_SAFE_ID_TOO_LONG_123456", Json.obj()) should haveStatus(
        BAD_REQUEST
      )
    }
  }

  "DesIfStubController.register" should {
    "return 200 for the individual route on a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj("regime" -> "ITSA", "individual" -> Json.obj("firstName" -> "A", "lastName" -> "B"))
      DesStub.registerIndividual("nino", "AA000001A", payload) should haveStatus(200)
    }

    "return 200 for the organisation route on a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload =
        Json.obj(
          "regime"       -> "ITSA",
          "organisation" -> Json.obj("organisationName" -> "Org", "organisationType" -> "AAAA")
        )
      DesStub.registerOrganisation("utr", "1234567890", payload) should haveStatus(200)
    }

    "return 401 for the individual route when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj("regime" -> "ITSA", "individual" -> Json.obj("firstName" -> "A", "lastName" -> "B"))
      DesStub.registerIndividual("nino", "AA000001A", payload) should haveStatus(UNAUTHORIZED)
    }

    "return 401 for the organisation route when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj(
        "regime"       -> "ITSA",
        "organisation" -> Json.obj("organisationName" -> "Org", "organisationType" -> "Company")
      )
      DesStub.registerOrganisation("utr", "1234567890", payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid identifier on individual route" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj("regime" -> "ITSA", "individual" -> Json.obj("firstName" -> "A", "lastName" -> "B"))
      DesStub.registerIndividual("nino", "BADNINO", payload) should haveStatus(BAD_REQUEST)
    }

    "return 400 for invalid identifier on organisation route" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj(
        "regime"       -> "ITSA",
        "organisation" -> Json.obj("organisationName" -> "Org", "organisationType" -> "Company")
      )
      DesStub.registerOrganisation("utr", "BAD", payload) should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.agentClientAuthorisationFlags" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload)) should haveStatus(201)
      DesStub.getSAAgentClientAuthorisationFlags("SA6012", "1234567890") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getSAAgentClientAuthorisationFlags("A1234", "1234567890") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid utr" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getSAAgentClientAuthorisationFlags("A1234", "BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.registerIndividualWithoutID" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "individual" -> Json.obj("firstName" -> "First", "lastName" -> "Last", "dateOfBirth" -> "1990-01-01"),
        "address"    -> Json.obj(
          "addressLine1" -> "1 Any Street",
          "addressLine2" -> "Abcd",
          "countryCode"  -> "GB",
          "postalCode"   -> "AB1 2CD"
        ),
        "contactDetails" -> Json.obj()
      )
      DesStub.registerIndividualWithoutID(payload) should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "individual"               -> Json.obj("firstName" -> "First", "lastName" -> "Last"),
        "address"                  -> Json.obj("addressLine1" -> "1 Any Street"),
        "contactDetails"           -> Json.obj()
      )
      DesStub.registerIndividualWithoutID(payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid payload" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "address"                  -> Json.obj("addressLine1" -> "1 Any Street"),
        "contactDetails"           -> Json.obj()
      )
      DesStub.registerIndividualWithoutID(payload) should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.registerOrganisationWithoutID" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "organisation"             -> Json.obj("organisationName" -> "Org"),
        "address"                  -> Json.obj(
          "addressLine1" -> "1 Any Street",
          "addressLine2" -> "Abcd",
          "countryCode"  -> "GB",
          "postalCode"   -> "AB1 2CD"
        ),
        "contactDetails" -> Json.obj()
      )
      DesStub.registerOrganisationWithoutID(payload) should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "organisation"             -> Json.obj("organisationName" -> "Org"),
        "address"                  -> Json.obj("addressLine1" -> "1 Any Street"),
        "contactDetails"           -> Json.obj()
      )
      DesStub.registerOrganisationWithoutID(payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid payload" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      val payload = Json.obj(
        "regime"                   -> "ITSA",
        "acknowledgementReference" -> "ACK123",
        "address"                  -> Json.obj("addressLine1" -> "1 Any Street"),
        "contactDetails"           -> Json.obj()
      )
      DesStub.registerOrganisationWithoutID(payload) should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.retrieveLegacyAgentClientPayeInformation" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      val employerAuths = Json.obj(
        "agentCode"   -> "AGENT01",
        "empAuthList" -> Json.arr(
          Json.obj(
            "empRef" -> Json.obj("districtNumber" -> "123", "reference" -> "AB12345"),
            "aoRef"  -> Json.obj(
              "districtNumber" -> "123",
              "payType"        -> "PAYE",
              "checkCode"      -> "AB12",
              "reference"      -> "AO123"
            )
          )
        )
      )

      Records.createEmployerAuths(employerAuths) should haveStatus(201)
      val queryPayload =
        Json.obj("empRefList" -> Json.arr(Json.obj("districtNumber" -> "123", "reference" -> "AB12345")))
      DesStub.retrieveLegacyAgentClientPayeInformation("AGENT01", queryPayload) should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      val payload = Json.obj("empRefList" -> Json.arr(Json.obj("districtNumber" -> "1", "reference" -> "AB12345")))
      DesStub.retrieveLegacyAgentClientPayeInformation("AGENT01", payload) should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid agent code" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.retrieveLegacyAgentClientPayeInformation("BAD", Json.obj()) should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.removeLegacyAgentClientPayeRelationship" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      val employerAuths = Json.obj(
        "agentCode"   -> "AGENT01",
        "empAuthList" -> Json.arr(
          Json.obj(
            "empRef" -> Json.obj("districtNumber" -> "123", "reference" -> "AB12345"),
            "aoRef"  -> Json.obj(
              "districtNumber" -> "123",
              "payType"        -> "PAYE",
              "checkCode"      -> "AB12",
              "reference"      -> "AO123"
            )
          )
        )
      )

      Records.createEmployerAuths(employerAuths) should haveStatus(201)
      DesStub.removeLegacyAgentClientPayeRelationship("AGENT01", "123", "AB12345") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.removeLegacyAgentClientPayeRelationship("AGENT01", "123", "AB12345") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid agent code" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.removeLegacyAgentClientPayeRelationship("bad", "123", "AB12345") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getCtReference" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createBusinessPartnerRecord(
        Json.parse(validBusinessPartnerRecordPayload),
        autoFill = false
      ) should haveStatus(201)
      DesStub.getCtReference("crn", "AA123456") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getCtReference("crn", "123456") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid identifier type" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getCtReference("badType", "12345") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getTrustKnownFactsUTR" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      userService
        .updateUser(
          session.userId,
          session.planetId,
          _.copy(
            credentialRole = Some(User.CR.Admin),
            name = Some("Trust User"),
            address = Some(User.Address(line1 = Some("1 Street"), countryCode = Some("GB")))
          )
        )
        .futureValue

      val currentUser =
        userService.findByUserId(session.userId, session.planetId).futureValue.getOrElse(fail("Missing user"))
      val groupId = currentUser.groupId.getOrElse(fail("Missing group id"))

      groupsService
        .updateGroup(
          groupId,
          session.planetId,
          group =>
            group.copy(
              affinityGroup = "Organisation",
              principalEnrolments = group.principalEnrolments :+ Enrolment("HMRC-TERS-ORG", "SAUTR", "1234567890")
            )
        )
        .futureValue

      DesStub.getTrustKnownFactsUtr("1234567890") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getTrustKnownFactsUtr("1234567890") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid utr" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getTrustKnownFactsUtr("BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getTrustKnownFactsURN" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      userService
        .updateUser(
          session.userId,
          session.planetId,
          _.copy(
            credentialRole = Some(User.CR.Admin),
            name = Some("Trust User"),
            address = Some(User.Address(line1 = Some("1 Street"), countryCode = Some("GB")))
          )
        )
        .futureValue

      val currentUser =
        userService.findByUserId(session.userId, session.planetId).futureValue.getOrElse(fail("Missing user"))
      val groupId = currentUser.groupId.getOrElse(fail("Missing group id"))

      groupsService
        .updateGroup(
          groupId,
          session.planetId,
          group =>
            group.copy(
              affinityGroup = "Organisation",
              principalEnrolments = group.principalEnrolments :+ Enrolment("HMRC-TERSNT-ORG", "URN", "AATRUST00000000")
            )
        )
        .futureValue

      DesStub.getTrustKnownFactsUrn("AATRUST00000000") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getTrustKnownFactsUrn("aatrust00000000") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid urn" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getTrustKnownFactsUrn("BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getCgtSubscription" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      userService
        .updateUser(
          session.userId,
          session.planetId,
          _.copy(
            credentialRole = Some(User.CR.Admin),
            name = Some("Joe Bloggs"),
            address = Some(User.Address(line1 = Some("1 Street"), countryCode = Some("GB")))
          )
        )
        .futureValue

      val currentUser =
        userService.findByUserId(session.userId, session.planetId).futureValue.getOrElse(fail("Missing user"))
      val groupId = currentUser.groupId.getOrElse(fail("Missing group id"))

      groupsService
        .updateGroup(
          groupId,
          session.planetId,
          group =>
            group.copy(principalEnrolments =
              group.principalEnrolments :+ Enrolment("HMRC-CGT-PD", "CGTPDRef", "XACGTP123456789")
            )
        )
        .futureValue

      get("/subscriptions/CGT/ZCGT/XACGTP123456789") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      get("/subscriptions/CGT/ZCGT/XACGTP123456789") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid cgt reference" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      get("/subscriptions/CGT/ZCGT/BAD") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getAmlsSubscriptionStatus" should {
    "return 200 for a happy path" in {
      given AuthContext = NotAuthorized
      DesStub.getAmlsSubscriptionStatus("XAML00000200000") should haveStatus(200)
    }

    "return 400 for invalid amls registration number" in {
      given AuthContext = NotAuthorized
      DesStub.getAmlsSubscriptionStatus("BAD").should(haveStatus(BAD_REQUEST))
    }
  }

  "DesIfStubController.getPPTSubscriptionDisplay" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createPPTSubscriptionDisplayRecord(Json.parse(validPPTSubscriptionDisplayPayload)) should haveStatus(201)
      DesStub.getPPTSubscriptionDisplayRecord("PPT", "XAPPT0001234567") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getPPTSubscriptionDisplayRecord("PPT", "XAPPT0001234567") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid regime" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getPPTSubscriptionDisplayRecord("BAD", "XAPPT0001234567") should haveStatus(BAD_REQUEST)
    }
  }

  "DesIfStubController.getPillar2SubscriptionDetails" should {
    "return 200 for a happy path" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)

      Records.createPillar2Record(Json.parse(validPillar2SubscriptionPayload)) should haveStatus(201)
      DesStub.getPillar2Record("XAPLR2222222222") should haveStatus(200)
    }

    "return 401 when no session exists" in {
      given AuthContext = NotAuthorized
      DesStub.getPillar2Record("XAPLR1234567890") should haveStatus(UNAUTHORIZED)
    }

    "return 400 for invalid plr reference" in {
      val session = SignIn.signInAndGetSession()
      given AuthContext = AuthContext.fromTokenAndSessionId(session.authToken, session.sessionId)
      DesStub.getPillar2Record("BAD") should haveStatus(BAD_REQUEST)
    }
  }
}
