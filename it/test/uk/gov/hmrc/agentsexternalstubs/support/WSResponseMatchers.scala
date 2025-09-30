/*
 * Copyright 2025 HM Revenue & Customs
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
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.http.HeaderNames
import play.api.libs.json.{JsArray, JsObject}
import play.api.libs.ws.WSResponse

import scala.util.{Failure, Success, Try}

trait WSResponseMatchers {

  def haveStatus(status: Int): Matcher[WSResponse] = new Matcher[WSResponse] {
    override def apply(left: WSResponse): MatchResult =
      MatchResult(
        left.status == status,
        s"Request returned ${statusType(left.status)} ${left.status} ${left.statusText} while we expected $status ${statusType(status)}, details: ${details(left)}",
        s"Got ${statusType(left.status)} ${left.status} ${left.statusText} as expected"
      )

    private def statusType(status: Int): String =
      if (status >= 200 && status < 300) s"success"
      else if (status >= 300 && status < 400) s"redirect"
      else if (status >= 400 && status < 500) s"failure"
      else s"internal server error"

    private def details(response: WSResponse): String = {
      val status = response.status
      if (status >= 200 && status < 300)
        s"${response.header(HeaderNames.CONTENT_TYPE).map(c => s"Content-Type: $c").getOrElse("")}"
      else if (status >= 300 && status < 400)
        s"${response.header(HeaderNames.LOCATION).map(l => s"Location: $l").getOrElse("")}"
      else if (status >= 400 && status < 500) s"${response.body}"
      else ""
    }
  }

  def haveValidJsonBody(matchers: Matcher[JsObject]*): Matcher[WSResponse] = new Matcher[WSResponse] {
    override def apply(left: WSResponse): MatchResult = Try(left.json) match {
      case Success(o: JsObject) =>
        matchers.foldLeft(MatchResult(true, "", ""))((a, b) => if (a.matches) b(o) else a)
      case Success(_) => MatchResult(true, "", "Have valid JSON body")
      case Failure(e) => MatchResult(false, s"Could not parse.tolerantJson body because of $e", "")
    }
  }

  def haveValidJsonArrayBody(matchers: Matcher[JsArray]*): Matcher[WSResponse] = new Matcher[WSResponse] {
    override def apply(left: WSResponse): MatchResult = Try(left.json) match {
      case Success(o: JsArray) =>
        matchers.foldLeft(MatchResult(true, "", ""))((a, b) => if (a.matches) b(o) else a)
      case Success(x) => MatchResult(false, s"JSON value should be an array but was $x", "")
      case Failure(e) => MatchResult(false, s"Could not parse.tolerantJson body because of $e", "")
    }
  }

}
