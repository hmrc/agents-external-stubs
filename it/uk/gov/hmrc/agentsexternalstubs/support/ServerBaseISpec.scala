package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import play.api.Application

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec
    extends BaseISpec with BeforeAndAfterAll with ScalaFutures with JsonMatchers with WSResponseMatchers {

  override def beforeAll(): Unit = {
    super.beforeAll()
    PlayServer.run()
  }

  override lazy val app: Application = PlayServer.app
  val port: Int = PlayServer.port
  val wireMockPort: Int = PlayServer.wireMockPort

  import scala.concurrent.duration._
  override implicit val defaultTimeout: FiniteDuration = 30.seconds

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(10, Milliseconds))

}
