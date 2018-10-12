package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json._

case class Id(value: String)

object Id {

  final val ID = "_id"

  implicit val reads: Reads[Id] = new Reads[Id] {
    override def reads(json: JsValue): JsResult[Id] = json match {
      case obj: JsObject =>
        (obj \ "$oid").asOpt[String] match {
          case Some(id) => JsSuccess(Id(id))
          case None     => JsError(s"Could not parse Id from $obj")
        }

      case o => JsError(s"Could not Id from $o, must be JsObject.")
    }
  }

  implicit val writes: Writes[Id] = new Writes[Id] {
    override def writes(id: Id): JsValue =
      Json.obj("$oid" -> JsString(id.value))
  }
}
