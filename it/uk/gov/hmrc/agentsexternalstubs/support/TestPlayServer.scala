package uk.gov.hmrc.agentsexternalstubs.support
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.{Lock, ReentrantLock}
import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import com.kenshoo.play.metrics.{DisabledMetricsFilter, Metrics, MetricsFilter}
import play.api.{Application, Logging}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.TestServer
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
import uk.gov.hmrc.play.bootstrap.audit.DisabledDatastreamMetricsProvider
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule

import java.net.ServerSocket
import scala.annotation.tailrec
import scala.concurrent.Await

object TestPlayServer {

  private val lock: Lock = new ReentrantLock()
  private val testServer: AtomicReference[TestServer] = new AtomicReference[TestServer]()

  val port: Int = Port.randomAvailable
  val wireMockPort: Int = Port.randomAvailable

  lazy val app: Application = appBuilder.build()

  def configuration: Seq[(String, Any)] = Seq(
    "microservice.services.auth.port"                      -> Port.randomAvailable,
    "microservice.services.citizen-details.port"           -> Port.randomAvailable,
    "microservice.services.users-groups-search.port"       -> Port.randomAvailable,
    "microservice.services.enrolment-store-proxy.port"     -> Port.randomAvailable,
    "microservice.services.tax-enrolments.port"            -> Port.randomAvailable,
    "microservice.services.des.port"                       -> Port.randomAvailable,
    "microservice.services.ni-exemption-registration.port" -> Port.randomAvailable,
    "microservice.services.agent-access-control.port"      -> wireMockPort,
    "microservice.services.api-platform-test-user.port"    -> wireMockPort,
    "metrics.enabled"                                      -> false,
    "auditing.enabled"                                     -> false,
    "mongodb.uri"                                          -> "mongodb://127.0.0.1:27017/agents-external-stubs-tests",
    "http.port"                                            -> port,
    "gran-perms-test-gen-max-clients"                      -> 10,
    "gran-perms-test-gen-max-agents"                       -> 5
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(configuration: _*)
      .disable[GraphiteMetricsModule]
      .overrides(bind[MetricsFilter].to[DisabledMetricsFilter])
      .overrides(bind(classOf[DatastreamMetrics]).toProvider(classOf[DisabledDatastreamMetricsProvider]))
      .overrides(bind[Metrics].to[TestMetrics])

  def runPlayServer(): Unit =
    if (lock.tryLock()) try if (testServer.get() == null) {
      println(s"Starting TestPlayServer at $port ... ")
      val server = TestServer(port, app)
      server.start()
      val wsClient = app.injector.instanceOf[WSClient]
      import scala.concurrent.duration._
      Await.result(wsClient.url(s"http://localhost:$port/ping/ping").withRequestTimeout(5.seconds).get(), 5.seconds)
      testServer.set(server)
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
        override def run(): Unit =
          if (lock.tryLock()) try if (testServer.get() != null) {
            println(s"Stopping TestPlayServer at $port ...")
            testServer.get().stop()
            testServer.set(null)
          } finally lock.unlock()
      }))
      println("ready.")
    } finally lock.unlock()
}

class TestMetrics extends Metrics {

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate("test")
  override def toJson: String = ""
}

// This class was copy-pasted from the hmrctest project, which is now deprecated.
object Port extends Logging {
  val rnd = new scala.util.Random
  val range = 8000 to 39999
  val usedPorts = List[Int]()

  @tailrec
  def randomAvailable: Int =
    range(rnd.nextInt(range.length)) match {
      case 8080 => randomAvailable
      case 8090 => randomAvailable
      case p: Int =>
        available(p) match {
          case false =>
            logger.debug(s"Port $p is in use, trying another")
            randomAvailable
          case true =>
            logger.debug("Taking port : " + p)
            usedPorts :+ p
            p
        }
    }

  private def available(p: Int): Boolean = {
    var socket: ServerSocket = null
    try if (!usedPorts.contains(p)) {
      socket = new ServerSocket(p)
      socket.setReuseAddress(true)
      true
    } else {
      false
    } catch {
      case t: Throwable => false
    } finally if (socket != null) socket.close()
  }
}
