package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import play.api.Application
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec
    extends BaseISpec with BeforeAndAfterAll with ScalaFutures with JsonMatchers with WSResponseMatchers {

  lazy val url = s"http://localhost:${TestPlayServer.port}"

  val app: Application = TestPlayServer.app
  val wireMockPort: Int = TestPlayServer.wireMockPort

  override def beforeAll(): Unit = {
    super.beforeAll()
    TestPlayServer.runPlayServer()
  }

  import scala.concurrent.duration._
  implicit val defaultTimeout: FiniteDuration = 30.seconds

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(10, Milliseconds))

}
