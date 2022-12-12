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

import play.api.libs.json._
import shapeless.record
import uk.gov.hmrc.agentsexternalstubs.syntax.|>

trait Record {

  val id: Option[String]
  def withId(id: Option[String]): Record

  def lookupKeys: Seq[String]
  def uniqueKey: Option[String] = None
}

object Record {

  final val ID = "_id"
  final val TYPE = "_record_type"

  final val reads: Reads[Record] = new Reads[Record] {
    override def reads(json: JsValue): JsResult[Record] = json match {
      case obj: JsObject =>
        ((obj \ TYPE).asOpt[String] match {
          case Some(typeName) => fromJson(typeName, obj)
          case None           => JsError("Missing record type field")
        }).map(_.withId((obj \ ID \ "$oid").asOpt[String]))

      case o => JsError(s"Cannot parse Record from $o, must be JsObject.")
    }
  }

  final val writes: OWrites[Record] = new OWrites[Record] {

    override def writes(record: Record): JsObject =
      toJson(record) match {
        case obj: JsObject =>
          obj
            .-("id")
            .|> { obj =>
              record.id
                .map(id => obj.+(ID -> Json.obj("$oid" -> JsString(id))))
                .getOrElse(obj)
            }
        case o => throw new IllegalStateException(s"Record must be serialized to JsObject, got $o instead")
      }
  }

  final def typeOf(r: Record): String = r.getClass.getSimpleName

  final def toJson(r: Record): JsValue = r match {
    case r: RelationshipRecord           => RelationshipRecord.formats.writes(r)
    case r: LegacyAgentRecord            => LegacyAgentRecord.formats.writes(r)
    case r: LegacyRelationshipRecord     => LegacyRelationshipRecord.formats.writes(r)
    case r: BusinessDetailsRecord        => BusinessDetailsRecord.formats.writes(r)
    case r: VatCustomerInformationRecord => VatCustomerInformationRecord.formats.writes(r)
    case r: BusinessPartnerRecord        => BusinessPartnerRecord.formats.writes(r)
    case r: EmployerAuths                => EmployerAuths.formats.writes(r)
    case r: PPTSubscriptionDisplayRecord => PPTSubscriptionDisplayRecord.formats.writes(r)
    case _                               => throw new UnsupportedOperationException(s"Cannot serialize $record")
  }

  final def fromJson(typeName: String, json: JsValue): JsResult[Record] = typeName match {
    case "RelationshipRecord"           => RelationshipRecord.formats.reads(json)
    case "LegacyAgentRecord"            => LegacyAgentRecord.formats.reads(json)
    case "LegacyRelationshipRecord"     => LegacyRelationshipRecord.formats.reads(json)
    case "BusinessDetailsRecord"        => BusinessDetailsRecord.formats.reads(json)
    case "VatCustomerInformationRecord" => VatCustomerInformationRecord.formats.reads(json)
    case "BusinessPartnerRecord"        => BusinessPartnerRecord.formats.reads(json)
    case "EmployerAuths"                => EmployerAuths.formats.reads(json)
    case "PPTSubscriptionDisplayRecord" => PPTSubscriptionDisplayRecord.formats.reads(json)
    case other                          => JsError(s"Record type $other not supported")
  }

  implicit val formats: OFormat[Record] = OFormat[Record](reads, writes)

}
