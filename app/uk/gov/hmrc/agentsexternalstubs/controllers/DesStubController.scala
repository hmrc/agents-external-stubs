/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.Instant

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Constraints, Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Urn, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.Generator.{urn, utr}
import uk.gov.hmrc.agentsexternalstubs.models.TrustDetailsResponse.getErrorResponseFor
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, SubscribeAgentServicesPayload, _}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesStubController @Inject()(
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  recordsRepository: RecordsRepository,
  employerAuthsRecordsService: EmployerAuthsRecordsService,
  cc: ControllerComponents
)(implicit usersService: UsersService, executionContext: ExecutionContext)
    extends BackendController(cc) with DesCurrentSession {

  import DesStubController._

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[CreateUpdateAgentRelationshipPayload] { payload =>
        CreateUpdateAgentRelationshipPayload
          .validate(payload)
          .fold(
            error => badRequestF("INVALID_SUBMISSION", error.mkString(", ")),
            _ =>
              if (payload.authorisation.action == "Authorise")
                relationshipRecordsService
                  .authorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
              else
                relationshipRecordsService
                  .deAuthorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
          )
      }
    }(SessionRecordNotFound)
  }

  def getRelationship(
    idtype: Option[String],
    `ref-no`: Option[String],
    arn: Option[String],
    agent: Boolean,
    `active-only`: Boolean,
    regime: String,
    from: Option[String],
    to: Option[String],
    relationship: Option[String],
    `auth-profile`: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      GetRelationships.form.bindFromRequest.fold(
        hasErrors => badRequestF("INVALID_SUBMISSION", hasErrors.errors.map(_.message).mkString(", ")),
        query => {
          relationshipRecordsService
            .findByQuery(query, session.planetId)
            .flatMap(records => {

              def checkSuspension(arn: Arn): Future[Result] =
                businessPartnerRecordsService.getBusinessPartnerRecord(arn, session.planetId) map {
                  case Some(bpr) =>
                    bpr.suspensionDetails match {
                      case Some(sd) =>
                        if (sd.suspendedRegimes.contains(regime))
                          forbidden("AGENT_SUSPENDED", "The remote endpoint has indicated that the agent is suspended")
                        else Ok(Json.toJson(GetRelationships.Response.from(records)))

                      case None => Ok(Json.toJson(GetRelationships.Response.from(records)))
                    }
                }

              records.headOption match {
                case Some(r) =>
                  checkSuspension(Arn(r.arn))

                case None =>
                  if (agent) {
                    checkSuspension(Arn(arn.getOrElse(throw new Exception("agent must have arn"))))
                  } else Future successful Ok(Json.toJson(GetRelationships.Response.from(records)))
              }
            })
        }
      )
    }(SessionRecordNotFound)
  }

  def getLegacyRelationshipsByUtr(utr: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validUtr(utr)
        .fold(
          error => badRequestF("INVALID_UTR", error),
          _ =>
            legacyRelationshipRecordsService
              .getLegacyRelationshipsByUtr(utr, session.planetId)
              .map(ninoWithAgentList => Ok(Json.toJson(GetLegacyRelationships.Response.from(ninoWithAgentList))))
        )
    }(SessionRecordNotFound)
  }

  def getLegacyRelationshipsByNino(nino: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validNinoNoSpaces(nino)
        .fold(
          error => badRequestF("INVALID_NINO", error),
          _ =>
            legacyRelationshipRecordsService
              .getLegacyRelationshipsByNino(nino, session.planetId)
              .map(ninoWithAgentList => Ok(Json.toJson(GetLegacyRelationships.Response.from(ninoWithAgentList))))
        )
    }(SessionRecordNotFound)
  }

  def getBusinessDetails(idType: String, idNumber: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      withValidIdentifier(idType, idNumber) {
        case ("nino", nino) =>
          businessDetailsRecordsService
            .getBusinessDetails(Nino(nino), session.planetId)
            .map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => notFound("NOT_FOUND_NINO")
            }
        case ("mtdbsa", mtdbsa) =>
          businessDetailsRecordsService
            .getBusinessDetails(MtdItId(mtdbsa), session.planetId)
            .map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => notFound("NOT_FOUND_MTDBSA")
            }
      }
    }(SessionRecordNotFound)
  }

  def getBusinessPartnerRecord(idType: String, idNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        withValidIdentifier(idType, idNumber) {
          case ("arn", arn) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Arn(arn), session.planetId)
              .map {
                case Some(record) => Ok(Json.toJson(record))
                case None         => notFound("NOT_FOUND")
              }
          case ("utr", utr) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Utr(utr), session.planetId)
              .map {
                case Some(record) => Ok(Json.toJson(record))
                case None         => notFound("NOT_FOUND")
              }
        }
      }(SessionRecordNotFound)
  }

  def getVatCustomerInformation(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validVrn(vrn)
        .fold(
          error => badRequestF("INVALID_VRN", error),
          _ =>
            vatCustomerInformationRecordsService.getCustomerInformation(vrn, session.planetId).map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => Ok(Json.obj())
          }
        )
    }(SessionRecordNotFound)
  }

  def subscribeAgentServicesWithUtr(identifier: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        RegexPatterns
          .validUtr(identifier)
          .fold(
            error => badRequestF("INVALID_UTR", error),
            _ =>
              withPayload[SubscribeAgentServicesPayload] { payload =>
                SubscribeAgentServicesPayload
                  .validate(payload)
                  .fold(
                    error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
                    _ =>
                      businessPartnerRecordsService
                        .getBusinessPartnerRecord(Utr(identifier), session.planetId)
                        .flatMap {
                          case None => badRequestF("NOT_FOUND")
                          case Some(existingRecord) =>
                            businessPartnerRecordsService
                              .store(
                                SubscribeAgentServices.toBusinessPartnerRecord(payload, existingRecord),
                                autoFill = false,
                                session.planetId)
                              .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
                              .map {
                                case Some(record) =>
                                  ok(SubscribeAgentServices.Response(record.safeId, record.agentReferenceNumber.get))
                                case None =>
                                  internalServerError("SERVER_ERROR", "BusinessPartnerRecord creation failed silently.")
                              }
                      }
                  )
            }
          )

      }(SessionRecordNotFound)
  }

  def subscribeAgentServicesWithSafeId(identifier: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        RegexPatterns
          .validSafeId(identifier)
          .fold(
            error => badRequestF("INVALID_SAFEID", error),
            _ =>
              withPayload[SubscribeAgentServicesPayload] { payload =>
                SubscribeAgentServicesPayload
                  .validate(payload)
                  .fold(
                    error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
                    _ =>
                      businessPartnerRecordsService
                        .getBusinessPartnerRecordBySafeId(identifier, session.planetId)
                        .flatMap {
                          case None => badRequestF("NOT_FOUND")
                          case Some(existingRecord) =>
                            val recordToCreate = SubscribeAgentServices.toBusinessPartnerRecord(payload, existingRecord)
                            businessPartnerRecordsService
                              .store(recordToCreate, autoFill = false, session.planetId)
                              .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
                              .map {
                                case Some(record) =>
                                  ok(SubscribeAgentServices.Response(record.safeId, record.agentReferenceNumber.get))
                                case None =>
                                  ok(SubscribeAgentServices
                                    .Response(recordToCreate.safeId, recordToCreate.agentReferenceNumber.get))
                              }
                      }
                  )
            }
          )

      }(SessionRecordNotFound)
  }

  def register(idType: String, idNumber: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[RegistrationPayload] { payload =>
          withValidIdentifier(idType, idNumber) {
            case ("utr", utr) =>
              businessPartnerRecordsService
                .getBusinessPartnerRecord(Utr(utr), session.planetId)
                .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record => true))
            case ("nino", nino) =>
              businessPartnerRecordsService
                .getBusinessPartnerRecord(Nino(nino), session.planetId)
                .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record => true))
            case ("eori", eori) =>
              businessPartnerRecordsService
                .getBusinessPartnerRecordByEori(eori, session.planetId)
                .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record => true))
          }
        }
      }(SessionRecordNotFound)
  }

  def agentClientAuthorisationFlags(agentref: String, utr: String): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        RegexPatterns
          .validUtr(utr)
          .fold(
            error => badRequestF("INVALID_UTR", error),
            _ =>
              legacyRelationshipRecordsService
                .getLegacyRelationshipByAgentIdAndUtr(agentref, utr, session.planetId)
                .map {
                  case Some(relationship) => ok(SAAgentClientAuthorisation.Response.from(relationship))
                  case None               => notFound("Resource not found")
              }
          )
      }(SessionRecordNotFound)
  }

  def registerIndividualWithoutID: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[RegistrationWithoutIdPayload] { payload =>
        if (payload.individual.isDefined) {
          val recordToCreate = RegistrationWithoutId.toBusinessPartnerRecord(payload)
          businessPartnerRecordsService
            .store(recordToCreate, autoFill = false, session.planetId)
            .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
            .map {
              case Some(record) =>
                ok(RegistrationWithoutId.responseFrom(record))
              case _ =>
                ok(RegistrationWithoutId.responseFrom(recordToCreate))
            }
        } else badRequestF("INVALID_PAYLOAD", "Expected individual but missing.")
      }
    }(SessionRecordNotFound)
  }

  def registerOrganisationWithoutID: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[RegistrationWithoutIdPayload] { payload =>
        if (payload.organisation.isDefined) {
          val recordToCreate = RegistrationWithoutId.toBusinessPartnerRecord(payload)
          businessPartnerRecordsService
            .store(recordToCreate, autoFill = false, session.planetId)
            .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
            .map {
              case Some(record) =>
                ok(RegistrationWithoutId.responseFrom(record))
              case _ =>
                ok(RegistrationWithoutId.responseFrom(recordToCreate))
            }
        } else badRequestF("INVALID_PAYLOAD", "Expected organisation but missing.")
      }
    }(SessionRecordNotFound)
  }

  def retrieveLegacyAgentClientPayeInformation(agentCode: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        RegexPatterns
          .validAgentCode(agentCode)
          .fold(
            _ => badRequestF("Invalid AgentRef"),
            _ =>
              withPayload[EmployerAuthsPayload] { payload =>
                employerAuthsRecordsService.getEmployerAuthsByAgentCode(agentCode, session.planetId).map {
                  case None => notFound("AgentRef not found")
                  case Some(record) =>
                    LegacyAgentClientPayeRelationship
                      .retrieve(payload, record) match {
                      case Some(r) => ok(r)
                      case None    => NoContent
                    }
                }
            }
          )
      }(SessionRecordNotFound)
  }

  def removeLegacyAgentClientPayeRelationship(
    agentCode: String,
    taxOfficeNumber: String,
    taxOfficeReference: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      Validator
        .product(
          Validator.checkFromEither(RegexPatterns.validAgentCode, "Invalid AgentRef"),
          Validator.checkFromEither(RegexPatterns.validTaxOfficeNumber, "Invalid TaxOfficeNumber"),
          Validator.checkFromEither(RegexPatterns.validTaxOfficeReference, "Invalid TaxOfficeReference")
        )((agentCode, taxOfficeNumber, taxOfficeReference))
        .fold(
          error => badRequestF(error.mkString(", ")),
          _ =>
            employerAuthsRecordsService
              .getEmployerAuthsByAgentCode(agentCode, session.planetId)
              .flatMap {
                case None => notFoundF("Relationship not found")
                case Some(record) =>
                  val newEmployerAuths =
                    LegacyAgentClientPayeRelationship.remove(record, taxOfficeNumber, taxOfficeReference)
                  if (newEmployerAuths.empAuthList.nonEmpty)
                    employerAuthsRecordsService
                      .store(newEmployerAuths, false, session.planetId)
                      .map(_ => Ok)
                  else
                    employerAuthsRecordsService
                      .delete(agentCode, session.planetId)
                      .map(_ => Ok)
            }
        )
    }(SessionRecordNotFound)
  }

  def getCtReference(idType: String, idValue: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      withValidIdentifier(idType, idValue) {
        case ("crn", crn) =>
          businessPartnerRecordsService
            .getBusinessPartnerRecordByCrn(crn, session.planetId)
            .map(_.flatMap(GetCtReference.Response.from))
            .map {
              case None     => notFound("NOT_FOUND", "The back end has indicated that CT UTR cannot be returned.")
              case response => ok(response)
            }
      }
    }(SessionRecordNotFound)
  }

  def getVatKnownFacts(vrn: String) = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validVrn(vrn)
        .fold(
          error => badRequestF("INVALID_VRN", error),
          _ =>
            vatCustomerInformationRecordsService.getVatKnownFacts(vrn, session.planetId).map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => NotFound
          }
        )
    }(SessionRecordNotFound)
  }

  def getTrustKnownFacts(trustTaxIdentifier: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validUtrOrUrn(trustTaxIdentifier)
        .fold(
          error => badRequestF(error.mkString(", ")),
          taxIdentifier => {
            val enrolmentKey = taxIdentifier match {
              case Utr(v) => EnrolmentKey("HMRC-TERS-ORG", Seq(Identifier("SAUTR", v)))
              case Urn(v) => EnrolmentKey("HMRC-TERSNT-ORG", Seq(Identifier("URN", v)))
            }
            usersService
              .findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
              .map {
                case Some(record) => {
                  val maybeUtr =
                    extractEnrolmentValue("HMRC-TERS-ORG")(record)
                  val maybeUrn =
                    extractEnrolmentValue("HMRC-TERSNT-ORG")(record)
                  val trustDetails = TrustDetailsResponse(
                    TrustDetails(
                      maybeUtr,
                      maybeUrn,
                      record.name.getOrElse(""),
                      TrustAddress(record.user.address),
                      "TERS"))
                  Ok(Json.toJson(trustDetails))
                }
                case None => getErrorResponseFor(trustTaxIdentifier)
              }
          }
        )
    }(SessionRecordNotFound)
  }
  private def extractEnrolmentValue(serviceKey: String)(record: User) =
    record.principalEnrolments
      .find(_.key == serviceKey)
      .flatMap(_.toEnrolmentKeyTag)
      .map(_.split('~').takeRight(1).mkString)

  def getCgtSubscription(regime: String, idType: String, cgtRef: String): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        (regime, idType) match {
          case ("CGT", "ZCGT") =>
            RegexPatterns
              .validCgtRef(cgtRef)
              .fold(
                _ => badRequestF("INVALID_IDVALUE", "Submission has not passed validation. Invalid parameter idValue."),
                _ =>
                  usersService
                    .findByPrincipalEnrolmentKey(
                      EnrolmentKey("HMRC-CGT-PD", Seq(Identifier("CGTPDRef", cgtRef))),
                      session.planetId)
                    .map {
                      case Some(record) =>
                        val tpd = record.affinityGroup match {
                          case Some("Individual") =>
                            TypeOfPersonDetails(
                              "Individual",
                              Left(IndividualName(record.firstName.getOrElse(""), record.lastName.getOrElse(""))))
                          case _ => TypeOfPersonDetails("Trustee", Right(OrganisationName(record.name.getOrElse(""))))
                        }

                        val addressDetails = CgtAddressDetails(
                          record.address.flatMap(_.line1).getOrElse(""),
                          record.address.flatMap(_.line2),
                          record.address.flatMap(_.line3),
                          record.address.flatMap(_.line4),
                          record.address.flatMap(_.countryCode).getOrElse(""),
                          record.address.flatMap(_.postcode),
                        )

                        val cgtSubscription: CgtSubscription =
                          CgtSubscription("CGT", SubscriptionDetails(tpd, addressDetails))
                        Ok(Json.toJson(cgtSubscription))
                      case None => notFound("NOT_FOUND", "Data not found  for the provided Registration Number.")
                  }
              )
          case ("CGT", _) =>
            badRequestF("INVALID_IDTYPE", "Submission has not passed validation. Invalid parameter idType.")
          case (_, "ZCGT") =>
            badRequestF("INVALID_REGIME", "Submission has not passed validation. Invalid parameter regimeValue.")
          case _ =>
            badRequestF(
              "INVALID_REQUEST",
              "Submission has not passed validation. Request not implemented by the backend.")
        }

      }(SessionRecordNotFound)
  }

  private def getOrCreateBusinessPartnerRecord[T <: Record](
    payload: RegistrationPayload,
    idType: String,
    idNumber: String,
    planetId: String)(matches: T => Boolean): Option[T] => Future[Result] = {
    case Some(record) =>
      if (matches(record)) okF(record, Registration.fixSchemaDifferences _)
      else notFoundF("NOT_FOUND", "BusinessPartnerRecord exists but fails match expectations.")
    case None =>
      if (payload.organisation.isDefined || payload.individual.isDefined) {
        businessPartnerRecordsService
          .store(Registration.toBusinessPartnerRecord(payload, idType, idNumber), autoFill = false, planetId)
          .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, planetId))
          .map {
            case Some(record: T) =>
              if (matches(record)) ok(record, Registration.fixSchemaDifferences _)
              else internalServerError("SERVER_ERROR", "Created BusinessPartnerRecord fails match expectations.")
            case _ =>
              internalServerError("SERVER_ERROR", "BusinessPartnerRecord creation failed silently.")
          }
      } else notFoundF("NOT_FOUND")
  }

  private def withValidIdentifier(idType: String, idNumber: String)(
    pf: PartialFunction[(String, String), Future[Result]])(implicit ec: ExecutionContext): Future[Result] =
    idType match {
      case "nino"   => validateIdentifier(RegexPatterns.validNinoNoSpaces, "INVALID_NINO", idType, idNumber)(pf)
      case "mtdbsa" => validateIdentifier(RegexPatterns.validMtdbsa, "INVALID_MTDBSA", idType, idNumber)(pf)
      case "utr"    => validateIdentifier(RegexPatterns.validUtr, "INVALID_UTR", idType, idNumber)(pf)
      case "urn"    => validateIdentifier(RegexPatterns.validUrn, "INVALID_URN", idType, idNumber)(pf)
      case "arn"    => validateIdentifier(RegexPatterns.validArn, "INVALID_ARN", idType, idNumber)(pf)
      case "vrn"    => validateIdentifier(RegexPatterns.validVrn, "INVALID_VRN", idType, idNumber)(pf)
      case "eori"   => validateIdentifier(RegexPatterns.validEori, "INVALID_EORI", idType, idNumber)(pf)
      case "crn"    => validateIdentifier(RegexPatterns.validCrn, "INVALID_CRN", idType, idNumber)(pf)
      case "safeId" => validateIdentifier(RegexPatterns.validSafeId, "INVALID_SAFEID", idType, idNumber)(pf)
      case _        => badRequestF("INVALID_IDTYPE")
    }

  private def validateIdentifier(matcher: RegexPatterns.Matcher, errorCode: String, idType: String, idNumber: String)(
    pf: PartialFunction[(String, String), Future[Result]]): Future[Result] =
    matcher(idNumber).fold(
      error => badRequestF(errorCode, error),
      _ =>
        if (pf.isDefinedAt((idType, idNumber))) pf((idType, idNumber))
        else badRequestF(errorCode, "Unsupported identifier type")
    )

}

object DesStubController {

  object AuthoriseRequest {

    def toRelationshipRecord(r: CreateUpdateAgentRelationshipPayload): RelationshipRecord =
      RelationshipRecord(
        regime = r.regime,
        arn = r.agentReferenceNumber,
        idType = r.idType.getOrElse("none"),
        refNumber = r.refNumber,
        active = false,
        relationshipType = r.relationshipType,
        authProfile = r.authProfile
      )

  }

  case class AuthoriseResponse(processingDate: Instant = Instant.now())

  object AuthoriseResponse {
    implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
  }

  object GetRelationships {

    private val queryConstraint: Constraint[RelationshipRecordQuery] = Constraint(
      q =>
        if (q.agent && q.arn.isEmpty) Invalid("Missing arn")
        else if (!q.agent && q.refNumber.isEmpty) Invalid("Missing ref-no")
        else if ((!q.activeOnly || q.to.isDefined) && q.from.isEmpty) Invalid("Missing from date")
        else if (!q.activeOnly && q.to.isEmpty) Invalid("Missing to date")
        else if ((q.regime == "VATC" || q.regime == "CGT") && q.relationship.isEmpty)
          Invalid(s"relationship type is mandatory for ${q.regime} regime")
        else if ((q.regime == "VATC" || q.regime == "CGT") && q.authProfile.isEmpty)
          Invalid(s"auth profile is mandatory for ${q.regime} regime")
        else Valid)

    val form: Form[RelationshipRecordQuery] = Form[RelationshipRecordQuery](
      mapping(
        "regime" -> nonEmptyText.verifying(Constraints.pattern("^[A-Z]{3,10}$".r, "regime", "Invalid regime")),
        "arn"    -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validArn, "arn"))),
        "idtype" -> default(
          nonEmptyText.verifying(Constraints.pattern("^[A-Z]{1,6}$".r, "idtype", "Invalid idtype")),
          "none"),
        "ref-no" -> optional(
          nonEmptyText.verifying(Constraints.pattern("^[0-9A-Za-z]{1,15}$".r, "ref-no", "Invalid ref-no"))),
        "active-only" -> boolean,
        "agent"       -> boolean,
        "from" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "from")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString)),
        "to" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "to")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString)),
        "relationship" -> optional(nonEmptyText.verifying("invalid relationship type", _ == "ZA01")),
        "auth-profile" -> optional(nonEmptyText.verifying("invalid auth profile", _ == "ALL00001"))
      )(RelationshipRecordQuery.apply)(RelationshipRecordQuery.unapply).verifying(queryConstraint))

    case class Individual(firstName: String, lastName: String)

    case class Organisation(organisationName: String)

    case class Relationship(
      referenceNumber: String,
      agentReferenceNumber: String,
      dateFrom: LocalDate,
      dateTo: Option[LocalDate] = None,
      contractAccountCategory: String,
      activity: Option[String] = None,
      relationshipType: Option[String] = None,
      authProfile: Option[String] = None,
      individual: Option[Individual] = None,
      organisation: Option[Organisation] = None
    )

    object Relationship {
      import play.api.libs.json.JodaWrites._
      import play.api.libs.json.JodaReads._

      implicit val writes1: Writes[Individual] = Json.writes[Individual]
      implicit val writes2: Writes[Organisation] = Json.writes[Organisation]
      implicit val writes3: Writes[Relationship] = Json.writes[Relationship]

      def from(record: RelationshipRecord): Relationship = Relationship(
        referenceNumber = record.refNumber,
        agentReferenceNumber = record.arn,
        dateFrom = record.startDate.getOrElse(throw new Exception("Missing startDate of relationship")),
        dateTo = record.endDate,
        contractAccountCategory = "33", // magic number!
        relationshipType = record.relationshipType,
        authProfile = record.authProfile,
        individual = decideIndividual(record),
        organisation = decideOrganisation(record)
      )

      def decideIndividual(record: RelationshipRecord): Option[Individual] =
        if (record.regime == "ITSA") {
          val nameParts: Array[String] =
            UserGenerator.nameForIndividual(record.idType + "/" + record.refNumber).split(" ")
          Some(Individual(nameParts.init.mkString(" "), nameParts.last))
        } else None

      def decideOrganisation(record: RelationshipRecord): Option[Organisation] =
        if (record.regime != "ITSA")
          Some(Organisation(UserGenerator.nameForOrganisation(record.idType + "/" + record.refNumber)))
        else None
    }

    case class Response(relationship: Seq[Relationship])

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]

      def from(records: Seq[RelationshipRecord]): Response =
        Response(relationship = records.map(Relationship.from))
    }
  }

  object GetLegacyRelationships {

    case class Response(agents: Seq[Response.LegacyAgent])

    object Response {
      def from(ninoWithAgentList: List[(String, LegacyAgentRecord)]): Response =
        Response(agents = ninoWithAgentList.map { case (nino, agent) => LegacyAgent.from(nino, agent) })

      case class LegacyAgent(
        id: String,
        nino: String,
        agentId: String,
        agentOwnRef: Option[String] = None,
        hasAgent: Option[Boolean] = None,
        isRegisteredAgent: Option[Boolean] = None,
        govAgentId: Option[String] = None,
        agentName: String,
        agentPhoneNo: Option[String] = None,
        address1: String,
        address2: String,
        address3: Option[String] = None,
        address4: Option[String] = None,
        postcode: Option[String] = None,
        isAgentAbroad: Boolean = false,
        agentCeasedDate: Option[String] = None
      )

      object LegacyAgent {

        def from(nino: String, a: LegacyAgentRecord): LegacyAgent = LegacyAgent(
          id = "",
          nino = nino,
          agentId = a.agentId,
          agentOwnRef = a.agentOwnRef,
          hasAgent = Some(true),
          isRegisteredAgent = a.isRegisteredAgent,
          govAgentId = a.govAgentId,
          agentName = a.agentName,
          agentPhoneNo = a.agentPhoneNo,
          address1 = a.address1,
          address2 = a.address2,
          address3 = a.address3,
          address4 = a.address4,
          postcode = a.postcode,
          isAgentAbroad = a.isAgentAbroad,
          agentCeasedDate = a.agentCeasedDate
        )
      }

      implicit val formats1: Format[LegacyAgent] = Json.format[LegacyAgent]
      implicit val formats: Format[Response] = Json.format[Response]
    }
  }

  object SubscribeAgentServices {

    def toBusinessPartnerRecord(
      payload: SubscribeAgentServicesPayload,
      existingRecord: BusinessPartnerRecord): BusinessPartnerRecord = {
      val address = payload.agencyAddress match {
        case SubscribeAgentServicesPayload.UkAddress(l1, l2, l3, l4, pc, cc) =>
          BusinessPartnerRecord.UkAddress(l1, l2, l3, l4, pc, cc)
        case SubscribeAgentServicesPayload.ForeignAddress(l1, l2, l3, l4, pc, cc) =>
          BusinessPartnerRecord.ForeignAddress(l1, l2, l3, l4, pc, cc)
      }
      existingRecord
        .modifyAgentReferenceNumber {
          case None => Some(Generator.arn(existingRecord.utr.getOrElse(existingRecord.safeId)).value)
        }
        .withAgencyDetails(
          Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option(payload.agencyName))
              .withAgencyAddress(Some(address))
              .withAgencyEmail(payload.agencyEmail)))
        .modifyContactDetails {
          case Some(contactDetails) =>
            Some(
              contactDetails
                .withPhoneNumber(payload.telephoneNumber)
                .withEmailAddress(payload.agencyEmail)
            )
        }
        .withAddressDetails(address)
        .withIsAnAgent(true)
        .withIsAnASAgent(true)
    }

    case class Response(safeId: String, agentRegistrationNumber: String)

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]
    }
  }

  object Registration {

    def toBusinessPartnerRecord(payload: RegistrationPayload, idType: String, idNumber: String): BusinessPartnerRecord =
      BusinessPartnerRecord
        .seed(idNumber)
        .withNino(if (idType == "nino") Some(idNumber) else None)
        .withUtr(if (idType == "utr") Some(idNumber) else None)
        .withEori(if (idType == "eori") Some(idNumber) else None)
        .withIsAnIndividual(payload.individual.isDefined)
        .withIsAnOrganisation(payload.organisation.isDefined)
        .withIsAnAgent(payload.isAnAgent)
        .withIsAnASAgent(false)
        .withIndividual(
          payload.individual.map(
            i =>
              BusinessPartnerRecord.Individual
                .seed(idNumber)
                .withFirstName(i.firstName)
                .withLastName(i.lastName)
                .modifyDateOfBirth { case dob => i.dateOfBirth.getOrElse(dob) }
          )
        )
        .withOrganisation(
          payload.organisation.map(
            o =>
              BusinessPartnerRecord.Organisation
                .seed(idNumber)
                .withOrganisationName(o.organisationName)
                .withIsAGroup(false)
                .withOrganisationType(o.organisationType)))

    def fixSchemaDifferences(value: JsValue): JsValue = value match {
      case obj: JsObject =>
        (obj \ "addressDetails").asOpt[JsObject] match {
          case Some(address) => obj.-("addressDetails").+("address" -> address)
          case None          => obj
        }
      case other => other
    }

  }

  object RegistrationWithoutId {

    def toBusinessPartnerRecord(payload: RegistrationWithoutIdPayload): BusinessPartnerRecord = {
      val seed = payload.identification.map(_.idNumber).getOrElse(payload.acknowledgementReference)
      BusinessPartnerRecord
        .seed(seed)
        .withNino(None)
        .withUtr(None)
        .withEori(None)
        .withIsAnIndividual(payload.individual.isDefined)
        .withIsAnOrganisation(payload.organisation.isDefined)
        .withIsAnAgent(payload.isAnAgent)
        .withIsAnASAgent(false)
        .withIndividual(
          payload.individual.map(
            i =>
              BusinessPartnerRecord.Individual
                .seed(seed)
                .withFirstName(i.firstName)
                .withLastName(i.lastName)
                .withDateOfBirth(i.dateOfBirth)
          )
        )
        .withOrganisation(
          payload.organisation.map(
            o =>
              BusinessPartnerRecord.Organisation
                .seed(seed)
                .withOrganisationName(o.organisationName)
                .withIsAGroup(payload.isAGroup)
                .withOrganisationType("0000")))
    }

    case class Response(processingDate: Instant = Instant.now(), sapNumber: String, safeId: String)

    object Response {
      implicit val formats: Format[Response] = Json.format[Response]
    }

    def responseFrom(record: BusinessPartnerRecord): Response =
      Response(sapNumber = Generator.patternValue("9999999999", record.safeId), safeId = record.safeId)

  }

  object SAAgentClientAuthorisation {

    case class Response(`Auth_64-8`: Boolean, `Auth_i64-8`: Boolean)

    object Response {

      def from(relationship: LegacyRelationshipRecord): Response =
        Response(relationship.`Auth_64-8`.getOrElse(false), relationship.`Auth_i64-8`.getOrElse(false))

      implicit val formats: Format[Response] = Json.format[Response]
    }

  }

  object LegacyAgentClientPayeRelationship {

    def remove(record: EmployerAuths, taxOfficeNumber: String, taxOfficeReference: String): EmployerAuths =
      record.copy(empAuthList = record.empAuthList.filterNot(e =>
        e.empRef.districtNumber == taxOfficeNumber && e.empRef.reference == taxOfficeReference))

    def retrieve(payload: EmployerAuthsPayload, record: EmployerAuths): Option[EmployerAuths] = {
      val filtered = record.copy(empAuthList = record.empAuthList.filter(e1 =>
        payload.empRefList.exists(e2 =>
          e2.districtNumber == e1.empRef.districtNumber && e2.reference == e1.empRef.reference)))
      if (filtered.empAuthList.isEmpty) None else Some(filtered)
    }
  }

  object GetCtReference {

    case class Response(CTUTR: String)

    object Response {
      def from(record: BusinessPartnerRecord): Option[Response] = record.utr.map(Response.apply)

      implicit val formats: Format[Response] = Json.format[Response]
    }

  }

}
