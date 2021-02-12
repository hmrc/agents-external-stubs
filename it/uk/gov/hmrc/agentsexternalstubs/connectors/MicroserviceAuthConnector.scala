package uk.gov.hmrc.agentsexternalstubs.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HttpPost

@Singleton
class MicroserviceAuthConnector @Inject() (appConfig: AppConfig, val http: HttpPost) extends PlayAuthConnector {

  override val serviceUrl = appConfig.authUrl.toString
}
