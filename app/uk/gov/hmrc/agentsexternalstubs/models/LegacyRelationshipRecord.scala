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

package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.Gen
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.identifiers.NinoWithoutSuffix

case class LegacyRelationshipRecord(
  agentId: String,
  nino: Option[String] = None,
  utr: Option[String] = None,
  id: Option[String] = None,
  `Auth_64-8`: Option[Boolean] = None,
  `Auth_i64-8`: Option[Boolean] = None
) extends Record {

  override def lookupKeys: Seq[String] =
    Seq(
      nino.toSeq.flatMap(LegacyRelationshipRecord.ninoKeys),
      utr.map(LegacyRelationshipRecord.utrKey).toSeq,
      Seq(LegacyRelationshipRecord.agentIdKey(agentId)),
      utr.map(LegacyRelationshipRecord.agentIdAndUtrKey(agentId, _)).toSeq
    ).flatten
  override def withId(id: Option[String]): LegacyRelationshipRecord = copy(id = id)
}

object LegacyRelationshipRecord extends RecordUtils[LegacyRelationshipRecord] {

  implicit val recordUtils: RecordUtils[LegacyRelationshipRecord] = this

  def agentIdKey(agentId: String): String = s"agentId:$agentId"
  def agentIdAndUtrKey(agentId: String, utr: String): String = s"agentId:$agentId;utr:${utr.replace(" ", "")}"
  def ninoKeys(nino: String): Seq[String] = NinoWithoutSuffix(nino).variations.map(variant => s"nino:$variant")
  def utrKey(utr: String): String = s"utr:${utr.replace(" ", "")}"

  import Validator._
  val validate: Validator[LegacyRelationshipRecord] = Validator(
    check(_.agentId.lengthMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.nino.isRight(RegexPatterns.validNinoNoSpaces), "Invalid nino"),
    check(_.utr.isRight(RegexPatterns.validUtr), "Invalid utr"),
    check(r => r.nino.isDefined || r.utr.isDefined, "Missing client identifier: nino or utr")
  )

  val writes: Writes[LegacyRelationshipRecord] = Json.writes[LegacyRelationshipRecord]

  import play.api.libs.functional.syntax._
  val reads: Reads[LegacyRelationshipRecord] =
    ((JsPath \ "agentId").read[String] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "id").readNullable[String] and
      (JsPath \ "Auth_64-8").readNullable[Boolean] and
      (JsPath \ "Auth_i64-8").readNullable[Boolean])(LegacyRelationshipRecord.apply _)

  implicit val formats: Format[LegacyRelationshipRecord] = Format(reads, writes)
  implicit val recordType: RecordMetaData[LegacyRelationshipRecord] =
    RecordMetaData[LegacyRelationshipRecord]

  val agentIdGen = Generator.pattern("999999")

  override val gen: Gen[LegacyRelationshipRecord] =
    for {
      agentId    <- agentIdGen
      auth_64_8  <- Generator.biasedOptionGen(Generator.booleanGen)
      auth_i64_8 <- Generator.biasedOptionGen(Generator.booleanGen)
    } yield LegacyRelationshipRecord(
      agentId = agentId,
      `Auth_64-8` = auth_64_8,
      `Auth_i64-8` = auth_i64_8
    )

  val ninoSanitizer: Update = seed => e => e.copy(nino = e.nino.orElse(Some(Generator.ninoNoSpaces(e.agentId).value)))
  val utrSanitizer: Update = seed => e => e.copy(utr = e.utr.orElse(Some(Generator.utr(e.agentId).value)))

  override val sanitizers: Seq[Update] = Seq(ninoSanitizer, utrSanitizer)
}
