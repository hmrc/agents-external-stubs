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

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubs.controllers.DesIfStubController.GetRelationships
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.{ForeignAddress, UkAddress}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.identifiers._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipStubController @Inject() (
  hipStubService: HipStubService,
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  recordsService: RecordsService,
  recordsRepository: RecordsRepository,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) with ExternalCurrentSession with Logging {

  def displayAgentRelationship: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateBaseHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(invalidHeadersResponse) =>
          Future.successful(Results.UnprocessableEntity(Json.toJson(invalidHeadersResponse)))
        case _ =>
          hipStubService.processDisplayRelationshipsQueryParameters(
            request.getQueryString("regime"),
            request.getQueryString("refNumber"),
            request.getQueryString("idType"),
            request.getQueryString("arn"),
            request.getQueryString("isAnAgent"),
            request.getQueryString("activeOnly"),
            request.getQueryString("dateFrom"),
            request.getQueryString("dateTo"),
            request.getQueryString("relationshipType"),
            request.getQueryString("authProfile")
          ) match {
            case Left(invalidQueryParametersResponse) =>
              Future.successful(Results.UnprocessableEntity(Json.toJson(invalidQueryParametersResponse)))
            case Right(relationshipRecordQuery) =>
              relationshipRecordsService
                .findByQuery(relationshipRecordQuery, session.planetId)
                .flatMap { records =>
                  records.headOption match {
                    case Some(record) =>
                      recordsService
                        .getRecordMaybeExt[BusinessPartnerRecord, Arn](Arn(record.arn), session.planetId) map {
                        case Some(businessPartnerRecord) =>
                          businessPartnerRecord.suspensionDetails match {
                            case Some(suspensionDetails) =>
                              if (suspensionDetails.suspendedRegimes.contains(relationshipRecordQuery.regime)) {
                                Results.UnprocessableEntity(
                                  Json.toJson(Errors("059", s"${record.arn} is currently suspended"))
                                )
                              } else {
                                Ok(Json.toJson(convertResponseToNewFormat(GetRelationships.Response.from(records))))
                              }
                            case None =>
                              Ok(Json.toJson(convertResponseToNewFormat(GetRelationships.Response.from(records))))
                          }
                        case None =>
                          logger.error("no business partner record found")
                          Results.UnprocessableEntity(
                            Json.toJson(Errors("009", "No Relationships with activity"))
                          )
                      }
                    case None =>
                      logger.error("no relationship record(s) found")
                      Future.successful(
                        Results.UnprocessableEntity(
                          Json.toJson(Errors("009", "No Relationships with activity"))
                        )
                      )
                  }
                }
          }
      }
    }(SessionRecordNotFound)
  }

  def getAgentSubscription(arn: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateBaseHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(_) =>
          Future.successful(
            Results.UnprocessableEntity(Json.toJson(Errors("003", "Request could not be processed")))
          )
        case Right(_) =>
          hipStubService.validateArn(arn) match {
            case Left(_) =>
              Future.successful(
                Results.UnprocessableEntity(Json.toJson(Errors("003", "Request could not be processed")))
              )
            case Right(_) =>
              recordsService
                .getRecordMaybeExt[BusinessPartnerRecord, Arn](Arn(arn), session.planetId)
                .map {
                  case Some(record) if agentIsSuspendedForSubscription(record) =>
                    Results.UnprocessableEntity(Json.toJson(Errors("058", "Agent is terminated")))
                  case Some(record) if !record.isAnASAgent =>
                    Results.UnprocessableEntity(Json.toJson(Errors("006", "Subscription Data Not Found")))
                  case Some(record) => Ok(Json.toJson(convertToGetAgentSubscriptionResponse(record)))
                  case None         => Results.UnprocessableEntity(Json.toJson(Errors("006", "Subscription Data Not Found")))
                }
                .recover { case e =>
                  logger.error("Incomplete subscription", e)
                  Results.InternalServerError(
                    Json.parse("""{"error":{"code":"500","message":"Internal server error","logID": "1234567890"}}""")
                  )
                }
          }
      }
    }(SessionRecordNotFound)
  }

  private def convertToGetAgentSubscriptionResponse(record: BusinessPartnerRecord): HipAgentSubscriptionResponse = {
    val (l1, l2, l3, l4, pc, cc) = record.addressDetails match {
      case UkAddress(l1, l2, l3, l4, pc, cc)      => (l1, l2, l3, l4, Some(pc), cc)
      case ForeignAddress(l1, l2, l3, l4, pc, cc) => (l1, l2, l3, l4, pc, cc)
    }

    HipAgentSubscriptionResponse(
      AgentSubscriptionDisplayResponse(
        processingDate = LocalDateTime.now().toString,
        utr = record.utr,
        name = record.agencyDetails.flatMap(_.agencyName).getOrElse(""),
        addr1 = l1,
        addr2 = l2,
        addr3 = l3,
        addr4 = l4,
        postcode = pc,
        country = cc,
        phone = record.agencyDetails.flatMap(_.agencyTelephone),
        email = record.agencyDetails.flatMap(_.agencyEmail).getOrElse(""),
        suspensionStatus = if (record.suspensionDetails.exists(_.suspensionStatus)) "T" else "F",
        regime = record.suspensionDetails.map(_.suspendedRegimes.toSeq),
        supervisoryBody = record.agencyDetails.flatMap(_.supervisoryBody),
        membershipNumber = record.agencyDetails.flatMap(_.membershipNumber),
        evidenceObjectReference = record.agencyDetails.flatMap(_.evidenceObjectReference),
        updateDetailsStatus = record.agencyDetails
          .flatMap(_.updateDetailsStatus)
          .getOrElse(throw new IllegalStateException("updateDetailsStatus is missing")),
        amlSupervisionUpdateStatus = record.agencyDetails
          .flatMap(_.amlSupervisionUpdateStatus)
          .getOrElse(throw new IllegalStateException("amlSupervisionUpdateStatus is missing")),
        directorPartnerUpdateStatus = record.agencyDetails
          .flatMap(_.directorPartnerUpdateStatus)
          .getOrElse(throw new IllegalStateException("directorPartnerUpdateStatus is missing")),
        acceptNewTermsStatus = record.agencyDetails
          .flatMap(_.acceptNewTermsStatus)
          .getOrElse(throw new IllegalStateException("acceptNewTermsStatus is missing")),
        reriskStatus = record.agencyDetails
          .flatMap(_.reriskStatus)
          .getOrElse(throw new IllegalStateException("reriskStatus is missing"))
      )
    )
  }

  def updateAgentRelationship: Action[JsValue] = Action(parse.json).async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateBaseHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(invalidHeadersResponse) =>
          Future.successful(Results.UnprocessableEntity(Json.toJson(invalidHeadersResponse)))
        case _ =>
          hipStubService.validateUpdateRelationshipPayload(request.body.as[UpdateRelationshipPayload]) match {
            case Left(invalidPayload) =>
              Future.successful(Results.UnprocessableEntity(Json.toJson(invalidPayload)))
            case Right(payload) =>
              recordsService
                .getRecordMaybeExt[BusinessPartnerRecord, Arn](Arn(payload.arn), session.planetId) flatMap {
                case Some(businessPartnerRecord) =>
                  if (agentIsSuspended(businessPartnerRecord, payload.regime))
                    Future.successful(
                      Results.UnprocessableEntity(
                        Json.toJson(Errors("059", s"${payload.arn} is currently suspended"))
                      )
                    )
                  else if (payload.action == "0001") {

                    def checkInsolvency(): Future[Boolean] = for {
                      mVatInfo <-
                        recordsService
                          .getRecordMaybeExt[VatCustomerInformationRecord, Vrn](
                            Vrn(payload.refNumber),
                            session.planetId
                          )
                      mInsolventFlag = mVatInfo.flatMap(_.approvedInformation.flatMap(_.customerDetails.isInsolvent))
                    } yield mInsolventFlag.contains(true)

                    def authorise(): Future[Result] = relationshipRecordsService
                      .authorise(
                        UpdateRelationshipPayload.toRelationshipRecord(payload),
                        session.planetId,
                        payload.isExclusiveAgent
                      )
                      .map(_ => Created(withProcessingDate).withHeaders(correlationId))

                    if (payload.regime != "VATC") authorise()
                    else
                      checkInsolvency().flatMap {
                        case true =>
                          Future.successful(
                            Results.UnprocessableEntity(
                              Json.toJson(Errors("094", "Insolvent Trader - request could not be completed"))
                            )
                          )
                        case false => authorise()
                      }

                  } else {
                    relationshipRecordsService
                      .deAuthorise(UpdateRelationshipPayload.toRelationshipRecord(payload), session.planetId)
                      .map(result =>
                        if (result.nonEmpty) Created(withProcessingDate).withHeaders(correlationId)
                        else UnprocessableEntity(Json.toJson(Errors("014", "No active relationship found")))
                      )
                  }

                case None =>
                  Future.successful(
                    Results.NotFound("no business partner record found")
                  )
              }
          }
      }
    }(SessionRecordNotFound)
  }

  // HIP  API#5266 ITSA Taxpayer Business Details (previously IF API#1171).
  // Ignores parts of the API that are not relevant to our use case.
  def itsaTaxPayerBusinessDetails: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateBaseHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(invalidHeadersResponse) =>
          Future.successful(Results.UnprocessableEntity(Json.toJson(invalidHeadersResponse)))
        case _ =>
          hipStubService.validateGetITSABusinessDetailsHeaders(
            request.headers.get("X-Message-Type"),
            request.headers.get("X-Regime-Type")
          ) match {
            case Left(invalidHeadersResponse) =>
              Future.successful(Results.UnprocessableEntity(Json.toJson(invalidHeadersResponse)))
            case _ =>
              hipStubService.processItsaTaxpayerBusinessDetailsQueryParameters(
                request.getQueryString("mtdReference"),
                request.getQueryString("nino")
              ) match {
                case Left(invalidQueryParametersResponse) =>
                  Future.successful(Results.UnprocessableEntity(Json.toJson(invalidQueryParametersResponse)))
                case Right(taxIdentifier) =>
                  taxIdentifier match {
                    case n: NinoWithoutSuffix =>
                      recordsService
                        .getRecordMaybeExt[BusinessDetailsRecord, NinoWithoutSuffix](n, session.planetId)
                        .map {
                          case Some(record) =>
                            Ok(
                              ItsaTaxpayerBusinessDetailsResponse.serialise(
                                processingDate = Instant.now(),
                                taxPayerDisplayResponse = record
                              )
                            )
                          case None => UnprocessableEntity(Json.toJson(Errors("006", "Subscription data not found")))
                        }
                    case m: MtdItId =>
                      recordsService
                        .getRecord[BusinessDetailsRecord, MtdItId](m, session.planetId)
                        .map {
                          case Some(record) =>
                            Ok(
                              ItsaTaxpayerBusinessDetailsResponse.serialise(
                                processingDate = Instant.now(),
                                taxPayerDisplayResponse = record
                              )
                            )
                          case None => UnprocessableEntity(Json.toJson(Errors("006", "Subscription data not found")))
                        }

                  }
              }
          }
      }
    }(SessionRecordNotFound)
  }

  def createAgentSubscription(safeId: String): Action[JsValue] = Action(parse.json).async(implicit request =>
    withCurrentSession { session =>
      hipStubService.validateBaseHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(_) =>
          Future.successful(Results.UnprocessableEntity(Json.toJson(Errors("003", "Request could not be processed"))))
        case _ =>
          RegexPatterns.validSafeId(safeId) match {
            case Left(_) =>
              Future.successful(
                Results.UnprocessableEntity(Json.toJson(Errors("003", "Request could not be processed")))
              )
            case Right(_) =>
              HipSubscribeAgentServicesPayload.validateCreateAgentSubscriptionPayload(
                request.body.as[HipSubscribeAgentServicesPayload]
              ) match {
                case Left(errors) =>
                  Future.successful(Results.UnprocessableEntity(Json.toJson(errors)))
                case Right(payload) =>
                  recordsService
                    .getRecordMaybeExt[BusinessPartnerRecord, SafeId](SafeId(safeId), session.planetId)
                    .flatMap {
                      case None =>
                        Future.successful(Results.UnprocessableEntity(Json.toJson(Errors("006", "SAFE ID Not found"))))
                      case Some(existingRecord) =>
                        if (existingRecord.isAnASAgent)
                          Future.successful(
                            Results.UnprocessableEntity(
                              Json.toJson(
                                Errors(
                                  "061",
                                  s"BP has already a valid Agent Subscription ${existingRecord.agentReferenceNumber.get}"
                                )
                              )
                            )
                          )
                        else {
                          val recordToCreate: BusinessPartnerRecord =
                            SubscribeAgentService.toBusinessPartnerRecord(payload, existingRecord)
                          recordsService
                            .store(recordToCreate, autoFill = false, session.planetId)
                            .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
                            .map {
                              case Some(record) =>
                                Results.Created(
                                  Json.toJson(
                                    HipResponse(LocalDateTime.now(), record.agentReferenceNumber.get)
                                  )
                                )
                              case None =>
                                Results.InternalServerError(
                                  Json.parse(
                                    """{"error":{"code":"500","message":"Internal server error","logID": "1234567890"}}"""
                                  )
                                )
                            }
                        }
                    }
              }
          }
      }
    }(SessionRecordNotFound)
  )

  private def agentIsSuspended(businessPartnerRecord: BusinessPartnerRecord, regime: String): Boolean =
    businessPartnerRecord.suspensionDetails.fold(false)(_.suspendedRegimes.contains(regime))

  private def agentIsSuspendedForSubscription(businessPartnerRecord: BusinessPartnerRecord): Boolean =
    businessPartnerRecord.suspensionDetails.exists { suspensionDetails =>
      suspensionDetails.suspensionStatus || suspensionDetails.suspendedRegimes.nonEmpty
    }

  private val withProcessingDate = Json.parse(s"""{"success":{"processingDate": "${LocalDateTime.now()}"}}""")
  private def correlationId(implicit request: Request[_]): (String, String) =
    "correlationId" -> request.headers.get("correlationid").get

  //TODO this can be removed and the underlying models refactored once the test packs have moved over to the new endpoint
  private def convertResponseToNewFormat(response: GetRelationships.Response): AgentRelationshipDisplayResponse =
    AgentRelationshipDisplayResponse(
      processingDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString,
      relationshipDisplayResponse = response.relationship.map(relationship =>
        RelationshipDisplayResponse(
          refNumber = relationship.referenceNumber,
          arn = relationship.agentReferenceNumber,
          individual = relationship.individual,
          organisation = relationship.organisation,
          dateFrom = relationship.dateFrom,
          dateTo = relationship.dateTo.getOrElse(LocalDate.parse("9999-12-31")),
          contractAccountCategory = relationship.contractAccountCategory,
          activity = relationship.activity,
          relationshipType = relationship.relationshipType,
          authProfile = relationship.authProfile
        )
      )
    )

}
