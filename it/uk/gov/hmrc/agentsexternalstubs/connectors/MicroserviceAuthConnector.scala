package uk.gov.hmrc.agentsexternalstubs.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HttpPost

@Singleton
class MicroserviceAuthConnector @Inject()(@Named("auth-baseUrl") baseUrl: URL, val http: HttpPost)
    extends PlayAuthConnector {

  override val serviceUrl = baseUrl.toString
}
