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
import uk.gov.hmrc.agentsexternalstubs.models.{DisplaySubscriptionForCbCRequestPayload, Generator}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class CountryByCountryController @Inject() (cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with HttpHelpers {

  def generateCbcRecord(): Unit = {}

  /** MTDP -> EIS -> ETMP
    */
  def displaySubscriptionForCbC: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[DisplaySubscriptionForCbCRequestPayload] { payload =>
      if (CbcId.isValid(payload.displaySubscriptionForCbCRequest.requestDetail.IDNumber)) {
        response(payload.displaySubscriptionForCbCRequest.requestDetail.IDNumber)
      } else {
        Future.successful(BadRequest("invalid cbcId"))
      }
    }
  }

  private def response(cbcId: String): Future[Result] =
    findResource(s"/resources/country-by-country/full-response-template.json")
      .map(
        _.map(
          _.replaceAll("%%%COUNTRY_BY_COUNTRY_ID%%%", cbcId)
            .replaceAll("%%%TRADING_NAME%%%", Generator.company.sample.get)
        )
      )
      .map(_.fold[Result](NotFound)(jsonStr => Ok(Json.parse(jsonStr))))

  private def findResource(resourcePath: String): Future[Option[String]] = Future {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(Source.fromInputStream(_).mkString)
  }

}
