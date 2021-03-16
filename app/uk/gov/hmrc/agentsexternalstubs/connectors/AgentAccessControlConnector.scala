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

package uk.gov.hmrc.agentsexternalstubs.connectors

import java.net.URL

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentAccessControlConnector @Inject() (appConfig: AppConfig, http: HttpGet) {

  def isAuthorisedForPaye(agentCode: String, empRef: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(
      new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/epaye-auth/agent/$agentCode/client/$empRef")
    )

  def isAuthorisedForSa(agentCode: String, saUtr: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/sa-auth/agent/$agentCode/client/$saUtr"))

  def isAuthorisedForMtdIt(agentCode: String, mtdItId: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(
      new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/mtd-it-auth/agent/$agentCode/client/$mtdItId")
    )

  def isAuthorisedForMtdVat(agentCode: String, vrn: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/mtd-vat-auth/agent/$agentCode/client/$vrn"))

  def isAuthorisedForAfi(agentCode: String, nino: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/afi-auth/agent/$agentCode/client/$nino"))

  def isAuthorisedForTrust(agentCode: String, utr: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/trust-auth/agent/$agentCode/client/$utr"))

  def isAuthorisedForCgt(agentCode: String, cgtRef: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    check(new URL(appConfig.agentAccessControlUrl + s"/agent-access-control/cgt-auth/agent/$agentCode/client/$cgtRef"))

  private def check(url: URL)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    http
      .GET[HttpResponse](url.toString)
      .map(_ => true)
      .recover { case Upstream4xxResponse(_, 401, _, _) =>
        false
      }

}
