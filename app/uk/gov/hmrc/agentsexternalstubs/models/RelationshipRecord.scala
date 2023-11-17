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
import uk.gov.hmrc.agentsexternalstubs.models.CreateUpdateAgentRelationshipPayload.Common

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class RelationshipRecord(
  regime: String,
  arn: String,
  idType: String,
  refNumber: String,
  active: Boolean = true,
  relationshipType: Option[String] = None,
  authProfile: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None,
  id: Option[String] = None
) extends Record {

  import RelationshipRecord._

  override def lookupKeys: Seq[String] =
    Seq(
      fullKey(regime, arn, idType, refNumber),
      agentKey(regime, arn),
      clientKey(regime, idType, refNumber),
      agentClientKey(arn, idType, refNumber),
      regime,
      arn,
      refNumber
    )

  override def withId(id: Option[String]): RelationshipRecord = copy(id = id)
}

object RelationshipRecord extends RecordUtils[RelationshipRecord] {

  implicit val recordUtils: RecordUtils[RelationshipRecord] = this

  implicit val formats: Format[RelationshipRecord] = Json.format[RelationshipRecord]
  implicit val recordType: RecordMetaData[RelationshipRecord] = RecordMetaData[RelationshipRecord]

  def fullKey(regime: String, arn: String, idType: String, refNumber: String): String =
    s"FK/$regime/$arn/$idType/$refNumber"

  def agentKey(regime: String, arn: String): String = s"AK/$regime/$arn"

  def agentKeys(arn: String): Seq[String] =
    Seq(s"AK/itsa/$arn", s"AK/vatc/$arn", s"AK/trs/$arn", s"AK/cgt/$arn", s"AK/ppt/$arn", s"AK/cbc/$arn")

  def clientKey(regime: String, idType: String, refNumber: String): String =
    s"CK/$regime/$idType/$refNumber"

  def agentClientKey(arn: String, idType: String, refNumber: String): String =
    s"ACK/$arn/$idType/$refNumber"

  override val gen: Gen[RelationshipRecord] = for {
    regime <- Gen.oneOf("ITSA", "VATC", "NI")
    arn    <- Generator.arnGen
    idType <- regime match {
                case "VATC" => Gen.const("vrn")
                case "NI"   => Gen.const("eori")
                case _      => Gen.const("mtdbsa")
              }
    refNumber <- regime match {
                   case "VATC" => Generator.vrnGen
                   case "NI"   => Generator.eoriGen
                   case _      => Generator.mtdbsaGen
                 }
    active <- Generator.booleanGen
  } yield RelationshipRecord(regime, arn, idType, refNumber, active)

  import Validator._

  val startDateSanitizer: Update = seed =>
    entity =>
      entity.copy(
        startDate =
          entity.startDate.orElse(Generator.get(Generator.date(1970, 2018).map(d => LocalDate.parse(d.toString)))(seed))
      )

  val endDateSanitizer: Update = seed =>
    entity =>
      if (entity.active) entity
      else
        entity.copy(
          endDate = entity.endDate.orElse(
            Generator.get(
              Generator
                .date(
                  entity.startDate.map(_.format(DateTimeFormatter.ofPattern("yyyy"))).getOrElse("1980").toInt - 1,
                  2018
                )
                .map(d => LocalDate.parse(d.toString))
            )(seed)
          )
        )

  val itsaRegimeSanitizer: Update = seed =>
    entity => entity.copy(regime = "ITSA", authProfile = None, relationshipType = None)

  val vatcRegimeSanitizer: Update = seed =>
    entity =>
      entity.copy(
        regime = "VATC",
        authProfile = Generator.get(Gen.oneOf(Common.authProfileEnum))(seed),
        relationshipType = Generator.get(Gen.oneOf(Common.relationshipTypeEnum))(seed)
      )

  val regimeSanitizer: Update = seed =>
    entity =>
      entity.regime match {
        case "ITSA" => itsaRegimeSanitizer(seed)(entity)
        case "VATC" => vatcRegimeSanitizer(seed)(entity)
        case _      => entity
      }

  override val sanitizers: Seq[Update] = Seq(
    startDateSanitizer,
    endDateSanitizer,
    regimeSanitizer
  )

  val validate: Validator[RelationshipRecord] = Validator(
    check(
      _.refNumber.matches(Common.refNumberPattern),
      s"""Invalid refNumber, does not matches regex ${Common.refNumberPattern}"""
    ),
    check(
      _.idType.matches(Common.idTypePattern),
      s"""Invalid idType, does not matches regex ${Common.idTypePattern}"""
    ),
    check(
      _.arn.matches(Common.agentReferenceNumberPattern),
      s"""Invalid agentReferenceNumber, does not matches regex ${Common.agentReferenceNumberPattern}"""
    ),
    check(
      _.regime.matches(Common.regimePattern),
      s"""Invalid regime, does not matches regex ${Common.regimePattern}"""
    ),
    check(
      _.relationshipType.isOneOf(Common.relationshipTypeEnum),
      "Invalid relationshipType, does not match allowed values"
    ),
    check(_.authProfile.isOneOf(Common.authProfileEnum), "Invalid authProfile, does not match allowed values")
  )

}
