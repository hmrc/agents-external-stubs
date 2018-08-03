package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID

import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentsexternalstubs.repository.{AuthenticatedSessionsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.stubs.DataStreamStubs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseISpec extends UnitSpec with MongoApp with WireMockSupport with DataStreamStubs {

  def app: Application
  protected def appBuilder: GuiceApplicationBuilder

  protected implicit val materializer = app.materializer

  override def commonStubs(): Unit =
    givenAuditConnector()

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Unit = {
    status(result) shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(app.injector.instanceOf[AuthenticatedSessionsRepository].ensureIndexes)
    await(app.injector.instanceOf[UsersRepository].ensureIndexes)
  }

  def randomId = UUID.randomUUID().toString

}
