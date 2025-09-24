/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class ConfigControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient = app.injector.instanceOf[WSClient]

  "ConfigController" when {

    "GET /agents-external-stubs/config/services" should {
      "return 200 with services json body" in {
        implicit val authSession: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "A")
        val result = Config.getServices()
        result should (haveStatus(200) and haveValidJsonBody(
          haveProperty[Seq[JsObject]]("services")
        ))
      }
    }
  }
}
