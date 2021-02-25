package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.libs.json.{Json, _}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.connectors.ExampleApiPlatformTestUserResponses
import uk.gov.hmrc.agentsexternalstubs.controllers.ErrorResponse._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._

class IfStubControllerISpec
    extends ServerBaseISpec with MongoDB with TestRequests with TestStubs with ExampleIfPayloads with WireMockSupport
    with ExampleApiPlatformTestUserResponses {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val repo = app.injector.instanceOf[RecordsRepository]
  lazy val controller = app.injector.instanceOf[IfStubController]

  "IfController" when {

    "GET /registration/relationship" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        await(
          repo.store(
            RelationshipRecord(
              regime = "VATC",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "987654321",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship for UTR" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "UTR",
              refNumber = "1234567890",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "TRS", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship for URN" should {
      "respond 200" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user, session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "ITSA",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "URN",
              refNumber = "XXTRUST80000001",
              active = true,
              startDate = Some(LocalDate.parse("2017-12-31"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "TRS", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "relationship",
          have.size(1),
          eachElement(
            haveProperty[String]("referenceNumber"),
            haveProperty[String]("agentReferenceNumber", be("ZARN1234567")),
            haveProperty[String]("dateFrom") and
              haveProperty[String]("contractAccountCategory", be("33")),
            haveProperty[JsObject]("individual", haveProperty[String]("firstName"), haveProperty[String]("lastName")) or
              haveProperty[JsObject]("organisation", haveProperty[String]("organisationName"))
          )
        )
      }

      "return 403 if the agent is suspended" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator
          .agent("foo", agentFriendlyName = "ABC123")
          .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "ZARN1234567")
        await(userService.createUser(user.copy(suspendedRegimes = Some(Set("ITSA"))), session.planetId))

        await(
          repo.store(
            RelationshipRecord(
              regime = "TRS",
              arn = "ZARN1234567",
              idType = "none",
              refNumber = "012345678901234",
              active = true,
              startDate = Some(LocalDate.parse("2012-01-01"))
            ),
            session.planetId
          )
        )

        val result =
          IfStub.getRelationship(regime = "ITSA", agent = true, `active-only` = true, arn = Some("ZARN1234567"))

        result should haveStatus(403)
        val errorResponse = Json.parse(result.body).as[ErrorResponse]
        errorResponse.code shouldBe "AGENT_SUSPENDED"
        errorResponse.reason shouldBe Some("The remote endpoint has indicated that the agent is suspended")
      }
    }

    "GET /registration/relationship/nino/:nino" should {
      "return 200 response if relationship exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createAgentResult = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createAgentResult should haveStatus(201)
        val createRelationshipResult = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createRelationshipResult should haveStatus(201)

        val result = IfStub.getLegacyRelationshipsByNino("AA123456A")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]](
          "agents",
          have.size(1),
          eachElement(
            haveProperty[String]("id"),
            haveProperty[String]("agentId"),
            haveProperty[String]("agentName"),
            haveProperty[String]("address1"),
            haveProperty[String]("address2"),
            haveProperty[Boolean]("isAgentAbroad")
          )
        )
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = IfStub.getLegacyRelationshipsByNino("HW827856C")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }

    "GET /registration/relationship/utr/:utr" should {
      "return 200 response if relationship exists" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createAgentResult = Records.createLegacyAgent(Json.parse(validLegacyAgentPayload))
        createAgentResult should haveStatus(201)
        val createRelationshipResult = Records.createLegacyRelationship(Json.parse(validLegacyRelationshipPayload))
        createRelationshipResult should haveStatus(201)

        val result = IfStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Seq[JsObject]](
            "agents",
            have.size(1),
            eachElement(
              haveProperty[String]("id"),
              haveProperty[String]("agentId"),
              haveProperty[String]("agentName"),
              haveProperty[String]("address1"),
              haveProperty[String]("address2"),
              haveProperty[Boolean]("isAgentAbroad")
            )
          )
        )
      }

      "return 200 response if relationship does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = IfStub.getLegacyRelationshipsByUtr("1234567890")
        result should haveStatus(200)
        result.json.as[JsObject] should haveProperty[Seq[JsObject]]("agents", have.size(0))
      }
    }
  }
}
