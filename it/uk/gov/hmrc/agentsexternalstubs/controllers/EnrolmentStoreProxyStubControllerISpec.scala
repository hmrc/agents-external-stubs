package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.AssignedClient
import uk.gov.hmrc.agentmtdidentifiers.model.{Identifier => MtdIdentifier}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.SetKnownFactsRequest
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, MongoDB, ServerBaseISpec, TestRequests}

class EnrolmentStoreProxyStubControllerISpec extends ServerBaseISpec with MongoDB with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  lazy val wsClient = app.injector.instanceOf[WSClient]

  "EnrolmentStoreProxyStubController" when {

    "GET /enrolment-store/groups/:groupId/delegated" when {

      "a single user is assigned to a client" should {
        "return id of the assigned user" in {
          implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

          Users.update(
            UserGenerator
              .agent(userId = "foo1", groupId = "group1")
              .withAssignedEnrolment("IR-SA", "UTR", "12345678")
          )

          val result = EnrolmentStoreProxyStub.getDelegatedEnrolments("group1")

          result should haveStatus(200)
          val json = result.json

          (json \ "clients").as[Seq[AssignedClient]] shouldBe Seq(
            AssignedClient("IR-SA", Seq(MtdIdentifier("UTR", "12345678")), None, "foo1")
          )
        }
      }
    }

    "GET /enrolment-store/enrolments/:enrolmentKey/users?type=principal" should {
      "respond 200 with user ids matching provided principal enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~12345678", "principal")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalUserIds").as[Seq[String]] shouldBe Seq("foo1")
        (json \ "delegatedUserIds").asOpt[Seq[String]] shouldBe None
      }

      "respond 200 with user ids matching provided assigned enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withAssignedEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withAssignedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~12345678", "delegated")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalUserIds").asOpt[Seq[String]] shouldBe None
        (json \ "delegatedUserIds").as[Seq[String]] should contain.only("foo2", "foo3")
      }

      "respond 200 with user ids matching provided principal and delegated enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withAssignedEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withAssignedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~12345678", "all")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalUserIds").as[Seq[String]] shouldBe Seq("foo1")
        (json \ "delegatedUserIds").as[Seq[String]] should contain.only("foo2", "foo3")
      }

      "respond 204 if enrolment key not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~87654321", "all")

        result should haveStatus(204)
      }

      "respond 400 if enrolment key is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~~87654321", "all")

        result should haveStatus(400)
      }
    }

    "GET /enrolment-store/enrolments/:enrolmentKey/groups?type=principal" should {
      "respond 200 with group ids matching provided principal enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getGroupIds("IR-SA~UTR~12345678", "principal")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalGroupIds").as[Seq[String]] should contain.only("group1")
        (json \ "delegatedGroupIds").asOpt[Seq[String]] shouldBe None
      }

      "respond 200 with group ids matching provided delegated enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getGroupIds("IR-SA~UTR~12345678", "delegated")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalGroupIds").asOpt[Seq[String]] shouldBe None
        (json \ "delegatedGroupIds").as[Seq[String]] should contain.only("group2")
      }

      "respond 200 with group ids matching provided principal and delegated enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .individual(userId = "foo2", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "87654321")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo4", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "87654321")
        )

        val otherSession: AuthenticatedSession = SignIn.signInAndGetSession("foo5")
        Users.update(
          UserGenerator
            .individual(userId = "foo3", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )(otherSession)

        val result = EnrolmentStoreProxyStub.getGroupIds("IR-SA~UTR~12345678", "all")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalGroupIds").as[Seq[String]] should contain.only("group1")
        (json \ "delegatedGroupIds").as[Seq[String]] should contain.only("group2")
      }

      "respond 400 if enrolment key is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = EnrolmentStoreProxyStub.getGroupIds("~UTR~87654321", "all")

        result should haveStatus(400)
      }

    }

    "POST /enrolment-store/groups/:groupId/enrolments/:enrolmentKey" should {
      "allocate principal enrolment to the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("00000000123166122235")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )
        Users.update(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "00000000123166122235",
            |    "friendlyName": "My Self Assessment",
            |    "type":         "principal",
            |    "verifiers": [
            |       {
            |          "key": "Postcode",
            |          "value": "aa11aa"
            |       },
            |       {
            |          "key": "NINO",
            |          "value": "aa123456a"
            |       }
            |    ]
            |}""".stripMargin)
        )

        result should haveStatus(201)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.principal should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "allocate delegated enrolment to the agent identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"))
        Users.create(UserGenerator.individual().withPrincipalEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(201)

        val user = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.enrolments.delegated should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "fail to allocate delegated enrolment to the agent if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"))
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(400)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.delegated.isEmpty shouldBe true
      }

      "allocate delegated enrolment to the agent identified by legacy-agentCode" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1", agentCode = "ABC123"))
        Users.create(UserGenerator.individual().withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo",
            |    "type" :         "delegated"
            |}""".stripMargin),
          `legacy-agentCode` = Some("ABC123")
        )

        result should haveStatus(201)

        val user = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.enrolments.delegated should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "fail to allocate delegated enrolment to the agent (identified by legacy-agentCode) if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1", agentCode = "ABC123"))
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo",
            |    "type" :         "delegated"
            |}""".stripMargin),
          `legacy-agentCode` = Some("ABC123")
        )

        result should haveStatus(400)
      }

      "return 400 if groupId does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo",
            |    "type":         "principal"
            |}""".stripMargin)
        )

        result should haveStatus(400)
      }

      "return 400 if userId does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo1",
            |    "type":         "principal"
            |}""".stripMargin)
        )

        result should haveStatus(400)
      }

      "return 404 if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("00000000123166122235")
        Users.update(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo1",
            |    "type":         "principal"
            |}""".stripMargin)
        )

        result should haveStatus(404)
      }

      "return 400 if enrolment key is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("00000000123166122235")
        Users.update(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "00000000123166122235",
          "IR-SA~UTR~",
          Json.parse("""{
            |    "userId" : "foo1",
            |    "type":         "principal"
            |}""".stripMargin)
        )

        result should haveStatus(400)
      }

      "return 409 if principal enrolment is already assigned" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA~UTR~12345678")
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId": "foo1",
            |    "type": "principal"
            |}""".stripMargin)
        )

        result should haveStatus(409)
      }

      "return 409 if delegated enrolment is already assigned" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "IR-SA~UTR~12345678",
            SetKnownFactsRequest
              .generate("IR-SA~UTR~12345678", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )
        Users.create(
          UserGenerator
            .individual(userId = "foo2", groupId = "group2")
            .withPrincipalEnrolment("IR-SA~UTR~12345678")
        )
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withDelegatedEnrolment("IR-SA~UTR~12345678")
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId": "foo1",
            |    "type": "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(409)
      }
    }

    "DELETE /enrolment-store/groups/:groupId/enrolments/:enrolmentKey" should {
      "deallocate principal enrolment from the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", "IR-SA~UTR~12345678")

        result should haveStatus(204)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.principal.isEmpty shouldBe true
      }

      "deallocate delegated enrolment from the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", "IR-SA~UTR~12345678")

        result should haveStatus(204)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.delegated.isEmpty shouldBe true
      }

      "deallocate delegated enrolment from the group identified by legacy-AgentCode" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1", agentCode = "ABCDEF")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678", `legacy-agentCode` = Some("ABCDEF"))

        result should haveStatus(204)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.delegated.isEmpty shouldBe true
      }

      "fail if groupId is not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1", agentCode = "ABCDEF")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678")

        result should haveStatus(400)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.delegated.isEmpty should not be true
      }

      "fail if legacy-AgentCode is not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1", agentCode = "ABCDEF")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678", `legacy-agentCode` = Some("FFFFFFF"))

        result should haveStatus(400)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.enrolments.delegated.isEmpty should not be true
      }
    }

    "PUT /enrolment-store/enrolments/:enrolmentKey" should {
      "return 204 NoContent" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val result = EnrolmentStoreProxyStub.setKnownFacts(
          enrolmentKey = "IR-SA~UTR~12345678",
          payload = Json.parse("""{
            |  "verifiers": [
            |    {
            |      "key": "Postcode",
            |      "value": "TF2 6NU"
            |    },
            |    {
            |      "key": "NINO",
            |      "value": "AB123456X"
            |    }
            |  ],
            |  "legacy": {
            |    "previousVerifiers": [
            |      {
            |        "key": "Postcode",
            |        "value": "TF2 6NU"
            |      },
            |      {
            |        "key": "NINO",
            |        "value": "AB123456X"
            |      }
            |    ]
            |  }
            |}
          """.stripMargin)
        )

        result should haveStatus(204)
      }
    }

    "DELETE /enrolment-store/enrolments/:enrolmentKey" should {
      "return 204 NoContent" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        EnrolmentStoreProxyStub.setKnownFacts(
          enrolmentKey = "IR-SA~UTR~12345678",
          payload = Json.parse("""{
            |  "verifiers": [
            |    {
            |      "key": "Postcode",
            |      "value": "TF2 6NU"
            |    },
            |    {
            |      "key": "NINO",
            |      "value": "AB123456X"
            |    }
            |  ]
            |}
                               """.stripMargin)
        )

        val result = EnrolmentStoreProxyStub.removeKnownFacts("IR-SA~UTR~12345678")

        result should haveStatus(204)
      }

      "return 204 if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.removeKnownFacts("IR-SA~UTR~12345678")

        result should haveStatus(204)
      }
    }

    "GET /enrolment-store/users/:userId/enrolments" should {
      "return 204 with an empty list of principal enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId)

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of principal enrolments if assigned to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolment = Enrolment("IR-SA", "UTR", "12345678")
        Users.update(
          UserGenerator
            .individual(userId = session.userId)
            .withPrincipalEnrolment(enrolment)
            .updateAssignedEnrolments(_ ++ enrolment.toEnrolmentKey.toSeq)
        )

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId)

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(1)) and haveProperty[Int]("totalRecords", be(1)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", oneOfValues("UTR", "Postcode", "NINO", "IsVIP", "DAT")) and haveProperty[
                String
              ]("value")
            )
          )
        )
      }

      "return 204 with an empty list of principal enrolments if allocated to the group but not to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolment = Enrolment("IR-SA", "UTR", "12345678")
        Users.update(
          UserGenerator
            .individual(userId = session.userId)
            .withPrincipalEnrolment(enrolment)
            .updateAssignedEnrolments(_ => Seq.empty)
        )

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId)

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 204 with an empty list of delegated enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `type` = "delegated")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of delegated enrolments if assigned to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolments = Seq(
          Enrolment("IR-SA", "UTR", "12345678"),
          Enrolment("IR-SA", "UTR", "12345670")
        )
        Users.update(
          UserGenerator
            .agent(userId = session.userId, agentCode = "ABCDEF")
            .updateDelegatedEnrolments(_ => enrolments)
            .updateAssignedEnrolments(_ => enrolments.flatMap(_.toEnrolmentKey))
        )

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `type` = "delegated")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(1)) and haveProperty[Int]("totalRecords", be(2)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", be("UTR")) and haveProperty[String](
                "value",
                oneOfValues("12345678", "12345670")
              )
            )
          )
        )
      }

      "return 204 with an empty list of delegated enrolments if assigned to the group but not to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .agent(userId = session.userId, agentCode = "ABCDEF")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345670")
            .updateAssignedEnrolments(_ => Seq.empty)
        )

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `type` = "delegated")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a paginated list of delegated enrolments assigned to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolments = Seq(
          Enrolment("IR-SA", "UTR", "12345670"),
          Enrolment("IR-SA", "UTR", "12345671"),
          Enrolment("IR-SA", "UTR", "12345672"),
          Enrolment("IR-SA", "UTR", "12345673"),
          Enrolment("IR-SA", "UTR", "12345674"),
          Enrolment("IR-SA", "UTR", "12345675"),
          Enrolment("IR-SA", "UTR", "12345676"),
          Enrolment("IR-SA", "UTR", "12345677"),
          Enrolment("IR-SA", "UTR", "12345678"),
          Enrolment("IR-SA", "UTR", "12345679"),
          Enrolment("IR-SA", "UTR", "12345680"),
          Enrolment("IR-SA", "UTR", "12345681"),
          Enrolment("IR-SA", "UTR", "12345682"),
          Enrolment("IR-SA", "UTR", "12345683"),
          Enrolment("IR-SA", "UTR", "12345684"),
          Enrolment("IR-SA", "UTR", "12345685"),
          Enrolment("IR-SA", "UTR", "12345686"),
          Enrolment("IR-SA", "UTR", "12345687"),
          Enrolment("IR-SA", "UTR", "12345688"),
          Enrolment("IR-SA", "UTR", "12345689"),
          Enrolment("IR-SA", "UTR", "12345690"),
          Enrolment("IR-SA", "UTR", "12345691")
        )
        val updateResult = Users.update(
          UserGenerator
            .agent(userId = session.userId)
            .updateDelegatedEnrolments(_ => enrolments)
            .updateAssignedEnrolments(_ => enrolments.flatMap(_.toEnrolmentKey))
        )
        updateResult should haveStatus(202)

        val result = EnrolmentStoreProxyStub
          .getUserEnrolments(session.userId, `type` = "delegated", `start-record` = Some(3), `max-records` = Some(12))

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(3)) and haveProperty[Int]("totalRecords", be(12)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", be("UTR")) and haveProperty[String](
                "value",
                oneOfValues(
                  "12345672",
                  "12345673",
                  "12345674",
                  "12345675",
                  "12345676",
                  "12345677",
                  "12345678",
                  "12345679",
                  "12345680",
                  "12345681",
                  "12345682",
                  "12345683"
                )
              )
            )
          )
        )
      }

      "return 404 if userId not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments("foo")

        result should haveStatus(404)
      }

      "return 400 if type param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `type` = "foo")

        result should haveStatus(400)
      }

      "return 400 if service param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, service = Some("FOO"))

        result should haveStatus(400)
      }

      "return 400 if start-record param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `start-record` = Some(-1))

        result should haveStatus(400)
      }

      "return 400 if max-records param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `max-records` = Some(1001))

        result should haveStatus(400)
      }
    }

    "GET /enrolment-store/groups/:groupId/enrolments" should {
      "return 204 with an empty list of principal enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(userId = session.userId, groupId = "group1")
        )

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of principal enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(userId = session.userId, groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678")
        )

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(1)) and haveProperty[Int]("totalRecords", be(1)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", oneOfValues("UTR", "Postcode", "NINO", "IsVIP", "DAT")) and haveProperty[
                String
              ]("value")
            )
          )
        )
      }

      "return 204 with an empty list of delegated enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        Users.update(
          UserGenerator
            .individual(userId = session.userId, groupId = "group1")
        )

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1", `type` = "delegated")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of delegated enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .agent(userId = session.userId, groupId = "group1", agentCode = "ABCDEF")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345670")
        )

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1", `type` = "delegated")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(1)) and haveProperty[Int]("totalRecords", be(2)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", be("UTR")) and haveProperty[String](
                "value",
                oneOfValues("12345678", "12345670")
              )
            )
          )
        )
      }

      "return 200 with a paginated list of delegated enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val updateResult = Users.update(
          UserGenerator
            .agent(userId = session.userId, groupId = "group1")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345670")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345671")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345672")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345673")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345674")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345675")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345676")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345677")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345679")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345680")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345681")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345682")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345683")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345684")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345685")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345686")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345687")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345688")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345689")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345690")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345691")
        )
        updateResult should haveStatus(202)

        val result = EnrolmentStoreProxyStub
          .getGroupEnrolments("group1", `type` = "delegated", `start-record` = Some(3), `max-records` = Some(12))

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(3)) and haveProperty[Int]("totalRecords", be(12)) and havePropertyArrayOf[
            JsObject
          ](
            "enrolments",
            haveProperty[String]("service", be("IR-SA")) and haveProperty[String]("state") and havePropertyArrayOf[
              JsObject
            ](
              "identifiers",
              haveProperty[String]("key", be("UTR")) and haveProperty[String](
                "value",
                oneOfValues(
                  "12345672",
                  "12345673",
                  "12345674",
                  "12345675",
                  "12345676",
                  "12345677",
                  "12345678",
                  "12345679",
                  "12345680",
                  "12345681",
                  "12345682",
                  "12345683"
                )
              )
            )
          )
        )
      }

      "return 404 if groupId not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("foo")

        result should haveStatus(404)
      }

      "return 400 if type param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("foo", `type` = "foo")

        result should haveStatus(400)
      }

      "return 400 if service param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("foo", service = Some("FOO"))

        result should haveStatus(400)
      }

      "return 400 if start-record param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("foo", `start-record` = Some(-1))

        result should haveStatus(400)
      }

      "return 400 if max-records param is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("foo", `max-records` = Some(1001))

        result should haveStatus(400)
      }
    }

    "PUT /tax-enrolments/groups/:groupId/enrolments/:enrolmentKey/friendly_name" should {
      "update a principal enrolment belonging to the groupId with the friendlyName specified in the request" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(userId = session.userId, groupId = "group2")
            .withPrincipalEnrolment("IR-SA~UTR~12345678")
        )
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"friendlyName": "friendlyHugs"}""")
        )

        result should haveStatus(204)

      }

      "update a delegated enrolment belonging to the groupId with the friendlyName specified in the request" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .agent(userId = session.userId, groupId = "group2", credentialRole = "Admin")
            .withDelegatedEnrolment("IR-SA~UTR~12345678")
        )
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"friendlyName": "friendlyHugs"}""")
        )

        result should haveStatus(204)
      }

      "return 400 BadRequest if the payload is invalid " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"somethingElse": "..."}""")
        )
        result should haveStatus(400)
      }

      "return 404 NotFound if the groupId does not exist " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"friendlyName": "friendlyHugs"}""")
        )
        result should haveStatus(404)
      }

      "return 404 NotFound if the enrolment is not found " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2", credentialRole = "Admin")
            .withDelegatedEnrolment("IR-SA~UTR~12345678")
        )
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~22222222",
          Json.parse("""{"friendlyName": "friendlyHugs"}""")
        )
        result should haveStatus(404)

      }

    }
    "POST /tax-enrolments/users/:userId/enrolments/:enrolmentKey (ES11)" should {
      val enrolmentKey = "HMRC-MTD-VAT~VRN~123456789"
      val anotherEnrolmentKey = "HMRC-MTD-VAT~VRN~987654321"
      val adminUser = UserGenerator
        .agent(userId = "testAdmin", groupId = "testGroup", credentialRole = "Admin")
        .withDelegatedEnrolment(enrolmentKey)
      val assistantUser = UserGenerator
        .agent(userId = "testAssistant", groupId = "testGroup", credentialRole = "Assistant")
      def setKnownFacts()(implicit ac: AuthContext) = EnrolmentStoreProxyStub
        .setKnownFacts(
          enrolmentKey,
          SetKnownFactsRequest
            .generate(enrolmentKey, _ => None)
            .getOrElse(throw new Exception("Could not generate known facts"))
        )

      "assign an enrolment to a user successfully" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        Users.create(assistantUser)
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(201)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.enrolments.assigned should contain.only(EnrolmentKey(enrolmentKey))
      }
      "return 400 Bad Request if the user was already assigned the enrolment" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        Users.create(assistantUser.updateAssignedEnrolments(_ => Seq(EnrolmentKey(enrolmentKey))))
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(400)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.enrolments.assigned should contain.only(EnrolmentKey(enrolmentKey))
      }
      "return 403 Forbidden if the enrolment is not allocated to the user's group" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser.updateDelegatedEnrolments(_ => Seq.empty))
        Users.create(assistantUser)
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(403)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.enrolments.assigned should be(empty)
      }
      "return 404 Not Found if the user id does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser.updateDelegatedEnrolments(_ => Seq.empty))
        Users.create(assistantUser)
        val result = EnrolmentStoreProxyStub.assignUser("bar", enrolmentKey)
        result should haveStatus(404)
      }
      "return 404 Not Found if the enrolment key does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        Users.create(assistantUser)
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", anotherEnrolmentKey)
        result should haveStatus(404)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.enrolments.assigned should be(empty)
      }
    }
    "DELETE /tax-enrolments/users/:userId/enrolments/:enrolmentKey (ES12)" should {
      val enrolmentKey = "HMRC-MTD-VAT~VRN~123456789"
      val adminUser = UserGenerator
        .agent(userId = "testAdmin", groupId = "testGroup", credentialRole = "Admin")
        .withDelegatedEnrolment(enrolmentKey)
      val assistantUser = UserGenerator
        .agent(userId = "testAssistant", groupId = "testGroup", credentialRole = "Assistant")
      def setKnownFacts()(implicit ac: AuthContext) = EnrolmentStoreProxyStub
        .setKnownFacts(
          enrolmentKey,
          SetKnownFactsRequest
            .generate(enrolmentKey, _ => None)
            .getOrElse(throw new Exception("Could not generate known facts"))
        )
      "deassign an enrolment successfully" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        Users.create(assistantUser.updateAssignedEnrolments(_ => Seq(EnrolmentKey(enrolmentKey))))
        val result = EnrolmentStoreProxyStub.deassignUser("testAssistant", enrolmentKey)
        result should haveStatus(204)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.enrolments.assigned should be(empty)
      }
      "return 204 No Content (but no error) if the enrolment was not assigned to the user in the first place" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        Users.create(assistantUser)
        val result = EnrolmentStoreProxyStub.deassignUser("testAssistant", enrolmentKey)
        result should haveStatus(204)
      }
      "return 404 Not Found if the user id does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser)
        val result = EnrolmentStoreProxyStub.deassignUser("bar", enrolmentKey)
        result should haveStatus(404)
      }

    }
  }
}
