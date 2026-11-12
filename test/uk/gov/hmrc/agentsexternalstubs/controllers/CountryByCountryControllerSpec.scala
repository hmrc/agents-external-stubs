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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentsexternalstubs.models.*
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, ExternalUserService, RecordsService}
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

import scala.concurrent.{ExecutionContext, Future}

class CountryByCountryControllerSpec extends BaseUnitSpec:

  private val cbcId = "XACBC0123456789"

  private val requestBody = Json.obj(
    "displaySubscriptionForCBCRequest" -> Json.obj(
      "requestCommon" -> Json.obj(
        "regime" -> "CBC",
        "receiptDate" -> "2026-11-12T15:23:14Z",
        "acknowledgementReference" -> "0123456789abcdef0123456789abcdef",
        "originatingSystem" -> "MDTP",
        "conversationID" -> "contract-test"
      ),
      "requestDetail" -> Json.obj(
        "IDType" -> "CBC",
        "IDNumber" -> cbcId
      )
    )
  )

  "displaySubscriptionForCbC" should:
    "find the CBC record globally when the backend request has no resolvable session" in:
      given ExecutionContext = ExecutionContext.global
      val recordsRepository = mock[RecordsRepository]
      val recordsService = new RecordsService(recordsRepository, mock[ExternalUserService])
      val record = CbcSubscriptionRecord.generateWith(cbcId, isUK = true)
      val authenticationService = mock[AuthenticationService]

      when(
        authenticationService.findByAuthTokenOrLookupExternal(eqTo("if-token"))(using
          any[ExecutionContext](),
          any()
        )
      ).thenReturn(Future.successful(None))
      when(authenticationService.findBySessionId(eqTo("ignored"))(using any[ExecutionContext]()))
        .thenReturn(Future.successful(None))

      when(
        recordsRepository.findByKeysAnyPlanet[CbcSubscriptionRecord](
          any[Seq[String]](),
          eqTo(2)
        )(using
          any[RecordMetaData[CbcSubscriptionRecord]](),
          any[Reads[CbcSubscriptionRecord]]()
        )
      ).thenReturn(Future.successful(Seq(Planet.DEFAULT -> record)))

      val controller = new CountryByCountryController(
        authenticationService,
        recordsService,
        stubControllerComponents()
      )

      val result = controller
        .displaySubscriptionForCbC(
          FakeRequest("POST", "/dac6/dct50d/v1")
            .withHeaders("Authorization" -> "Bearer if-token", "X-Session-ID" -> "ignored")
            .withBody[JsValue](requestBody)
        )

      status(result) shouldBe OK
      (contentAsJson(result) \ "displaySubscriptionForCBCResponse" \ "responseDetail" \ "subscriptionID")
        .as[String] shouldBe cbcId
