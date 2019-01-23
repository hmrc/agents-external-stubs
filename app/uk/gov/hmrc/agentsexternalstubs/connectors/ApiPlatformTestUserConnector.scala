/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiPlatformTestUserConnector @Inject()(@Named("api-platform-test-user-baseUrl") baseUrl: URL, http: HttpGet) {

  def getIndividualUserByNino(nino: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(baseUrl, s"/individuals/nino/$nino"))

  def getIndividualUserBySaUtr(
    saUtr: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(baseUrl, s"/individuals/sautr/$saUtr"))

  def getIndividualUserByVrn(vrn: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(baseUrl, s"/individuals/vrn/$vrn"))

  def getOrganisationUserByEmpRef(
    empRef: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(baseUrl, s"/organisations/empref/$empRef"))

  def getOrganisationUserByVrn(vrn: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    getUser(new URL(baseUrl, s"/organisations/vrn/$vrn"))

  private def getUser(url: URL)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TestUser]] =
    http.GET[TestUser](url.toString).map(Some.apply).recover {
      case _: NotFoundException => None
    }

}
