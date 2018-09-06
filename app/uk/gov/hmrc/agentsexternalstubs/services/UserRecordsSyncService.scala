package uk.gov.hmrc.agentsexternalstubs.services
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.AgentRecord.{AddressDetails, AgencyDetails, Individual, UkAddress}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserRecordsSyncService @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  agentRecordsService: AgentRecordsService) {

  type UserRecordsSync = PartialFunction[User, Future[Unit]]

  implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

  val businessDetailsForMtdItIndividual: UserRecordsSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-IT") => {
      val nino = user.nino.map(_.value.replace(" ", "")).getOrElse(Generator.ninoNoSpaces(user.userId).value)
      val mtdbsa = user
        .findIdentifierValue("HMRC-MTD-IT", "MTDITID")
        .getOrElse(Generator.mtdbsa(user.userId).value)
      val record = BusinessDetailsRecord
        .generate(user.userId)
        .withNino(nino)
        .withMtdbsa(mtdbsa)
        .modifyBusinessData {
          case Some(businessData :: _) => Some(Seq(businessData.withCessationDate(None).withCessationReason(None)))
        }
      val maybeRecord1 = businessDetailsRecordsService.getBusinessDetails(Nino(nino), user.planetId.get)
      val maybeRecord2 = businessDetailsRecordsService.getBusinessDetails(MtdItId(mtdbsa), user.planetId.get)
      Future
        .sequence(Seq(maybeRecord1, maybeRecord2))
        .map(_.collect { case Some(x) => x })
        .map(_.toList match {
          case r1 :: r2 :: Nil if r1.id == r2.id => record.withId(r2.id)
          case r :: Nil                          => record.withId(r.id)
          case Nil                               => record
        })
        .flatMap(
          entity =>
            businessDetailsRecordsService
              .store(entity, autoFill = false, user.planetId.get)
              .map(_ => ()))
    }
  }

  val vatCustomerInformationForMtdVatIndividual: UserRecordsSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") => {
      val vrn = user
        .findIdentifierValue("HMRC-MTD-VAT", "VRN")
        .getOrElse(throw new IllegalStateException("Expected individual having VRN identifier."))
      val record = VatCustomerInformationRecord
        .generate(user.userId)
        .withVrn(vrn)
        .withApprovedInformation(
          Some(
            ApprovedInformation
              .generate(user.userId)
              .withCustomerDetails(
                CustomerDetails
                  .generate(user.userId)
                  .withIndividual(Some(IndividualName()
                    .withFirstName(user.firstName)
                    .withLastName(user.lastName)))
                  .withDateOfBirth(user.dateOfBirth.map(_.toString("yyyy-MM-dd")))
              )
              .withDeregistration(None)
          ))
      vatCustomerInformationRecordsService
        .getCustomerInformation(vrn, user.planetId.get)
        .map {
          case Some(existing) => record.withId(existing.id)
          case None           => record
        }
        .flatMap(entity =>
          vatCustomerInformationRecordsService.store(entity, autoFill = false, user.planetId.get).map(_ => ()))
    }
  }

  val vatCustomerInformationForMtdVatOrganisation: UserRecordsSync = {
    case User.Organisation(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") => {
      val vrn = user
        .findIdentifierValue("HMRC-MTD-VAT", "VRN")
        .getOrElse(throw new IllegalStateException("Expected individual having VRN identifier."))
      val record = VatCustomerInformationRecord
        .generate(user.userId)
        .withVrn(vrn)
        .withApprovedInformation(
          Some(
            ApprovedInformation
              .generate(user.userId)
              .withCustomerDetails(
                CustomerDetails
                  .generate(user.userId)
                  .withOrganisationName(user.name)
              )
              .withDeregistration(None)
          ))
      vatCustomerInformationRecordsService
        .getCustomerInformation(vrn, user.planetId.get)
        .map {
          case Some(existing) => record.withId(existing.id)
          case None           => record
        }
        .flatMap(entity =>
          vatCustomerInformationRecordsService.store(entity, autoFill = false, user.planetId.get).map(_ => ()))
    }
  }

  val recordForAnAgent: UserRecordsSync = {
    case User.Agent(user) => {
      val agentRecord = AgentRecord
        .generate(user.userId)
        .withBusinessPartnerExists(true)
        .withIndividual(
          Some(
            Individual
              .generate(user.userId)
              .withFirstName(user.firstName.getOrElse("John"))
              .withLastName(user.lastName.getOrElse("Smith"))
          ))
        .withAgencyDetails(
          Some(
            AgencyDetails
              .generate(user.userId)
              .withAgencyName(user.agentFriendlyName.map(_.take(40)))))

      val utr = Generator.utr(user.userId)
      val address = UkAddress.generate(user.userId)
      user
        .findIdentifierValue("HMRC-AS-AGENT", "AgentReferenceNumber") match {
        case Some(arn) =>
          val ar = agentRecord
            .withAgentReferenceNumber(Option(arn))
            .withIsAnAgent(true)
            .withIsAnASAgent(true)
            .withAgencyDetails(
              Some(
                AgencyDetails
                  .generate(user.userId)
                  .withAgencyAddress(Some(address))
                  .withAgencyName(user.agentFriendlyName)))
            .withAddressDetails(address)
          agentRecordsService
            .getAgentRecord(Arn(arn), user.planetId.get)
            .map {
              case Some(existingRecord) => ar.withId(existingRecord.id)
              case None                 => ar
            }
            .flatMap(entity => agentRecordsService.store(entity, autoFill = false, user.planetId.get).map(_ => ()))
        case None if user.principalEnrolments.isEmpty =>
          val ar = agentRecord
            .withUtr(Option(utr))
            .withIsAnAgent(false)
            .withIsAnASAgent(false)
          agentRecordsService
            .getAgentRecord(Utr(utr), user.planetId.get)
            .map {
              case Some(existingRecord) => ar.withId(existingRecord.id)
              case None                 => ar
            }
            .flatMap(entity => agentRecordsService.store(entity, autoFill = false, user.planetId.get).map(_ => ()))
        case _ =>
          Future.successful(())
      }
    }
  }

  val userRecordsSyncOperations: Seq[UserRecordsSync] = Seq(
    businessDetailsForMtdItIndividual,
    vatCustomerInformationForMtdVatIndividual,
    vatCustomerInformationForMtdVatOrganisation,
    recordForAnAgent
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
