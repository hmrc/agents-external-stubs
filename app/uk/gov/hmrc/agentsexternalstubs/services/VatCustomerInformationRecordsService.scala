package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatCustomerInformationRecordsService @Inject()(val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: VatCustomerInformationRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] = {
    val entity = if (autoFill) VatCustomerInformationRecord.sanitize(record.vrn)(record) else record
    VatCustomerInformationRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          recordsRepository.store(entity, planetId)
        }
      )
  }

  def getCustomerInformation(vrn: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[VatCustomerInformationRecord]] =
    findByKey[VatCustomerInformationRecord](VatCustomerInformationRecord.uniqueKey(vrn), planetId).map(_.headOption)
}
