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

package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json._

case class Link(rel: String, href: String)

object Link {
  implicit val formats: Format[Link] = Json.format[Link]
}

object RestfulResponse {

  def apply[E](entity: E, links: Link*)(implicit writes: Writes[E]): JsValue =
    writes.writes(entity) match {
      case obj: JsObject => if (links.isEmpty) obj else obj ++ Json.obj("_links" -> links)
      case arr: JsArray  => arr
      case _             => throw new IllegalStateException("Json object expected")
    }

  def apply(links: Link*): JsValue =
    Json.obj("_links" -> links)
}

case class Links(`_links`: Seq[Link]) {

  def rel(rel: String): Option[String] = `_links`.find(_.rel == rel).map(_.href)

  def self: Option[String] = rel("self")
}

object Links {
  implicit val reads: Reads[Links] = Json.reads[Links]
}
