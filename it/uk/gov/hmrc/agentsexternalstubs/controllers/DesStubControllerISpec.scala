package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, RelationshipRecord}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{JsonMatchers, MongoDbPerSuite, ServerBaseISpec, TestRequests}

import scala.language.higherKinds

class DesStubControllerISpec
    extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs with JsonMatchers {

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]
  val repo = app.injector.instanceOf[RecordsRepository]

  "DesController" when {

    "POST /registration/relationship" should {
      "respond 200 when authorising for ITSA" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",  
                       |   "agentReferenceNumber": "ZARN1234567",  
                       |   "regime": "ITSA",
                       |   "authorisation": {    
                       |     "action": "Authorise",    
                       |     "isExclusiveAgent": true  
                       |   }
                       |}
          """.stripMargin))
        result.status shouldBe 200
      }

      "respond 200 when de-authorising an ITSA relationship" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = DesStub.authoriseOrDeAuthoriseRelationship(
          Json.parse("""
                       |{
                       |   "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
                       |   "refNumber": "012345678901234",
                       |   "agentReferenceNumber": "ZARN1234567",
                       |   "regime": "ITSA",
                       |   "authorisation": {
                       |     "action": "De-Authorise"
                       |   }
                       |}
                     """.stripMargin))
        result.status shouldBe 200
      }
    }

    "GET /registration/relationship" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))),
            session.planetId
          ))

        await(
          repo.store(
            RelationshipRecord(
              regime = "VATC",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "987654321",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))),
            session.planetId
          ))

        val result =
          DesStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result.status shouldBe 200
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1) and eachElement(
            haveProperty[String]("referenceNumber") and
              haveProperty[String]("agentReferenceNumber", be("ZARN1234567")) and
              haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")) and (haveProperty[JsObject](
              "individual",
              haveProperty[String]("firstName") and haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName")))
          )
        )
      }
    }
  }
}
