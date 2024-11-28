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
import uk.gov.hmrc.agentsexternalstubs.controllers.DesIfStubController.GetRelationships.{Individual, Organisation}

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter

case class AgentRelationshipDisplayResponse(
  processingDate: String,
  relationshipDisplayResponse: Seq[RelationshipDisplayResponse]
)

object AgentRelationshipDisplayResponse {
  implicit val format: OFormat[AgentRelationshipDisplayResponse] = Json.format[AgentRelationshipDisplayResponse]
}

case class RelationshipDisplayResponse(
  refNumber: String,
  arn: String,
  individual: Option[Individual],
  organisation: Option[Organisation],
  dateFrom: LocalDate,
  dateTo: LocalDate,
  contractAccountCategory: String,
  activity: Option[String],
  relationshipType: Option[String],
  authProfile: Option[String]
)

object RelationshipDisplayResponse {
  implicit val format: OFormat[RelationshipDisplayResponse] = Json.format[RelationshipDisplayResponse]
}

case class Errors(
  code: String = "",
  text: String = "",
  processingDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
)

object Errors {
  implicit val formats: OFormat[Errors] = Json.format[Errors]
}
