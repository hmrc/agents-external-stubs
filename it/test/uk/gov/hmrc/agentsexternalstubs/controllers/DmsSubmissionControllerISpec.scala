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

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.support.ServerBaseISpec

class DmsSubmissionControllerISpec extends ServerBaseISpec {

  "POST /dms-submission/submit" should {
    "return 202" in {

      val ws: WSClient = app.injector.instanceOf[WSClient]

      val result = ws.url(s"$url/dms-submission/submit").post(Json.obj())

      await(result).status shouldBe ACCEPTED

    }
  }
}
