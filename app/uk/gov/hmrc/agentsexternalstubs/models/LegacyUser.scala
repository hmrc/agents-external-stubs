/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.agentsexternalstubs.models.User.{AdditionalInformation, Address}
import uk.gov.hmrc.domain.Nino

case class LegacyUser(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  agentId: Option[String] = None,
  planetId: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty,
  suspendedRegimes: Option[Set[String]] = None
)

object LegacyUser {
  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaReads._

  import play.api.libs.functional.syntax._

  implicit val reads: Reads[LegacyUser] = (
    (JsPath \ "userId").readNullable[String].map(_.orNull) and
      (JsPath \ "groupId").readNullable[String] and
      (JsPath \ "affinityGroup").readNullable[String] and
      (JsPath \ "confidenceLevel").readNullable[Int] and
      (JsPath \ "credentialStrength").readNullable[String] and
      (JsPath \ "credentialRole").readNullable[String] and
      (JsPath \ "nino").readNullable[Nino] and
      (JsPath \ "principalEnrolments").read[Seq[Enrolment]] and
      (JsPath \ "delegatedEnrolments").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "dateOfBirth").readNullable[LocalDate] and
      (JsPath \ "agentCode").readNullable[String] and
      (JsPath \ "agentFriendlyName").readNullable[String] and
      (JsPath \ "agentId").readNullable[String] and
      (JsPath \ "planetId").readNullable[String] and
      (JsPath \ "isNonCompliant").readNullable[Boolean] and
      (JsPath \ "complianceIssues").readNullable[Seq[String]] and
      (JsPath \ "recordIds").readNullable[Seq[String]].map(_.map(_.distinct).getOrElse(Seq.empty)) and
      (JsPath \ "address").readNullable[Address] and
      (JsPath \ "additionalInformation").readNullable[AdditionalInformation] and
      (JsPath \ "strideRoles").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "suspendedRegimes").readNullable[Set[String]]
  )(LegacyUser.apply _)

  implicit val writes: Writes[LegacyUser] = Json.writes[LegacyUser]
}
