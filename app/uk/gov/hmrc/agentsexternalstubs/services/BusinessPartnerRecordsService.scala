package uk.gov.hmrc.agentsexternalstubs.services

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessPartnerRecordsService @Inject()(
  val recordsRepository: RecordsRepository,
  externalUserService: ExternalUserService,
  usersServiceProvider: Provider[UsersService])
    extends RecordsService {

  def store(record: BusinessPartnerRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] = {
    val entity = if (autoFill) BusinessPartnerRecord.sanitize(record.safeId)(record) else record
    BusinessPartnerRecord
      .validate(entity)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          recordsRepository.store(entity, planetId)
        }
      )
  }

  def getBusinessPartnerRecord(arn: Arn, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.agentReferenceNumberKey(arn.value), planetId)
      .map(_.headOption)

  def getBusinessPartnerRecord(utr: Utr, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    externalUserService.tryLookupExternalUserIfMissing(utr, planetId, usersServiceProvider.get.createUser(_, _))(id =>
      findByKey[BusinessPartnerRecord](BusinessPartnerRecord.utrKey(id.value), planetId).map(_.headOption))

  def getBusinessPartnerRecord(nino: Nino, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    externalUserService.tryLookupExternalUserIfMissing(nino, planetId, usersServiceProvider.get.createUser(_, _))(id =>
      findByKey[BusinessPartnerRecord](BusinessPartnerRecord.ninoKey(id.value), planetId).map(_.headOption))

  def getBusinessPartnerRecordByEori(eori: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.eoriKey(eori), planetId).map(_.headOption)

  def getBusinessPartnerRecordByEoriOrUtr(eori: String, utr: String, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    externalUserService.tryLookupExternalUserIfMissing(Utr(utr), planetId, usersServiceProvider.get.createUser(_, _))(
      n =>
        findByKeys[BusinessPartnerRecord](
          Seq(BusinessPartnerRecord.eoriKey(eori), BusinessPartnerRecord.utrKey(utr)),
          planetId).map(_.headOption))

}
