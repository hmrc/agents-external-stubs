package uk.gov.hmrc.agentsexternalstubs.support

import cats.data.Validated
import org.scalatest.matchers.{MatchResult, Matcher}

trait ValidatedMatchers {

  def beValid[T]: Matcher[Validated[List[String], T]] =
    new Matcher[Validated[List[String], T]] {
      override def apply(validated: Validated[List[String], T]): MatchResult =
        MatchResult(
          validated.isValid,
          validated.fold(errors => s"Validation failed with " + errors.mkString(", "), _ => ""),
          "Validation passed.")
    }

  def be_Valid[T]: Matcher[Validated[String, T]] =
    new Matcher[Validated[String, T]] {
      override def apply(validated: Validated[String, T]): MatchResult =
        MatchResult(
          validated.isValid,
          validated.fold(error => s"Validation failed with " + error, _ => ""),
          "Validation passed.")
    }

}
