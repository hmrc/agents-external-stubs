package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.it.Port

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec extends BaseISpec with OneServerPerSuite with ScalaFutures {

  override implicit lazy val app: Application = appBuilder.build()

  implicit val ec: ExecutionContext = app.actorSystem.dispatcher

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(1, Seconds))

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"            -> Port.randomAvailable,
        "microservice.services.citizen-details.port" -> Port.randomAvailable,
        "metrics.enabled"                            -> true,
        "auditing.enabled"                           -> true,
        "auditing.consumer.baseUri.host"             -> wireMockHost,
        "auditing.consumer.baseUri.port"             -> wireMockPort,
        "mongodb.uri"                                -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "http.port"                                  -> this.port
      )

}
