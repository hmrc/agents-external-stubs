package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class KnownFact(key: String, value: String) {
  override def toString: String = s"${key.toUpperCase}~$value"
}

object KnownFact {
  implicit val formats: Format[KnownFact] = Json.format[KnownFact]
  implicit val ordering: Ordering[KnownFact] = Ordering.by(_.key)
}
