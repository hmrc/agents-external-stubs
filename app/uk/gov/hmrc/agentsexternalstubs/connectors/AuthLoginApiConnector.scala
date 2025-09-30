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

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.models.AuthLoginApi
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthLoginApiConnector @Inject() (appConfig: AppConfig, http: HttpClientV2) {

  def loginToGovernmentGateway(
    authLoginApiRequest: AuthLoginApi.Request
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[AuthLoginApi.Response] = {
    val url = appConfig.authLoginApiUrl + s"/government-gateway/session/login"
    for {
      response <-
        http
          .post(url"$url")
          .withBody(Json.toJson(authLoginApiRequest))
          .execute[HttpResponse]
      token <-
        response.header(HeaderNames.AUTHORIZATION) match {
          case None =>
            Future.failed(
              new Exception(
                "AUTHORIZATION header expected but missing on /government-gateway/session/login response"
              )
            )
          case Some(token) => Future.successful(token)
        }
      sessionAuthorityUri <-
        response.header(HeaderNames.LOCATION) match {
          case None =>
            Future.failed(
              new Exception(
                "LOCATION header expected but missing on /government-gateway/session/login response"
              )
            )
          case Some(uri) => Future.successful(uri)
        }
    } yield AuthLoginApi.Response(token, sessionAuthorityUri)

  }
}
