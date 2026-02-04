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

package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, OFormat}

case class HipAgentSubscriptionResponse(
  processingDate: String,
  agentSubscriptionDisplayResponse: Seq[AgentSubscriptionDisplayResponse]
)

object HipAgentSubscriptionResponse {
  implicit val format: OFormat[HipAgentSubscriptionResponse] = Json.format[HipAgentSubscriptionResponse]
}

case class AgentSubscriptionDisplayResponse(
  utr: Option[String],
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: String,
  phone: Option[String],
  email: String,
  suspensionStatus: Boolean,
  regimes: Option[Set[String]],
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: String,
  updateDetailsLastUpdated: String,
  updateDetailsLastSuccessfullyComplete: String,
  amlSupervisionUpdateStatus: String,
  amlSupervisionUpdateLastUpdated: String,
  amlSupervisionUpdateLastSuccessfullyComplete: String,
  directorPartnerUpdateStatus: String,
  directorPartnerUpdateLastUpdated: String,
  directorPartnerUpdateLastSuccessfullyComplete: String,
  acceptNewTermsStatus: String,
  acceptNewTermsLastUpdated: String,
  acceptNewTermsLastSuccessfullyComplete: String,
  reriskStatus: String,
  reriskLastUpdated: String,
  reriskLastSuccessfullyComplete: String
)

object AgentSubscriptionDisplayResponse {
  implicit val format: OFormat[AgentSubscriptionDisplayResponse] = Json.format[AgentSubscriptionDisplayResponse]
}
