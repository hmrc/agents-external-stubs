/*
 * Copyright 2021 HM Revenue & Customs
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

case class Id(value: String)

object Id {

  final val ID = "_id"

  object internal {

    val reads: Reads[Id] = new Reads[Id] {
      override def reads(json: JsValue): JsResult[Id] = json match {
        case obj: JsObject =>
          (obj \ "$oid").asOpt[String] match {
            case Some(id) => JsSuccess(Id(id))
            case None     => JsError(s"Could not parse Id from $obj")
          }

        case o => JsError(s"Could not Id from $o, must be JsObject.")
      }
    }

    val writes: Writes[Id] = new Writes[Id] {
      override def writes(id: Id): JsValue =
        Json.obj("$oid" -> JsString(id.value))
    }
  }

  val internalFormats = Format(internal.reads, internal.writes)

  object external {

    val reads: Reads[Id] = new Reads[Id] {
      override def reads(json: JsValue): JsResult[Id] = json match {
        case s: JsString => JsSuccess(Id(s.value))
        case o           => JsError(s"Could not Id from $o, must be JsString.")
      }
    }

    val writes: Writes[Id] = new Writes[Id] {
      override def writes(id: Id): JsValue = JsString(id.value)
    }
  }

  val externalFormats = Format(external.reads, external.writes)
}
