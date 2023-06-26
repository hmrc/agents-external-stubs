package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.controllers.SpecialCasesController.writes
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, EnrolmentKey, Identifier, SpecialCase, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.{AuthenticatedSessionsRepository, GroupsRepository, KnownFactsRepository, RecordsRepository, SpecialCasesRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.support._

class PlanetsControllerISpec extends ServerBaseISpec with TestRequests with ExampleDesPayloads {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val usersRepository: UsersRepository = app.injector.instanceOf[UsersRepository]
  lazy val recordsRepository: RecordsRepository = app.injector.instanceOf[RecordsRepository]
  lazy val knownFactsRepository: KnownFactsRepository = app.injector.instanceOf[KnownFactsRepository]
  lazy val specialCasesRepository: SpecialCasesRepository = app.injector.instanceOf[SpecialCasesRepository]
  lazy val authSessionRepository: AuthenticatedSessionsRepository =
    app.injector.instanceOf[AuthenticatedSessionsRepository]
  lazy val groupsRepository: GroupsRepository = app.injector.instanceOf[GroupsRepository]

  "PlanetsController" when {

    "DELETE /agents-external-stubs/planets/:planetId" should {
      "remove all planet related data and return 204" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = EnrolmentKey("HMRC-MTD-IT", Seq(Identifier("MTDITID", "XAAA12345678901")))
        Seq(
          Users.create(UserGenerator.individual(), Some(AG.Individual)), // this should also create a group
          Users.create(UserGenerator.agent(), Some(AG.Agent)),
          Records.createBusinessDetails(Json.parse(validBusinessDetailsPayload)),
          Records.createVatCustomerInformation(Json.parse(validVatCustomerInformationPayload)),
          Records.createLegacyAgent(Json.parse(validLegacyAgentPayload)),
          Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload)),
          Records.createBusinessPartnerRecord(Json.parse(validBusinessPartnerRecordPayload)),
          Records.createRelationship(Json.parse(validRelationshipPayload)),
          KnownFacts.createKnownFacts(Json.parse(s"""
            |{ "enrolmentKey": "${enrolmentKey.toString}",
            |  "identifiers": [
            |   {
            |     "key": "MTDITID",
            |     "value": "XAAA12345678901"
            |   }
            |  ],
            | "verifiers": [
            |   {
            |     "key": "NINO",
            |     "value": ""
            |   }
            |  ]
            |} """.stripMargin)),
          SpecialCases.createSpecialCase(
            SpecialCase(
              SpecialCase.RequestMatch("/test1"),
              SpecialCase.Response(404, Some("{foo}"), Seq(SpecialCase.Header("foo", "bar")))
            )
          )
        ).map(_ should haveStatus(201))

        val result = Planets.destroy(session.planetId)
        result should haveStatus(204)

        await(usersRepository.findByPlanetId(session.planetId)(100)).size shouldBe 0
        await(recordsRepository.findByPlanetId(session.planetId, limit = Some(100))).size shouldBe 0
        await(specialCasesRepository.findByPlanetId(session.planetId)(100)).size shouldBe 0
        await(knownFactsRepository.findByEnrolmentKey(enrolmentKey, session.planetId)).size shouldBe 0
        await(authSessionRepository.findByPlanetId(session.planetId)).size shouldBe 0
        await(groupsRepository.findByPlanetId(session.planetId, None)(100)).size shouldBe 0
      }
    }
  }
}
