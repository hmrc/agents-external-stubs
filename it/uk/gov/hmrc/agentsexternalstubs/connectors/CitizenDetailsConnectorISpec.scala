package uk.gov.hmrc.agentsexternalstubs.connectors

import org.joda.time.LocalDate
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}
import uk.gov.hmrc.domain.Nino

class CitizenDetailsConnectorISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[CitizenDetailsConnector]

  "CitizenDetailsConnector" when {

    "getCitizenDateOfBirth" should {
      "return dateOfBirth" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(
          UserGenerator
            .individual(userId = "foo", nino = "HW 82 78 56 C", name = "Alan Brian Foo-Foe", dateOfBirth = "1975-12-18")
        )

        val result = await(connector.getCitizenDateOfBirth(Nino("HW827856C")))
        result.flatMap(_.dateOfBirth) shouldBe Some(LocalDate.parse("1975-12-18"))
      }
    }
  }
}
