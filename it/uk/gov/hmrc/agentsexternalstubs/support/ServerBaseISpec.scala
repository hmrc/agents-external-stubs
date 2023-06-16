package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import play.api.Application

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec
    extends BaseISpec with BeforeAndAfterAll with ScalaFutures with JsonMatchers with WSResponseMatchers with MongoDB {

  val playServer: TestPlayServer = TestPlayServer
  def port: Int = playServer.port
  def wireMockPort: Int = playServer.wireMockPort

  override def beforeAll(): Unit = {
    super.beforeAll()
    playServer.run()
  }

  override lazy val app: Application = playServer.app

  val url = s"http://localhost:$port"

  import scala.concurrent.duration._
  implicit val defaultTimeout: FiniteDuration = 30.seconds

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(50, Milliseconds))

}
