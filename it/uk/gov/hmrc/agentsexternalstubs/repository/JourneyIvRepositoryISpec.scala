package uk.gov.hmrc.agentsexternalstubs.repository
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.JourneyType.UpliftNino
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.{Journey, JourneyCreation, ServiceContract}
import uk.gov.hmrc.agentsexternalstubs.support.{AppBaseISpec, MongoDB}

class JourneyIvRepositoryISpec extends AppBaseISpec with MongoDB {

  val repo: JourneyIvRepository = app.injector.instanceOf[JourneyIvRepository]

  val testJourneyId: String = repo.journeyId
  val serviceContract: ServiceContract = ServiceContract("aif","/good" ,"/bad" ,200)
  val journeyCreation = JourneyCreation(serviceContract, UpliftNino)
  val testJourneyStored: Journey = Journey(testJourneyId, UpliftNino, serviceContract)

  "createJourney" should {
    "return a journeyId" in {
      val journeyId: String = await(repo.createJourneyId(journeyCreation))

      journeyId shouldBe testJourneyId
    }
  }

  "getJourney" should {
    "return a journey" in {
      val journeyId: String = await(repo.createJourneyId(journeyCreation))

      val resultOpt: Option[Journey] = await(repo.getJourneyInfo(journeyId))

      resultOpt shouldBe Some(testJourneyStored)
    }

    "return no journey" in {
      val resultOpt: Option[Journey] = await(repo.getJourneyInfo("1234"))

      resultOpt shouldBe None
    }
  }

  "deleteJourney" should {
    "delete a journey" in {

      val journeyCreation = JourneyCreation(serviceContract, UpliftNino)

      val journeyId: String = await(repo.createJourneyId(journeyCreation))

      val journeyOptBefore: Option[Journey] = await(repo.getJourneyInfo(journeyId))

      journeyOptBefore shouldBe Some(testJourneyStored)

      await(repo.deleteJourneyRecord(testJourneyId))

      val journeyOptDeleted: Option[Journey] = await(repo.getJourneyInfo(journeyId))

      journeyOptDeleted shouldBe None
    }
  }

}
