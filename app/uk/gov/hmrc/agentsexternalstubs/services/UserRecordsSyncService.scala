package uk.gov.hmrc.agentsexternalstubs.services
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessDetailsRecord, User, VatCustomerInformationRecord}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class UserRecordsSyncService @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService) {

  type UserRecordsSync = PartialFunction[User, Future[Unit]]

  val businessDetailsForMtdItIndividual: UserRecordsSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-IT") =>
      businessDetailsRecordsService.store(
        BusinessDetailsRecord
          .seed(user.userId)
          .withNino(
            user.nino
              .map(_.value.replace(" ", ""))
              .getOrElse(throw new IllegalStateException("Expected user with NINO.")))
          .withMtdbsa(
            user
              .findIdentifierValue("HMRC-MTD-IT", "MTDITID")
              .getOrElse(throw new IllegalStateException("Expected user with MTDITID identifier."))),
        autoFill = true,
        user.planetId.get
      )
  }

  val vatCustomerInformationForMtdVatIndividual: UserRecordsSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") =>
      vatCustomerInformationRecordsService.store(
        VatCustomerInformationRecord
          .seed(user.userId)
          .withVrn(
            user
              .findIdentifierValue("HMRC-MTD-VAT", "VRN")
              .getOrElse(throw new IllegalStateException("Expected user with VRN identifier.")))
          .withApprovedInformation(
            Some(
              ApprovedInformation
                .seed(user.userId)
                .withCustomerDetails(
                  CustomerDetails
                    .seed(user.userId)
                    .withIndividual(Some(IndividualName()
                      .withFirstName(user.name.map(_.split(" ").dropRight(1).mkString(" ")))
                      .withLastName(user.name.map(_.split(" ").last))))
                    .withDateOfBirth(user.dateOfBirth.map(_.toString("yyyy-MM-dd")))
                ))),
        autoFill = true,
        user.planetId.get
      )
  }

  val userRecordsSyncOperations: Seq[UserRecordsSync] = Seq(
    businessDetailsForMtdItIndividual,
    vatCustomerInformationForMtdVatIndividual
  )

  final val syncUserToRecords: Option[User] => Future[Unit] = {
    case None => Future.successful(())
    case Some(user) =>
      Future
        .sequence(
          userRecordsSyncOperations
            .map(f => if (f.isDefinedAt(user)) f(user) else Future.successful(())))
        .map(_.reduce((_, _) => ()))
  }

  final val syncAfterUserRemoved: User => Future[Unit] =
    user => Future.successful(())

}
