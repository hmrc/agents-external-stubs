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

package uk.gov.hmrc.agentsexternalstubs.connectors

import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class CitizenDetailsConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[CitizenDetailsConnector]

  "CitizenDetailsConnector" when {

    "getCitizenDateOfBirth" should {
      "return dateOfBirth" in {
        userService
          .createUser(
            UserGenerator
              .individual(
                userId = "foo",
                nino = "HW 82 78 56 C",
                name = "Alan Brian Foo-Foe",
                dateOfBirth = "1975-12-18"
              ),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = await(connector.getCitizenDateOfBirth(Nino("HW827856C")))
        result.flatMap(_.dateOfBirth) shouldBe Some(LocalDate.parse("1975-12-18"))
      }
    }
  }
}
