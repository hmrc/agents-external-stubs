package uk.gov.hmrc.agentsexternalstubs.support
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.libs.json.{JsArray, JsObject, JsValue, Reads}

import scala.reflect.ClassTag

trait JsonMatchers {

  def haveProperty[T: Reads](name: String, matcher: Matcher[T] = null)(
    implicit classTag: ClassTag[T]): Matcher[JsObject] =
    new Matcher[JsObject] {
      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[T] match {
          case Some(value) =>
            if (matcher != null) matcher(value) match {
              case x =>
                x.copy(
                  rawNegatedFailureMessage = s"At `$name` ${x.rawNegatedFailureMessage}",
                  rawMidSentenceNegatedFailureMessage = s"at `$name` ${x.rawMidSentenceNegatedFailureMessage}",
                  rawFailureMessage = s"at `$name` ${x.rawFailureMessage}",
                  rawMidSentenceFailureMessage = s"at `$name` ${x.rawMidSentenceFailureMessage}"
                )
            } else MatchResult(true, "", s"JSON have property `$name`")
          case _ =>
            MatchResult(
              false,
              s"JSON should have property `$name` of type ${classTag.runtimeClass.getSimpleName}, but ${if (obj.fields.isEmpty) "was empty."
              else "had only"} ${obj.fields
                .map(f => s"${f._1}:${f._2.getClass.getSimpleName}")
                .mkString(", ")}",
              "."
            )
        }
    }

  def havePropertyArrayOf[T: Reads](name: String, matcher: Matcher[T] = null)(
    implicit classTag: ClassTag[T]): Matcher[JsObject] =
    new Matcher[JsObject] {
      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[JsArray] match {
          case Some(array) =>
            if (matcher != null)
              array.value
                .map(_.as[T])
                .foldLeft(MatchResult(true, "", ""))((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
            else MatchResult(true, "", s"JSON have property `$name`")
          case _ =>
            MatchResult(
              false,
              s"JSON should have array property `$name` of item type ${classTag.runtimeClass.getSimpleName}, but ${if (obj.fields.isEmpty)
                "was empty."} ${obj.fields
                .map(f => s"${f._1}:${f._2.getClass.getSimpleName}")
                .mkString(", ")}",
              "."
            )
        }
    }

  def notHaveProperty(name: String): Matcher[JsObject] =
    new Matcher[JsObject] {
      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[JsValue] match {
          case Some(value) =>
            MatchResult(false, s"JSON should not have property `$name` but we got value $value", s"")
          case None =>
            MatchResult(true, "", s"JSON does not have property `$name`")
        }
    }

  def eachElement[T](matcher: Matcher[T]): Matcher[Seq[T]] = new Matcher[Seq[T]] {
    override def apply(left: Seq[T]): MatchResult =
      left.foldLeft(MatchResult(true, "", ""))((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
  }

  def eachArrayElement[T: Reads](matcher: Matcher[T])(implicit classTag: ClassTag[T]): Matcher[JsArray] =
    new Matcher[JsArray] {
      override def apply(left: JsArray): MatchResult =
        left.value
          .map(_.as[T])
          .foldLeft(MatchResult(true, "", ""))((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
    }

  def oneOfValues[T](values: T*): Matcher[T] = new Matcher[T] {
    override def apply(left: T): MatchResult =
      MatchResult(
        values.contains(left),
        s"$left is an unexpected value, should be one of ${values.mkString("[", ",", "]")}",
        s"$left was expected")
  }

}
