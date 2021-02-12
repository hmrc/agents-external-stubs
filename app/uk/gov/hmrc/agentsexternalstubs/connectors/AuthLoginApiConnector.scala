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
import play.api.http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.AuthLoginApi
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthLoginApiConnector @Inject() (appConfig: AppConfig, http: HttpPost with HttpGet) {

  def loginToGovernmentGateway(
    authLoginApiRequest: AuthLoginApi.Request
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[AuthLoginApi.Response] = {
    val url = new URL(appConfig.authLoginApiUrl + s"/government-gateway/session/login")
    for {
      response <- http.POST[AuthLoginApi.Request, HttpResponse](url.toString, authLoginApiRequest)
      token <- response.header(HeaderNames.AUTHORIZATION) match {
                 case None        => Future.failed(new Exception("Authorization header expected but missing"))
                 case Some(token) => Future.successful(token)
               }
    } yield AuthLoginApi.Response(token)

  }
}
