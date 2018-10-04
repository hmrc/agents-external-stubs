package uk.gov.hmrc.agentsexternalstubs.services
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.{AgencyDetails, Individual}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserToRecordsSyncService @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  BusinessPartnerRecordsService: BusinessPartnerRecordsService,
  knownFactsRepository: KnownFactsRepository) {

  type SaveRecordId = String => Future[Unit]
  type UserRecordsSync = SaveRecordId => PartialFunction[User, Future[Unit]]

  final val userRecordsSyncOperations: Seq[UserRecordsSync] = Seq(
    Sync.businessDetailsForMtdItIndividual,
    Sync.vatCustomerInformationForMtdVatIndividual,
    Sync.vatCustomerInformationForMtdVatOrganisation,
    Sync.businessPartnerRecordForAnAgent
  )

  final val syncUserToRecords: SaveRecordId => Option[User] => Future[Unit] = saveRecordId => {
    case Some(user) if user.isNonCompliant.forall(_ == false) =>
      Future
        .sequence(
          userRecordsSyncOperations
            .map(f => if (f(saveRecordId).isDefinedAt(user)) f(saveRecordId)(user) else Future.successful(())))
        .map(_.reduce((_, _) => ()))
    case _ => Future.successful(())
  }

  final val syncAfterUserRemoved: User => Future[Unit] =
    user => Future.successful(())

  private object Sync {

    implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

    private val dateFormatddMMyy = DateTimeFormatter.ofPattern("dd/MM/yy")
    private val dateFormatyyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val businessDetailsForMtdItIndividual: UserRecordsSync = saveRecordId => {
      case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-IT") => {
        val nino = user.nino
          .map(_.value.replace(" ", ""))
          .getOrElse(Generator.ninoNoSpaces(user.userId).value)
        val mtdbsa = user
          .findIdentifierValue("HMRC-MTD-IT", "MTDITID")
          .getOrElse(Generator.mtdbsa(user.userId).value)

        val address = user.address
          .map(
            a =>
              if (a.isUKAddress)
                BusinessDetailsRecord.UkAddress(
                  addressLine1 = a.line1.getOrElse("1 Kingdom Road"),
                  addressLine2 = a.line2,
                  addressLine3 = a.line3,
                  addressLine4 = a.line4,
                  postalCode = a.postcode.getOrElse(""),
                  countryCode = a.countryCode.getOrElse("GB")
                )
              else
                BusinessDetailsRecord.ForeignAddress(
                  addressLine1 = a.line1.getOrElse("2 Foreign Road"),
                  addressLine2 = a.line2,
                  addressLine3 = a.line3,
                  addressLine4 = a.line4,
                  postalCode = a.postcode,
                  countryCode = a.countryCode.getOrElse("IE")
              ))
          .getOrElse(BusinessDetailsRecord.UkAddress.generate(user.userId))

        val record = BusinessDetailsRecord
          .generate(user.userId)
          .withNino(nino)
          .withMtdbsa(mtdbsa)
          .modifyBusinessData {
            case Some(businessData :: _) =>
              Some(
                Seq(
                  businessData
                    .withCessationDate(None)
                    .withCessationReason(None)
                    .withBusinessAddressDetails(Some(address))
                ))
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
                .flatMap(saveRecordId))

      }
    }

    val vatCustomerInformationForMtdVatIndividual: UserRecordsSync = saveRecordId => {
      case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") => {
        val vrn = user
          .findIdentifierValue("HMRC-MTD-VAT", "VRN")
          .getOrElse(throw new IllegalStateException("Expected individual having VRN identifier."))
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("VATRegistrationDate")
                .map(LocalDate.parse(_, dateFormatddMMyy))
                .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)))
          .flatMap(vatRegistrationDateOpt => {

            val address = user.address
              .map(
                a =>
                  if (a.isUKAddress)
                    VatCustomerInformationRecord.UkAddress(
                      line1 = a.line1.getOrElse("1 Kingdom Road"),
                      line2 = a.line2.getOrElse("Brighton"),
                      line3 = a.line3,
                      line4 = a.line4,
                      postCode = a.postcode.getOrElse(""),
                      countryCode = a.countryCode.getOrElse("GB")
                    )
                  else
                    VatCustomerInformationRecord.ForeignAddress(
                      line1 = a.line1.getOrElse("2 Foreign Road"),
                      line2 = a.line2.getOrElse("Cork"),
                      line3 = a.line3,
                      line4 = a.line4,
                      postCode = a.postcode,
                      countryCode = a.countryCode.getOrElse("IE")
                  ))
              .getOrElse(VatCustomerInformationRecord.UkAddress.generate(user.userId))

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
                        .withOrganisationName(None)
                        .withIndividual(Some(IndividualName()
                          .withFirstName(user.firstName)
                          .withLastName(user.lastName)))
                        .withDateOfBirth(user.dateOfBirth.map(_.toString("yyyy-MM-dd")))
                        .modifyEffectiveRegistrationDate {
                          case date => vatRegistrationDateOpt.map(_.format(dateFormatyyyyMMdd)).orElse(date)
                        }
                    )
                    .modifyPPOB {
                      case ppob => ppob.withAddress(address)
                    }
                    .withDeregistration(None)
                ))
            for {
              entity <- vatCustomerInformationRecordsService.getCustomerInformation(vrn, user.planetId.get).map {
                         case Some(existing) => record.withId(existing.id)
                         case None           => record
                       }
              recordId <- vatCustomerInformationRecordsService.store(entity, autoFill = false, user.planetId.get)
              _        <- saveRecordId(recordId)
            } yield ()
          })
      }
    }

    val vatCustomerInformationForMtdVatOrganisation: UserRecordsSync = saveRecordId => {
      case User.Organisation(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-VAT") => {
        val vrn = user
          .findIdentifierValue("HMRC-MTD-VAT", "VRN")
          .getOrElse(throw new IllegalStateException("Expected individual having VRN identifier."))
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("VATRegistrationDate")
                .map(LocalDate.parse(_, dateFormatddMMyy))
                .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)))
          .flatMap(vatRegistrationDateOpt => {

            val address = user.address
              .map(
                a =>
                  if (a.isUKAddress)
                    VatCustomerInformationRecord.UkAddress(
                      line1 = a.line1.getOrElse("1 Kingdom Road"),
                      line2 = a.line2.getOrElse("Brighton"),
                      line3 = a.line3,
                      line4 = a.line4,
                      postCode = a.postcode.getOrElse(""),
                      countryCode = a.countryCode.getOrElse("GB")
                    )
                  else
                    VatCustomerInformationRecord.ForeignAddress(
                      line1 = a.line1.getOrElse("2 Foreign Road"),
                      line2 = a.line2.getOrElse("Cork"),
                      line3 = a.line3,
                      line4 = a.line4,
                      postCode = a.postcode,
                      countryCode = a.countryCode.getOrElse("IE")
                  ))
              .getOrElse(VatCustomerInformationRecord.UkAddress.generate(user.userId))

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
                        .withIndividual(None)
                        .withDateOfBirth(None)
                        .withOrganisationName(user.name)
                        .modifyEffectiveRegistrationDate {
                          case date => vatRegistrationDateOpt.map(_.format(dateFormatyyyyMMdd)).orElse(date)
                        }
                    )
                    .modifyPPOB {
                      case ppob => ppob.withAddress(address)
                    }
                    .withDeregistration(None)
                ))
            vatCustomerInformationRecordsService
              .getCustomerInformation(vrn, user.planetId.get)
              .map {
                case Some(existing) => record.withId(existing.id)
                case None           => record
              }
              .flatMap(
                entity =>
                  vatCustomerInformationRecordsService
                    .store(entity, autoFill = false, user.planetId.get)
                    .flatMap(saveRecordId))
          })
      }
    }

    val businessPartnerRecordForAnAgent: UserRecordsSync = saveRecordId => {
      case User.Agent(user) => {
        val address = user.address
          .map(
            a =>
              if (a.isUKAddress)
                BusinessPartnerRecord.UkAddress(
                  addressLine1 = a.line1.getOrElse("1 Kingdom Road"),
                  addressLine2 = a.line2,
                  addressLine3 = a.line3,
                  addressLine4 = a.line4,
                  postalCode = a.postcode.getOrElse(""),
                  countryCode = a.countryCode.getOrElse("GB")
                )
              else
                BusinessPartnerRecord.ForeignAddress(
                  addressLine1 = a.line1.getOrElse("2 Foreign Road"),
                  addressLine2 = a.line2,
                  addressLine3 = a.line3,
                  addressLine4 = a.line4,
                  postalCode = a.postcode,
                  countryCode = a.countryCode.getOrElse("IE")
              ))
          .getOrElse(BusinessPartnerRecord.UkAddress.generate(user.userId))
        val record = BusinessPartnerRecord
          .generate(user.userId)
          .withBusinessPartnerExists(true)
          .withIsAnOrganisation(false)
          .withIsAnIndividual(true)
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
                .withAgencyAddress(Some(address))
                .withAgencyName(user.agentFriendlyName.map(_.take(40)))))
          .withAddressDetails(address)
        val utr = Generator.utr(user.userId)
        user
          .findIdentifierValue("HMRC-AS-AGENT", "AgentReferenceNumber") match {
          case Some(arn) =>
            knownFactsRepository
              .findByEnrolmentKey(EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> arn), user.planetId.get)
              .map(_.flatMap(_.getVerifierValue("AgencyPostcode")))
              .flatMap(postcodeOpt => {
                val ar = record
                  .withAgentReferenceNumber(Option(arn))
                  .withIsAnAgent(true)
                  .withIsAnASAgent(true)
                  .modifyAgencyDetails {
                    case Some(ad) =>
                      Some(ad.modifyAgencyAddress {
                        case Some(aa: BusinessPartnerRecord.UkAddress) =>
                          Some(aa.modifyPostalCode { case pc => postcodeOpt.getOrElse(pc) })
                      })
                  }
                  .modifyAddressDetails {
                    case ad: BusinessPartnerRecord.UkAddress =>
                      ad.modifyPostalCode { case pc => postcodeOpt.getOrElse(pc) }
                  }
                BusinessPartnerRecordsService
                  .getBusinessPartnerRecord(Arn(arn), user.planetId.get)
                  .map {
                    case Some(existingRecord) => ar.withId(existingRecord.id)
                    case None                 => ar
                  }
                  .flatMap(
                    entity =>
                      BusinessPartnerRecordsService
                        .store(entity, autoFill = false, user.planetId.get)
                        .flatMap(saveRecordId))
              })

          case None if user.principalEnrolments.isEmpty =>
            val ar = record
              .withUtr(Option(utr))
              .withIsAnAgent(false)
              .withIsAnASAgent(false)
            BusinessPartnerRecordsService
              .getBusinessPartnerRecord(Utr(utr), user.planetId.get)
              .map {
                case Some(existingRecord) => ar.withId(existingRecord.id)
                case None                 => ar
              }
              .flatMap(
                entity =>
                  BusinessPartnerRecordsService
                    .store(entity, autoFill = false, user.planetId.get)
                    .flatMap(saveRecordId))

          case _ =>
            Future.successful(())
        }
      }
    }

  }

}
