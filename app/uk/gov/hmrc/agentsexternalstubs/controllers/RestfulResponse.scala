package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json._

case class Link(rel: String, link: String)

object Link {
  implicit val linkWrites: Writes[Link] = Json.writes[Link]
}

object RestfulResponse {

  def apply[E](entity: E, links: Link*)(implicit writes: Writes[E]): JsValue =
    writes.writes(entity) match {
      case obj: JsObject => obj ++ Json.obj("_links" -> links)
      case arr: JsArray  => arr
      case _             => throw new IllegalStateException("Json object expected")
    }
}
