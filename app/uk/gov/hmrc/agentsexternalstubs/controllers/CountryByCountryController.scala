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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentsexternalstubs.models.{CbcSubscriptionRecord, DisplaySubscriptionForCbC, DisplaySubscriptionForCbCRequestPayload}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GenericRecordsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class CountryByCountryController @Inject() (
  val authenticationService: AuthenticationService,
  genericRecordsService: GenericRecordsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with HttpHelpers with CurrentSession {

  /** MTDP -> (EIS -> ETMP) */
  def displaySubscriptionForCbC: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession(session =>
      withPayload[DisplaySubscriptionForCbCRequestPayload] { payload =>
        DisplaySubscriptionForCbCRequestPayload
          .validate(payload)
          .fold(
            errors => // add other error responses? 409, 503
              errorResponse(400, "Invalid JSON document", errors.toString()),
            _ => {
              val cbcId = payload.displaySubscriptionForCBCRequest.requestDetail.IDNumber
              genericRecordsService
                .getRecord[CbcSubscriptionRecord, CbcId](CbcId(cbcId), session.planetId)
                .flatMap(maybeRecord =>
                  maybeRecord.fold(
                    errorResponse(NOT_FOUND)
                  )(record =>
                    Future.successful(
                      Ok(Json.toJson(DisplaySubscriptionForCbC.fromRecord(record)))
                    )
                  )
                )
            }
          )
      }
    )(SessionRecordNotFound)
  }

  private def errorResponse(
    code: Int,
    message: String = "Record not found",
    detail: String = "Record not found"
  ) =
    findResource(s"/resources/country-by-country/error-response-template.json")
      .map(
        _.map(
          _.replaceAll("%%%ERROR_CODE%%%", code.toString)
            .replaceAll("%%%ERROR_MESSAGE%%%", message)
            .replaceAll("%%%ERROR_MESSAGE_DETAIL%%%", detail)
        )
      )
      .map(
        _.fold[Result](InternalServerError("error response not found"))(jsonStr =>
          code match {
            case NOT_FOUND   => NotFound(Json.parse(jsonStr))
            case BAD_REQUEST => BadRequest(Json.parse(jsonStr))
            case _           => InternalServerError(Json.parse(jsonStr))
          }
        )
      )

  private def findResource(resourcePath: String): Future[Option[String]] = Future {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(Source.fromInputStream(_).mkString)
  }

}
