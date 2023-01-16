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

import akka.actor.{ActorRef, ActorSystem}
import play.api.Logging
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.AgencyDetails
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models.{AG, BusinessDetailsRecord, BusinessPartnerRecord, Enrolment, Generator, Group, PPTSubscriptionDisplayRecord, Record, User, VatCustomerInformationRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.{GroupsRepositoryMongo, RecordsRepositoryMongo, UsersRepositoryMongo}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AgencyCreator @Inject() (
  usersRepository: UsersRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo
)(implicit
  actorSystem: ActorSystem,
  executionContext: ExecutionContext
) extends Logging {

  private val dataCreationActor: ActorRef =
    actorSystem.actorOf(
      DataCreationActor.props(usersRepository, recordsRepository, groupsRepository)
    )

  def create(agencyCreationPayload: AgencyCreationPayload): Unit = {
    persistUsers(agencyCreationPayload)
    persistAgentRecord(agencyCreationPayload)
    persistClientRecords(agencyCreationPayload)
    persistGroups(agencyCreationPayload)
  }

  private def persistUsers(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(s"Creating users for '${agencyCreationPayload.planetId}'")

    (List(agencyCreationPayload.agentUser) ++ agencyCreationPayload.clients ++ agencyCreationPayload.teamMembers)
      .foreach(user => dataCreationActor ! UserCreationPayload(user, agencyCreationPayload.planetId))
  }

  private def persistAgentRecord(agencyCreationPayload: AgencyCreationPayload): Unit = {
    val agentBusinessPartnerRecord = BusinessPartnerRecord
      .generate(agencyCreationPayload.agentUser.userId)
      .withBusinessPartnerExists(businessPartnerExists = true)
      .withIsAnOrganisation(isAnOrganisation = false)
      .withIsAnIndividual(isAnIndividual = true)
      .withAgentReferenceNumber(
        agencyCreationPayload.agentUser.assignedPrincipalEnrolments.headOption
          .flatMap(_.identifiers.headOption.map(_.value))
      )
      .withAgencyDetails(
        Some(
          AgencyDetails
            .generate(agencyCreationPayload.agentUser.userId)
            .withAgencyName(Some("Fancy agency"))
            .withAgencyEmail(Some(Generator.email(agencyCreationPayload.agentUser.userId)))
        )
      )

    dataCreationActor ! RecordCreationPayload(
      agentBusinessPartnerRecord,
      agencyCreationPayload.planetId
    )
  }

  private def persistClientRecords(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(s"Creating client records for '${agencyCreationPayload.planetId}'")

    agencyCreationPayload.clients
      .map(assembleClientRecord)
      .collect { case Some(record) => record }
      .foreach(record => dataCreationActor ! RecordCreationPayload(record, agencyCreationPayload.planetId))
  }

  private def persistGroups(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(
      s"Creating groups for '${agencyCreationPayload.planetId}'. Auto-populating friendly name for clients: ${agencyCreationPayload.populateFriendlyNames}"
    )

    agencyCreationPayload.agentUser.groupId.foreach { groupId =>
      val agentGroup = Group(
        planetId = agencyCreationPayload.planetId,
        groupId = groupId,
        affinityGroup = AG.Agent,
        principalEnrolments = agencyCreationPayload.agentUser.assignedPrincipalEnrolments.map(Enrolment.from),
        delegatedEnrolments = agencyCreationPayload.clients.zipWithIndex.flatMap { case (client, index) =>
          client.assignedPrincipalEnrolments
            .map(ek =>
              if (agencyCreationPayload.populateFriendlyNames)
                Enrolment.from(ek).copy(friendlyName = Some(s"Client ${index + 1}"))
              else Enrolment.from(ek)
            )
        }
      )

      dataCreationActor ! GroupCreationPayload(agentGroup, agencyCreationPayload.planetId)
    }

    agencyCreationPayload.clients.foreach { client =>
      client.groupId.foreach { groupId =>
        val clientGroup = Group(
          planetId = agencyCreationPayload.planetId,
          groupId = groupId,
          affinityGroup = AG.Individual,
          principalEnrolments = client.assignedPrincipalEnrolments.map(Enrolment.from)
        )

        dataCreationActor ! GroupCreationPayload(clientGroup, agencyCreationPayload.planetId)
      }
    }
  }

  def assembleClientRecord(client: User): Option[Record] =
    (for {
      enrolmentKey <- client.assignedPrincipalEnrolments.headOption
      identifier   <- enrolmentKey.identifiers.headOption
    } yield enrolmentKey.service match {
      case "HMRC-MTD-IT" =>
        Some(
          BusinessDetailsRecord
            .generate(identifier.toString)
            .withMtdbsa(identifier.value)
            .withBusinessData(
              Some(Seq(BusinessData.generate(identifier.value).withTradingName(Generator.tradingNameGen.sample)))
            )
        )
      case "HMRC-MTD-VAT" =>
        Some(
          VatCustomerInformationRecord(
            identifier.value,
            approvedInformation = Some(
              ApprovedInformation(
                CustomerDetails(organisationName = Generator.tradingNameGen.sample, mandationStatus = "1"),
                PPOB.seed("PPOB")
              )
            )
          )
        )
      case "HMRC-CGT-PD" =>
        Some(BusinessDetailsRecord.generate(identifier.toString).withCgtPdRef(Some(identifier.value)))
      case "HMRC-PPT-ORG" =>
        Some(
          PPTSubscriptionDisplayRecord
            .generateWith(
              if (client.dateOfBirth.nonEmpty) Some(AG.Individual) else Some(AG.Organisation),
              Generator.forename().sample,
              Generator.surname.sample,
              None,
              identifier.value
            )
        )
      case "HMRC-TERS-ORG" =>
        Some(BusinessDetailsRecord.generate(identifier.toString))
      case "HMRC-TERSNT-ORG" =>
        Some(BusinessDetailsRecord.generate(identifier.toString))
      case _ => Option.empty[Record]
    }).flatten
}
