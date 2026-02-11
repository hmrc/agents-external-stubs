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

package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, Writes}

import java.time.{Instant, LocalDateTime}

sealed trait SubscribeAgentServiceResponse

case class DesIfResponse(safeId: String, agentRegistrationNumber: String) extends SubscribeAgentServiceResponse

object DesIfResponse {
  implicit val writes: Writes[DesIfResponse] = Json.writes[DesIfResponse]
}

case class HipResponse(processingDate: LocalDateTime, arn: String) extends SubscribeAgentServiceResponse

object HipResponse {
  implicit val writes: Writes[HipResponse] = Json.writes[HipResponse]
}

case class HipAmendAgentSubscriptionResponse(processingDate: Instant)

object HipAmendAgentSubscriptionResponse {
  implicit val writes: Writes[HipAmendAgentSubscriptionResponse] = Json.writes[HipAmendAgentSubscriptionResponse]
}
