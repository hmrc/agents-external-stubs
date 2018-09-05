package uk.gov.hmrc.agentsexternalstubs.models

import scala.reflect.ClassTag

trait RecordMetaData[T <: Record] {

  val typeName: String
  val fieldNames: Seq[String]
  val utils: RecordUtils[T]
}

object RecordMetaData {

  def apply[T <: Record](utilities: RecordUtils[T])(implicit classTag: ClassTag[T]): RecordMetaData[T] = {

    val properties =
      classTag.runtimeClass.getDeclaredFields.map(_.getName).toSet.-("id").+(Record.ID).+(Record.TYPE).toSeq

    new RecordMetaData[T] {
      override val typeName: String = classTag.runtimeClass.getSimpleName
      override val fieldNames: Seq[String] = properties
      override val utils: RecordUtils[T] = utilities
    }
  }
}
