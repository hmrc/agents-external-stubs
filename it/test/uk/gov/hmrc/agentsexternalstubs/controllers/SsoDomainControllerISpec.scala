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
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class SsoDomainControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "SsoValidateDomainController" when {

    "GET /sso/validate/domain/localhost" should {
      "return NoContent" in {
        val response = SsoValidateDomain.validate("localhost")
        response.status shouldBe 204

      }
    }

    "GET /sso/domains" should {
      "returns allowlisted domains" in {
        val response = SsoGetDomains.getDomains
        response.status shouldBe 200
        Json
          .parse(response.body)
          .toString shouldBe """{"internalDomains":["localhost"],"externalDomains":["127.0.0.1","online-qa.ibt.hmrc.gov.uk","ibt.hmrc.gov.uk"]}"""

      }
    }

  }

}
