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

package uk.gov.hmrc.agentsexternalstubs.controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.AgencyDetails
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.Common
import uk.gov.hmrc.agentsexternalstubs.models.User.{AG, CR, Enrolments}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class PerfDataRequest(numAgents: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)

object PerfDataRequest {
  implicit val format: OFormat[PerfDataRequest] = Json.format[PerfDataRequest]
}

case class AgencyCreationPayload(planetId: String, agentUser: User, clients: List[User], teamMembers: List[User])

case class UserCreationPayload(user: User, planetId: String)
case class RecordCreationPayload(record: Record, planetId: String)

class PerfDataController @Inject() (
  val authenticationService: AuthenticationService,
  cc: ControllerComponents,
  usersRepository: UsersRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  authenticatedSessionsRepository: AuthenticatedSessionsRepository,
  knownFactsRepository: KnownFactsRepositoryMongo,
  specialCasesRepository: SpecialCasesRepositoryMongo,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession with Logging {

  private val dataCreationActor: ActorRef =
    actorSystem.actorOf(
      DataCreationActor.props(usersRepository, recordsRepository)
    )

  private val AGENT_NINO_OFFSET = 90000

  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      new Thread {
        override def run(): Unit = {
          dropCollections()
          reapplyIndexes()
          generateData(perfDataRequest)
          logger.info(s"Done with data generation")
        }
      }.start()

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent + perfDataRequest.teamMembersPerAgent)} 'users', and " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent)} 'records'"
      )
    }
  }

  private def dropCollections(): Unit = {

    def dropCollectionOf[R <: ReactiveRepository[_, _]](repository: R): Unit = {
      repository.drop
      logger.info(s"Dropped '${repository.collection.name}' collection if it existed")
    }

    dropCollectionOf(usersRepository)
    dropCollectionOf(recordsRepository)
    dropCollectionOf(authenticatedSessionsRepository)
    dropCollectionOf(knownFactsRepository)
    dropCollectionOf(specialCasesRepository)
  }

  private def reapplyIndexes(): Unit = {
    usersRepository.ensureIndexes
    recordsRepository.ensureIndexes
    authenticatedSessionsRepository.ensureIndexes
    knownFactsRepository.ensureIndexes
    specialCasesRepository.ensureIndexes

    logger.info(s"Re-applied indexes")
  }

  private def generateData(perfDataRequest: PerfDataRequest): Unit =
    for (indexAgency <- (1 to perfDataRequest.numAgents).toList) {
      val agencyCreationPayload =
        assembleAgency(indexAgency, perfDataRequest.clientsPerAgent, perfDataRequest.teamMembersPerAgent)

      persistUsers(agencyCreationPayload)
      persistAgentRecord(agencyCreationPayload)
      persistClientRecords(agencyCreationPayload)
    }

  private def assembleAgency(
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

    type ClientType = String
    type ServiceKey = String

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

    def clientTypesAndServiceKeys: Seq[(ClientType, ServiceKey)] = {
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

    clientTypesAndServiceKeys.zipWithIndex.toList.foldLeft(List.empty[User]) {
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

object DataCreationActor {
  def props(usersRepository: UsersRepositoryMongo, recordsRepository: RecordsRepositoryMongo)(implicit
    executionContext: ExecutionContext
  ): Props = Props(new DataCreationActor(usersRepository, recordsRepository))

}

class DataCreationActor(usersRepository: UsersRepositoryMongo, recordsRepository: RecordsRepositoryMongo)(implicit
  executionContext: ExecutionContext
) extends Actor with Logging {

  import uk.gov.hmrc.agentsexternalstubs.syntax.|>

  override def receive: Receive = {
    case UserCreationPayload(user: User, planetId: String) =>
      usersRepository
        .create(user, planetId)
        .recover { case ex =>
          logger.error(s"Could not create user ${user.userId} of $planetId: ${ex.getMessage}")
        }
    case RecordCreationPayload(entity: Record, planetId: String) =>
      recordsRepository
        .rawStore(recordAsJson(entity, planetId))
        .map(id => logger.debug(s"Created record of id $id"))
        .recover { case ex =>
          logger.error(s"Could not create record for ${entity.uniqueKey} of $planetId: ${ex.getMessage}")
        }
  }

  private def recordAsJson(record: Record, planetId: String): JsObject = {
    val PLANET_ID = "_planetId"
    val UNIQUE_KEY = "_uniqueKey"
    val KEYS = "_keys"
    val TYPE = "_record_type"
    val typeName = Record.typeOf(record)

    Json
      .toJson[Record](record)
      .as[JsObject]
      .+(PLANET_ID -> JsString(planetId))
      .+(TYPE -> JsString(typeName))
      .+(
        KEYS -> JsArray(
          record.uniqueKey
            .map(key => record.lookupKeys :+ key)
            .getOrElse(record.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, typeName)))
        )
      )
      .|> { obj =>
        record.uniqueKey
          .map(uniqueKey => obj.+(UNIQUE_KEY -> JsString(keyOf(uniqueKey, planetId, typeName))))
          .getOrElse(obj)
      }
  }

  private def keyOf[T <: Record](key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

}
