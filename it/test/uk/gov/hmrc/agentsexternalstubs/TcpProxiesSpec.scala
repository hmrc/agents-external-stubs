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

package uk.gov.hmrc.agentsexternalstubs

import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.test.WsTestClient
import uk.gov.hmrc.agentsexternalstubs.support.ServerBaseISpec
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig

class TcpProxiesSpec extends ServerBaseISpec with WsTestClient with TableDrivenPropertyChecks:

  private val services = Table(("service", "port"), app.injector.instanceOf[TcpProxies].proxiedServices.toSeq*)

  "TcpProxies" when:
    "launched" should:
      "forward to correct server port" in:
        port shouldBe app.injector.instanceOf[AppConfig].httpPort

    forAll(services):
      case (service, given Port) =>
        s"opening a connection to $service" should:
          "forward packets to the agent-external-stubs instead" in:
            val response = wsUrl("/ping/ping").execute("GET").futureValue
            response.status shouldBe 200
