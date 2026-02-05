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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, EnrolmentKey}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class RoboticsControllerISpec
    extends ServerBaseISpec with TestRequests with TestStubs with WireMockSupport with Eventually {

  final override val playServer: TestPlayServer = new TestPlayServer {
    override def configuration: Seq[(String, Any)] =
      super.configuration ++ Seq(
        "microservice.services.agent-services-account.host"     -> "localhost",
        "microservice.services.agent-services-account.port"     -> wireMockPort,
        "microservice.services.agent-services-account.protocol" -> "http",
        "robotics.callback.delay"                               -> 0,
        "robotics.known-facts.delay"                            -> 0
      )
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val knownFactsRepository: KnownFactsRepository = app.injector.instanceOf[KnownFactsRepository]

  "RoboticsController POST /RTServer/rest/nice/rti/ra/invocation" should {

    "return 200 and create known facts for SA (CESA)" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      stubFor(
        WireMock
          .post(urlEqualTo("/robotics/callback"))
          .willReturn(aResponse().withStatus(204))
      )

      val requestId = "REQ-2025-001234"
      val operationData =
        Json.stringify(
          Json.obj(
            "requestId"    -> requestId,
            "targetSystem" -> "CESA",
            "agentDetails" -> Json.obj(
              "address" -> Json.obj(
                "postcode" -> "AA1 1AA"
              )
            )
          )
        )
      val payload: JsObject = Json.obj(
        "requestData" -> Json.arr(
          Json.obj(
            "workflowData" -> Json.obj(
              "arguments" -> Json.arr(
                Json.obj(
                  "type"  -> "string",
                  "value" -> operationData
                )
              )
            )
          )
        )
      )

      val expectedAgentId =
        uk.gov.hmrc.agentsexternalstubs.models.GroupGenerator.agentId(requestId)

      val enrolmentKey =
        EnrolmentKey.from("IR-SA-AGENT", "IRAgentReference" -> expectedAgentId)

      await(
        knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)
      ).size shouldBe 0

      val response = Robotics.invokeRobotics(payload)

      response should haveStatus(200)
      (response.json \ "request_Id").as[String] shouldBe requestId

      val requestReference = requestId.take(12).toUpperCase

      eventually {
        verifyCallbackRequest(requestReference, "CESA")

        verifyKnownFacts(enrolmentKey, "IRAGENTPOSTCODE", "AA1 1AA", session.planetId)
      }
    }

    "return 200 and create known facts for CT (COTAX)" in {
      implicit val session: AuthenticatedSession =
        SignIn.signInAndGetSession()

      stubFor(
        WireMock
          .post(urlEqualTo("/robotics/callback"))
          .willReturn(aResponse().withStatus(204))
      )

      val requestId = "REQ-CT-0001"
      val operationData =
        Json.stringify(
          Json.obj(
            "requestId"    -> requestId,
            "targetSystem" -> "COTAX",
            "agentDetails" -> Json.obj(
              "address" -> Json.obj(
                "postcode" -> "BB2 2BB"
              )
            )
          )
        )
      val payload: JsObject = Json.obj(
        "requestData" -> Json.arr(
          Json.obj(
            "workflowData" -> Json.obj(
              "arguments" -> Json.arr(
                Json.obj(
                  "type"  -> "string",
                  "value" -> operationData
                )
              )
            )
          )
        )
      )

      val agentId =
        uk.gov.hmrc.agentsexternalstubs.models.GroupGenerator.agentId(requestId)

      val enrolmentKey =
        EnrolmentKey.from("IR-CT-AGENT", "IRAgentReference" -> agentId)

      await(
        knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)
      ).size shouldBe 0

      val response = Robotics.invokeRobotics(payload)

      response should haveStatus(200)

      val requestReference = requestId.take(12).toUpperCase

      eventually {
        verifyCallbackRequest(requestReference, "COTAX")

        verifyKnownFacts(enrolmentKey, "POSTCODE", "BB2 2BB", session.planetId)
      }
    }
  }

  private def verifyCallbackRequest(requestReference: String, regime: String): Unit =
    WireMock.verify(
      1,
      postRequestedFor(urlPathEqualTo("/robotics/callback"))
        .withRequestBody(
          matchingJsonPath("$.requestReference", equalTo(requestReference))
        )
        .withRequestBody(
          matchingJsonPath("$.status", equalTo("COMPLETED"))
        )
        .withRequestBody(
          matchingJsonPath("$.regime", equalTo(regime))
        )
    )

  private def verifyKnownFacts(
    enrolmentKey: EnrolmentKey,
    postCodeKey: String,
    expectedPostcode: String,
    planetId: String
  ) = {
    val knownFacts =
      await(
        knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId)
      )

    knownFacts shouldBe defined
    val kf = knownFacts.get

    kf.verifiers.size should be > 0
    val postcode = kf.verifiers
      .find(_.key.equalsIgnoreCase(postCodeKey))
      .map(_.value)

    postcode shouldBe Some(expectedPostcode)
  }
}
