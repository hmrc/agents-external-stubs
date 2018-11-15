package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import reactivemongo.api.Cursor
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipRecordsService @Inject()(recordsRepository: RecordsRepository) {

  private val MAX_DOCS = 1000

  def store(record: RelationshipRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] = {
    val entity = if (autoFill) RelationshipRecord.sanitize(record.arn)(record) else record
    RelationshipRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          recordsRepository.store(entity, planetId)
        }
      )
  }

  def authorise(relationship: RelationshipRecord, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      existing <- findByKey(
                   RelationshipRecord.clientKey(relationship.regime, relationship.idType, relationship.refNumber),
                   planetId)
      _ <- deActivate(existing, planetId)
      _ <- recordsRepository
            .store[RelationshipRecord](relationship.copy(active = true, startDate = Some(LocalDate.now())), planetId)
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
    implicit ec: ExecutionContext): Future[Seq[String]] =
    Future.sequence(
      relationships
        .filter(_.active)
        .map(r => r.copy(active = false, endDate = Some(LocalDate.now)))
        .map(r => recordsRepository.store[RelationshipRecord](r, planetId)))

  def findByKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[List[RelationshipRecord]] =
    recordsRepository.cursor[RelationshipRecord](key, planetId).collect[List](MAX_DOCS, Cursor.FailOnError())

  def findByQuery(query: RelationshipRecordQuery, planetId: String)(
    implicit ec: ExecutionContext): Future[List[RelationshipRecord]] = {

    val maybeActiveOnly: RelationshipRecord => Boolean = r => if (query.activeOnly) r.active else true

    val maybeFromDate: RelationshipRecord => Boolean = r =>
      if (query.activeOnly) true
      else query.from.forall(qf => r.startDate.forall(rf => !rf.isBefore(qf)))

    val maybeToDate: RelationshipRecord => Boolean = r =>
      if (query.activeOnly) true else query.to.forall(qt => r.startDate.forall(rt => !rt.isAfter(qt)))

    val key =
      if (query.agent)
        RelationshipRecord.agentKey(query.regime, query.arn.getOrElse(throw new Exception("Missing arn parameter")))
      else
        RelationshipRecord.clientKey(
          query.regime,
          query.idType,
          query.refNumber.getOrElse(throw new Exception("Missing refNumber parameter")))

    findByKey(key, planetId)
      .map(
        _.filter(maybeActiveOnly)
          .filter(maybeFromDate)
          .filter(maybeToDate))
  }

}

case class RelationshipRecordQuery(
  regime: String,
  arn: Option[String] = None,
  idType: String,
  refNumber: Option[String] = None,
  activeOnly: Boolean = true,
  agent: Boolean,
  from: Option[LocalDate] = None,
  to: Option[LocalDate] = None)
