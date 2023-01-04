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

package uk.gov.hmrc.agentsexternalstubs.controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.Common
import uk.gov.hmrc.agentsexternalstubs.models.User.CR
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

case class PerfDataRequest(numAgents: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)

object PerfDataRequest {
  implicit val format: OFormat[PerfDataRequest] = Json.format[PerfDataRequest]
}

case class AgencyCreationPayload(
  planetId: String,
  agentUser: User,
  agentGroup: Group,
  clients: Seq[(User, Group)],
  teamMembers: Seq[User]
)

case class UserCreationPayload(user: User, group: Option[Group], planetId: String)
case class ClientRecordCreationPayload(record: Record, planetId: String)

class PerfDataController @Inject() (
  val authenticationService: AuthenticationService,
  cc: ControllerComponents,
  usersRepository: UsersRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  authenticatedSessionsRepository: AuthenticatedSessionsRepository,
  knownFactsRepository: KnownFactsRepositoryMongo,
  specialCasesRepository: SpecialCasesRepositoryMongo,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession with Logging {

  private val dataCreationActor: ActorRef =
    actorSystem.actorOf(
      DataCreationActor.props(usersRepository, groupsRepository, recordsRepository)
    )

  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      new Thread {
        override def run(): Unit = {
          dropCollections()
          reapplyIndexes()
          generateData(perfDataRequest)
        }
      }.start()

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent + perfDataRequest.teamMembersPerAgent)} 'users', and " +
          s"${perfDataRequest.numAgents * perfDataRequest.clientsPerAgent} 'records'"
      )
    }
  }

  private def dropCollections(): Unit = {

    def dropCollectionOf[R <: PlayMongoRepository[_]](repository: R): Unit = {
      Await.result(repository.collection.drop.toFuture, Duration("20s"))
      logger.info(s"Dropped '${repository.collection.namespace}' collection if it existed")
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
      persistClientRecords(agencyCreationPayload)
    }

  private def assembleAgency(
    indexAgency: Int,
    clientsPerAgent: Int,
    teamMembersPerAgent: Int
  ): AgencyCreationPayload = {
    val planetId = f"perf-test-planet-$indexAgency%03d"
    val (agentUser, agentGroup) = buildMainAgentUser(indexAgency)
    val clients: Seq[(User, Group)] = buildClientsForAgent(indexAgency, clientsPerAgent)
    val teamMembers = buildTeamMembersForAgent(indexAgency, teamMembersPerAgent, agentUser)

    AgencyCreationPayload(
      planetId = planetId,
      agentUser = agentUser,
      agentGroup = agentGroup.copy(delegatedEnrolments = clients.map(_._2).flatMap(_.principalEnrolments)),
      clients = clients,
      teamMembers
    )

  }

  private def buildMainAgentUser(indexAgency: Int): (User, Group) = {
    val userId = f"perf-test-agent-$indexAgency%03d"
    val groupId = UserGenerator.groupId(userId)
    val principalEnrolments = Seq(
      Enrolment(
        "HMRC-AS-AGENT",
        identifiers = Some(Seq(Identifier("AgentReferenceNumber", Generator.arn(indexAgency.toString).value)))
      )
    )

    val user = User(
      userId = userId,
      groupId = Some(groupId),
      credentialRole = Some(CR.Admin)
    )

    val group =
      Group(planetId = "", groupId = groupId, affinityGroup = AG.Agent, principalEnrolments = principalEnrolments)

    (user, group)
  }

  def buildClientsForAgent(indexAgency: Int, numClients: Int): List[(User, Group)] = {

    type ClientType = String
    type ServiceKey = String

    def generateClient(clientType: ClientType, serviceKey: ServiceKey, index: Int): (User, Group) = {
      val seed = System.nanoTime().toString

      val (idKey, idVal) = serviceKey match {
        case "HMRC-MTD-IT"     => ("MTDITID", Generator.mtdbsa(seed).value)
        case "HMRC-MTD-VAT"    => ("VRN", Generator.vrn(seed).value)
        case "HMRC-CGT-PD"     => ("CGTPDRef", Generator.cgtPdRef(seed))
        case "HMRC-PPT-ORG"    => ("EtmpRegistrationNumber", Generator.regex(Common.pptReferencePattern).sample.get)
        case "HMRC-TERS-ORG"   => ("SAUTR", Generator.utr(seed))
        case "HMRC-TERSNT-ORG" => ("URN", Generator.urn(seed).value)
      }

      val user = clientType match {
        case AG.Individual =>
          UserGenerator
            .individual(
              userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d",
              confidenceLevel = 250,
              nino = f"AB${index + 1}%06dC"
            )
        case AG.Organisation =>
          UserGenerator
            .organisation(userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d")
      }
      val group = Group(
        planetId = "",
        groupId = "",
        affinityGroup = clientType,
        principalEnrolments = Seq(Enrolment(serviceKey, idKey, idVal))
      )

      (user, group)
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
        clientTypes.count(_ == AG.Individual)
      )
      val organisationEnrolments: Seq[ServiceKey] = pickFromDistribution(
        genMethod,
        GranPermsGenRequest.defaultOrganisationServiceDistribution,
        clientTypes.count(_ == AG.Organisation)
      )
      individualEnrolments.map((AG.Individual, _)) ++ organisationEnrolments.map((AG.Organisation, _))
    }

    clientTypesAndServiceKeys.zipWithIndex.toList.foldLeft(List.empty[(User, Group)]) {
      case (generatedClientsOfAgent, ((clientType, serviceKey), index: Int)) =>
        @tailrec
        def generateUniqueClientForAgent: (User, Group) = {
          val (generatedClientUser, generatedClientGroup) = generateClient(clientType, serviceKey, index)

          generatedClientGroup.principalEnrolments.headOption match {
            case None =>
              generateUniqueClientForAgent
            case Some(enrolment) =>
              if (
                generatedClientsOfAgent
                  .flatMap(_._2.principalEnrolments)
                  .exists(_.toEnrolmentKeyTag == enrolment.toEnrolmentKeyTag)
              ) {
                generateUniqueClientForAgent
              } else {
                (generatedClientUser, generatedClientGroup)
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
          credentialRole = "User"
        )
    }

  private def persistUsers(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(s"Creating users for '${agencyCreationPayload.planetId}'")

    val agent: Seq[(User, Option[Group])] = Seq(
      (agencyCreationPayload.agentUser, Some(agencyCreationPayload.agentGroup))
    )
    val clients: Seq[(User, Option[Group])] = agencyCreationPayload.clients.map { case (u, g) => (u, Some(g)) }
    // we don't need to create new groups for teamMembers as they share the same group as the same group as the agent
    val teamMembers: Seq[(User, Option[Group])] = agencyCreationPayload.teamMembers.map((_, Option.empty[Group]))

    (agent ++ clients ++ teamMembers)
      .map { case (user, maybeGroup) =>
        UserCreationPayload(user, maybeGroup, agencyCreationPayload.planetId)
      }
      .foreach(userCreationPayload => dataCreationActor ! userCreationPayload)

  }

  private def persistClientRecords(agencyCreationPayload: AgencyCreationPayload): Unit = {
    logger.info(s"Creating client records for '${agencyCreationPayload.planetId}'")

    agencyCreationPayload.clients
      .map { case (_, group) => group }
      .map(assembleRecord)
      .collect { case Some(record) => record }
      .map(record => ClientRecordCreationPayload(record, agencyCreationPayload.planetId))
      .foreach(clientRecordCreationPayload => dataCreationActor ! clientRecordCreationPayload)
  }

  def assembleRecord(group: Group): Option[Record] =
    (for {
      enrolment   <- group.principalEnrolments.headOption
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
              Some(group.affinityGroup),
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
  def props(
    usersRepository: UsersRepositoryMongo,
    groupsRepository: GroupsRepositoryMongo,
    recordsRepository: RecordsRepositoryMongo
  )(implicit
    executionContext: ExecutionContext
  ): Props = Props(new DataCreationActor(usersRepository, groupsRepository, recordsRepository))

}

class DataCreationActor(
  usersRepository: UsersRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo
)(implicit
  executionContext: ExecutionContext
) extends Actor with Logging {

  import uk.gov.hmrc.agentsexternalstubs.syntax.|>

  override def receive: Receive = {
    case UserCreationPayload(user: User, maybeGroup, planetId: String) =>
      for {
        _ <- maybeGroup.fold(Future.successful(()))(group =>
               groupsRepository.create(group.copy(planetId = planetId), planetId).recover { case ex =>
                 logger.error(s"Could not create group ${group.groupId} of $planetId: ${ex.getMessage}")
               }
             )
        _ <- usersRepository
               .create(user, planetId)
               .recover { case ex =>
                 logger.error(s"Could not create user ${user.userId} of $planetId: ${ex.getMessage}")
               }
      } yield ()
    case ClientRecordCreationPayload(entity: Record, planetId: String) =>
      recordsRepository
        .rawStore(recordAsJson(entity, planetId))
        .map(id => logger.debug(s"Created record of id $id"))
        .recover { case ex =>
          logger.error(s"Could not create record for ${entity.uniqueKey} of $planetId: ${ex.getMessage}")
        }
  }

  private def recordAsJson(record: Record, planetId: String): JsonAbuse[Record] = {
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

}
