package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

abstract class AppBaseISpec extends BaseISpec with OneAppPerSuite {

  override implicit lazy val app: Application = appBuilder.build()

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled"                 -> true,
        "auditing.enabled"                -> true,
        "auditing.consumer.baseUri.host"  -> wireMockHost,
        "auditing.consumer.baseUri.port"  -> wireMockPort,
        "proxies.start"                   -> "false"
      )

}
