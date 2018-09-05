package uk.gov.hmrc.agentsexternalstubs.services
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.AgentRecord.{AgencyDetails, Individual, Organisation}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class UserRecordsSyncService @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  agentRecordsService: AgentRecordsService) {

  type UserRecordsSync = PartialFunction[User, Future[Unit]]

  val businessDetailsForMtdItIndividual: UserRecordsSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-IT") =>
      businessDetailsRecordsService.store(
        BusinessDetailsRecord
          .seed(user.userId)
          .withNino(
            user.nino
              .map(_.value.replace(" ", ""))
              .getOrElse(throw new IllegalStateException("Expected individual having NINO.")))
          .withMtdbsa(
            user
              .findIdentifierValue("HMRC-MTD-IT", "MTDITID")
              .getOrElse(throw new IllegalStateException("Expected individual having MTDITID identifier."))),
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
              .getOrElse(throw new IllegalStateException("Expected individual having VRN identifier.")))
          .withApprovedInformation(
            Some(
              ApprovedInformation
                .seed(user.userId)
                .withCustomerDetails(
                  CustomerDetails
                    .seed(user.userId)
                    .withIndividual(Some(IndividualName()
                      .withFirstName(user.firstName)
                      .withLastName(user.lastName)))
                    .withDateOfBirth(user.dateOfBirth.map(_.toString("yyyy-MM-dd")))
                ))),
        autoFill = true,
        user.planetId.get
      )
  }

  val vatCustomerInformationForMtdVatOrganisation: UserRecordsSync = {
    case User.Organisation(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") =>
      vatCustomerInformationRecordsService.store(
        VatCustomerInformationRecord
          .seed(user.userId)
          .withVrn(
            user
              .findIdentifierValue("HMRC-MTD-VAT", "VRN")
              .getOrElse(throw new IllegalStateException("Expected organisation having VRN identifier.")))
          .withApprovedInformation(
            Some(
              ApprovedInformation
                .seed(user.userId)
                .withCustomerDetails(
                  CustomerDetails
                    .seed(user.userId)
                    .withOrganisationName(user.name)
                ))),
        autoFill = true,
        user.planetId.get
      )
  }

  val agentRecordForHmrcAsAgent: UserRecordsSync = {
    case User.Agent(user) if user.principalEnrolments.exists(_.key == "HMRC-AS-AGENT") =>
      agentRecordsService.store(
        AgentRecord
          .seed(user.userId)
          .withAgentReferenceNumber(
            user
              .findIdentifierValue("HMRC-AS-AGENT", "AgentReferenceNumber")
              .orElse(throw new IllegalStateException("Expected agent having ARN identifier.")))
          .withIndividual(
            Some(
              Individual
                .seed(user.userId)
                .withFirstName(user.firstName.getOrElse("John"))
                .withLastName(user.lastName.getOrElse("Smith"))
            ))
          .withAgencyDetails(
            Some(
              AgencyDetails
                .seed(user.userId)
                .withAgencyName(user.agentFriendlyName))),
        autoFill = true,
        user.planetId.get
      )
  }

  val userRecordsSyncOperations: Seq[UserRecordsSync] = Seq(
    businessDetailsForMtdItIndividual,
    vatCustomerInformationForMtdVatIndividual,
    vatCustomerInformationForMtdVatOrganisation,
    agentRecordForHmrcAsAgent
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
