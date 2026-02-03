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

import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, EnrolmentKey}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.support._

import scala.concurrent.duration._

class RoboticsControllerISpec extends ServerBaseISpec with TestRequests with Eventually {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 200.millis)
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val knownFactsRepository: KnownFactsRepository = app.injector.instanceOf[KnownFactsRepository]

  "RoboticsController POST /RTServer/rest/nice/rti/ra/invocation" should {

    "return 200 and create known facts for SA (CESA)" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val requestId = "REQ-2025-001234"

      val payload: JsObject = Json
        .parse(
          s"""
            |{
            |  "requestData": {
            |    "workflowData": {
            |      "arguments": {
            |        "type": "string",
            |        "value": {
            |          "requestId": "$requestId",
            |          "targetSystem": "CESA",
            |          "agentDetails": {
            |            "address": {
            |              "postcode": "AA1 1AA"
            |            }
            |          }
            |        }
            |      }
            |    }
            |  }
            |}
            |""".stripMargin
        )
        .as[JsObject]

      val expectedAgentId =
        uk.gov.hmrc.agentsexternalstubs.models.GroupGenerator.agentId(requestId)

      val enrolmentKey =
        EnrolmentKey.from("IR-SA-AGENT", "IRAgentReference" -> expectedAgentId)

      await(
        knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)
      ).size shouldBe 0

      val response = Robotics.invokeRobotics(payload)

      response should haveStatus(200)
      println(response.json)
      (response.json \ "request_Id").as[String] shouldBe requestId

      eventually {
        val knownFacts =
          await(
            knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)
          )

        knownFacts shouldBe defined
        val kf = knownFacts.get

        kf.verifiers.size should be > 0
        val postcode = kf.verifiers
          .find(_.key.equalsIgnoreCase("IRAGENTPOSTCODE"))
          .map(_.value)

        postcode shouldBe Some("AA1 1AA")
      }
    }

    "return 200 and create known facts for CT (COTAX)" in {
      implicit val session: AuthenticatedSession =
        SignIn.signInAndGetSession()

      val requestId = "REQ-CT-0001"

      val payload = Json.parse(
        s"""
          |{
          |  "requestData": {
          |    "workflowData": {
          |      "arguments": {
          |        "type": "string",
          |        "value": {
          |          "requestId": "$requestId",
          |          "targetSystem": "COTAX",
          |          "agentDetails": {
          |            "address": {
          |              "postcode": "BB2 2BB"
          |            }
          |          }
          |        }
          |      }
          |    }
          |  }
          |}
          |""".stripMargin
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

      eventually {
        val knownFacts =
          await(
            knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)
          )

        knownFacts shouldBe defined
        val kf = knownFacts.get

        kf.verifiers.size should be > 0
        val postcode = kf.verifiers
          .find(_.key.equalsIgnoreCase("POSTCODE"))
          .map(_.value)

        postcode shouldBe Some("BB2 2BB")
      }
    }
  }
}
