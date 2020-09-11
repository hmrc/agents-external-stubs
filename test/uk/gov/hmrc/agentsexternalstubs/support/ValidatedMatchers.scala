/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
