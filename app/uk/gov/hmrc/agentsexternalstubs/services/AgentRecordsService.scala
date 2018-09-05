package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.AgentRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentRecordsService @Inject()(val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: AgentRecord, autoFill: Boolean, planetId: String)(implicit ec: ExecutionContext): Future[String] =
    AgentRecord
      .validate(record)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          val entity = if (autoFill) AgentRecord.sanitize(record.safeId)(record) else record
          recordsRepository.store(entity, planetId)
        }
      )

  def getAgentRecord(arn: Arn, planetId: String)(implicit ec: ExecutionContext): Future[Option[AgentRecord]] =
    findByKey[AgentRecord](AgentRecord.uniqueKey(arn.value), planetId).map(_.headOption)

  def getAgentRecord(utr: Utr, planetId: String)(implicit ec: ExecutionContext): Future[Option[AgentRecord]] =
    findByKey[AgentRecord](AgentRecord.utrKey(utr.value), planetId).map(_.headOption)

}
