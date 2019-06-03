package uk.gov.hmrc.agentsexternalstubs.services

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{VatCustomerInformationRecord, VatKnownFacts}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatCustomerInformationRecordsService @Inject()(
  val recordsRepository: RecordsRepository,
  externalUserService: ExternalUserService,
  usersServiceProvider: Provider[UsersService])
    extends RecordsService {

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
    externalUserService
      .tryLookupExternalUserIfMissingForIdentifier(Vrn(vrn), planetId, usersServiceProvider.get.createUser(_, _))(
        id =>
          findByKey[VatCustomerInformationRecord](VatCustomerInformationRecord.uniqueKey(id.value), planetId)
            .map(_.headOption))

  def getVatKnownFacts(vrn: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[VatKnownFacts]] =
    getCustomerInformation(vrn, planetId).map(record =>
      VatKnownFacts.fromVatCustomerInformationRecord(record.get).orElse(None))
}
