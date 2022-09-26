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
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.Common
import uk.gov.hmrc.agentsexternalstubs.models.User.{AG, CR, Enrolments}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, PPOB}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class PerfDataRequest(numAgents: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)

object PerfDataRequest {
  implicit val format: OFormat[PerfDataRequest] = Json.format[PerfDataRequest]
}

case class AgencyCreationPayload(planetId: String, agentUser: User, clients: List[User], teamMembers: List[User])

case class UserCreationPayload(user: User, planetId: String)
case class ClientRecordCreationPayload(record: Record, planetId: String)

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

  type ClientType = String
  type ServiceKey = String

  private val dataCreationActor: ActorRef =
    actorSystem.actorOf(
      DataCreationActor.props(usersRepository, recordsRepository)
    )

  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      dropCollections().map { _ =>
        process(perfDataRequest)
      }

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent + perfDataRequest.teamMembersPerAgent)} records"
      )

    }
  }

  private def dropCollections(): Future[Unit] =
    Future {
      usersRepository.drop
      logger.info(s"Dropped '${usersRepository.collection.name}' collection if it existed")
      recordsRepository.drop
      logger.info(s"Dropped '${recordsRepository.collection.name}' collection if it existed")
      authenticatedSessionsRepository.drop
      logger.info(s"Dropped '${authenticatedSessionsRepository.collection.name}' collection if it existed")
      knownFactsRepository.drop
      logger.info(s"Dropped '${knownFactsRepository.collection.name}' collection if it existed")
      specialCasesRepository.drop
      logger.info(s"Dropped '${specialCasesRepository.collection.name}' collection if it existed")
    }

  private def process(perfDataRequest: PerfDataRequest): Unit = {

    def assembleAgency(indexAgency: Int) = {
      val planetId = f"perf-test-planet-$indexAgency%03d"
      val agentUser = buildMainAgentUser(indexAgency)
      val clients = buildClientsForAgent(indexAgency, perfDataRequest.clientsPerAgent)
      val teamMembers = buildTeamMembersForAgent(indexAgency, perfDataRequest.teamMembersPerAgent, agentUser)

      AgencyCreationPayload(
        planetId,
        agentUser.copy(enrolments = agentUser.enrolments.copy(delegated = clients.flatMap(_.enrolments.principal))),
        clients,
        teamMembers
      )

    }

    for (indexAgency <- (1 to perfDataRequest.numAgents).toList) {
      val agencyCreationPayload = assembleAgency(indexAgency)

      Future(persistUsers(agencyCreationPayload))
      Future(persistClientRecords(agencyCreationPayload))
    }
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

  def buildClientsForAgent(indexAgency: Int, numClients: Int): List[User] = {
    val clientTypeAndEnrolmentToGenerate: Seq[(ClientType, ServiceKey)] = {
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

    clientTypeAndEnrolmentToGenerate.zipWithIndex.toList.map { case ((clientType, service), index: Int) =>
      val seed = s"$indexAgency-$index"

      val (idKey, idVal) = service match {
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
            .individual(userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d", confidenceLevel = 250)
            .withPrincipalEnrolment(service = service, identifierKey = idKey, identifierValue = idVal)
        case User.AG.Organisation =>
          UserGenerator
            .organisation(userId = f"perf-test-$indexAgency%04d-C${index + 1}%05d")
            .withPrincipalEnrolment(service = service, identifierKey = idKey, identifierValue = idVal)
      }
    }
  }

  private def buildTeamMembersForAgent(indexAgency: Int, teamMembersPerAgent: Int, agentUser: User) =
    (1 to teamMembersPerAgent).toList.map { index =>
      UserGenerator
        .agent(
          userId = f"perf-test-$indexAgency%04d-A$index%05d",
          groupId = agentUser.groupId.orNull,
          agentCode = agentUser.agentCode.orNull,
          credentialRole = "User"
        )
    }

  private def persistUsers(agencyCreationPayload: AgencyCreationPayload): Unit = {
    (List(agencyCreationPayload.agentUser) ++ agencyCreationPayload.clients ++ agencyCreationPayload.teamMembers)
      .map(user => UserCreationPayload(user, agencyCreationPayload.planetId))
      .foreach(userCreationPayload => dataCreationActor ! userCreationPayload)

    logger.info(s"Queued creation of users for '${agencyCreationPayload.agentUser.userId}'")
  }

  private def persistClientRecords(agencyCreationPayload: AgencyCreationPayload) = {
    agencyCreationPayload.clients
      .map(assembleRecord)
      .collect { case Some(record) => record }
      .map(record => ClientRecordCreationPayload(record, agencyCreationPayload.planetId))
      .foreach(clientRecordCreationPayload => dataCreationActor ! clientRecordCreationPayload)

    logger.info(s"Queued creation of client records for '${agencyCreationPayload.agentUser.userId}'")
  }

  def assembleRecord(client: User): Option[Record] =
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
    case ClientRecordCreationPayload(entity: Record, planetId: String) =>
      val PLANET_ID = "_planetId"
      val UNIQUE_KEY = "_uniqueKey"
      val KEYS = "_keys"
      val TYPE = "_record_type"

      val typeName = Record.typeOf(entity)
      val json: JsObject = Json
        .toJson[Record](entity)
        .as[JsObject]
        .+(PLANET_ID -> JsString(planetId))
        .+(TYPE -> JsString(typeName))
        .+(
          KEYS -> JsArray(
            entity.uniqueKey
              .map(key => entity.lookupKeys :+ key)
              .getOrElse(entity.lookupKeys)
              .map(key => JsString(keyOf(key, planetId, typeName)))
          )
        )
        .|> { obj =>
          entity.uniqueKey
            .map(uniqueKey => obj.+(UNIQUE_KEY -> JsString(keyOf(uniqueKey, planetId, typeName))))
            .getOrElse(obj)
        }

      recordsRepository.rawStore(json)
  }

  private def keyOf[T <: Record](key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

}
