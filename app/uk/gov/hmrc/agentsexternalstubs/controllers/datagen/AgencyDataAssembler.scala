/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.Common
import uk.gov.hmrc.agentsexternalstubs.models.User.{AG, CR, Enrolments}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, Generator, GranPermsGenRequest, Identifier, User, UserGenerator}

import scala.annotation.tailrec
import scala.util.Random

case class AgencyCreationPayload(planetId: String, agentUser: User, clients: List[User], teamMembers: List[User])

class AgencyDataAssembler {

  private type ClientType = String
  private type ServiceKey = String

  private val AGENT_NINO_OFFSET = 90000

  def build(
    indexAgency: Int,
    clientsPerAgent: Int,
    teamMembersPerAgent: Int
  ): AgencyCreationPayload = {
    val planetId = f"perf-test-planet-$indexAgency%03d"
    val agentUser = buildMainAgentUser(indexAgency)
    val clients = buildClientsForAgent(indexAgency, clientsPerAgent)
    val teamMembers = buildTeamMembersForAgent(indexAgency, teamMembersPerAgent, agentUser)

    AgencyCreationPayload(
      planetId,
      agentUser.copy(enrolments = agentUser.enrolments.copy(delegated = clients.flatMap(_.enrolments.principal))),
      clients,
      teamMembers
    )

  }

  private def buildMainAgentUser(indexAgency: Int) = {
    val principalEnrolments = Seq(
      Enrolment(
        "HMRC-AS-AGENT",
        identifiers = Some(Seq(Identifier("AgentReferenceNumber", Generator.arn(indexAgency.toString).value)))
      )
    )

    val userId = f"perf-test-agent-$indexAgency%03d"

    User(
      userId = userId,
      groupId = Some(UserGenerator.groupId(userId)),
      affinityGroup = Some(AG.Agent),
      credentialRole = Some(CR.Admin),
      enrolments = Enrolments(
        principalEnrolments,
        Seq.empty,
        Seq.empty
      ),
      agentCode = Some(UserGenerator.agentCode(userId))
    )
  }

  private def buildClientsForAgent(indexAgency: Int, numClients: Int): List[User] = {

    def generateClient(clientType: ClientType, serviceKey: ServiceKey, index: Int): User = {
      val seed = System.nanoTime().toString

      val (idKey, idVal) = serviceKey match {
        case "HMRC-MTD-IT"     => ("MTDITID", Generator.mtdbsa(seed).value)
        case "HMRC-MTD-VAT"    => ("VRN", Generator.vrn(seed).value)
        case "HMRC-CGT-PD"     => ("CGTPDRef", Generator.cgtPdRef(seed))
        case "HMRC-PPT-ORG"    => ("EtmpRegistrationNumber", Generator.regex(Common.pptReferencePattern).sample.get)
        case "HMRC-TERS-ORG"   => ("SAUTR", Generator.utr(seed))
        case "HMRC-TERSNT-ORG" => ("URN", Generator.urn(seed).value)
      }

      clientType match {
        case User.AG.Individual =>
          UserGenerator
            .individual(
              userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d",
              confidenceLevel = 250,
              nino = f"AB${index + 1}%06dC"
            )
            .withPrincipalEnrolment(service = serviceKey, identifierKey = idKey, identifierValue = idVal)
        case User.AG.Organisation =>
          UserGenerator
            .organisation(userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d")
            .withPrincipalEnrolment(service = serviceKey, identifierKey = idKey, identifierValue = idVal)
      }
    }

    clientTypesAndServiceKeys(numClients).zipWithIndex.toList.foldLeft(List.empty[User]) {
      case (generatedClientsOfAgent, ((clientType, serviceKey), index: Int)) =>
        @tailrec
        def generateUniqueClientForAgent: User = {
          val generatedClient = generateClient(clientType, serviceKey, index)

          generatedClient.enrolments.principal.headOption match {
            case None =>
              generateUniqueClientForAgent
            case Some(enrolment) =>
              if (
                generatedClientsOfAgent
                  .flatMap(_.enrolments.principal)
                  .exists(_.toEnrolmentKeyTag == enrolment.toEnrolmentKeyTag)
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
          agentCode = agentUser.agentCode.orNull,
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
      clientTypes.count(_ == User.AG.Individual)
    )
    val organisationEnrolments: Seq[ServiceKey] = pickFromDistribution(
      genMethod,
      GranPermsGenRequest.defaultOrganisationServiceDistribution,
      clientTypes.count(_ == User.AG.Organisation)
    )
    individualEnrolments.map((User.AG.Individual, _)) ++ organisationEnrolments.map((User.AG.Organisation, _))
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
