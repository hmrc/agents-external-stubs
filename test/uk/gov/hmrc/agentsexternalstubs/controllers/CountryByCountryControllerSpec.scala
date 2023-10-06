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

import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentsexternalstubs.models.Generator.cbcId
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, CbCSubscriptionRecordsService}
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, SessionKeys}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CountryByCountryControllerSpec extends BaseUnitSpec with MockFactory {

  trait TestScope {
    val mockAuthService: AuthenticationService = mock[AuthenticationService]
    val mockCbcService: CbCSubscriptionRecordsService = mock[CbCSubscriptionRecordsService]
    val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
    implicit val eC: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    def jsRequest(method: String, uri: String, jsonBody: JsValue): FakeRequest[JsValue] =
      FakeRequest[JsValue](method, uri, authSessionHeaders, jsonBody).withSession(SessionKeys.authToken -> "Bearer XYZ")

    def requestWithAuthSession(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(method, uri)
        .withHeaders(authSessionHeaders)
        .withSession(SessionKeys.authToken -> "Bearer XYZ")

    val controller = new CountryByCountryController(
      mockAuthService,
      mockCbcService,
      controllerComponents
    )

    // cbc service mock
    def expectGetCbcSubscriptionRecord(
      maybeRecord: Option[CbcSubscriptionRecord]
    ): CallHandler3[CbcId, String, ExecutionContext, Future[Option[CbcSubscriptionRecord]]] =
      (mockCbcService
        .getCbcSubscriptionRecord(_: CbcId, _: String)(_: ExecutionContext))
        .expects(*, *, *)
        .returns(Future.successful(maybeRecord))

    val validCbcStr: String = cbcId("fort").value
    val email = "test@foo.uk"
    val acknowledgementReference: String = UUID.randomUUID().toString.replace("-", "")

    val validPayload: JsValue =
      Json.toJson(
        DisplaySubscriptionForCbCRequestPayload(
          DisplaySubscriptionForCBCRequest(
            CbCRequestCommon("CBC", None, LocalDateTime.now(), acknowledgementReference),
            CbCRequestDetail(IDNumber = validCbcStr)
          )
        )
      )

  }

  "POST /dac6/dct50d/v1 (display subscription for cbc)" should {
    "return 200 with country by country subscription details" in new TestScope {
      //given
      val expectedRecord: CbcSubscriptionRecord =
        CbcSubscriptionRecord
          .seed("foo")
          .withCbcId(validCbcStr)
          .withEmail(email)

      expectGetCbcSubscriptionRecord(Some(expectedRecord))

      //when
      val result: Future[Result] =
        controller.displaySubscriptionForCbC.apply(
          jsRequest("POST", "/dac6/dct50d/v1", validPayload)
        )
      //then
      status(result) shouldBe 200
    }

    "return NOT_FOUND with error response if no record" in new TestScope {
      //given
      expectGetCbcSubscriptionRecord(None)

      //when
      val result: Future[Result] =
        controller.displaySubscriptionForCbC.apply(
          jsRequest("POST", "/dac6/dct50d/v1", validPayload)
        )
      //then
      status(result) shouldBe 404
    }

    "throw BadRequestException if bad payload" in new TestScope {
      val result: Future[Result] =
        controller.displaySubscriptionForCbC.apply(
          jsRequest(
            "POST",
            "/dac6/dct50d/v1",
            Json.parse("""{"displaySubscriptionForCBCRequest": {} }""".stripMargin)
          )
        )

      //then
      a[BadRequestException] shouldBe thrownBy(
        status(result) shouldBe 400
      )
    }

    "return unauthorised without session" in new TestScope {
      // given no auth session headers, when request
      val result: Future[Result] =
        controller.displaySubscriptionForCbC.apply(
          FakeRequest("POST", "/dac6/dct50d/v1", FakeHeaders(), Json.parse("""{"no": "doesn't matter"}"""))
        )

      status(result) shouldBe 401
    }
  }

}
