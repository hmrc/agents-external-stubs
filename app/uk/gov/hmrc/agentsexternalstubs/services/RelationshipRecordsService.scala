package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipRecordsService @Inject()(recordsRepository: RecordsRepository) {

  private val MAX_DOCS = 1000

  def authorise(relationship: RelationshipRecord, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      existing <- findByKey(
                   RelationshipRecord.clientKey(relationship.regime, relationship.idType, relationship.refNumber),
                   planetId)
      _ <- deActivate(existing, planetId)
      _ <- recordsRepository.store(relationship.copy(active = true, startDate = Some(LocalDate.now())), planetId)
    } yield ()

  def deAuthorise(relationship: RelationshipRecord, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      existing <- findByKey(
                   RelationshipRecord
                     .fullKey(relationship.regime, relationship.arn, relationship.idType, relationship.refNumber),
                   planetId)
      _ <- deActivate(existing, planetId)

    } yield ()

  private def deActivate(relationships: Seq[RelationshipRecord], planetId: String)(
    implicit ec: ExecutionContext): Future[Seq[Unit]] =
    Future.sequence(
      relationships
        .filter(_.active)
        .map(r => r.copy(active = false, endDate = Some(LocalDate.now)))
        .map(r => recordsRepository.store(r, planetId)))

  def findByKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[List[RelationshipRecord]] =
    recordsRepository.cursor(key, planetId).collect[List](MAX_DOCS)
}
