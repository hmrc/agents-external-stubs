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
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

case class GroupInfo(groupId: String, affinityGroup: Option[String], agentCode: Option[AgentCode])

object GroupInfo {
  implicit val formats: Format[GroupInfo] = Json.format[GroupInfo]
}

@Singleton
class UsersGroupsSearchConnector @Inject() (appConfig: AppConfig, httpGet: HttpGet, metrics: Metrics) {

  def getGroupInfo(groupId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GroupInfo] = {
    val url = new URL(appConfig.usersGroupsSearchUrl + s"/users-groups-search/groups/$groupId")
    httpGet.GET[GroupInfo](url.toString)
  }
}
