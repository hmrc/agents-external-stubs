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

package uk.gov.hmrc.agentsexternalstubs.models.admin

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc.{ResponseHeader, Result}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{Id, Validator}

import java.net.URLEncoder

case class SpecialCase(
  requestMatch: SpecialCase.RequestMatch,
  response: SpecialCase.Response,
  planetId: Option[String] = None,
  id: Option[Id] = None
)

object SpecialCase {

  final val UNIQUE_KEY = "_key"

  case class RequestMatch(
    path: String,
    method: String = "GET",
    body: Option[String] = None,
    contentType: Option[String] = None
  ) {

    val toKey = SpecialCase.matchKey(method, path)
  }

  case class Header(name: String, value: String)

  case class Response(status: Int, body: Option[String] = None, headers: Seq[Header] = Seq.empty) {

    def asResult: Result =
      Result(
        header = ResponseHeader(status, headers.map(h => h.name -> h.value).toMap),
        body = body
          .map(b => HttpEntity.Strict(ByteString(b), headers.find(_.name == HeaderNames.CONTENT_TYPE).map(_.value)))
          .getOrElse(HttpEntity.NoEntity)
      )

  }

  def uniqueKey(key: String, planetId: String): String = s"$key@$planetId"
  def matchKey(method: String, path: String): String =
    s"$method ${path.split("/").map(URLEncoder.encode(_, "utf-8")).mkString("/")}"

  import Validator._

  val validate: Validator[SpecialCase] = Validator(
    check(
      _.requestMatch.method.isOneOf(Seq("GET", "POST", "PUT", "DELETE")),
      "Request match method must be one of GET, POST, PUT or DELETE"
    ),
    check(
      _.requestMatch.contentType.isOneOf(Seq("json", "form", "text")),
      "Request match contentType must be one of json, form, text"
    )
  )

  import play.api.libs.functional.syntax._

  implicit val formats1: Format[RequestMatch] = Json.format[RequestMatch]
  implicit val formats2: Format[Header] = Json.format[Header]

  val responseReads: Reads[Response] = ((JsPath \ "status").read[Int] and
    (JsPath \ "body").readNullable[String] and
    (JsPath \ "headers")
      .readNullable[Seq[Header]]
      .map(_.getOrElse(Seq.empty)))(Response.apply _)
  implicit val responseFormats: Format[Response] = Format(responseReads, Json.writes[Response])

  object internal {

    implicit val idFormats: Format[Id] = Id.internalFormats

    val reads: Reads[SpecialCase] =
      ((JsPath \ "requestMatch").read[RequestMatch] and
        (JsPath \ "response").read[Response] and
        (JsPath \ "planetId").readNullable[String] and
        (JsPath \ "_id").readNullable[Id])(SpecialCase.apply _)

    type Transformer = JsObject => JsObject

    private def planetIdOf(json: JsObject): String =
      (json \ "planetId").asOpt[String].getOrElse(Planet.DEFAULT)

    private final val addUniqueKey: Transformer = json => {
      val key = (json \ "requestMatch").as[RequestMatch].toKey
      val planetId = planetIdOf(json)
      json + ((UNIQUE_KEY, JsString(uniqueKey(key, planetId))))
    }

    private final val renameId: Transformer = json => {
      (json \ "id").asOpt[JsObject] match {
        case None     => json
        case Some(id) => json.-("id").+(Id.ID -> id)
      }
    }

    val writes: OWrites[SpecialCase] = Json
      .writes[SpecialCase]
      .transform(addUniqueKey)
      .transform(renameId)
  }

  object external {

    implicit val idFormats: Format[Id] = Id.externalFormats
    val reads: Reads[SpecialCase] = Json.reads[SpecialCase]
    val writes: OWrites[SpecialCase] = Json.writes[SpecialCase]
  }
}
