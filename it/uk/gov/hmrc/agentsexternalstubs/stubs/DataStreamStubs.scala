package uk.gov.hmrc.agentsexternalstubs.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.agentsexternalstubs.support.WireMockSupport

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
  }

  private def auditUrl = "/write/audit"

}
