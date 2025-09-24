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

import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support._

class DataStreamStubControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient = app.injector.instanceOf[WSClient]

  "DataStreamStubController" when {

    "POST /write/audit" should {
      "return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        DataStreamStubs.writeAudit("{}") should haveStatus(204)
      }
    }

    "POST /write/audit/merged" should {
      "return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        DataStreamStubs.writeAuditMerged("{}") should haveStatus(204)
      }
    }
  }
}
