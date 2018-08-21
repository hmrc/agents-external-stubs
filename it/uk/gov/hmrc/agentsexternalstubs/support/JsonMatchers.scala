package uk.gov.hmrc.agentsexternalstubs.support
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.libs.json.{JsObject, Reads}

import scala.reflect.ClassTag

trait JsonMatchers {

  def haveProperty[T: Reads](name: String, matcher: Matcher[T] = null)(implicit classTag: ClassTag[T]) =
    new Matcher[JsObject] {
      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[T] match {
          case Some(value) =>
            if (matcher != null) matcher(value) else MatchResult(true, "", s"JSON have property $name")
          case _ =>
            MatchResult(
              false,
              s"JSON should have property $name of the ${classTag.runtimeClass}",
              ""
            )
        }
    }

  def eachElement[T](matcher: Matcher[T]): Matcher[Seq[T]] = new Matcher[Seq[T]] {
    override def apply(left: Seq[T]): MatchResult =
      left.foldLeft(MatchResult(true, "", ""))((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
  }

}
