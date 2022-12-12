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

package uk.gov.hmrc.agentsexternalstubs.repository

import play.api.libs.json.{Format, JsObject, JsResult, JsValue, OFormat}

/** Utility type to represent an object to which we may want to add a few extra fields (that are not part of the model)
  * when serialising to Json. Typically used to insert extra fields like "updated", "keys", etc. that we want to store
  * into Mongo alongside the actual data but without having to pollute the core model with such fields.
  */
final case class JsonAbuse[+A: OFormat](
  value: A,
  extraFields: Map[String, JsValue] = Map.empty
) {
  def addField(fieldName: String, fieldValue: JsValue) =
    this.copy(extraFields = this.extraFields + (fieldName -> fieldValue))
}

object JsonAbuse {

  /** Format that turns the given type to Json and inserts the extra fields into the resulting object.
    * @param extractExtraFieldsOnRead: When deserialising from Json, shall we attempt to extract
    * the extra fields (if any) or only the core model?
    * 'false' means that these fields are 'one-way' only (i.e. we can write them but we can't read them back)
    * which is not a problem if we only need to use the extra fields in Mongo queries etc. which stay at Json level.
    * 'true' will make all these extra fields available after deserialising, at the cost of performance.
    */
  def format[A](extractExtraFieldsOnRead: Boolean = false)(implicit fa: OFormat[A]): Format[JsonAbuse[A]] =
    new OFormat[JsonAbuse[A]] {
      override def reads(json: JsValue): JsResult[JsonAbuse[A]] = for {
        value <- fa.reads(json)
        extraFields: Map[String, JsValue] = json match {
                                              case JsObject(fields) if extractExtraFieldsOnRead =>
                                                fields.toMap -- fa
                                                  .writes(value)
                                                  .keys // remove 'standard' fields from all fields
                                              case _ => Map.empty
                                            }
      } yield JsonAbuse(value, extraFields)

      override def writes(o: JsonAbuse[A]): JsObject =
        fa.writes(o.value) ++ JsObject(o.extraFields)
    }

  def fromJson[A](json: JsObject)(implicit fa: OFormat[A]): Option[JsonAbuse[A]] =
    format(true).reads(json).asOpt
}
