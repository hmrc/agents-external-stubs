/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentUserClientDetailsConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  val baseUrl = appConfig.agentUserClientDetailsUrl
  implicit val hc: HeaderCarrier = HeaderCarrier()
  def deleteTestData(arn: String, groupId: String): Future[Unit] =
    httpClientV2
      .delete(url"$baseUrl/test-only/agent-user-client-details/delete-test-data/arn/$arn/groupId/$groupId")
      .execute[HttpResponse]
      .map(_ => ())
}
