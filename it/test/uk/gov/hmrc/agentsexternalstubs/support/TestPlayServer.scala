/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.support

import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.TestServer
import play.api.Application
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
import uk.gov.hmrc.play.bootstrap.audit.DisabledDatastreamMetricsProvider
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule
import uk.gov.hmrc.play.bootstrap.metrics.{DisabledMetricsFilter, Metrics, MetricsFilter}

import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.concurrent.Await
import scala.util.Using

trait TestPlayServer:

  private val testServer: AtomicReference[Option[TestServer]] = new AtomicReference(None)
  private val lock: Lock = new ReentrantLock()

  val port: Int = FreePort.ports._1
  val wireMockPort: Int = FreePort.ports._2

  lazy val app: Application = appBuilder.build()

  def configuration: Seq[(String, Any)] = Seq(
    "microservice.services.agent-access-control.port"   -> wireMockPort,
    "microservice.services.api-platform-test-user.port" -> wireMockPort,
    "metrics.enabled"                                   -> false,
    "auditing.enabled"                                  -> false,
    "mongodb.uri"                                       -> MongoDB.uri,
    "http.port"                                         -> port,
    "gran-perms-test-gen-max-clients"                   -> 10,
    "gran-perms-test-gen-max-agents"                    -> 5
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(configuration*)
      .disable[GraphiteMetricsModule]
      .overrides(bind[MetricsFilter].to[DisabledMetricsFilter])
      .overrides(bind(classOf[DatastreamMetrics]).toProvider(classOf[DisabledDatastreamMetricsProvider]))
      .overrides(bind[Metrics].to[TestMetrics])

  private def withLock(op: => Unit): Unit =
    if lock.tryLock() then
      try
        op
      finally
        lock.unlock()
    else ()

  def run(): Unit =
    withLock:
      testServer
        .get()
        .fold {
          println(s"Starting TestPlayServer at $port ... ")
          val server = TestServer(port, app)
          server.start()
          val wsClient = app.injector.instanceOf[WSClient]
          import scala.concurrent.duration.*
          Await.result(wsClient.url(s"http://localhost:$port/ping/ping").withRequestTimeout(5.seconds).get(), 5.seconds)
          testServer.set(Some(server))
          Runtime.getRuntime.addShutdownHook(new Thread(() => stop()))
          println("ready.")
        }(identity)

  def stop(): Unit =
    withLock:
      testServer
        .get()
        .fold(identity)(server =>
          println(s"Stopping TestPlayServer at $port ...")
          server.stop()
          testServer.set(None)
        )

object TestPlayServer extends TestPlayServer

class TestMetrics extends Metrics {

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate("test")
}

object FreePort:
  val ports: (Int, Int) = Using.resources(new ServerSocket(0), new ServerSocket(0)): (server, wiremock) =>
    (server.getLocalPort, wiremock.getLocalPort)
