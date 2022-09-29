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

import akka.actor.{ActorRef, ActorSystem}
import play.api.Logging
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.AgencyDetails
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessDetailsRecord, BusinessPartnerRecord, Generator, PPTSubscriptionDisplayRecord, Record, User, VatCustomerInformationRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.{RecordsRepositoryMongo, UsersRepositoryMongo}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AgencyCreator @Inject() (usersRepository: UsersRepositoryMongo, recordsRepository: RecordsRepositoryMongo)(
  implicit
  actorSystem: ActorSystem,
  executionContext: ExecutionContext
) extends Logging {

  private val dataCreationActor: ActorRef =
    actorSystem.actorOf(
      DataCreationActor.props(usersRepository, recordsRepository)
    )

  def create(agencyCreationPayload: AgencyCreationPayload): Unit = {
    persistUsers(agencyCreationPayload)
    persistAgentRecord(agencyCreationPayload)
    persistClientRecords(agencyCreationPayload)
  }

  private def persistUsers(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(s"Creating users for '${agencyCreationPayload.planetId}'")

    (List(agencyCreationPayload.agentUser) ++ agencyCreationPayload.clients ++ agencyCreationPayload.teamMembers)
      .map(user => UserCreationPayload(user, agencyCreationPayload.planetId))
      .foreach(userCreationPayload => dataCreationActor ! userCreationPayload)
  }

  private def persistAgentRecord(agencyCreationPayload: AgencyCreationPayload): Unit = {
    val agentBusinessPartnerRecord = BusinessPartnerRecord
      .generate(agencyCreationPayload.agentUser.userId)
      .withBusinessPartnerExists(true)
      .withIsAnOrganisation(false)
      .withIsAnIndividual(true)
      .withAgentReferenceNumber(
        agencyCreationPayload.agentUser.enrolments.principal.headOption
          .flatMap(_.identifiers.flatMap(d => d.headOption.map(_.value)))
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
      .map(record => RecordCreationPayload(record, agencyCreationPayload.planetId))
      .foreach(recordCreationPayload => dataCreationActor ! recordCreationPayload)
  }

  def assembleClientRecord(client: User): Option[Record] =
    (for {
      enrolment   <- client.enrolments.principal.headOption
      identifiers <- enrolment.identifiers
      identifier  <- identifiers.headOption
    } yield enrolment.key match {
      case "HMRC-MTD-IT" =>
        Some(
          BusinessDetailsRecord
            .generate(identifier.toString)
            .withMtdbsa(identifier.value)
            .withBusinessData(
              Some(Seq(BusinessData.generate(identifier.value).withTradingName(Some("Trading Name"))))
            )
        )
      case "HMRC-MTD-VAT" =>
        Some(
          VatCustomerInformationRecord(
            identifier.value,
            approvedInformation = Some(
              ApprovedInformation(
                CustomerDetails(organisationName = Some("VAT CLient"), mandationStatus = "1"),
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
              client.affinityGroup,
              Some("Jane"),
              Some("Doe"),
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
