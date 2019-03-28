package uk.gov.hmrc.agentsexternalstubs.support
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.libs.json.{JsArray, JsObject, JsValue, Reads}

import scala.reflect.ClassTag

trait JsonMatchers {

  private val EMPTY = MatchResult(true, "", "")

  def haveProperty[T: Reads](name: String, matchers: Matcher[T]*)(implicit classTag: ClassTag[T]): Matcher[JsObject] =
    new Matcher[JsObject] {
      val matcher =
        if (matchers.nonEmpty) matchers.reduce(_ and _)
        else new Matcher[T] { override def apply(left: T): MatchResult = EMPTY }

      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[T] match {
          case Some(value) =>
            matcher(value) match {
              case x =>
                x.copy(
                  rawNegatedFailureMessage = s"at `$name` ${x.rawNegatedFailureMessage}",
                  rawMidSentenceNegatedFailureMessage = s"at `$name` ${x.rawMidSentenceNegatedFailureMessage}",
                  rawFailureMessage = s"at `$name` ${x.rawFailureMessage}",
                  rawMidSentenceFailureMessage = s"at `$name` ${x.rawMidSentenceFailureMessage}"
                )
            }
          case _ =>
            val failureMessage =
              s"JSON should have property `$name` of type ${classTag.runtimeClass.getSimpleName}, but ${if (obj.fields.isEmpty) "was empty."
              else "had only"} ${obj.fields
                .map(f => s"${f._1}:${f._2.getClass.getSimpleName}")
                .mkString(", ")}"
            MatchResult(
              matches = false,
              rawFailureMessage = failureMessage,
              rawNegatedFailureMessage = ".",
              rawMidSentenceFailureMessage = failureMessage,
              rawMidSentenceNegatedFailureMessage = ""
            )
        }
    }

  def havePropertyArrayOf[T: Reads](name: String, matchers: Matcher[T]*)(
    implicit classTag: ClassTag[T]): Matcher[JsObject] =
    new Matcher[JsObject] {
      val matcher =
        if (matchers.nonEmpty) matchers.reduce(_ and _)
        else new Matcher[T] { override def apply(left: T): MatchResult = EMPTY }

      override def apply(obj: JsObject): MatchResult =
        (obj \ name).asOpt[JsArray] match {
          case Some(array) =>
            array.value
              .map(_.as[T])
              .foldLeft(EMPTY)((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
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

  def eachElement[T](matchers: Matcher[T]*): Matcher[Seq[T]] = new Matcher[Seq[T]] {
    val matcher =
      if (matchers.nonEmpty) matchers.reduce(_ and _)
      else new Matcher[T] { override def apply(left: T): MatchResult = EMPTY }

    override def apply(left: Seq[T]): MatchResult =
      left.foldLeft(MatchResult(true, "", ""))((a: MatchResult, v: T) => if (a.matches) matcher(v) else a)
  }

  def eachArrayElement[T: Reads](matchers: Matcher[T]*)(implicit classTag: ClassTag[T]): Matcher[JsArray] =
    new Matcher[JsArray] {
      val matcher =
        if (matchers.nonEmpty) matchers.reduce(_ and _)
        else new Matcher[T] { override def apply(left: T): MatchResult = EMPTY }

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
