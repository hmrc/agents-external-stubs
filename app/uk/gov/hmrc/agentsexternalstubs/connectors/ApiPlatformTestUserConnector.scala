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
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.agentsexternalstubs.models.AG
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

@Singleton
class ApiPlatformTestUserConnector @Inject() (appConfig: AppConfig, http: HttpClientV2) {

  def getIndividualUserByNino(nino: String)(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/individuals/nino/$nino", AG.Individual)

  def getIndividualUserByShortNino(
    nino: String
  )(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/individuals/shortnino/$nino", AG.Individual)

  def getIndividualUserBySaUtr(
    saUtr: String
  )(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/individuals/sautr/$saUtr", AG.Individual)

  def getIndividualUserByVrn(vrn: String)(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/individuals/vrn/$vrn", AG.Individual)

  def getOrganisationUserByEmpRef(
    empRef: String
  )(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/organisations/empref/$empRef", AG.Organisation)

  def getOrganisationUserByVrn(vrn: String)(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/organisations/vrn/$vrn", AG.Organisation)

  def getOrganisationUserByPillar2Id(
    pillar2Id: String
  )(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(appConfig.apiPlatformTestUserUrl + s"/organisations/pillar2Id/$pillar2Id", AG.Organisation)

  private def getUser(
    url: String,
    affinityGroup: String
  )(using c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    http
      .get(url"$url")
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case OK =>
            val payload = response.json.as[JsObject] + ("affinityGroup" -> JsString(affinityGroup))
            Option.apply(payload.as[TestUser])
          case _ => None
        }
      )

}
