package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json._

case class Link(rel: String, href: String)

object Link {
  implicit val formats: Format[Link] = Json.format[Link]
}

object RestfulResponse {

  def apply[E](entity: E, links: Link*)(implicit writes: Writes[E]): JsValue =
    writes.writes(entity) match {
      case obj: JsObject => obj ++ Json.obj("_links" -> links)
      case arr: JsArray  => arr
      case _             => throw new IllegalStateException("Json object expected")
    }

  def apply(links: Link*): JsValue =
    Json.obj("_links" -> links)
}

case class Links(`_links`: Seq[Link]) {
  def rel(rel: String): Option[String] = `_links`.find(_.rel == rel).map(_.href)
}

object Links {
  implicit val reads: Reads[Links] = Json.reads[Links]
}
