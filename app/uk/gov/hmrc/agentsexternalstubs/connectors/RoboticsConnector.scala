/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoboticsConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  private val callbackUrl: String = appConfig.agentServicesAccountUrl
  implicit val hc: HeaderCarrier = HeaderCarrier()

  def sendCallback(payload: JsValue): Future[Unit] =
    httpClientV2
      .post(url"$callbackUrl/robotics/callback")
      .withBody(payload)
      .execute[HttpResponse]
      .map(_ => ())
}
