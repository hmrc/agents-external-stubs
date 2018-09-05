package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{LegacyAgentRecord, LegacyRelationshipRecord, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LegacyRelationshipRecordsService @Inject()(recordsRepository: RecordsRepository) {

  def store(record: LegacyRelationshipRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] =
    LegacyRelationshipRecord
      .validate(record)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          val entity = if (autoFill) LegacyRelationshipRecord.sanitize(record.agentId)(record) else record
          recordsRepository.store(entity, planetId)
        }
      )

  def store(record: LegacyAgentRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] =
    LegacyAgentRecord
      .validate(record)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          val entity = if (autoFill) LegacyAgentRecord.sanitize(record.agentId)(record) else record
          recordsRepository.store(entity, planetId)
        }
      )

  def getLegacyRelationshipsByNino(nino: String, planetId: String)(
    implicit ec: ExecutionContext): Future[List[(String, LegacyAgentRecord)]] =
    findRelationshipsByKey(LegacyRelationshipRecord.ninoKey(nino), planetId)
      .flatMap(rr => getNinosWithAgents(rr.distinct, planetId))

  def getLegacyRelationshipsByUtr(nino: String, planetId: String)(
    implicit ec: ExecutionContext): Future[List[(String, LegacyAgentRecord)]] =
    findRelationshipsByKey(LegacyRelationshipRecord.utrKey(nino), planetId)
      .flatMap(rr => getNinosWithAgents(rr.distinct, planetId))

  def getLegacyRelationship(id: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[LegacyRelationshipRecord]] =
    recordsRepository.findById[LegacyRelationshipRecord](id, planetId)

  def getLegacyAgent(id: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[LegacyAgentRecord]] =
    recordsRepository.findById[LegacyAgentRecord](id, planetId)

  private def getNinosWithAgents(relationships: List[LegacyRelationshipRecord], planetId: String)(
    implicit ec: ExecutionContext): Future[List[(String, LegacyAgentRecord)]] =
    Future
      .sequence(
        relationships
          .map(_.agentId)
          .distinct
          .map(aid =>
            findAgentByKey(LegacyAgentRecord.agentIdKey(aid), planetId).map(agentOpt =>
              agentOpt.map(agent => (aid, agent)))))
      .map(_.collect { case Some(x) => x }.toMap)
      .map(
        agentsMap =>
          relationships.map(
            r =>
              (
                r.nino.getOrElse(UserGenerator.ninoNoSpaces(r.agentId).value),
                agentsMap.getOrElse(
                  r.agentId, {
                    val address = UserGenerator.address(r.agentId)
                    val agent = LegacyAgentRecord(
                      agentId = r.agentId,
                      agentName = UserGenerator.nameForAgent(r.agentId),
                      address1 = address.street,
                      address2 = address.town)
                    LegacyAgentRecord.sanitize(agent.agentId)(agent)
                  }
                ))))

  private def findAgentByKey(key: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[LegacyAgentRecord]] =
    recordsRepository.cursor[LegacyAgentRecord](key, planetId).headOption

  private def findRelationshipsByKey(key: String, planetId: String)(
    implicit ec: ExecutionContext): Future[List[LegacyRelationshipRecord]] =
    recordsRepository.cursor[LegacyRelationshipRecord](key, planetId).collect[List](1000)

}
