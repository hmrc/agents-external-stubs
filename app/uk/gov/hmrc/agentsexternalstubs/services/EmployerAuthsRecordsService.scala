package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.EmployerAuths
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmployerAuthsRecordsService @Inject()(val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: EmployerAuths, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] = {
    val entity = if (autoFill) EmployerAuths.sanitize(record.agentCode)(record) else record
    EmployerAuths
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          recordsRepository.store(entity, planetId)
        }
      )
  }

  def getEmployerAuthsByAgentCode(agentCode: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[EmployerAuths]] =
    findByKey[EmployerAuths](EmployerAuths.uniqueKey(agentCode), planetId).map(_.headOption)

  def delete(agentCode: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeRecord <- getEmployerAuthsByAgentCode(agentCode, planetId)
      result <- maybeRecord.flatMap(_.id) match {
                 case Some(recordId) => recordsRepository.remove(recordId, planetId)
                 case None           => Future.successful(())
               }
    } yield result
}
