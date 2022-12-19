package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID
import akka.stream.Materializer
import com.kenshoo.play.metrics.{DisabledMetricsFilter, Metrics, MetricsFilter}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestData}
import org.scalatestplus.play.guice.{GuiceOneAppPerTest, GuiceOneServerPerSuite}
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
import uk.gov.hmrc.play.bootstrap.audit.DisabledDatastreamMetricsProvider
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule

import scala.concurrent.Await

abstract class BaseISpec extends UnitSpec with CleanMongoCollectionSupport with GuiceOneAppPerTest {

  val port: Int = Port.randomAvailable
  val wireMockPort: Int = Port.randomAvailable
  lazy val url = s"http://localhost:$port"

  override def newAppForTest(testData: TestData): Application = {

    def configuration: Seq[(String, Any)] = Seq(
      "http.port"                                              -> port,
      "microservice.services.auth.port"                        -> Port.randomAvailable,
      "microservice.services.citizen-details.port"             -> Port.randomAvailable,
      "microservice.services.users-groups-search.port"         -> Port.randomAvailable,
      "microservice.services.enrolment-store-proxy.port"       -> Port.randomAvailable,
      "microservice.services.tax-enrolments.port"              -> Port.randomAvailable,
      "microservice.services.des.port"                         -> Port.randomAvailable,
      "microservice.services.ni-exemption-registration.port"   -> Port.randomAvailable,
      "microservice.services.companies-house-api-proxy.port"   -> Port.randomAvailable,
      "microservice.services.sso.port"                         -> Port.randomAvailable,
      "microservice.services.file-upload.port"                 -> Port.randomAvailable,
      "microservice.services.file-upload-frontend.port"        -> Port.randomAvailable,
      "microservice.services.user-details.port"                -> Port.randomAvailable,
      "microservice.services.personal-details-validation.port" -> Port.randomAvailable,
      "microservice.services.identity-verification.port"       -> Port.randomAvailable,
      "auditing.consumer.baseUri.port"                         -> Port.randomAvailable,
      "microservice.services.agent-access-control.port"        -> wireMockPort,
      "microservice.services.api-platform-test-user.port"      -> wireMockPort,
      "metrics.enabled"                                        -> false,
      "auditing.enabled"                                       -> false,
      "mongodb.uri"                                            -> mongoUri,
      "gran-perms-test-gen-max-clients"                        -> 10,
      "gran-perms-test-gen-max-agents"                         -> 5
    )

    val appBuilder: GuiceApplicationBuilder =
      new GuiceApplicationBuilder()
        .configure(configuration: _*)
        .disable[GraphiteMetricsModule]
        .overrides(bind[MetricsFilter].to[DisabledMetricsFilter])
        .overrides(bind(classOf[DatastreamMetrics]).toProvider(classOf[DisabledDatastreamMetricsProvider]))
        .overrides(bind[Metrics].to[TestMetrics])
        .overrides(bind[MongoComponent].toInstance(mongoComponent))

    val app = appBuilder.build()

//    val wsClient = app.injector.instanceOf[WSClient]
//    import scala.concurrent.duration._
//    Await.result(wsClient.url(s"http://localhost:$port/ping/ping").withRequestTimeout(5.seconds).get(), 5.seconds)
    app
  }

  protected implicit lazy val materializer: Materializer = app.materializer
  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit lazy val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Unit = {
    status(result) shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def randomId = UUID.randomUUID().toString

}
