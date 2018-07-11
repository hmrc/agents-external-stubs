package uk.gov.hmrc.agentsexternalstubs.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HttpPost
import uk.gov.hmrc.play.http.ws.WSPost

@Singleton
class MicroserviceAuthConnector @Inject()(@Named("auth-baseUrl") baseUrl: URL) extends PlayAuthConnector {

  override val serviceUrl = baseUrl.toString

  override def http = new HttpPost with WSPost {
    override val hooks = NoneRequired
  }
}
