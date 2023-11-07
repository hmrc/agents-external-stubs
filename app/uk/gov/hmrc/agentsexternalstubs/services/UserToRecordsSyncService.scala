/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.services
import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId, PlrIdType, SuspensionDetails}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.{AgencyDetails, Individual, Organisation}
import uk.gov.hmrc.agentsexternalstubs.models.VatCustomerInformationRecord.{ApprovedInformation, CustomerDetails, IndividualName}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserToRecordsSyncService @Inject() (
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  BusinessPartnerRecordsService: BusinessPartnerRecordsService,
  knownFactsRepository: KnownFactsRepository,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  employerAuthsRecordsService: EmployerAuthsRecordsService,
  pptSubscriptionDisplayRecordsService: PPTSubscriptionDisplayRecordsService,
  cbcSubscriptionRecordsService: CbCSubscriptionRecordsService,
  pillar2RecordsService: Pillar2RecordsService,
  usersRepository: UsersRepository
)(implicit ec: ExecutionContext) {

  type SaveRecordId = String => Future[Unit]
  type UserAndGroupRecordsSync = SaveRecordId => PartialFunction[(User, Group), Future[Unit]]

  final val userAndGroupRecordsSyncOperations: Seq[UserAndGroupRecordsSync] = Seq(
    Sync.businessDetailsForMtdItIndividual,
    Sync.pptSubscriptionDisplayRecordForPptReference,
    Sync.vatCustomerInformationForMtdVatIndividual,
    Sync.businessDetailsForCgt,
    Sync.vatCustomerInformationForMtdVatOrganisation,
    Sync.vatCustomerInformationForMtdVatAgent,
    Sync.businessPartnerRecordForAnAgent,
    Sync.legacySaAgentRecord,
    Sync.businessPartnerRecordForIRCTOrganisation,
    Sync.legacyPayeAgentInformation,
    Sync.cbcSubscriptionRecord,
    Sync.pillar2Record
  )

  final def syncUserToRecords(saveRecordId: SaveRecordId, user: User, group: Group): Future[Unit] =
    Future
      .sequence(
        userAndGroupRecordsSyncOperations
          .map(f =>
            if (f(saveRecordId).isDefinedAt((user, group))) f(saveRecordId)((user, group))
            else Future.successful(())
          )
      )
      .map(_.reduce((_, _) => ()))

  def syncGroup(group: Group): Future[Unit] = for {
    users <- usersRepository.findByGroupId(group.groupId, group.planetId)(limit = None)
    _ <- Future.traverse(users) { user =>
           def saveRecordId(recordId: String): Future[Unit] =
             usersRepository.syncRecordId(user.userId, recordId, group.planetId).map(_ => ())
           syncUserToRecords(saveRecordId, user, group)
         }
  } yield ()

  final val syncAfterUserRemoved: User => Future[Unit] = _ => Future.successful(())

  private object Sync {

    implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

    private val dateFormatddMMyy = DateTimeFormatter.ofPattern("dd/MM/yy")
    private val dateFormatyyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // TODO could create a flexible match for x returning (group, identifier value)?
    final val MtdItIndividualMatch = Group.Matches(_ == AG.Individual, "HMRC-MTD-IT")
    final val CbcUkMatch = Group.Matches(_ == AG.Organisation, "HMRC-CBC-ORG")
    final val CbcNonUkMatch = Group.Matches(_ == AG.Organisation, "HMRC-CBC-NONUK-ORG")
    final val CgtMatch = Group.Matches(ag => ag == AG.Individual || ag == AG.Organisation, "HMRC-CGT-PD")
    final val MtdVatIndividualMatch = Group.Matches(_ == AG.Individual, "HMRC-MTD-VAT")
    final val MtdVatOrganisationMatch = Group.Matches(_ == AG.Organisation, "HMRC-MTD-VAT")
    final val IRCTOrganisationMatch = Group.Matches(_ == AG.Organisation, "IR-CT")
    final val Pillar2Match = Group.Matches(_ == AG.Organisation, "HMRC-PILLAR2-ORG")

    final val MtdVatAgentMatch = Group.Matches(_ == AG.Agent, "HMCE-VAT-AGNT")

    // these care about the group being reused
    final val PptReferenceMatch =
      Group.Matches(ag => ag == AG.Individual || ag == AG.Organisation, "HMRC-PPT-ORG")
    final val SaAgentMatch = Group.Matches(_ == AG.Agent, "IR-SA-AGENT")
    final val PayeAgentMatch = Group.Matches(_ == AG.Agent, "IR-PAYE-AGENT")

    val pillar2Record: UserAndGroupRecordsSync = saveRecordId => { case (user, Pillar2Match(_, plrId)) =>
      val pillar2Record = Pillar2Record.generate(plrId).withPlrReference(plrId)
      val identifier = Identifier(PlrIdType.id, plrId)
      val ek = EnrolmentKey(uk.gov.hmrc.agentmtdidentifiers.model.Service.Pillar2.id, Seq(identifier))

      val verifiers =
        Seq.empty // (verifiers should include registration date but I am not sure of which key to use for it.)
      val knownFacts =
        KnownFacts(ek, identifiers = Seq(identifier), verifiers = verifiers, planetId = Some(user.planetId.get))

      for {
        _ <- pillar2RecordsService.store(pillar2Record, autoFill = false, user.planetId.get).flatMap(saveRecordId)
        _ <- knownFactsRepository.upsert(knownFacts, user.planetId.get)
      } yield ()
    }

    val businessDetailsForMtdItIndividual: UserAndGroupRecordsSync = saveRecordId => {
      case (user, MtdItIndividualMatch(_, mtdbsa)) =>
        val nino = user.nino
          .map(_.value.replace(" ", ""))
          .getOrElse(Generator.ninoNoSpaces(user.userId).value)

        val address = user.address
          .map(a =>
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
              )
          )
          .getOrElse(BusinessDetailsRecord.UkAddress.generate(user.userId))

        Future {
          BusinessDetailsRecord
            .generate(user.userId)
            .withNino(nino)
            .withMtdbsa(mtdbsa)
            .modifyBusinessData { case Some(businessData :: _) =>
              Some(
                Seq(
                  businessData
                    .withCessationDate(None)
                    .withCessationReason(None)
                    .withBusinessAddressDetails(Some(address))
                )
              )
            }
        }.flatMap(record =>
          Future
            .sequence(
              Seq(
                businessDetailsRecordsService.getBusinessDetails(Nino(nino), user.planetId.get),
                businessDetailsRecordsService.getBusinessDetails(MtdItId(mtdbsa), user.planetId.get)
              )
            )
            .map(_.collect { case Some(x) => x })
            .map(_.toList match {
              case r1 :: r2 :: Nil if r1.id == r2.id => record.withId(r2.id)
              case r :: Nil                          => record.withId(r.id)
              case Nil                               => record
            })
            .flatMap(entity =>
              businessDetailsRecordsService
                .store(entity, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)
            )
        )
    }

    private def getKnownFacts(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[KnownFacts]] =
      knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId)

    val cbcSubscriptionRecord: UserAndGroupRecordsSync = saveRecordId => {
      case (user, CbcUkMatch(_, _)) =>
        val enrolmentKey = user.assignedPrincipalEnrolments.filter(_.service.contains("HMRC-CBC-ORG")).head
        val cbcId = enrolmentKey.identifiers.filter(_.key == "cbcId").map(id => id.value).head
        val cbcRecord = CbcSubscriptionRecord.generateWith(cbcId, isUK = true)

        // overrides email with value generated from record
        def updateKnownFacts(knownFacts: Option[KnownFacts]) = {
          val updatedKnownFacts = Seq(KnownFact("Email", cbcRecord.primaryContact.head.email))
          knownFactsRepository.upsert(knownFacts.get.copy(verifiers = updatedKnownFacts), user.planetId.get)
        }

        getKnownFacts(enrolmentKey, user.planetId.get) map updateKnownFacts flatMap { _ =>
          cbcSubscriptionRecordsService
            .store(cbcRecord, autoFill = false, user.planetId.get)
            .flatMap(saveRecordId)
        }

      case (user, CbcNonUkMatch(_, cbcId)) =>
        val enrolmentKey = user.assignedPrincipalEnrolments.filter(_.service.contains("HMRC-CBC-NONUK-ORG")).head
        val cbcRecord = CbcSubscriptionRecord.generateWith(cbcId, isUK = false)

        // overrides email with value generated from record
        def updateKnownFacts(knownFacts: Option[KnownFacts]) = {
          val updatedKnownFacts = knownFacts.get.verifiers.filterNot(_.key == "Email") ++ Seq(
            KnownFact("Email", cbcRecord.primaryContact.head.email)
          )
          knownFactsRepository.upsert(knownFacts.get.copy(verifiers = updatedKnownFacts), user.planetId.get)
        }

        getKnownFacts(enrolmentKey, user.planetId.get) map updateKnownFacts flatMap { _ =>
          cbcSubscriptionRecordsService
            .store(cbcRecord, autoFill = false, user.planetId.get)
            .flatMap(saveRecordId)
        }
    }

    val pptSubscriptionDisplayRecordForPptReference: UserAndGroupRecordsSync = saveRecordId => {
      case (user, PptReferenceMatch(group, pptReference)) =>
        def knownFactsForPptRegDate = knownFactsRepository.findByEnrolmentKey(
          EnrolmentKey.from("HMRC-PPT-ORG", "EtmpRegistrationNumber" -> pptReference),
          user.planetId.get
        )

        def getPptRegDate(knownFacts: Option[KnownFacts]) = knownFacts.fold(Option.empty[String])(
          _.getVerifierValue("PPTRegistrationDate")
            .map(date => LocalDate.parse(date, dateFormatddMMyy))
            .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
            .map(_.format(dateFormatyyyyMMdd))
        )

        knownFactsForPptRegDate map getPptRegDate flatMap { pptRegistrationDate =>
          val subscriptionDisplayRecord = PPTSubscriptionDisplayRecord
            .generateWith(
              Some(group.affinityGroup),
              user.firstName,
              user.lastName,
              pptRegistrationDate,
              pptReference
            )
          pptSubscriptionDisplayRecordsService
            .store(subscriptionDisplayRecord, autoFill = false, user.planetId.get)
            .flatMap(saveRecordId)
        }
    }

    val businessDetailsForCgt: UserAndGroupRecordsSync = saveRecordId => { case (user, CgtMatch(_, cgtRef)) =>
      val nino = user.nino
        .map(_.value.replace(" ", ""))
        .getOrElse(Generator.ninoNoSpaces(user.userId).value)

      val address = user.address
        .map(a =>
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
            )
        )
        .getOrElse(BusinessDetailsRecord.UkAddress.generate(user.userId))

      Future {
        BusinessDetailsRecord
          .generate(user.userId)
          .withNino(nino)
          .withCgtPdRef(Some(cgtRef))
          .modifyBusinessData { case Some(businessData :: _) =>
            Some(
              Seq(
                businessData
                  .withCessationDate(None)
                  .withCessationReason(None)
                  .withBusinessAddressDetails(Some(address))
              )
            )
          }
      }.flatMap(record =>
        Future
          .sequence(
            Seq(
              businessDetailsRecordsService.getBusinessDetails(Nino(nino), user.planetId.get),
              businessDetailsRecordsService.getBusinessDetails(CgtRef(cgtRef), user.planetId.get)
            )
          )
          .map(_.collect { case Some(x) => x })
          .map(_.toList match {
            case r1 :: r2 :: Nil if r1.id == r2.id => record.withId(r2.id)
            case r :: Nil                          => record.withId(r.id)
            case Nil                               => record
          })
          .flatMap(entity =>
            businessDetailsRecordsService
              .store(entity, autoFill = false, user.planetId.get)
              .flatMap(saveRecordId)
          )
      )
    }

    val vatCustomerInformationForMtdVatIndividual: UserAndGroupRecordsSync = saveRecordId => {
      case (user, MtdVatIndividualMatch(_, vrn)) =>
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("VATRegistrationDate")
                .map(LocalDate.parse(_, dateFormatddMMyy))
                .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
            )
          )
          .flatMap { vatRegistrationDateOpt =>
            val address = user.address
              .map(a =>
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
                  )
              )
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
                          .withIndividual(
                            Some(
                              IndividualName()
                                .withFirstName(user.firstName)
                                .withLastName(user.lastName)
                            )
                          )
                          .withDateOfBirth(user.dateOfBirth.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                          .modifyEffectiveRegistrationDate { case date =>
                            vatRegistrationDateOpt.map(_.format(dateFormatyyyyMMdd)).orElse(date)
                          }
                          .withInsolvencyFlag()
                      )
                      .modifyPPOB { case ppob =>
                        ppob.withAddress(address)
                      }
                      .withDeregistration(None)
                  )
                )
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)
            )
          }
    }

    val vatCustomerInformationForMtdVatOrganisation: UserAndGroupRecordsSync = saveRecordId => {
      case (user, MtdVatOrganisationMatch(_, vrn)) =>
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("VATRegistrationDate")
                .map(LocalDate.parse(_, dateFormatddMMyy))
                .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
            )
          )
          .flatMap { vatRegistrationDateOpt =>
            val address = user.address
              .map(a =>
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
                  )
              )
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
                          .modifyEffectiveRegistrationDate { case date =>
                            vatRegistrationDateOpt.map(_.format(dateFormatyyyyMMdd)).orElse(date)
                          }
                      )
                      .modifyPPOB { case ppob =>
                        ppob.withAddress(address)
                      }
                      .withDeregistration(None)
                  )
                )
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)
            )
          }
    }

    val vatCustomerInformationForMtdVatAgent: UserAndGroupRecordsSync = saveRecordId => {
      case (user, MtdVatAgentMatch(_, vrn)) =>
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMCE-VAT-AGNT", "AgentRefNo" -> vrn), user.planetId.get)
          .map(
            _.flatMap(
              _.getVerifierValue("IREFFREGDATE")
                .map(LocalDate.parse(_, dateFormatddMMyy))
                .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
            )
          )
          .flatMap { vatRegistrationDateOpt =>
            val address = user.address
              .map(a =>
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
                  )
              )
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
                          .modifyEffectiveRegistrationDate { case date =>
                            vatRegistrationDateOpt.map(_.format(dateFormatyyyyMMdd)).orElse(date)
                          }
                      )
                      .modifyPPOB { case ppob =>
                        ppob.withAddress(address)
                      }
                      .withDeregistration(None)
                  )
                )
            }.flatMap(record =>
              vatCustomerInformationRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)
            )
          }
    }

    val businessPartnerRecordForAnAgent: UserAndGroupRecordsSync = saveRecordId => { case (user, Group.Agent(group)) =>
      val address = user.address
        .map(a =>
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
            )
        )
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
            )
          )
          .withAgencyDetails(
            Some(
              AgencyDetails
                .generate(user.userId)
                .withAgencyAddress(Some(address))
                .withAgencyName(group.agentFriendlyName.map(_.take(40)))
            )
          )
          .withSuspensionDetails(SuspensionDetails(group.suspendedRegimes.nonEmpty, Some(group.suspendedRegimes)))
          .withAddressDetails(address)
      }.flatMap { record =>
        val utr = Generator.utr(user.userId)
        val crn = Generator.crn(user.userId)
        group
          .findIdentifierValue("HMRC-AS-AGENT", "AgentReferenceNumber") match {
          case Some(arn) =>
            knownFactsRepository
              .findByEnrolmentKey(EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> arn), user.planetId.get)
              .map(_.flatMap(_.getVerifierValue("AgencyPostcode")))
              .flatMap { postcodeOpt =>
                val ar = record
                  .withAgentReferenceNumber(Option(arn))
                  .withCrn(Option(crn))
                  .withIsAnAgent(true)
                  .withIsAnASAgent(true)
                  .modifyAgencyDetails { case Some(ad) =>
                    Some(ad.modifyAgencyAddress { case Some(aa: BusinessPartnerRecord.UkAddress) =>
                      Some(aa.modifyPostalCode { case pc => postcodeOpt.getOrElse(pc) })
                    })
                  }
                  .modifyAddressDetails { case ad: BusinessPartnerRecord.UkAddress =>
                    ad.modifyPostalCode { case pc => postcodeOpt.getOrElse(pc) }
                  }
                BusinessPartnerRecordsService
                  .store(ar, autoFill = false, user.planetId.get)
                  .flatMap(saveRecordId)
              }

          case None if group.principalEnrolments.isEmpty =>
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
      }
    }

    val businessPartnerRecordForIRCTOrganisation: UserAndGroupRecordsSync = saveRecordId => {
      case (user, IRCTOrganisationMatch(_, utr)) =>
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("IR-CT", "UTR" -> utr), user.planetId.get)
          .map(_.flatMap(_.getVerifierValue("Postcode")))
          .flatMap { postcode =>
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
                .withOrganisation(
                  Some(
                    Organisation
                      .generate(user.userId)
                  )
                )
            }.flatMap(record =>
              BusinessPartnerRecordsService
                .store(record, autoFill = false, user.planetId.get)
                .flatMap(saveRecordId)
            )
          }
    }

    val legacySaAgentRecord: UserAndGroupRecordsSync = saveRecordId => { case (user, SaAgentMatch(group, saAgentRef)) =>
      val agentRecord = LegacyAgentRecord(
        agentId = saAgentRef,
        govAgentId = group.agentId,
        agentName = group.agentFriendlyName.orElse(user.name).getOrElse("John Doe"),
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
               group.delegatedEnrolments
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
                 }
             )
      } yield ()
    }

    val legacyPayeAgentInformation: UserAndGroupRecordsSync = saveRecordId => {
      case (user, PayeAgentMatch(group, agentReference)) =>
        val delegatedEmpRefs: Set[EmployerAuths.EmpAuth.EmpRef] = group
          .findDelegatedIdentifierValues("IR-PAYE")
          .map(s => EmployerAuths.EmpAuth.EmpRef(s(0), s(1)))
          .toSet
        group.agentCode match {
          case None => Future.successful(())
          case Some(agentCode) =>
            for {
              recordOpt <- employerAuthsRecordsService.getEmployerAuthsByAgentCode(agentCode, user.planetId.get)
              _ <- recordOpt match {
                     case Some(existing) =>
                       val recordEmpRefs: Set[EmployerAuths.EmpAuth.EmpRef] = existing.empAuthList.map(_.empRef).toSet
                       val empAuthsToAdd = (delegatedEmpRefs -- recordEmpRefs)
                         .map(empRef =>
                           EmployerAuths.EmpAuth(
                             empRef = empRef,
                             aoRef = EmployerAuths.EmpAuth.AoRef(empRef.districtNumber, "0", "0", empRef.reference),
                             `Auth_64-8` = true,
                             Auth_OAA = true,
                             agentClientRef = Some(agentReference)
                           )
                         )
                         .toSeq
                       val record = existing.copy(empAuthList = existing.empAuthList ++ empAuthsToAdd)
                       if (record.empAuthList.nonEmpty)
                         employerAuthsRecordsService
                           .store(record, autoFill = false, user.planetId.get)
                           .flatMap(saveRecordId)
                       else
                         employerAuthsRecordsService
                           .delete(agentCode, user.planetId.get)
                           .flatMap(_ => saveRecordId("--" + record.id.get))
                     case None =>
                       val empAuthList = delegatedEmpRefs
                         .map(empRef =>
                           EmployerAuths.EmpAuth(
                             empRef = empRef,
                             aoRef = EmployerAuths.EmpAuth.AoRef(empRef.districtNumber, "0", "0", empRef.reference),
                             `Auth_64-8` = true,
                             Auth_OAA = true,
                             agentClientRef = Some(agentReference)
                           )
                         )
                         .toSeq
                       val record = EmployerAuths(agentCode = agentCode, empAuthList = empAuthList)
                       if (record.empAuthList.nonEmpty)
                         employerAuthsRecordsService
                           .store(record, autoFill = false, user.planetId.get)
                           .flatMap(saveRecordId)
                       else Future.successful("")
                   }
            } yield ()
        }
    }
  }

}
