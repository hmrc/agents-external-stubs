package uk.gov.hmrc.agentsexternalstubs.models
import java.net.URLEncoder

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc.{ResponseHeader, Result}
import play.mvc.Http.HeaderNames

case class SpecialCase(
  requestMatch: SpecialCase.RequestMatch,
  response: SpecialCase.Response,
  planetId: Option[String] = None,
  id: Option[Id] = None)

object SpecialCase {

  final val UNIQUE_KEY = "_key"

  case class RequestMatch(
    path: String,
    method: String = "GET",
    body: Option[String] = None,
    contentType: Option[String] = None) {

    val toKey = SpecialCase.matchKey(method, path)
  }

  case class Header(key: String, value: String)

  case class Response(status: Int, body: Option[String] = None, headers: Seq[Header] = Seq.empty) {

    def asResult: Result =
      Result(
        header = ResponseHeader(status, headers.map(h => h.key -> h.value).toMap),
        body = body
          .map(b => HttpEntity.Strict(ByteString(b), headers.find(_.key == HeaderNames.CONTENT_TYPE).map(_.value)))
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
      "Request match method must be one of GET, POST, PUT or DELETE"),
    check(
      _.requestMatch.contentType.isOneOf(Seq("json", "form", "text")),
      "Request match contentType must be one of json, form, text")
  )

  implicit val formats1: Format[RequestMatch] = Json.format[RequestMatch]
  implicit val formats2: Format[Header] = Json.format[Header]
  implicit val formats3: Format[Response] = Json.format[Response]

  object internal {

    implicit val idFormats: Format[Id] = Id.internalFormats

    import play.api.libs.functional.syntax._
    val reads: Reads[SpecialCase] =
      ((JsPath \ "requestMatch").read[RequestMatch] and
        (JsPath \ "response").read[Response] and
        (JsPath \ "planetId").readNullable[String] and
        (JsPath \ "_id").readNullable[Id])((a, b, c, d) => SpecialCase.apply(a, b, c, d))

    type Transformer = JsObject => JsObject

    private def planetIdOf(json: JsObject): String =
      (json \ "planetId").asOpt[String].getOrElse("hmrc")

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
