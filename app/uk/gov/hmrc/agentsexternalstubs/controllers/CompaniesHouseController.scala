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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentsexternalstubs.models.Generator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class CompaniesHouseController @Inject() (cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def findCompany(companyNumber: String): Action[AnyContent] = Action.async {
    companyResponse(companyNumber)
  }

  def findCompanyOfficers(companyNumber: String, surname: Option[String]): Action[AnyContent] = Action.async {
    companyOfficersResponse(companyNumber, surname.getOrElse(Generator.surname.sample.get).toUpperCase)
  }

  private def companyResponse(companyNumber: String): Future[Result] =
    // Based on https://github.com/hmrc/iv-test-data/tree/main/conf/resources/company-house/company
    findResource(s"/resources/companies-house/company-template.json")
      .map(
        _.map(
          _.replaceAll("%%%COMPANY_NUMBER%%%", companyNumber)
            .replaceAll("%%%COMPANY_NAME%%%", Generator.company.sample.get)
        )
      )
      .map(_.fold[Result](NotFound)(jsonStr => Ok(Json.parse(jsonStr))))

  private def companyOfficersResponse(companyNumber: String, surname: String): Future[Result] =
    // Based on https://github.com/hmrc/iv-test-data/tree/main/conf/resources/company-house/officers
    findResource(s"/resources/companies-house/company-officers-template.json")
      .map(
        _.map(
          _.replaceAll("%%%COMPANY_NUMBER%%%", companyNumber)
            .replaceAll("%%%SURNAME%%%", surname)
        )
      )
      .map(_.fold[Result](NotFound)(jsonStr => Ok(Json.parse(jsonStr))))

  private def findResource(resourcePath: String): Future[Option[String]] = Future {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(Source.fromInputStream(_).mkString)
  }

}
