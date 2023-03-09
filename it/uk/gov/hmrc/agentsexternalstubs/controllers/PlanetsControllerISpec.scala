package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.{RecordsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.support._

class PlanetsControllerISpec extends ServerBaseISpec with TestRequests with ExampleDesPayloads {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val usersRepository = app.injector.instanceOf[UsersRepository]
  lazy val recordsRepository = app.injector.instanceOf[RecordsRepository]

  "PlanetsController" when {

    "DELETE /agents-external-stubs/planets/:planetId" should {
      "remove all planet related data and return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Seq(
          Users.create(UserGenerator.individual(), Some(AG.Individual)),
          Users.create(UserGenerator.agent(), Some(AG.Agent)),
          Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload)),
          Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload)),
          Records.createLegacyAgent(Json.parse(validLegacyAgentPayload)),
          Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload)),
          Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload)),
          Records.createRelationship(Json.parse(validRelationshipPayload))
        ).map(_ should haveStatus(201))

        val result = Planets.destroy(session.planetId)
        result should haveStatus(204)

        await(usersRepository.findByPlanetId(session.planetId)(100)).size shouldBe 0
        await(
          recordsRepository
            .findByPlanetId(session.planetId, limit = Some(100))
        ).size shouldBe 0
      }
    }
  }
}
