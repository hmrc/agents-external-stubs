package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.JourneyType.UpliftNino
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.{Journey, JourneyCreation, ServiceContract}
import uk.gov.hmrc.agentsexternalstubs.repository.JourneyIvRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDB, ServerBaseISpec, TestRequests}

class IdentityVerificationControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  val controller = app.injector.instanceOf[IdentityVerificationController]
  val repo = app.injector.instanceOf[JourneyIvRepository]

  val fakeRequest = FakeRequest("POST", "/journey")

  val testJourneyId: String = repo.journeyId
  val serviceContract: ServiceContract = ServiceContract("aif","/good","/bad",200)
  val journeyCreation = JourneyCreation(serviceContract, UpliftNino)
  val testJourneyStored: Journey = Journey(testJourneyId, UpliftNino, serviceContract)

  "POST /journey" should {

    "return 201 for creating a journeyId" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val result: WSResponse = await(IvJourney.createJourneyId(journeyCreation))

      result.status shouldBe 201

      (result.json \ "journeyId").as[String] shouldBe testJourneyId
    }
  }

  "GET /journey/:journeyId" should {
    "return 200 for returning a journey" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val response: WSResponse = await(IvJourney.createJourneyId(journeyCreation))

      val journeyId = (response.json \ "journeyId").as[String]

      journeyId shouldBe testJourneyId

      val result = IvJourney.getJourneyId(journeyId)

      result.status shouldBe 200

      result.json.as[Journey] shouldBe testJourneyStored
    }

    "return 404 for not returning a journey" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val result = IvJourney.getJourneyId("1234")

      result.status shouldBe 404
    }
  }

  "DELETE /journey/:journeyId" should {
    "return 204 for removing a journey" in {
      implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

      val response = IvJourney.createJourneyId(journeyCreation)

      val journeyId = (response.json \ "journeyId").as[String]

      journeyId shouldBe testJourneyId

      val journey = IvJourney.getJourneyId(journeyId)

      journey.json.as[Journey] shouldBe testJourneyStored

      val result = IvJourney.deleteJourney(journeyId)

      result.status shouldBe 204
    }
  }
}
