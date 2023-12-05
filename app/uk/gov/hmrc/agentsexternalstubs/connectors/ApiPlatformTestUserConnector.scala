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

package uk.gov.hmrc.agentsexternalstubs.connectors

import play.api.http.Status.OK
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http._

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class ApiPlatformTestUserConnector @Inject() (appConfig: AppConfig, http: HttpGet) {

  def getIndividualUserByNino(nino: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(appConfig.apiPlatformTestUserUrl + s"/individuals/nino/$nino"))

  def getIndividualUserBySaUtr(
    saUtr: String
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(appConfig.apiPlatformTestUserUrl + s"/individuals/sautr/$saUtr"))

  def getIndividualUserByVrn(vrn: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(appConfig.apiPlatformTestUserUrl + s"/individuals/vrn/$vrn"))

  def getOrganisationUserByEmpRef(
    empRef: String
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(appConfig.apiPlatformTestUserUrl + s"/organisations/empref/$empRef"))

  def getOrganisationUserByVrn(vrn: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(appConfig.apiPlatformTestUserUrl + s"/organisations/vrn/$vrn"))

  private def getUser(url: URL)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    http
      .GET[HttpResponse](url.toString)
      .map(response =>
        response.status match {
          case OK => Option.apply(response.json.as[TestUser])
          case _  => None
        }
      )

}
