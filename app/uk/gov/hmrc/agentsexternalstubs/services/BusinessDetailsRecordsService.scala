package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsRecordsService @Inject()(val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: BusinessDetailsRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] = {
    val entity = if (autoFill) BusinessDetailsRecord.sanitize(record.safeId)(record) else record
    BusinessDetailsRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          recordsRepository.store(entity, planetId)
        }
      )
  }

  def getBusinessDetails(nino: Nino, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessDetailsRecord]] =
    findByKey[BusinessDetailsRecord](BusinessDetailsRecord.ninoKey(nino.value), planetId).map(_.headOption)

  def getBusinessDetails(mtdbsa: MtdItId, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessDetailsRecord]] =
    findByKey[BusinessDetailsRecord](BusinessDetailsRecord.mtdbsaKey(mtdbsa.value), planetId).map(_.headOption)

}
