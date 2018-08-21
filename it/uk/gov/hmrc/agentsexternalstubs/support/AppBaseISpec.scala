package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentsexternalstubs.stubs.DataStreamStubs

abstract class AppBaseISpec extends BaseISpec with WireMockSupport with OneAppPerSuite with DataStreamStubs {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                  -> wireMockPort,
        "microservice.services.citizen-details.port"       -> wireMockPort,
        "microservice.services.users-groups-search.port"   -> wireMockPort,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "microservice.services.tax-enrolments.port"        -> wireMockPort,
        "microservice.services.des.port"                   -> wireMockPort,
        "metrics.enabled"                                  -> true,
        "auditing.enabled"                                 -> false,
        "mongodb.uri"                                      -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "proxies.start"                                    -> "false"
      )

  override def commonStubs(): Unit =
    givenAuditConnector()

}
