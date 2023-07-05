/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.agentsexternalstubs.controllers.datagen.AgencyDataAssembler
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

class AgencyDataAssemblerSpec extends UnitSpec {

  "build AgencyCreationPayload with correct data" in {
    val assembler = new AgencyDataAssembler

    val indexAgency = 1
    val clientsPerAgent = 2
    val teamMembersPerAgent = 3
    val populateFriendlyNames = true

    val payload = assembler.build(indexAgency, clientsPerAgent, teamMembersPerAgent, populateFriendlyNames)

    // Assertions for AgencyCreationPayload
    payload.planetId shouldEqual "p-001"
    payload.clients should have size clientsPerAgent
    payload.teamMembers should have size teamMembersPerAgent
    payload.populateFriendlyNames shouldEqual true

    // Assertions for buildMainAgentUser indirectly
    payload.agentUser.userId shouldEqual "perf-test-agent-001"
    payload.agentUser.groupId shouldBe defined
    payload.agentUser.name shouldEqual Some("Main Agent User 001")
    payload.agentUser.credentialRole shouldEqual Some(User.CR.Admin)
    payload.agentUser.assignedPrincipalEnrolments should have size 1
  }
  "buildClientsForAgent" should {
    "generate the correct number of clients" in {
      val assembler = new AgencyDataAssembler
      val indexAgency = 1
      val numClients = 5
      val clients = assembler.buildClientsForAgent(indexAgency, numClients)
      clients should have size numClients
    }

    "generate clients with unique assignedPrincipalEnrolments" in {
      val assembler = new AgencyDataAssembler
      val indexAgency = 1
      val numClients = 5
      val clients = assembler.buildClientsForAgent(indexAgency, numClients)
      val enrolmentTags = clients.flatMap(_.assignedPrincipalEnrolments.map(_.tag))
      enrolmentTags.toSet should have size enrolmentTags.size
    }
  }

}
