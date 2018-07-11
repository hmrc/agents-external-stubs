package uk.gov.hmrc.agentsexternalstubs.binders

import play.api.mvc.PathBindable

class SimpleObjectBinder[T](bind: String => T, unbind: T => String)(implicit m: Manifest[T]) extends PathBindable[T] {
  override def bind(key: String, value: String): Either[String, T] =
    try {
      Right(bind(value))
    } catch {
      case e: Throwable =>
        Left(s"Cannot parse parameter '$key' with value '$value' as '${m.runtimeClass.getSimpleName}'")
    }

  def unbind(key: String, value: T): String = unbind(value)
}
