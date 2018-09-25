package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class Identifier(key: String, value: String) {
  override def toString: String = s"${key.toUpperCase}~${value.replace(" ", "")}"
}

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
  implicit val ordering: Ordering[Identifier] = Ordering.by(_.key)
}
