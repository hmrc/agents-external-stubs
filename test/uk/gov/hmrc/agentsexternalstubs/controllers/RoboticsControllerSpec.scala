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

import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class RoboticsControllerSpec extends BaseUnitSpec {

  "validateTargetSystem" should {
    "return Right for CESA" in {
      val operationData =
        Json.stringify(
          Json.obj("targetSystem" -> "CESA")
        )
      val json = requestData(operationData)

      RoboticsController.validateTargetSystem(json) shouldBe Right("CESA")
    }

    "return Right for COTAX" in {
      val operationData =
        Json.stringify(
          Json.obj("targetSystem" -> "COTAX")
        )

      val json = requestData(operationData)

      RoboticsController.validateTargetSystem(json) shouldBe Right("COTAX")
    }

    "return Left with missing targetSystem" in {
      val operationData =
        Json.stringify(
          Json.obj("targetSystem" -> "")
        )

      val json = requestData(operationData)

      val result = RoboticsController.validateTargetSystem(json)
      result.swap.getOrElse(fail("Expected Left")).header.status shouldBe 400
    }

    "return Left with invalid targetSystem" in {
      val operationData =
        Json.stringify(
          Json.obj("targetSystem" -> "INVALID")
        )

      val json = requestData(operationData)

      val result = RoboticsController.validateTargetSystem(json)
      result.swap.getOrElse(fail("Expected Left")).header.status shouldBe 400
    }
  }

  private def requestData(operationData: String) = Json.obj(
    "requestData" -> Json.arr(
      Json.obj(
        "workflowData" -> Json.obj(
          "arguments" -> Json.arr(
            Json.obj(
              "type" -> "string",
              "value" -> operationData
            )
          )
        )
      )
    )
  )
}
