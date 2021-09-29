package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsObject, Json, Reads, Writes}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.mvc.Http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.agentsexternalstubs.models.SpecialCase.RequestMatch
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, SpecialCase, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.SpecialCasesRepository
import uk.gov.hmrc.agentsexternalstubs.support._

class SpecialCasesControllerISpec extends ServerBaseISpec with MongoDB with TestRequests {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val repo = app.injector.instanceOf[SpecialCasesRepository]

  implicit val reads: Reads[SpecialCase] = SpecialCase.external.reads
  implicit val writes: Writes[SpecialCase] = SpecialCase.external.writes

  import scala.concurrent.duration._
  override implicit val defaultTimeout = 30 seconds

  "SpecialCasesController" when {

    "GET /agents-external-stubs/special-cases" should {
      "return 200 with all the entities on the planet" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val entity1 = SpecialCase(RequestMatch("/test1", "GET"), SpecialCase.Response(200), Some(session.planetId))
        val entity2 = SpecialCase(RequestMatch("/test2", "POST"), SpecialCase.Response(201), Some(session.planetId))
        val entity3 = SpecialCase(RequestMatch("/test3", "PUT"), SpecialCase.Response(202), Some(session.planetId))
        val entity4 =
          SpecialCase(RequestMatch("/test4", "DELETE"), SpecialCase.Response(204), Some(session.planetId))
        val entity5 = SpecialCase(RequestMatch("/test5", "GET"), SpecialCase.Response(202), Some(session.planetId))

        await(repo.upsert(entity1, session.planetId))
        await(repo.upsert(entity2, session.planetId))
        await(repo.upsert(entity3, session.planetId))
        await(repo.upsert(entity4, session.planetId))
        await(repo.upsert(entity5, session.planetId + "_"))

        val result = SpecialCases.getAllSpecialCases
        result should haveStatus(200)
        result should haveValidJsonArrayBody(
          eachArrayElement(
            haveProperty[JsObject]("requestMatch") and haveProperty[JsObject]("response") and haveProperty[String](
              "planetId",
              be(session.planetId)
            )
          )
        )
      }

      "return 204 if none found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val entity1 = SpecialCase(RequestMatch("/test1", "GET"), SpecialCase.Response(200), Some(session.planetId))
        val entity2 = SpecialCase(RequestMatch("/test2", "POST"), SpecialCase.Response(201), Some(session.planetId))
        val entity3 = SpecialCase(RequestMatch("/test3", "PUT"), SpecialCase.Response(202), Some(session.planetId))
        val entity4 =
          SpecialCase(RequestMatch("/test4", "DELETE"), SpecialCase.Response(204), Some(session.planetId))

        await(repo.upsert(entity1, session.planetId + "_"))
        await(repo.upsert(entity2, session.planetId + "_"))
        await(repo.upsert(entity3, session.planetId + "_"))
        await(repo.upsert(entity4, session.planetId + "_"))

        val result = SpecialCases.getAllSpecialCases
        result should haveStatus(204)
      }
    }

    "GET /agents-external-stubs/special-cases/:id" should {
      "return 200 with an entity" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val specialCase = SpecialCase(
          SpecialCase.RequestMatch("/test"),
          SpecialCase.Response(500, Some("FOO"), Seq(SpecialCase.Header("zig", "foo")))
        )
        val id = await(repo.upsert(specialCase, session.planetId))

        val result =
          SpecialCases.getSpecialCase(id)
        result.status shouldBe 200

        val specialCase2 = result.json.as[SpecialCase]
        specialCase2.requestMatch.path shouldBe "/test"
        specialCase2.response.status shouldBe 500
        specialCase2.response.body shouldBe Some("FOO")
        specialCase2.response.headers shouldBe Seq(SpecialCase.Header("zig", "foo"))
      }
    }

    "POST /agents-external-stubs/special-cases" should {
      "create a special case and return entity location" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val createResult =
          SpecialCases.createSpecialCase(
            SpecialCase(
              SpecialCase.RequestMatch("/test1"),
              SpecialCase.Response(404, Some("{foo}"), Seq(SpecialCase.Header("foo", "bar")))
            )
          )
        createResult.status shouldBe 201

        val result = createResult.header(HeaderNames.LOCATION).map(get).get
        result.status shouldBe 200
        val specialCase = result.json.as[SpecialCase]
        specialCase.requestMatch.path shouldBe "/test1"
        specialCase.response.status shouldBe 404
        specialCase.response.body shouldBe Some("{foo}")
        specialCase.response.headers shouldBe Seq(SpecialCase.Header("foo", "bar"))
      }
    }

    "PUT /agents-external-stubs/special-cases/:id" should {
      "update a special case and return entity location" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val specialCase = SpecialCase(
          SpecialCase.RequestMatch("/test1/test2/test3"),
          SpecialCase.Response(404, Some("{zoo}"), Seq(SpecialCase.Header("zig", "zag")))
        )
        val id = await(repo.upsert(specialCase, session.planetId))

        val result =
          SpecialCases.updateSpecialCase(id, specialCase.copy(response = specialCase.response.copy(status = 400)))
        result.status shouldBe 202

        val updated = SpecialCases.getSpecialCase(id)
        updated.status shouldBe 200
        val specialCase2 = updated.json.as[SpecialCase]
        specialCase2.requestMatch.path shouldBe "/test1/test2/test3"
        specialCase2.response.status shouldBe 400
        specialCase2.response.body shouldBe Some("{zoo}")
        specialCase2.response.headers shouldBe Seq(SpecialCase.Header("zig", "zag"))
      }
    }

    "DELETE /agents-external-stubs/special-cases/:id" should {
      "remove special case" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val specialCase = SpecialCase(
          SpecialCase.RequestMatch("/test3"),
          SpecialCase.Response(404, None, Seq(SpecialCase.Header("zig", "zag")))
        )
        val id = await(repo.upsert(specialCase, session.planetId))

        val result =
          SpecialCases.deleteSpecialCase(id)
        result.status shouldBe 204

        val removed = SpecialCases.getSpecialCase(id)
        removed.status shouldBe 404
      }
    }

    "specialCase" should {

      "replace an ordinary GET response" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val user = UserGenerator.individual(session.userId)
        Users.update(user)
        val result1 = CitizenDetailsStub.getCitizen("nino", user.nino.get.value)
        result1 should haveStatus(200)

        val result2 = SpecialCases.createSpecialCase(
          SpecialCase(
            requestMatch = SpecialCase.RequestMatch(path = s"/citizen-details/nino/${user.nino.get.value}"),
            response = SpecialCase
              .Response(427, Some("""{"a":"b"}"""), Seq(SpecialCase.Header(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)))
          )
        )
        result2 should haveStatus(201)
        val sc = get(result2.header(HeaderNames.LOCATION).get).json.as[SpecialCase]

        val result3 = CitizenDetailsStub.getCitizen("nino", user.nino.get.value)
        result3 should haveStatus(427)
        result3.header(HeaderNames.CONTENT_TYPE) shouldBe Some("application/json")
        (result3.json \ "a").as[String] shouldBe "b"

        val result4 =
          SpecialCases.updateSpecialCase(sc.id.get.value, sc.copy(response = sc.response.copy(status = 583)))
        result4 should haveStatus(202)

        val result5 = CitizenDetailsStub.getCitizen("nino", user.nino.get.value)
        result5 should haveStatus(583)

        val result6 = SpecialCases.deleteSpecialCase(sc.id.get.value)
        result6 should haveStatus(204)

        val result7 = CitizenDetailsStub.getCitizen("nino", user.nino.get.value)
        result7 should haveStatus(200)
      }

      "replace an ordinary POST response" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val agent = UserGenerator.agent()
        Users.create(agent)
        Users.create(UserGenerator.individual().withPrincipalEnrolment("HMRC-MTD-IT", "MTDITID", "RLWA69482506648"))

        val result1 = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          agent.groupId.get,
          "HMRC-MTD-IT~MTDITID~RLWA69482506648",
          Json.parse(s"""{
            |    "userId" : "${agent.userId}",
            |    "type" : "delegated"
            |}""".stripMargin),
          `legacy-agentCode` = agent.agentCode
        )

        result1 should haveStatus(201)

        val result2 = SpecialCases.createSpecialCase(
          SpecialCase(
            requestMatch = SpecialCase.RequestMatch(
              method = "POST",
              path =
                s"/enrolment-store-proxy/enrolment-store/groups/${agent.groupId.get}/enrolments/HMRC-MTD-IT~MTDITID~RLWA69482506648?legacy-agentCode=${agent.agentCode.get}"
            ),
            response = SpecialCase.Response(506)
          )
        )
        result2 should haveStatus(201)

        val result3 = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          agent.groupId.get,
          "HMRC-MTD-IT~MTDITID~RLWA69482506648",
          Json.parse(s"""{
            |    "userId" : "${agent.userId}",
            |    "type" : "delegated"
            |}""".stripMargin),
          `legacy-agentCode` = agent.agentCode
        )

        result3 should haveStatus(506)
      }

      "replace an ordinary GET response having X-SessionID only" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result2 = SpecialCases.createSpecialCase(
          SpecialCase(
            requestMatch = SpecialCase.RequestMatch(path = s"/registration/business-details/foo/bar"),
            response = SpecialCase
              .Response(427, Some("""{"a":"b"}"""), Seq(SpecialCase.Header(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)))
          )
        )
        result2 should haveStatus(201)
        val sc = get(result2.header(HeaderNames.LOCATION).get).json.as[SpecialCase]

        val result3 =
          DesStub.getBusinessDetails("foo", "bar")(AuthContext.fromTokenAndSessionId("foo", session.sessionId))
        result3 should haveStatus(427)
        result3.header(HeaderNames.CONTENT_TYPE) shouldBe Some("application/json")
        (result3.json \ "a").as[String] shouldBe "b"

        val result4 =
          SpecialCases.updateSpecialCase(sc.id.get.value, sc.copy(response = sc.response.copy(status = 583)))
        result4 should haveStatus(202)

        val result5 =
          DesStub.getBusinessDetails("foo", "bar")(AuthContext.fromTokenAndSessionId("foo", session.sessionId))
        result5 should haveStatus(583)

        val result6 = SpecialCases.deleteSpecialCase(sc.id.get.value)
        result6 should haveStatus(204)

        val result7 =
          DesStub.getBusinessDetails("foo", "bar")(AuthContext.fromTokenAndSessionId("foo", session.sessionId))
        result7 should haveStatus(400)
      }
    }
  }
}
