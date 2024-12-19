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

import play.api.libs.json.{Format, Json}

case class UpdateRelationshipPayload(
  regime: String,
  refNumber: String,
  idType: Option[String],
  arn: String,
  action: String,
  isExclusiveAgent: Boolean,
  relationshipType: Option[String],
  authProfile: Option[String]
)

object UpdateRelationshipPayload {
  implicit val format: Format[UpdateRelationshipPayload] = Json.format[UpdateRelationshipPayload]

  def toRelationshipRecord(payload: UpdateRelationshipPayload): RelationshipRecord =
    RelationshipRecord(
      regime = payload.regime,
      arn = payload.arn,
      idType = payload.idType.getOrElse(throw new Exception("regimes currently supported require idType.")),
      refNumber = payload.refNumber,
      relationshipType = payload.relationshipType,
      authProfile = payload.authProfile
    )
}
