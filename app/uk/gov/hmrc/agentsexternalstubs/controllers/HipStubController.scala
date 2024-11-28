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

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result, Results}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.agentsexternalstubs.controllers.DesIfStubController.GetRelationships
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Instant, LocalDate}
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
    extends BackendController(cc) with DesCurrentSession {

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
                  def checkSuspension(arn: Arn): Future[Result] =
                    recordsService.getRecordMaybeExt[BusinessPartnerRecord, Arn](arn, session.planetId) map {
                      case Some(bpr) =>
                        bpr.suspensionDetails match {
                          case Some(sd) =>
                            if (sd.suspendedRegimes.contains(relationshipRecordQuery.regime)) {
                              Results.UnprocessableEntity(
                                Json.toJson(Errors("059", s"${arn.value} is currently suspended"))
                              )
                            } else {
                              Ok(Json.toJson(convertResponseToNewFormat(GetRelationships.Response.from(records))))
                            }
                          case None =>
                            Ok(Json.toJson(convertResponseToNewFormat(GetRelationships.Response.from(records))))
                        }
                      case None => notFound("INVALID_SUBMISSION", "No BusinessPartnerRecord found")
                    }
                  records.headOption match {
                    case Some(r) =>
                      checkSuspension(Arn(r.arn))
                    case None =>
                      if (relationshipRecordQuery.agent) {
                        checkSuspension(
                          Arn(relationshipRecordQuery.arn.getOrElse(throw new Exception("agent must have arn")))
                        )
                      } else {
                        Future.successful(
                          Ok(Json.toJson(convertResponseToNewFormat(GetRelationships.Response.from(records))))
                        )
                      }
                  }
                }
          }
      }
    }(SessionRecordNotFound)
  }

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
