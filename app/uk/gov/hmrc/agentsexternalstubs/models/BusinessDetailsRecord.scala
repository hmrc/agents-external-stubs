package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}

case class BusinessDetailsRecord(id: Option[String] = None) extends Record {

  import BusinessDetailsRecord._

  override def keys: Seq[String] = Seq()
  override def withId(id: Option[String]): BusinessDetailsRecord = copy(id = id)
}

object BusinessDetailsRecord {

  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]

}
