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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentPermissionsConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  val baseUrl = appConfig.agentPermissionsUrl
  implicit val hc: HeaderCarrier = HeaderCarrier()
  def deleteTestData(arn: String): Future[Unit] =
    httpClientV2
      .delete(new URL(s"$baseUrl/test-only/agent-permissions/delete-test-data/arn/$arn"))
      .execute[HttpResponse]
      .map(_ => ())

}
