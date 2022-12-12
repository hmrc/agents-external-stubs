package uk.gov.hmrc.agentsexternalstubs.support

import java.util.UUID
import akka.stream.Materializer
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.HeaderCarrierConverter

abstract class BaseISpec extends UnitSpec with BeforeAndAfterEach {

  def app: Application

  protected implicit lazy val materializer: Materializer = app.materializer
  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit lazy val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  override def beforeEach(): Unit =
    app.injector.instanceOf[MongoComponent].database.drop().toFuture().futureValue

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
