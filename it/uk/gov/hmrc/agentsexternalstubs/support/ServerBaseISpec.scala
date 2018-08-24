package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.it.Port

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec
    extends BaseISpec with OneServerPerSuite with ScalaFutures with JsonMatchers with WSResponseMatchers {

  override lazy val port: Int = Port.randomAvailable

  override implicit lazy val app: Application = appBuilder.build()

  implicit val ec: ExecutionContext = app.actorSystem.dispatcher

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(1, Seconds))

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                  -> Port.randomAvailable,
        "microservice.services.citizen-details.port"       -> Port.randomAvailable,
        "microservice.services.users-groups-search.port"   -> Port.randomAvailable,
        "microservice.services.enrolment-store-proxy.port" -> Port.randomAvailable,
        "microservice.services.tax-enrolments.port"        -> Port.randomAvailable,
        "microservice.services.des.port"                   -> Port.randomAvailable,
        "metrics.enabled"                                  -> true,
        "auditing.enabled"                                 -> false,
        "mongodb.uri"                                      -> Mongo.uri,
        "http.port"                                        -> this.port
      )

}
