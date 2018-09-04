package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.Application

import scala.concurrent.ExecutionContext

abstract class AppBaseISpec extends BaseISpec with ScalaFutures with JsonMatchers with WSResponseMatchers {

  override val app: Application = PlayServer.app
  val port: Int = PlayServer.port

  implicit val ec: ExecutionContext = app.actorSystem.dispatcher

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(1, Seconds))

}
