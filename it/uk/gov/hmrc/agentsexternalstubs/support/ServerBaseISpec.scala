package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec extends BaseISpec with ScalaFutures with JsonMatchers with WSResponseMatchers {

  import scala.concurrent.duration._
  implicit val defaultTimeout: FiniteDuration = 30.seconds

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(10, Milliseconds))

}
