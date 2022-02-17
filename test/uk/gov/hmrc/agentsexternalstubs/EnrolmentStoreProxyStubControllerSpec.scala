/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, Request, Result}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, EnrolmentKey}
import uk.gov.hmrc.agentsexternalstubs.repository.{DuplicateUserException, KnownFactsRepository}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyStubControllerSpec extends UnitSpec {

  trait Setup {

    implicit val ex: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val mockAuthenticationService: AuthenticationService = mock[AuthenticationService]
    val mockKnownFactsRepository: KnownFactsRepository = mock[KnownFactsRepository]
    val mockUsersService: UsersService = mock[UsersService]
    val cc: ControllerComponents = Helpers.stubControllerComponents()

    val controller: EnrolmentStoreProxyStubController = new EnrolmentStoreProxyStubController(
      mockAuthenticationService,
      mockKnownFactsRepository,
      cc
    )(mockUsersService, ex) {
      override def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
        ifSessionNotFound: => Future[Result]
      )(implicit request: Request[T], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
        body(
          AuthenticatedSession("sessionId", "foo1", "authToken", "principal", "planetId")
        )
    }
    val validRequestBody: JsValue = Json.parse("""{"userId": "foo1","type": "principal"}""".stripMargin)
  }

  "allocateGroupEnrolment" should {
    "return 409 if mongodb returns DuplicateUserException" in new Setup {
      when(
        mockUsersService.allocateEnrolmentToGroup(
          anyString(),
          anyString(),
          any[EnrolmentKey](),
          anyString(),
          any[Option[String]](),
          anyString()
        )(any[ExecutionContext]())
      )
        .thenReturn(Future.failed(DuplicateUserException("")))
      val result: Future[Result] =
        controller.allocateGroupEnrolment("group1", EnrolmentKey("IR-SA~UTR~12345678"), None)(
          FakeRequest().withBody(validRequestBody)
        )
      status(result) shouldBe Status.CONFLICT
    }
  }

}
