package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Enrolment, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, ServerBaseISpec, TestRequests}

class EnrolmentStoreProxyStubControllerISpec
    extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]

  "EnrolmentStoreProxyStubController" when {

    "GET /enrolment-store/enrolments/:enrolmentKey/users?type=principal" should {
      "respond 200 with user ids matching provided principal enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~12345678", "principal")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalUserIds").as[Seq[String]] shouldBe Seq("foo1")
        (json \ "delegatedUserIds").asOpt[Seq[String]] shouldBe None
      }

      "respond 200 with user ids matching provided delegated enrolment key" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

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
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

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
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

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
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

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
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

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
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .individual(userId = "foo2", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "87654321"))
        Users.create(
          UserGenerator
            .agent(userId = "foo3", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))
        Users.create(
          UserGenerator
            .agent(userId = "foo4", groupId = "group2")
            .withDelegatedEnrolment("IR-SA", "UTR", "87654321"))

        val otherSession: AuthenticatedSession = SignIn.signInAndGetSession("foo5", planetId = "saturn")
        Users.update(
          UserGenerator
            .individual(userId = "foo3", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))(otherSession)

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
        user.principalEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "allocate delegated enrolment to the agent identified by userId and groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("0000000021313132")
        Users.update(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"))
        Users.create(UserGenerator.individual().withPrincipalEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
                       |    "userId" : "0000000021313132",
                       |    "type" :         "delegated"
                       |}""".stripMargin)
        )

        result should haveStatus(201)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.delegatedEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "fail to allocate delegated enrolment to the agent if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("0000000021313132")
        Users.update(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
                       |    "userId" : "0000000021313132",
                       |    "type" :         "delegated"
                       |}""".stripMargin)
        )

        result should haveStatus(400)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.delegatedEnrolments.isEmpty shouldBe true
      }

      "allocate delegated enrolment to the agent identified by legacy-agentCode" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("0000000021313132")
        Users.update(UserGenerator.agent(userId = "0000000021313132", groupId = "group1", agentCode = "ABC123"))
        Users.create(UserGenerator.individual().withPrincipalEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
                       |    "userId" : "0000000021313132",
                       |    "type" :         "delegated"
                       |}""".stripMargin),
          `legacy-agentCode` = Some("ABC123")
        )

        result should haveStatus(201)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.delegatedEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))
      }

      "fail to allocate delegated enrolment to the agent (identified by legacy-agentCode) if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("0000000021313132")
        Users.update(UserGenerator.agent(userId = "0000000021313132", groupId = "group1", agentCode = "ABC123"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
                       |    "userId" : "0000000021313132",
                       |    "type" :         "delegated"
                       |}""".stripMargin),
          `legacy-agentCode` = Some("ABC123")
        )

        result should haveStatus(400)
      }

      "return 400 if groupId does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("00000000123166122235")
        Users.update(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"))

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{
                       |    "userId" : "00000000123166122235",
                       |    "type":         "principal"
                       |}""".stripMargin)
        )

        result should haveStatus(400)
      }

      "return 400 if userId does not exist" in {
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

        result should haveStatus(400)
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
    }

    "DELETE /enrolment-store/groups/:groupId/enrolments/:enrolmentKey" should {
      "deallocate principal enrolment from the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withPrincipalEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", "IR-SA~UTR~12345678")

        result should haveStatus(204)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.principalEnrolments.isEmpty shouldBe true
      }

      "deallocate delegated enrolment from the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.update(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withDelegatedEnrolment("IR-SA", "UTR", "12345678"))

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", "IR-SA~UTR~12345678")

        result should haveStatus(204)

        val user = await(userService.findByUserId(session.userId, session.planetId)).get
        user.principalEnrolments.isEmpty shouldBe true
      }
    }
  }
}
