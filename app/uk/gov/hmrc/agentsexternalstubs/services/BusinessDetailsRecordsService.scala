package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsRecordsService @Inject()(recordsRepository: RecordsRepository) {

  def store(record: BusinessDetailsRecord, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    BusinessDetailsRecord
      .validate(record)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => recordsRepository.store(record, planetId)
      )

  def getBusinessDetails(nino: Nino, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessDetailsRecord]] =
    findByKey(BusinessDetailsRecord.ninoKey(nino.value), planetId).map(_.headOption)

  def getBusinessDetails(mtdbsa: MtdItId, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessDetailsRecord]] =
    findByKey(BusinessDetailsRecord.mtdbsaKey(mtdbsa.value), planetId).map(_.headOption)

  private def findByKey(key: String, planetId: String)(
    implicit ec: ExecutionContext): Future[List[BusinessDetailsRecord]] =
    recordsRepository.cursor[BusinessDetailsRecord](key, planetId).collect[List](1000)

}
