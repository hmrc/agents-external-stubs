package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import play.api.Application

import scala.concurrent.ExecutionContext

abstract class AppBaseISpec
    extends BaseISpec with ScalaFutures with JsonMatchers with WSResponseMatchers with MongoDB with BeforeAndAfterEach {

  override lazy val app: Application = TestPlayServer.app
  val port: Int = TestPlayServer.port

  implicit val ec: ExecutionContext = app.actorSystem.dispatcher

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(10, Milliseconds))

}
