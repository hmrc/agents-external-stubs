package uk.gov.hmrc.agentsexternalstubs.support
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse

trait WSResponseMatchers {

  def haveStatus(status: Int): Matcher[WSResponse] = new Matcher[WSResponse] {
    override def apply(left: WSResponse): MatchResult =
      MatchResult(
        left.status == status,
        s"Request returned ${statusType(left.status)} ${left.status} ${left.statusText} while we expected $status ${statusType(
          status)}, details: ${details(left)}",
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

}
