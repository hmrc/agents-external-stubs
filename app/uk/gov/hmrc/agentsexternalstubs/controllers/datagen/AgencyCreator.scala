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
import play.api.libs.json.{JsArray, JsString}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData.BusinessAddressDetails
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.AgencyDetails
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgencyCreator @Inject() (
  usersRepository: UsersRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo,
  knownFactsRepository: KnownFactsRepository
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def create(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
    logger.info(s"Deleting any existing data for '${agencyCreationPayload.planetId}'")

    for {
      _ <- usersRepository.destroyPlanet(agencyCreationPayload.planetId)
      _ <- recordsRepository.destroyPlanet(agencyCreationPayload.planetId)
      _ <- groupsRepository.destroyPlanet(agencyCreationPayload.planetId)
      _ <- knownFactsRepository.destroyPlanet(agencyCreationPayload.planetId)

      _ <- persistUsers(agencyCreationPayload)
      _ <- persistAgentRecord(agencyCreationPayload)
      _ <- persistClientRecords(agencyCreationPayload)
      _ <- persistGroups(agencyCreationPayload)
      _ <- persistRelationshipRecords(agencyCreationPayload)
    } yield ()
  }

  private def persistUsers(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
    logger.info(s"Creating users for '${agencyCreationPayload.planetId}'")

    val usersToCreate =
      List(agencyCreationPayload.agentUser) ++ agencyCreationPayload.clients ++ agencyCreationPayload.teamMembers

    executeSerially(usersToCreate) { user =>
      usersRepository
        .create(user, agencyCreationPayload.planetId)
        .recover { case ex =>
          logger.error(s"Could not create user ${user.userId} of ${agencyCreationPayload.planetId}: ${ex.getMessage}")
        }
    }
  }

  private def persistAgentRecord(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
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

    recordsRepository
      .rawStore(recordAsJson(agentBusinessPartnerRecord, agencyCreationPayload.planetId))
      .map(id => logger.debug(s"Created record of id $id"))
      .recover { case ex =>
        logger.error(
          s"Could not create record for ${agentBusinessPartnerRecord.uniqueKey} of ${agencyCreationPayload.planetId}: ${ex.getMessage}"
        )
      }

  }

  private def persistRelationshipRecords(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
    logger.info(s"Creating relationship records for '${agencyCreationPayload.planetId}'")

    val arn = agencyCreationPayload.agentUser.assignedPrincipalEnrolments.headOption.map(_.tag.split('~').last).get

    val records = agencyCreationPayload.clients
      .map(client => assembleRelationshipRecord(client, arn))
      .collect { case Some(record) => record }

    executeSerially(records) { record =>
      recordsRepository
        .rawStore(recordAsJson(record, agencyCreationPayload.planetId))
        .map(id => logger.debug(s"Created record of id $id"))
        .recover { case ex =>
          logger.error(
            s"Could not create record for ${record.uniqueKey} of ${agencyCreationPayload.planetId}: ${ex.getMessage}"
          )
        }
    }
  }

  def assembleRelationshipRecord(client: User, arn: String): Option[RelationshipRecord] =
    for {
      enrolmentKey <- client.assignedPrincipalEnrolments.headOption
      identifier   <- enrolmentKey.identifiers.headOption
    } yield enrolmentKey.service match {
      case "HMRC-MTD-IT" =>
        RelationshipRecord(
          regime = "ITSA",
          arn = arn,
          idType = "none",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
      case "HMRC-MTD-VAT" =>
        RelationshipRecord(
          regime = "VATC",
          arn = arn,
          idType = "VRN",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
      case "HMRC-CGT-PD" =>
        RelationshipRecord(
          regime = "CGT",
          arn = arn,
          idType = "ZCGT",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
      case "HMRC-PPT-ORG" =>
        RelationshipRecord(
          regime = "PPT",
          arn = arn,
          idType = "ZPPT",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
      case "HMRC-TERS-ORG" =>
        RelationshipRecord(
          regime = "TRS",
          arn = arn,
          idType = "UTR",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
      case "HMRC-TERSNT-ORG" =>
        RelationshipRecord(
          regime = "TRS",
          arn = arn,
          idType = "URN",
          refNumber = identifier.value,
          relationshipType = Some("ZA01"),
          authProfile = Some("ALL00001"),
          startDate = Some(LocalDate.now)
        )
    }

  private def persistClientRecords(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
    logger.info(s"Creating client records for '${agencyCreationPayload.planetId}'")

    val records = agencyCreationPayload.clients
      .map(assembleClientRecord)
      .collect { case Some(record) => record }

    executeSerially(records) { record =>
      recordsRepository
        .rawStore(recordAsJson(record, agencyCreationPayload.planetId))
        .map(id => logger.debug(s"Created record of id $id"))
        .recover { case ex =>
          logger.error(
            s"Could not create record for ${record.uniqueKey} of ${agencyCreationPayload.planetId}: ${ex.getMessage}"
          )
        }
    }
  }

  private def persistGroups(agencyCreationPayload: AgencyCreationPayload): Future[Unit] = {
    logger.info(
      s"Creating groups for '${agencyCreationPayload.planetId}'. Auto-populating friendly name for clients: ${agencyCreationPayload.populateFriendlyNames}"
    )

    def updateKnownFacts(user: User, group: Group, planetId: String): Future[Unit] =
      Future
        .sequence(
          group.principalEnrolments
            .map(_.toEnrolmentKey.flatMap(ek => KnownFacts.generate(ek, user.userId, user.facts)))
            .collect { case Some(x) => x }
            .map(knownFacts =>
              knownFactsRepository.upsert(knownFacts.applyProperties(User.knownFactsOf(user)), planetId)
            )
        )
        .map(_ => ())

    def persistAgentGroup: Future[Unit] = agencyCreationPayload.agentUser.groupId match {
      case Some(groupId) =>
        val agentGroup = Group(
          planetId = agencyCreationPayload.planetId,
          groupId = groupId,
          affinityGroup = AG.Agent,
          agentId = Some(agencyCreationPayload.agentId),
          agentCode = Some(agencyCreationPayload.agentCode),
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

        groupsRepository
          .create(agentGroup, agencyCreationPayload.planetId)
          .flatMap(_ => updateKnownFacts(agencyCreationPayload.agentUser, agentGroup, agencyCreationPayload.planetId))
          .recover { case ex =>
            logger.error(
              s"Could not create group ${agentGroup.groupId} of ${agencyCreationPayload.planetId}: ${ex.getMessage}"
            )
          }

      case _ =>
        Future successful (())
    }

    def persistClientGroups: Future[Unit] = executeSerially(agencyCreationPayload.clients) { client =>
      client.groupId match {
        case Some(groupId) =>
          val clientGroup = Group(
            planetId = agencyCreationPayload.planetId,
            groupId = groupId,
            affinityGroup = AG.Individual,
            principalEnrolments = client.assignedPrincipalEnrolments.map(Enrolment.from)
          )

          groupsRepository
            .create(clientGroup, agencyCreationPayload.planetId)
            .flatMap(_ => updateKnownFacts(client, clientGroup, agencyCreationPayload.planetId))
            .recover { case ex =>
              logger.error(
                s"Could not create group ${clientGroup.groupId} of ${agencyCreationPayload.planetId}: ${ex.getMessage}"
              )
            }
        case _ =>
          Future successful (())
      }

    }

    persistAgentGroup flatMap { _ =>
      persistClientGroups
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
            .withMtdId(identifier.value)
            .withBusinessData(
              Some(
                Seq(
                  BusinessData
                    .generate(identifier.value)
                    .withBusinessAddressDetails(
                      BusinessAddressDetails.gen.sample
                    )
                    .withTradingName(
                      Generator.tradingNameGen.suchThat(_.nonEmpty).suchThat(_.length <= 105).sample orElse Option(
                        "Trading Name"
                      )
                    )
                )
              )
            )
        )
      case "HMRC-MTD-VAT" =>
        Some(
          VatCustomerInformationRecord(
            identifier.value,
            approvedInformation = Some(
              ApprovedInformation(
                CustomerDetails(
                  organisationName =
                    Generator.tradingNameGen.suchThat(_.nonEmpty).suchThat(_.length <= 105).sample orElse Option(
                      "Trading Name"
                    ),
                  mandationStatus = "1",
                  effectiveRegistrationDate = Generator.date(1970, 2022).sample.map(_.toString)
                ),
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
              Generator.forename().suchThat(_.nonEmpty).sample,
              Generator.surname.suchThat(_.nonEmpty).sample,
              Generator.date(2020, 2022).sample.map(_.toString),
              identifier.value
            )
        )
      case "HMRC-TERS-ORG" =>
        Some(BusinessDetailsRecord.generate(identifier.toString))
      case "HMRC-TERSNT-ORG" =>
        Some(BusinessDetailsRecord.generate(identifier.toString))
      case _ => Option.empty[Record]
    }).flatten

  private def recordAsJson(record: Record, planetId: String): JsonAbuse[Record] = {
    import uk.gov.hmrc.agentsexternalstubs.syntax.|>

    val PLANET_ID = "_planetId"
    val UNIQUE_KEY = "_uniqueKey"
    val KEYS = "_keys"
    val TYPE = "_record_type"
    val typeName = Record.typeOf(record)

    JsonAbuse(record)
      .addField(PLANET_ID, JsString(planetId))
      .addField(TYPE, JsString(typeName))
      .addField(
        KEYS,
        JsArray(
          record.uniqueKey
            .map(key => record.lookupKeys :+ key)
            .getOrElse(record.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, typeName)))
        )
      )
      .|> { obj =>
        record.uniqueKey
          .map(uniqueKey => obj.addField(UNIQUE_KEY, JsString(keyOf(uniqueKey, planetId, typeName))))
          .getOrElse(obj)
      }
  }

  private def keyOf[T <: Record](key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

  def executeSerially[A](list: Iterable[A])(fn: A => Future[Unit]): Future[Unit] =
    list.foldLeft(Future successful (())) { (accFutureList, nextItem) =>
      accFutureList.flatMap { _ =>
        fn(nextItem)
      }
    }
}
