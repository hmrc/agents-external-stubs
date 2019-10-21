package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats._

case class NinoClStoreEntry(
  credId: String,
  nino: Nino,
  confidenceLevel: Option[ConfidenceLevel],
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime])

object NinoClStoreEntry {

  implicit val ninoCLStoreEntryFormat: OFormat[NinoClStoreEntry] = Json.format

  implicit val ninoQueryStringBinder: QueryStringBindable[Nino] = new QueryStringBindable[Nino] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Nino]] =
      params
        .get(key)
        .flatMap(_.headOption.map { value =>
          if (Nino.isValid(value)) {
            Right(Nino(value))
          } else {
            Left(s"Invalid Nino: $value")
          }
        })

    override def unbind(key: String, value: Nino): String = value.value

  }
  implicit val ninoPathBinder: PathBindable[Nino] = new PathBindable[Nino] {
    override def bind(key: String, value: String): Either[String, Nino] =
      if (Nino.isValid(value)) {
        Right(Nino(value))
      } else {
        Left(s"Invalid Nino: $value")
      }
    override def unbind(key: String, value: Nino): String = value.value
  }
}
