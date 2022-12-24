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
import com.kenshoo.play.metrics.Metrics

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
class CitizenDetailsConnector @Inject() (appConfig: AppConfig, http: HttpGet with HttpDelete, metrics: Metrics) {

  def getCitizenDateOfBirth(
    nino: Nino
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[CitizenDateOfBirth]] = {
    val url = new URL(appConfig.citizenDetailsUrl + s"/citizen-details/nino/${nino.value}")
    http.GET[Option[CitizenDateOfBirth]](url.toString).recover { case _ =>
      None
    }
  }
}
