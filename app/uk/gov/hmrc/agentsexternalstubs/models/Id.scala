package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json._

case class Id(value: String)

object Id {

  final val ID = "_id"

  object internal {

    val reads: Reads[Id] = new Reads[Id] {
      override def reads(json: JsValue): JsResult[Id] = json match {
        case obj: JsObject =>
          (obj \ "$oid").asOpt[String] match {
            case Some(id) => JsSuccess(Id(id))
            case None     => JsError(s"Could not parse Id from $obj")
          }

        case o => JsError(s"Could not Id from $o, must be JsObject.")
      }
    }

    val writes: Writes[Id] = new Writes[Id] {
      override def writes(id: Id): JsValue =
        Json.obj("$oid" -> JsString(id.value))
    }
  }

  val internalFormats = Format(internal.reads, internal.writes)

  object external {

    val reads: Reads[Id] = new Reads[Id] {
      override def reads(json: JsValue): JsResult[Id] = json match {
        case s: JsString => JsSuccess(Id(s.value))
        case o           => JsError(s"Could not Id from $o, must be JsString.")
      }
    }

    val writes: Writes[Id] = new Writes[Id] {
      override def writes(id: Id): JsValue = JsString(id.value)
    }
  }

  val externalFormats = Format(external.reads, external.writes)
}
