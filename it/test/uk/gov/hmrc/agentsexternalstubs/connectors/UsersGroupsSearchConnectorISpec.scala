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
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}

class UsersGroupsSearchConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[UsersGroupsSearchConnector]

  "UsersGroupsSearchConnector" when {

    "getGroupInfo" should {
      "return group information" in {
        val userFoo = UserGenerator.agent(userId = "foo", groupId = "foo-group-1")
        userService.createUser(userFoo, planetId = "testPlanetId", affinityGroup = Some(AG.Agent)).futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanetId")

        val currentUser = Users.get(session.userId).json.as[User]
        val maybeAgentCode = Groups.get(currentUser.groupId.get).json.as[Group].agentCode

        val groupInfo = await(connector.getGroupInfo("foo-group-1"))
        groupInfo.groupId shouldBe "foo-group-1"
        groupInfo.affinityGroup shouldBe Some("Agent")
        groupInfo.agentCode.map(_.value) shouldBe maybeAgentCode
      }
    }
  }
}
