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
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, EnrolmentKey, Generator, Services}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.support._

class RoboticsControllerISpec extends ServerBaseISpec with TestRequests with Eventually {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val knownFactsRepository: KnownFactsRepository = app.injector.instanceOf[KnownFactsRepository]

  "RoboticsController POST /RTServer/rest/nice/rti/ra/invocation" should {

    val scenarios = Seq(
      ("CESA", Some("AA1 1AA"), "CREATE", "IR-SA-AGENT", "IRAGENTPOSTCODE", "Agent Created Successfully"),
      ("CESA", None, "CREATE", "IR-SA-AGENT", "IRAGENTPOSTCODE", "Agent Created Successfully"),
      (
        "CESA",
        Some("AA1 1AA"),
        "UPDATE",
        "IR-SA-AGENT",
        "IRAGENTPOSTCODE",
        "Agent Updated their {name}/{address}/{contact} Successfully"
      ),
      ("COTAX", Some("BB2 2BB"), "CREATE", "IR-CT-AGENT", "POSTCODE", "Agent Created Successfully"),
      ("COTAX", None, "CREATE", "IR-CT-AGENT", "POSTCODE", "Agent Created Successfully"),
      (
        "COTAX",
        Some("BB2 2BB"),
        "UPDATE",
        "IR-CT-AGENT",
        "POSTCODE",
        "Agent Updated their {name}/{address}/{contact} Successfully"
      )
    )

    scenarios.foreach { case (targetSystem, postcode, operationRequired, _, postCodeKey, _) =>
      val postcodeDescription = postcode.fold("without postcode")(pc => s"with postcode $pc")

      s"return 200 and handle $operationRequired for $targetSystem $postcodeDescription" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val requestId = s"REQ-${targetSystem.take(2)}-${System.currentTimeMillis().toString.takeRight(6)}"

        val operationData = Json.stringify(
          Json.obj(
            "requestId"         -> requestId,
            "targetSystem"      -> targetSystem,
            "operationRequired" -> operationRequired
          ) ++ postcode.fold(Json.obj())(pc => Json.obj("postcode" -> pc))
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

        val serviceName =
          if (targetSystem == "CESA") "IR-SA-AGENT" else "IR-CT-AGENT"

        val service =
          Services(serviceName).getOrElse(
            fail(s"Service $serviceName not found")
          )

        val identifier =
          service.identifiers.headOption.getOrElse(
            fail(s"No identifiers for service $serviceName")
          )

        val generatedValue =
          Generator
            .get(identifier.valueGenerator)(session.userId)
            .getOrElse(fail("Generator failed"))

        val enrolmentKey = EnrolmentKey.from(service.name, identifier.name -> generatedValue)

        // Ensure no known facts exist yet
        await(knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)).size shouldBe 0

        val response = Robotics.invokeRobotics(payload)
        response should haveStatus(200)
        (response.json \ "requestId").as[String] shouldBe requestId

        // Verify known facts creation
        eventually {
          verifyKnownFacts(enrolmentKey, postCodeKey, postcode, session.planetId)
        }
      }
    }
  }

  private def verifyKnownFacts(
    enrolmentKey: EnrolmentKey,
    postCodeKey: String,
    expectedPostcode: Option[String],
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

    expectedPostcode match {
      case Some(value) => postcode shouldBe Some(value)
      case None        => postcode should not be empty
    }
  }
}
