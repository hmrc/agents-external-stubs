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

package uk.gov.hmrc.agentsexternalstubs.services

import uk.gov.hmrc.agentsexternalstubs.models._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import cats._
import cats.data._
import cats.implicits._

import scala.util.Random

@Singleton
class GranPermsService @Inject() (
  usersService: UsersService
) {

  type ClientType = String
  type EnrolmentKey = String

  def massGenerateClients(planetId: String, genRequest: GranPermsGenRequest, idNaming: Option[Int => String] = None)(
    implicit ec: ExecutionContext
  ): Future[List[User]] = {
    val idFunction: Int => String = idNaming.getOrElse(n => f"${genRequest.idPrefix}%sC$n%03d")
    val clientTypeAndEnrolmentToGenerate: Seq[(ClientType, EnrolmentKey)] = {
      val genMethod = genRequest.genMethod.getOrElse(GranPermsGenRequest.GenMethodProportional)
      val clientTypes: Seq[ClientType] = pickFromDistribution(
        genMethod,
        genRequest.clientTypeDistribution.getOrElse(GranPermsGenRequest.defaultClientTypeDistribution),
        genRequest.numberOfClients
      )
      val individualEnrolments: Seq[EnrolmentKey] = pickFromDistribution(
        genMethod,
        genRequest.individualServiceDistribution.getOrElse(GranPermsGenRequest.defaultIndividualServiceDistribution),
        clientTypes.count(_ == User.AG.Individual)
      )
      val organisationEnrolments: Seq[EnrolmentKey] = pickFromDistribution(
        genMethod,
        genRequest.organisationServiceDistribution.getOrElse(
          GranPermsGenRequest.defaultOrganisationServiceDistribution
        ),
        clientTypes.count(_ == User.AG.Organisation)
      )
      individualEnrolments.map((User.AG.Individual, _)) ++ organisationEnrolments.map((User.AG.Organisation, _))
    }
    clientTypeAndEnrolmentToGenerate.zipWithIndex.toList.traverse { case ((clientType, enrolmentKey), i: Int) =>
      val user = clientType match {
        case User.AG.Individual =>
          UserGenerator
            .individual(userId = idFunction(i), confidenceLevel = 250)
            .withPrincipalEnrolment(service = enrolmentKey, identifierKey = "", identifierValue = "")
        case User.AG.Organisation =>
          UserGenerator
            .organisation(userId = idFunction(i))
            .withPrincipalEnrolment(service = enrolmentKey, identifierKey = "", identifierValue = "")
      }
      usersService.createUser(
        planetId = planetId,
        user = user
      )
    }
  }

  def massGenerateAgents(
    planetId: String,
    genRequest: GranPermsGenRequest,
    groupId: Option[String],
    agentCode: Option[String],
    delegatedEnrolments: Seq[Enrolment],
    idNaming: Option[Int => String] = None
  )(implicit ec: ExecutionContext): Future[List[User]] = {
    val idFn: Int => String = idNaming.getOrElse(x => f"${genRequest.idPrefix}%sA$x%02d")
    (1 to genRequest.numberOfAgents).toList.traverse { x =>
      val user = UserGenerator
        .agent(
          userId = idFn(x),
          groupId = groupId.orNull,
          agentCode = agentCode.orNull,
          delegatedEnrolments = delegatedEnrolments
        )
      usersService.createUser(
        planetId = planetId,
        user = user
      )
    }
  }

  def massGenerateAgentsAndClients(
    planetId: String,
    currentUser: User,
    genRequest: GranPermsGenRequest
  )(implicit ec: ExecutionContext): Future[(Seq[User], Seq[User])] = for {
    clients <- massGenerateClients(planetId, genRequest)
    delegatedEnrols = clients.flatMap(_.enrolments.principal)
    _ <-
      usersService
        .updateUser(currentUser.userId, planetId, _ => currentUser.updateDelegatedEnrolments(_ => delegatedEnrols))
    agents <- massGenerateAgents(
                planetId,
                genRequest,
                currentUser.groupId,
                currentUser.agentCode,
                delegatedEnrolments = Seq.empty
              )
  } yield (agents, clients)

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
    (0 until n).toSeq.map { i =>
      val fractionalIndex = (i.toDouble + 0.5) / n.toDouble
      val chosenIndex = intervalPartition.indexWhere(fractionalIndex < _)
      if (chosenIndex < 0) // not found - should only ever happen (rarely) due to rounding errors
        normalisedDistribution.last._1
      else
        normalisedDistribution(chosenIndex)._1
    }
  }

}
