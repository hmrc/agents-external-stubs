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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.agentsexternalstubs.controllers.RoboticsControllerValidateRequestSpec.makeRequest
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class RoboticsControllerValidateRequestSpec extends BaseUnitSpec {

  "validateRequest" should {

    "return Right for a fully valid request" in {
      val operationData = Json.stringify(
        Json.obj(
          "targetSystem"      -> "CESA",
          "postcode"          -> "AA1 1AA",
          "operationRequired" -> "CREATE",
          "requestId"         -> "REQ-123"
        )
      )

      val req = makeRequest(operationData)
      val result = RoboticsController.validateRequest(req)
      result.isRight shouldBe true

      val roboticsReq = result.toOption.get
      roboticsReq.targetSystem shouldBe "CESA"
      roboticsReq.postcode shouldBe "AA1 1AA"
      roboticsReq.operationRequired shouldBe "CREATE"
      roboticsReq.requestId shouldBe "REQ-123"
      roboticsReq.correlationId shouldBe "CID-123"
    }

    "return Left if requestData key is missing" in {
      val body = Json.obj("otherData" -> Json.arr())
      val req = FakeRequest().withBody(body).withHeaders("correlationId" -> "CID-123")

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if requestData array is empty" in {
      val body = Json.obj("requestData" -> Json.arr())
      val req = FakeRequest().withBody(body).withHeaders("correlationId" -> "CID-123")

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if workflowData.arguments array is missing" in {
      val operationData = Json.stringify(Json.obj("targetSystem" -> "CESA"))
      val body = Json.obj(
        "requestData" -> Json.arr(
          Json.obj("workflowData" -> Json.obj()) // no arguments
        )
      )
      val req = FakeRequest().withBody(body).withHeaders("correlationId" -> "CID-123")

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if workflowData.arguments array is empty" in {
      val body = Json.obj(
        "requestData" -> Json.arr(
          Json.obj(
            "workflowData" -> Json.obj(
              "arguments" -> Json.arr() // empty
            )
          )
        )
      )
      val req = FakeRequest().withBody(body).withHeaders("correlationId" -> "CID-123")

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if workflowData is invalid JSON" in {
      val body = Json.obj(
        "requestData" -> Json.arr(
          Json.obj(
            "workflowData" -> Json.obj(
              "arguments" -> Json.arr(
                Json.obj(
                  "type"  -> "string",
                  "value" -> "{invalid-json}"
                )
              )
            )
          )
        )
      )
      val req = FakeRequest().withBody(body).withHeaders("correlationId" -> "CID-123")

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if targetSystem is missing" in {
      val operationData = Json.stringify(Json.obj("postcode" -> "AA1 1AA", "operationRequired" -> "CREATE"))
      val req = makeRequest(operationData)

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if targetSystem is invalid" in {
      val operationData = Json.stringify(
        Json.obj("targetSystem" -> "INVALID", "postcode" -> "AA1 1AA", "operationRequired" -> "CREATE")
      )
      val req = makeRequest(operationData)

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if postcode is missing" in {
      val operationData = Json.stringify(Json.obj("targetSystem" -> "CESA", "operationRequired" -> "CREATE"))
      val req = makeRequest(operationData)

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if operationRequired is missing" in {
      val operationData = Json.stringify(Json.obj("targetSystem" -> "CESA", "postcode" -> "AA1 1AA"))
      val req = makeRequest(operationData)

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }

    "return Left if CorrelationId header is missing" in {
      val operationData = Json.stringify(
        Json.obj("targetSystem" -> "CESA", "postcode" -> "AA1 1AA", "operationRequired" -> "CREATE")
      )
      val req = makeRequest(operationData, correlationId = None)

      val result = RoboticsController.validateRequest(req)
      result.isLeft shouldBe true
      result.left.get.header.status shouldBe 400
    }
  }

}

object RoboticsControllerValidateRequestSpec {
  def requestData(operationData: String): JsValue = Json.obj(
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

  def makeRequest(operationData: String, correlationId: Option[String] = Some("CID-123")): Request[JsValue] = {
    val req = FakeRequest().withBody(requestData(operationData))
    correlationId match {
      case Some(cid) => req.withHeaders("correlationId" -> cid)
      case None      => req
    }
  }
}
