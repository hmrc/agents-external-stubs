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
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.admin.UserGenerator
case class LegacyAgentRecord(
  agentId: String,
  agentOwnRef: Option[String] = None,
  isRegisteredAgent: Option[Boolean] = None,
  govAgentId: Option[String] = None,
  agentName: String,
  agentPhoneNo: Option[String] = None,
  address1: String,
  address2: String,
  address3: Option[String] = None,
  address4: Option[String] = None,
  postcode: Option[String] = None,
  isAgentAbroad: Boolean = false,
  agentCeasedDate: Option[String] = None,
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(agentId)
  override def lookupKeys: Seq[String] = Seq(LegacyAgentRecord.agentIdKey(agentId))

  override def withId(id: Option[String]): LegacyAgentRecord = copy(id = id)
}

object LegacyAgentRecord extends RecordUtils[LegacyAgentRecord] {

  def agentIdKey(agentId: String): String = s"agentId:$agentId"

  import Validator._

  val validate: Validator[LegacyAgentRecord] = Validator(
    check(_.agentId.lengthMinMaxInclusive(1, 6), "Invalid agentId"),
    check(_.agentOwnRef.lengthMinMaxInclusive(1, 20), "Invalid agentOwnRef"),
    check(_.govAgentId.lengthMinMaxInclusive(1, 12), "Invalid govAgentId"),
    check(_.agentName.lengthMinMaxInclusive(1, 56), "Invalid agentName"),
    check(_.agentPhoneNo.lengthMinMaxInclusive(1, 20), "Invalid agentPhoneNo"),
    check(_.address1.lengthMinMaxInclusive(1, 28), "Invalid address1"),
    check(_.address2.lengthMinMaxInclusive(1, 28), "Invalid address2"),
    check(_.address3.lengthMinMaxInclusive(1, 28), "Invalid address3"),
    check(_.address4.lengthMinMaxInclusive(1, 28), "Invalid address4"),
    check(_.postcode.isRight(RegexPatterns.validPostcode), "Invalid postcode")
  )

  implicit val formats: Format[LegacyAgentRecord] = Json.format[LegacyAgentRecord]
  implicit val recordType: RecordMetaData[LegacyAgentRecord] = RecordMetaData[LegacyAgentRecord](LegacyAgentRecord)

  val agentIdGen = Generator.pattern("999999")

  override val gen: Gen[LegacyAgentRecord] =
    for {
      agentId           <- agentIdGen
      agentName         <- UserGenerator.nameForAgentGen
      address1          <- Generator.address4Lines35Gen.map(_.line1.take(28))
      address2          <- Generator.address4Lines35Gen.map(_.line3.take(28))
      postcode          <- Generator.biasedOptionGen(Generator.postcode)
      isRegisteredAgent <- Generator.biasedOptionGen(Generator.booleanGen)
      agentCeasedDate   <- Generator.optionGen(Generator.date(1990, 2018).map(Generator.`date_yyyy-MM-dd`.format))
      agentPhoneNo      <- Generator.biasedOptionGen(Generator.ukPhoneNumber)
    } yield LegacyAgentRecord(
      agentId = agentId,
      agentName = agentName,
      address1 = address1,
      address2 = address2,
      postcode = postcode,
      isRegisteredAgent = isRegisteredAgent,
      agentCeasedDate = agentCeasedDate,
      agentPhoneNo = agentPhoneNo
    )

  val agentPhoneNoSanitizer: Update = seed =>
    e => e.copy(agentPhoneNo = e.agentPhoneNo.orElse(Generator.get(Generator.ukPhoneNumber)(seed)))

  val postcodeSanitizer: Update = seed =>
    e => e.copy(postcode = e.postcode.orElse(Generator.get(Generator.postcode)(seed)))

  override val sanitizers: Seq[Update] = Seq(agentPhoneNoSanitizer, postcodeSanitizer)
}
