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

package uk.gov.hmrc.agentsexternalstubs.controllers.datagen

import play.api.Logging
import uk.gov.hmrc.agentmtdidentifiers.model.PlrIdType
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.Common
import uk.gov.hmrc.agentsexternalstubs.models.User.CR
import uk.gov.hmrc.agentsexternalstubs.models._

import scala.annotation.tailrec
import scala.util.Random

case class AgencyCreationPayload(
  planetId: String,
  agentCode: String,
  agentId: String,
  agentUser: User,
  clients: List[User],
  teamMembers: List[User],
  populateFriendlyNames: Boolean
)

class AgencyDataAssembler extends Logging {

  private type ClientType = String
  private type ServiceKey = String

  private val AGENT_NINO_OFFSET = 90000

  def build(
    indexAgency: Int,
    clientsPerAgent: Int,
    teamMembersPerAgent: Int,
    populateFriendlyNames: Boolean
  ): AgencyCreationPayload = {
    val planetId = f"p-$indexAgency%03d"

    logger.info(s"Assembling data for '$planetId'. Can take a while...")

    val agentUser = buildMainAgentUser(indexAgency)
    val clients = buildClientsForAgent(indexAgency, clientsPerAgent)
    val teamMembers = buildTeamMembersForAgent(indexAgency, teamMembersPerAgent, agentUser)
    val agentCode = UserGenerator.agentCode(indexAgency.toString)
    val agentId = UserGenerator.agentId(indexAgency.toString)

    AgencyCreationPayload(
      planetId,
      agentCode,
      agentId,
      agentUser.copy(assignedDelegatedEnrolments = clients.flatMap(_.assignedPrincipalEnrolments)),
      clients,
      teamMembers,
      populateFriendlyNames
    )

  }

  private def buildMainAgentUser(indexAgency: Int) = {
    val principalEnrolments = Seq(
      EnrolmentKey(
        "HMRC-AS-AGENT",
        identifiers = Seq(Identifier("AgentReferenceNumber", Generator.arn(indexAgency.toString).value))
      )
    )

    val userId = f"perf-test-agent-$indexAgency%03d"

    User(
      userId = userId,
      groupId = Some(UserGenerator.groupId(userId)),
      name = Option(f"Main Agent User $indexAgency%03d"),
      credentialRole = Some(CR.Admin),
      assignedPrincipalEnrolments = principalEnrolments
    )
  }

  def buildClientsForAgent(indexAgency: Int, numClients: Int): List[User] = {

    def generateClient(clientType: ClientType, serviceKey: ServiceKey, index: Int): User = {
      val seed = System.nanoTime().toString
      val identifiers: Seq[Identifier] = serviceKey match {
        case "HMRC-MTD-IT"  => Seq(Identifier("MTDITID", Generator.mtdbsa(seed).value))
        case "HMRC-MTD-VAT" => Seq(Identifier("VRN", Generator.vrn(seed).value))
        case "HMRC-CBC-ORG" =>
          Seq(Identifier("cbcId", Generator.cbcId(seed).value), Identifier("UTR", Generator.utr(seed)))
        case "HMRC-CBC-NONUK-ORG" => Seq(Identifier("cbcId", Generator.cbcId(seed).value))
        case "HMRC-CGT-PD"        => Seq(Identifier("CGTPDRef", Generator.cgtPdRef(seed)))
        case "HMRC-PPT-ORG" =>
          Seq(Identifier("EtmpRegistrationNumber", Generator.regex(Common.pptReferencePattern).sample.get))
        case "HMRC-TERS-ORG"    => Seq(Identifier("SAUTR", Generator.utr(seed)))
        case "HMRC-TERSNT-ORG"  => Seq(Identifier("URN", Generator.urn(seed).value))
        case "HMRC-PILLAR2-ORG" => Seq(Identifier(PlrIdType.enrolmentId, Generator.plrId(seed).value))
      }

      val userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d"

      clientType match {
        case AG.Individual =>
          UserGenerator
            .individual(
              userId = userId,
              groupId = UserGenerator.groupId(userId),
              confidenceLevel = 250,
              credentialRole = User.CR.Admin,
              nino = f"AB${index + 1}%06dC"
            )
            .withAssignedPrincipalEnrolment(service = serviceKey, identifiers)
        case AG.Organisation =>
          UserGenerator
            .organisation(userId = userId, groupId = UserGenerator.groupId(userId))
            .withAssignedPrincipalEnrolment(service = serviceKey, identifiers)
      }
    }

    clientTypesAndServiceKeys(numClients).zipWithIndex.toList.foldLeft(List.empty[User]) {
      case (generatedClientsOfAgent, ((clientType, serviceKey), index: Int)) =>
        @tailrec
        def generateUniqueClientForAgent: User = {

          val countGeneratedClients = generatedClientsOfAgent.size
          if (countGeneratedClients % 5000 == 0 && countGeneratedClients > 0) {
            logger.info(s"Assembled clients: $countGeneratedClients ...")
          }

          val generatedClient = generateClient(clientType, serviceKey, index)

          generatedClient.assignedPrincipalEnrolments.headOption match {
            case None =>
              generateUniqueClientForAgent
            case Some(enrolment) =>
              if (
                generatedClientsOfAgent
                  .flatMap(_.assignedPrincipalEnrolments)
                  .exists(_.tag == enrolment.tag)
              ) {
                generateUniqueClientForAgent
              } else {
                generatedClient
              }
          }
        }

        generatedClientsOfAgent :+ generateUniqueClientForAgent
    }
  }

  private def buildTeamMembersForAgent(indexAgency: Int, teamMembersPerAgent: Int, agentUser: User) =
    (1 to teamMembersPerAgent).toList.map { index =>
      UserGenerator
        .agent(
          userId = f"perf-test-$indexAgency%04d-A$index%05d",
          groupId = agentUser.groupId.orNull,
          credentialRole = "User",
          nino = f"AB${index + AGENT_NINO_OFFSET + 1}%06dC"
        )
    }

  private def clientTypesAndServiceKeys(numClients: Int): Seq[(ClientType, ServiceKey)] = {
    val genMethod = GranPermsGenRequest.GenMethodProportional
    val clientTypes: Seq[ClientType] = pickFromDistribution(
      genMethod,
      GranPermsGenRequest.defaultClientTypeDistribution,
      numClients
    )
    val individualEnrolments: Seq[ServiceKey] = pickFromDistribution(
      genMethod,
      GranPermsGenRequest.defaultIndividualServiceDistribution,
      clientTypes.count(_ == AG.Individual)
    )
    val organisationEnrolments: Seq[ServiceKey] = pickFromDistribution(
      genMethod,
      GranPermsGenRequest.defaultOrganisationServiceDistribution,
      clientTypes.count(_ == AG.Organisation)
    )
    individualEnrolments.map((AG.Individual, _)) ++ organisationEnrolments.map((AG.Organisation, _))
  }

  private def pickFromDistribution[A](method: String, distribution: Map[A, Double], n: Int): Seq[A] = method match {
    case GranPermsGenRequest.GenMethodRandom =>
      Seq.fill(n)(pickFromDistributionRandomly(distribution))
    case GranPermsGenRequest.GenMethodProportional =>
      pickFromDistributionProportionally[A](distribution, n)
  }

  private def pickFromDistributionRandomly[A](distribution: Map[A, Double]): A = {
    require(distribution.nonEmpty)
    val normalisedDistribution = distribution.mapValues(_ / distribution.values.sum).toSeq
    val randomNumber = Random.nextDouble()
    val chosenIndex = normalisedDistribution.map(_._2).scan(0.0)(_ + _).tail.indexWhere(randomNumber < _)
    if (chosenIndex < 0) // not found - should only ever happen (rarely) due to rounding errors
      normalisedDistribution.last._1
    else
      normalisedDistribution(chosenIndex)._1
  }

  private def pickFromDistributionProportionally[A](distribution: Map[A, Double], n: Int): Seq[A] = {
    require(distribution.nonEmpty)
    val normalisedDistribution = distribution.mapValues(_ / distribution.values.sum).toSeq
    val intervalPartition = normalisedDistribution.map(_._2).scan(0.0)(_ + _).tail
    (0 until n).map { i =>
      val fractionalIndex = (i.toDouble + 0.5) / n.toDouble
      val chosenIndex = intervalPartition.indexWhere(fractionalIndex < _)
      if (chosenIndex < 0) // not found - should only ever happen (rarely) due to rounding errors
        normalisedDistribution.last._1
      else
        normalisedDistribution(chosenIndex)._1
    }
  }

}
