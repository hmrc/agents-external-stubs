/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, EnrolmentKey, KnownFact, KnownFacts, User}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class KnownFactsControllerSpec extends BaseUnitSpec {

  trait TestScope {
    val mockKnownFactsRepo: KnownFactsRepository = mock[KnownFactsRepository]
    val mockUsersRepo: UsersRepository = mock[UsersRepository]
    val mockAuthService: AuthenticationService = mock[AuthenticationService]
    val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
    implicit val eC: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val authSessionHeaders: FakeHeaders = FakeHeaders(
      Seq(
        AuthenticatedSession.TAG_SESSION_ID    -> "session123",
        AuthenticatedSession.TAG_USER_ID       -> "userId",
        AuthenticatedSession.TAG_AUTH_TOKEN    -> "good",
        AuthenticatedSession.TAG_PROVIDER_TYPE -> "off",
        AuthenticatedSession.TAG_PLANET_ID     -> "earth467"
      )
    )

    def jsRequest(method: String, uri: String, jsonBody: JsValue): FakeRequest[JsValue] =
      FakeRequest[JsValue](method, uri, authSessionHeaders, jsonBody).withSession(SessionKeys.authToken -> "Bearer XYZ")

    def requestWithAuthSession(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(method, uri)
        .withHeaders(authSessionHeaders)
        .withSession(SessionKeys.authToken -> "Bearer XYZ")

    val controller = new KnownFactsController(
      mockKnownFactsRepo,
      mockUsersRepo,
      mockAuthService,
      controllerComponents
    )

    // known facts repo mocks
    def expectFindKnownFactByEnrolmentKey(knownFacts: Option[KnownFacts]): OngoingStubbing[Future[Option[KnownFacts]]] =
      when(mockKnownFactsRepo.findByEnrolmentKey(any[EnrolmentKey], any[String])(any[ExecutionContext]))
        .thenReturn(Future.successful(knownFacts))

    def expectUpsertKnownFacts: OngoingStubbing[Future[Unit]] =
      when(mockKnownFactsRepo.upsert(any[KnownFacts], any[String]))
        .thenReturn(Future.unit)

    def expectDeleteKnownFacts: OngoingStubbing[Future[Unit]] =
      when(mockKnownFactsRepo.delete(any[EnrolmentKey], any[String]))
        .thenReturn(Future.unit)

    // user repo mocks
    def expectFindUserByPrincipalEnrolmentKey(user: Option[User] = None): OngoingStubbing[Future[Option[User]]] =
      when(mockUsersRepo.findByPrincipalEnrolmentKey(any[EnrolmentKey], any[String]))
        .thenReturn(Future.successful(user))

    def expectFindUserByDelegatedEnrolmentKey(user: Seq[User] = Seq.empty): OngoingStubbing[Future[Seq[User]]] =
      when(mockUsersRepo.findByDelegatedEnrolmentKey(any[EnrolmentKey], any[String])(any[Int]))
        .thenReturn(Future.successful(user))

    val enrolmentKeyStr = "HMRC-MTD-IT~MTDITID~XAAA12345678901"
    val enrolment: EnrolmentKey = EnrolmentKey(enrolmentKeyStr)

    val knownFacts: KnownFacts =
      KnownFacts(enrolment, enrolment.identifiers, Seq(KnownFact("NINO", "AB087054B")), Some("earth467"))
    val matchingKnownFacts: KnownFacts = KnownFacts(enrolment, enrolment.identifiers, Seq.empty, Some("earth467"))
  }

  "GET /known-facts/:enrolmentKey" should {
    "return 200 with EnrolmentInfo (not known facts!)" in new TestScope {
      //given
      expectFindKnownFactByEnrolmentKey(Some(matchingKnownFacts))
      expectFindUserByPrincipalEnrolmentKey()
      expectFindUserByDelegatedEnrolmentKey()
      //when
      val result: Future[Result] =
        controller.getKnownFacts(enrolment).apply(requestWithAuthSession("GET", s"/known-facts/$enrolmentKeyStr"))
      //then
      status(result) shouldBe 200 // add check for EnrolmentInfo?
    }

    "return NOT_FOUND if not in repo" in new TestScope {
      //given
      expectFindKnownFactByEnrolmentKey(None)
      //when
      val result: Future[Result] =
        controller.getKnownFacts(enrolment).apply(requestWithAuthSession("GET", s"/known-facts/$enrolmentKeyStr"))
      //then
      status(result) shouldBe 404
    }

    "return unauthorised without session" in new TestScope {
      // given no auth session, when request
      // Note: will be the same for all endpoints apart from createPAYEKnownFacts, all require auth session headers
      val result: Future[Result] =
        controller.getKnownFacts(enrolment)(FakeRequest("GET", s"/known-facts/$enrolmentKeyStr"))
      status(result) shouldBe 401
    }
  }

  "POST /known-facts" should {
    "create a sanitized known fact and return 201" in new TestScope {
      //given
      expectUpsertKnownFacts

      val jsonPayload = Json.parse(s"""
        |{ "enrolmentKey": "$enrolmentKeyStr",
        |  "identifiers": [
        |   {
        |     "key": "MTDITID",
        |     "value": "XAAA12345678901"
        |   }
        |  ],
        | "verifiers": [
        |   {
        |     "key": "NINO",
        |     "value": ""
        |   }
        |  ]
        |} """.stripMargin)

      val request: FakeRequest[JsValue] = jsRequest("POST", s"/known-facts", jsonPayload)

      //then
      val result: Future[Result] = controller.createKnownFacts()(request)
      //when
      status(result) shouldBe 201
    }
  }

  "PUT /known-facts/:enrolmentKey" should {
    "create known facts if none found and return 201" in new TestScope {
      expectFindKnownFactByEnrolmentKey(None)
      expectUpsertKnownFacts

      val json: JsValue = Json.parse(s"""
        |{ "enrolmentKey": "$enrolmentKeyStr",
        |  "identifiers": [
        |   {
        |     "key": "MTDITID",
        |     "value": "XAAA098765432112"
        |   }
        |  ],
        | "verifiers": [
        |   {
        |     "key": "NINO",
        |     "value": "AB087054B"
        |   }
        |  ]
        |} """.stripMargin)
      val request: FakeRequest[JsValue] = jsRequest("PUT", s"/known-facts/$enrolmentKeyStr", json)

      val result: Future[Result] = controller.upsertKnownFacts(enrolment).apply(request)
      status(result) shouldBe 201
    }

    "update known facts and return 202" in new TestScope {
      expectFindKnownFactByEnrolmentKey(Some(matchingKnownFacts))
      expectUpsertKnownFacts

      val json: JsValue = Json.parse(s"""
        |{ "enrolmentKey": "$enrolmentKeyStr",
        |  "identifiers": [
        |   {
        |     "key": "MTDITID",
        |     "value": "XAAA098765432112"
        |   }
        |  ],
        | "verifiers": [
        |   {
        |     "key": "NINO",
        |     "value": "AB087054B"
        |   }
        |  ]
        |} """.stripMargin)
      val request: FakeRequest[JsValue] = jsRequest("PUT", s"/known-facts/$enrolmentKeyStr", json)

      val result: Future[Result] = controller.upsertKnownFacts(enrolment).apply(request)
      status(result) shouldBe 202
    }
  }

  "PUT /known-facts/:enrolmentKey/verifier" should {
    "update known fact verifier and return 202" in new TestScope {
      expectFindKnownFactByEnrolmentKey(Some(matchingKnownFacts))
      expectUpsertKnownFacts

      val json: JsValue = Json.parse(s"""
        |{
        |     "key": "NINO",
        |     "value": "AB087054B"
        |} """.stripMargin)
      val request: FakeRequest[JsValue] = jsRequest("PUT", s"/known-facts/$enrolmentKeyStr", json)

      val result: Future[Result] = controller.upsertKnownFactVerifier(enrolment).apply(request)
      status(result) shouldBe 202
    }

    "return NOT_FOUND if nothing found for enrolmentKey" in new TestScope {
      expectFindKnownFactByEnrolmentKey(None)

      val json: JsValue = Json.parse(s"""|{
        |     "key": "NINO",
        |     "value": "AB087054B"
        |} """.stripMargin)
      val request: FakeRequest[JsValue] = jsRequest("PUT", s"/known-facts/$enrolmentKeyStr", json)

      val result: Future[Result] = controller.upsertKnownFactVerifier(enrolment).apply(request)
      status(result) shouldBe 404
    }
  }

  "DELETE /known-facts/:enrolmentKey" should {
    "remove known fact if exists and return 204" in new TestScope {
      //given
      expectFindKnownFactByEnrolmentKey(Some(matchingKnownFacts))
      expectDeleteKnownFacts

      val request = requestWithAuthSession("DELETE", s"/known-facts/$enrolmentKeyStr")
      //when
      val result = controller.deleteKnownFacts(enrolment).apply(request)
      //then
      status(result) shouldBe 204
    }

    "return NOT_FOUND if known facts do not exist" in new TestScope {
      //given
      expectFindKnownFactByEnrolmentKey(None)

      val request = requestWithAuthSession("DELETE", s"/known-facts/$enrolmentKeyStr")
      //when
      val result = controller.deleteKnownFacts(enrolment).apply(request)
      //then
      status(result) shouldBe 404
    }
  }

  "POST /known-facts/regime/PAYE/:agentId " should {
    "create PAYE known facts and return 204" in new TestScope {
      expectUpsertKnownFacts

      val agentId = "ABC123"
      val request = jsRequest(
        "POST",
        s"/known-facts/regime/PAYE/$agentId",
        Json.parse("""{ "postCode": "AA1 1AA" }""")
      )

      val result = controller.createPAYEKnownFacts(agentId).apply(request)

      status(result) shouldBe 204

      val expectedPlanetId = request.headers.get(AuthenticatedSession.TAG_PLANET_ID).getOrElse("")
      val captor = ArgumentCaptor.forClass(classOf[KnownFacts])
      verify(mockKnownFactsRepo).upsert(captor.capture(), eqTo(expectedPlanetId))
      val saved = captor.getValue

      saved.enrolmentKey shouldBe EnrolmentKey.from("IR-PAYE-AGENT", "IRAgentReference" -> agentId)
      saved.identifiers shouldBe saved.enrolmentKey.identifiers
      saved.verifiers shouldBe Seq(KnownFact("IRAgentPostcode", "AA1 1AA"))
      saved.planetId shouldBe None
    }
  }

}
