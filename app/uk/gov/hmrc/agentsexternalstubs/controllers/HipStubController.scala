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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result, Results}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.agentsexternalstubs.controllers.DesIfStubController.GetRelationships
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipStubController @Inject() (
  hipStubService: HipStubService,
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  recordsService: RecordsService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) with DesCurrentSession with Logging {

  def displayAgentRelationship: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateHeaders(
        request.headers.get("X-Transmitting-System"),
        request.headers.get("X-Originating-System"),
        request.headers.get("correlationid"),
        request.headers.get("X-Receipt-Date")
      ) match {
        case Left(invalidHeadersResponse) =>
          Future.successful(Results.UnprocessableEntity(Json.toJson(invalidHeadersResponse)))
        case _ =>
          hipStubService.processQueryParameters(
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

  def updateAgentRelationship: Action[JsValue] = Action(parse.json).async { implicit request =>
    withCurrentSession { session =>
      hipStubService.validateHeaders(
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
                        else UnprocessableEntity(Json.toJson(Errors("014", "No active relationship")))
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

  private def agentIsSuspended(businessPartnerRecord: BusinessPartnerRecord, regime: String): Boolean =
    businessPartnerRecord.suspensionDetails.fold(false)(_.suspendedRegimes.contains(regime))

  private val withProcessingDate = Json.parse(s"""{"processingDate": "${LocalDateTime.now()}"}""")
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
