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

import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class CitizenDateOfBirth(dateOfBirth: Option[LocalDate])

object CitizenDateOfBirth {
  val format = DateTimeFormatter.ofPattern("ddMMyyyy")
  implicit val reads: Reads[CitizenDateOfBirth] =
    (JsPath \ "dateOfBirth")
      .readNullable[String]
      .map {
        case Some(dob) => CitizenDateOfBirth(Some(LocalDate.parse(dob, format)))
        case None      => CitizenDateOfBirth(None)
      }
}

@Singleton
class CitizenDetailsConnector @Inject() (appConfig: AppConfig, http: HttpClientV2, metrics: Metrics) {

  def getCitizenDateOfBirth(
    nino: Nino
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[CitizenDateOfBirth]] = {
    val url = appConfig.citizenDetailsUrl + s"/citizen-details/nino/${nino.value}"
    http
      .get(url"$url")
      .execute[Option[CitizenDateOfBirth]]
      .recover { case _ => None }

  }
}
