package uk.gov.hmrc.agentsexternalstubs.services
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.{AgencyDetails, Individual, Organisation}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models.{User, _}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserToRecordsSyncService @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  BusinessPartnerRecordsService: BusinessPartnerRecordsService,
  knownFactsRepository: KnownFactsRepository,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  employerAuthsRecordsService: EmployerAuthsRecordsService) {

  type SaveRecordId = String => Future[Unit]
  type UserRecordsSync = SaveRecordId => PartialFunction[User, Future[Unit]]

  final val userRecordsSyncOperations: Seq[UserRecordsSync] = Seq(
    Sync.businessDetailsForMtdItIndividual,
    Sync.vatCustomerInformationForMtdVatIndividual,
    Sync.vatCustomerInformationForMtdVatOrganisation,
    Sync.vatCustomerInformationForMtdVatAgent,
    Sync.businessPartnerRecordForAnAgent,
    Sync.legacySaAgentRecord,
    Sync.businessPartnerRecordForIRCTOrganisation,
    Sync.legacyPayeAgentInformation
  )

  final val syncUserToRecords: SaveRecordId => User => Future[Unit] = saveRecordId => { user: User =>
    Future
      .sequence(
        userRecordsSyncOperations
          .map(f => if (f(saveRecordId).isDefinedAt(user)) f(saveRecordId)(user) else Future.successful(())))
      .map(_.reduce((_, _) => ()))
  }

  final val syncAfterUserRemoved: User => Future[Unit] =
    user => Future.successful(())

  private object Sync {

    implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

    private val dateFormatddMMyy = DateTimeFormatter.ofPattern("dd/MM/yy")
    private val dateFormatyyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    final val MtdItIndividualMatch = User.Matches(_ == User.AG.Individual, "HMRC-MTD-IT")

    val businessDetailsForMtdItIndividual: UserRecordsSync = saveRecordId => {
      case MtdItIndividualMatch(user, mtdbsa) => {
        val nino = user.nino
          .map(_.value.replace(" ", ""))
          .getOrElse(Generator.ninoNoSpaces(user.userId).value)

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

        Future {
          BusinessDetailsRecord
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
        }.flatMap(record =>
          Future
            .sequence(Seq(
              businessDetailsRecordsService.getBusinessDetails(Nino(nino), user.planetId.get),
              businessDetailsRecordsService.getBusinessDetails(MtdItId(mtdbsa), user.planetId.get)
            ))
            .map(_.collect { case Some(x) => x })
            .map(_.toList match {
              case r1 :: r2 :: Nil if r1.id == r2.id => record.withId(r2.id)
              case r :: Nil                          => record.withId(r.id)
              case Nil                               => record
            })
            .flatMap(entity =>
              businessDetailsRecordsService
                .store(entity, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)))
      }
    }

    final val MtdVatIndividualMatch = User.Matches(_ == User.AG.Individual, "HMRC-MTD-VAT")

    val vatCustomerInformationForMtdVatIndividual: UserRecordsSync = saveRecordId => {
      case MtdVatIndividualMatch(user, vrn) => {
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

            Future {
              VatCustomerInformationRecord
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
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId))
          })
      }
    }

    final val MtdVatOrganisationMatch = User.Matches(_ == User.AG.Organisation, "HMRC-MTD-VAT")

    val vatCustomerInformationForMtdVatOrganisation: UserRecordsSync = saveRecordId => {
      case MtdVatOrganisationMatch(user, vrn) => {
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

            Future {
              VatCustomerInformationRecord
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
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId))
          })
      }
    }

    final val MtdVatAgentMatch = User.Matches(_ == User.AG.Agent, "HMCE-VAT-AGNT")

    val vatCustomerInformationForMtdVatAgent: UserRecordsSync = saveRecordId => {
      case MtdVatAgentMatch(user, vrn) => {
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMCE-VAT-AGNT", "AgentRefNo" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("IREFFREGDATE")
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

            Future {
              VatCustomerInformationRecord
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
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
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
        Future {
          BusinessPartnerRecord
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
        }.flatMap(record => {
          val utr = Generator.utr(user.userId)
          val crn = Generator.crn(user.userId)
          user
            .findIdentifierValue("HMRC-AS-AGENT", "AgentReferenceNumber") match {
            case Some(arn) =>
              knownFactsRepository
                .findByEnrolmentKey(
                  EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> arn),
                  user.planetId.get)
                .map(_.flatMap(_.getVerifierValue("AgencyPostcode")))
                .flatMap(postcodeOpt => {
                  val ar = record
                    .withAgentReferenceNumber(Option(arn))
                    .withCrn(Option(crn))
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
                    .store(ar, autoFill = false, user.planetId.get)
                    .flatMap(saveRecordId)
                })

            case None if user.principalEnrolments.isEmpty =>
              val ar = record
                .withUtr(Option(utr))
                .withCrn(Option(crn))
                .withIsAnAgent(false)
                .withIsAnASAgent(false)
              BusinessPartnerRecordsService
                .store(ar, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)

            case _ =>
              Future.successful(())
          }
        })
      }
    }

    final val IRCTOrganisationMatch = User.Matches(_ == User.AG.Organisation, "IR-CT")

    val businessPartnerRecordForIRCTOrganisation: UserRecordsSync = saveRecordId => {
      case IRCTOrganisationMatch(user, utr) => {
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("IR-CT", "UTR" -> utr), user.planetId.get)
          .map(_.flatMap(_.getVerifierValue("Postcode")))
          .flatMap(postcode => {
            Future {
              BusinessPartnerRecord
                .generate(user.userId)
                .withEori(None)
                .withUtr(Some(utr))
                .modifyAddressDetails {
                  case address: BusinessPartnerRecord.UkAddress =>
                    address.withPostalCode(postcode.getOrElse(address.postalCode))
                  case address: BusinessPartnerRecord.ForeignAddress =>
                    address.withPostalCode(postcode.orElse(address.postalCode))
                }
                .withBusinessPartnerExists(true)
                .withIsAnOrganisation(true)
                .withIsAnIndividual(false)
                .withIsAnAgent(false)
                .withIsAnASAgent(false)
                .withOrganisation(Some(
                  Organisation
                    .generate(user.userId)
                ))
            }.flatMap(record =>
              BusinessPartnerRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId))
          })
      }
    }

    final val SaAgentMatch = User.Matches(_ == User.AG.Agent, "IR-SA-AGENT")

    val legacySaAgentRecord: UserRecordsSync = saveRecordId => {
      case SaAgentMatch(user, saAgentRef) =>
        val agentRecord = LegacyAgentRecord(
          agentId = saAgentRef,
          govAgentId = user.agentId,
          agentName = user.agentFriendlyName.orElse(user.name).getOrElse("John Doe"),
          address1 = user.address.flatMap(_.line1).getOrElse(Generator.address(user.userId).street),
          address2 = user.address.flatMap(_.line2).getOrElse(Generator.address(user.userId).town),
          address3 = user.address.flatMap(_.line3),
          address4 = user.address
            .flatMap(_.line4)
            .orElse(user.address.flatMap(_.countryCode).flatMap { case "GB" => None; case x => Some(x) }),
          postcode = user.address.flatMap(_.postcode),
          isRegisteredAgent = Some(true),
          isAgentAbroad = !user.address.exists(_.countryCode.contains("GB"))
        )
        for {
          entity <- legacyRelationshipRecordsService
                     .getLegacyAgentByAgentId(saAgentRef, user.planetId.get)
                     .map {
                       case Some(relationship) => agentRecord.withId(relationship.id)
                       case None               => agentRecord
                     }
          id <- legacyRelationshipRecordsService.store(entity, autoFill = false, user.planetId.get)
          _  <- saveRecordId(id)
          _ <- Future.sequence(
                user.delegatedEnrolments
                  .filter(_.key == "IR-SA")
                  .map(_.identifierValueOf("UTR"))
                  .map {
                    case Some(utr) =>
                      legacyRelationshipRecordsService
                        .getLegacyRelationshipByAgentIdAndUtr(saAgentRef, utr, user.planetId.get)
                        .flatMap {
                          case Some(relationship) =>
                            relationship.id.map(saveRecordId).getOrElse(Future.successful(()))
                          case None =>
                            val relationship = LegacyRelationshipRecord(
                              agentId = saAgentRef,
                              utr = Some(utr),
                              `Auth_64-8` = Some(true),
                              `Auth_i64-8` = Some(true)
                            )
                            legacyRelationshipRecordsService
                              .store(relationship, autoFill = false, user.planetId.get)
                              .flatMap(saveRecordId)
                        }
                    case None => Future.successful(())
                  })
        } yield ()
    }

    final val PayeAgentMatch = User.Matches(_ == User.AG.Agent, "IR-PAYE-AGENT")

    val legacyPayeAgentInformation: UserRecordsSync = saveRecordId => {
      case PayeAgentMatch(user, agentReference) =>
        val delegatedEmpRefs: Set[EmployerAuths.EmpAuth.EmpRef] = user
          .findDelegatedIdentifierValues("IR-PAYE")
          .map(s => EmployerAuths.EmpAuth.EmpRef(s(0), s(1)))
          .toSet
        user.agentCode match {
          case None => Future.successful(())
          case Some(agentCode) =>
            for {
              recordOpt <- employerAuthsRecordsService.getEmployerAuthsByAgentCode(agentCode, user.planetId.get)
              _ <- recordOpt match {
                    case Some(existing) =>
                      val recordEmpRefs: Set[EmployerAuths.EmpAuth.EmpRef] = existing.empAuthList.map(_.empRef).toSet
                      val empAuthsToAdd = (delegatedEmpRefs -- recordEmpRefs)
                        .map(
                          empRef =>
                            EmployerAuths.EmpAuth(
                              empRef = empRef,
                              aoRef = EmployerAuths.EmpAuth.AoRef(empRef.districtNumber, "0", "0", empRef.reference),
                              true,
                              true,
                              agentClientRef = Some(agentReference)))
                        .toSeq
                      val record = existing.copy(empAuthList = existing.empAuthList ++ empAuthsToAdd)
                      if (record.empAuthList.nonEmpty)
                        employerAuthsRecordsService
                          .store(record, false, user.planetId.get)
                          .flatMap(saveRecordId)
                      else
                        employerAuthsRecordsService
                          .delete(agentCode, user.planetId.get)
                          .flatMap(_ => saveRecordId("--" + record.id.get))
                    case None =>
                      val empAuthList = delegatedEmpRefs
                        .map(
                          empRef =>
                            EmployerAuths.EmpAuth(
                              empRef = empRef,
                              aoRef = EmployerAuths.EmpAuth.AoRef(empRef.districtNumber, "0", "0", empRef.reference),
                              true,
                              true,
                              agentClientRef = Some(agentReference)))
                        .toSeq
                      val record = EmployerAuths(agentCode = agentCode, empAuthList = empAuthList)
                      if (record.empAuthList.nonEmpty)
                        employerAuthsRecordsService
                          .store(record, false, user.planetId.get)
                          .flatMap(saveRecordId)
                      else Future.successful("")
                  }
            } yield ()
        }
    }
  }

}
