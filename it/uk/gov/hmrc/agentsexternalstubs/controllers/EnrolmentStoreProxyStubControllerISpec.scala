package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.AssignedClient
import uk.gov.hmrc.agentmtdidentifiers.model.{Identifier => _}
import uk.gov.hmrc.agentsexternalstubs.controllers.EnrolmentStoreProxyStubController.{EnrolmentsFromKnownFactsRequest, SetKnownFactsRequest}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{AuthContext, ServerBaseISpec, TestRequests}

class EnrolmentStoreProxyStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "EnrolmentStoreProxyStubController" when {

    "GET /enrolment-store/groups/:groupId/delegated" when {

      "a single user is assigned to a client" should {
        "return id of the assigned user" in {
          userService
            .createUser(
              UserGenerator
                .agent(userId = "foo1", groupId = "group1")
                .withAssignedDelegatedEnrolment("IR-SA", "UTR", "12345678"),
              planetId = "testPlanetId",
              affinityGroup = Some(AG.Agent)
            )
            .futureValue
          implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanetId")

          val result = EnrolmentStoreProxyStub.getDelegatedEnrolments("group1")

          result should haveStatus(200)
          val json = result.json

          (json \ "clients").as[Seq[AssignedClient]] shouldBe Seq(
            AssignedClient("IR-SA~UTR~12345678", None, "foo1")
          )
        }
      }
    }

    "GET /enrolment-store/enrolments/:enrolmentKey/users?type=principal" should {
      "respond 200 with user ids matching provided principal enrolment key" in {
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo1")
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo2")
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

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
            .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
          Some(AG.Agent)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
          Some(AG.Agent)
        )

        val result = EnrolmentStoreProxyStub.getUserIds("IR-SA~UTR~12345678", "delegated")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalUserIds").asOpt[Seq[String]] shouldBe None
        (json \ "delegatedUserIds").as[Seq[String]] should contain.only("foo2", "foo3")
      }

      "respond 200 with user ids matching provided principal and delegated enrolment key" in {
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo1")
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo2")
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo3")
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

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
            .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2")
            .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
          Some(AG.Agent)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo3")
            .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
          Some(AG.Agent)
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
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo1", groupId = "group1")
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo2", groupId = "group2")
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

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
            .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo2", groupId = "group2")
            .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
          Some(AG.Agent)
        )

        val result = EnrolmentStoreProxyStub.getGroupIds("IR-SA~UTR~12345678", "delegated")

        result should haveStatus(200)
        val json = result.json
        (json \ "principalGroupIds").asOpt[Seq[String]] shouldBe None
        (json \ "delegatedGroupIds").as[Seq[String]] should contain.only("group2")
      }

      "respond 200 with group ids matching provided principal and delegated enrolment key" in {
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo1", groupId = "group1")
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo2", groupId = "group1", credentialRole = User.CR.Assistant)
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "87654321").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo3", groupId = "group2")
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo4", groupId = "group2", credentialRole = User.CR.Assistant)
              .withAssignedDelegatedEnrolment(Enrolment("IR-SA", "UTR", "87654321").toEnrolmentKey.get),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        // create a user on another planet
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo3", groupId = "group1")
              .withAssignedPrincipalEnrolment(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get),
            planetId = "otherPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.getGroupIds("IR-SA~UTR~12345678", "all")

        result should haveStatus(200)
        Thread.sleep(1000)
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
        userService
          .createUser(
            UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession("00000000123166122235", planetId = "testPlanet")
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

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.principalEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user: User = await(userService.findByUserId("00000000123166122235", session.planetId)).get
        user.assignedPrincipalEnrolments should contain.only(EnrolmentKey("IR-SA~UTR~12345678"))
      }

      "allocate delegated enrolment to the group identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"), Some(AG.Agent))
        Users.create(
          UserGenerator.individual().withAssignedPrincipalEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Individual)
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "0000000021313132",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(201)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user: User = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey("IR-SA~UTR~12345678"))
      }
      "DelegationType to be secondary for HMRC-MTD-IT-SUPP" in {
        val delegationType =
          DelegationEnrolmentKeys(EnrolmentKey("HMRC-MTD-IT-SUPP", Seq(Identifier("MTDITID", "ZIZI45093893553"))))

        delegationType.isPrimary shouldBe false
        delegationType.primaryEnrolmentKey.tag shouldBe "HMRC-MTD-IT~MTDITID~ZIZI45093893553"
        delegationType.delegatedEnrolmentKey.tag shouldBe "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553"
        delegationType.delegationEnrolments.map(_.tag) should contain allElementsOf List(
          "HMRC-MTD-IT~MTDITID~ZIZI45093893553",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553"
        )
      }

      "DelegationType to be primary for IR-SA" in {
        val delegationType =
          DelegationEnrolmentKeys(EnrolmentKey("IR-SA", Seq(Identifier("UTR", "12345678"))))

        delegationType.isPrimary shouldBe true
        delegationType.primaryEnrolmentKey.tag shouldBe "IR-SA~UTR~12345678"
        delegationType.delegatedEnrolmentKey.tag shouldBe "IR-SA~UTR~12345678"
        delegationType.delegationEnrolments.map(_.tag) should contain allElementsOf List("IR-SA~UTR~12345678")
      }

      "allocate delegated secondary enrolment to the group" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        val delegationType =
          DelegationEnrolmentKeys(EnrolmentKey("HMRC-MTD-IT-SUPP", Seq(Identifier("MTDITID", "ZIZI45093893553"))))

        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"), Some(AG.Agent))
        Users.create(
          UserGenerator
            .individual()
            .withAssignedPrincipalEnrolment(delegationType.primaryEnrolmentKey),
          Some(AG.Individual)
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          delegationType.delegatedEnrolmentKey.tag,
          Json.parse("""{
            |    "userId" : "0000000021313132",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(201)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments should contain.only(Enrolment("HMRC-MTD-IT-SUPP", "MTDITID", "ZIZI45093893553"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user: User = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553"))
      }

      "allocate delegated secondary enrolment to two groups identified by groupId" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"), Some(AG.Agent))
        Users.create(UserGenerator.agent(userId = "0000000021313133", groupId = "group2"), Some(AG.Agent))
        Users.create(
          UserGenerator
            .individual()
            .withAssignedPrincipalEnrolment(EnrolmentKey("HMRC-MTD-IT~MTDITID~ZIZI45093893553")),
          Some(AG.Individual)
        )

        //Delegate first agent
        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553",
          Json.parse("""{
            |    "userId" : "0000000021313132",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(201)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments should contain.only(Enrolment("HMRC-MTD-IT-SUPP", "MTDITID", "ZIZI45093893553"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user: User = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553"))

        //Delegate second agent
        val result2 = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group2",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553",
          Json.parse("""{
            |    "userId" : "0000000021313133",
            |    "type" :         "delegated"
            |}""".stripMargin)
        )

        result2 should haveStatus(201)

        val group2: Group = await(groupsService.findByGroupId("group2", session.planetId)).get
        group2.delegatedEnrolments should contain.only(Enrolment("HMRC-MTD-IT-SUPP", "MTDITID", "ZIZI45093893553"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user2: User = await(userService.findByUserId("0000000021313133", session.planetId)).get
        user2.assignedDelegatedEnrolments should contain.only(EnrolmentKey("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553"))
      }

      "return 409 for secondary enrolment if delegated secondary enrolment is already assigned " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "HMRC-MTD-IT~MTDITID~ZIZI45093893553",
            SetKnownFactsRequest
              .generate("HMRC-MTD-IT~MTDITID~ZIZI45093893553", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )
        Users.create(
          UserGenerator
            .individual(userId = "foo2", groupId = "group2")
            .withAssignedPrincipalEnrolment(EnrolmentKey("HMRC-MTD-IT~MTDITID~ZIZI45093893553")),
          Some(AG.Individual)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553")),
          Some(AG.Agent)
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553",
          Json.parse("""{
            |    "userId": "foo1",
            |    "type": "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(409)
      }

      "return 409 for secondary enrolment if delegated primary enrolment is already assigned \"" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "HMRC-MTD-IT~MTDITID~ZIZI45093893553",
            SetKnownFactsRequest
              .generate("HMRC-MTD-IT~MTDITID~ZIZI45093893553", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )
        Users.create(
          UserGenerator
            .individual(userId = "foo2", groupId = "group2")
            .withAssignedPrincipalEnrolment(EnrolmentKey("HMRC-MTD-IT~MTDITID~ZIZI45093893553")),
          Some(AG.Individual)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("HMRC-MTD-IT~MTDITID~ZIZI45093893553")),
          Some(AG.Agent)
        )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553",
          Json.parse("""{
            |    "userId": "foo1",
            |    "type": "delegated"
            |}""".stripMargin)
        )

        result should haveStatus(409)
      }

      "return 400, principal allocation fail for secondary enrolmentKey " in {
        userService
          .createUser(
            UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession("00000000123166122235", planetId = "testPlanet")
        EnrolmentStoreProxyStub
          .setKnownFacts(
            "HMRC-MTD-IT~MTDITID~ZIZI45093893553",
            SetKnownFactsRequest
              .generate("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553", _ => None)
              .getOrElse(throw new Exception("Could not generate known facts"))
          )

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553",
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

        result should haveStatus(400)
      }

      "fail to allocate delegated enrolment to the agent if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(UserGenerator.agent(userId = "0000000021313132", groupId = "group1"), Some(AG.Agent))
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

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments.isEmpty shouldBe true
      }

      "allocate delegated enrolment to the group identified by legacy-agentCode" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(
          UserGenerator.agent(userId = "0000000021313132", groupId = "group1"),
          Some(AG.Agent),
          agentCode = Some("ABC123")
        )
        Users.create(
          UserGenerator
            .individual()
            .copy(assignedPrincipalEnrolments = Seq(Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get)),
          affinityGroup = Some(AG.Individual)
        )
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
            |    "userId" : "0000000021313132",
            |    "type" :         "delegated"
            |}""".stripMargin),
          `legacy-agentCode` = Some("ABC123")
        )

        result should haveStatus(201)

        val group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments should contain.only(Enrolment("IR-SA", "UTR", "12345678"))

        // The user specified in the request should now have the new enrolment assigned to them
        val user: User = await(userService.findByUserId("0000000021313132", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey("IR-SA~UTR~12345678"))
      }

      "fail to allocate delegated enrolment to the agent (identified by legacy-agentCode) if enrolment does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.create(
          UserGenerator.agent(userId = "0000000021313132", groupId = "group1"),
          Some(AG.Agent),
          agentCode = Some("ABC123")
        )
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
        Users.create(UserGenerator.individual(userId = "00000000123166122235", groupId = "group1"), Some(AG.Individual))
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

      "return 403 if userId does not exist" in {
        userService
          .createUser(
            UserGenerator.agent(userId = "00000000123166122235", groupId = "group1"),
            "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession("00000000123166122235", planetId = "testPlanet")

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

        result should haveStatus(403)
      }

      "return 404 if enrolment does not exist" in {
        userService
          .createUser(
            UserGenerator.agent(userId = "00000000123166122235", groupId = "group1"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        userService
          .createUser(
            UserGenerator.individual(userId = "foo1", groupId = "group2"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession("00000000123166122235", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.allocateEnrolmentToGroup(
          "group1",
          "IR-SA~UTR~12345678",
          Json.parse("""{
            |    "userId" : "foo1",
            |    "type": "delegated"
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
        Users.create(
          UserGenerator
            .individual(userId = "foo1", groupId = "group1")
            .withAssignedPrincipalEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Individual)
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
            .withAssignedPrincipalEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Individual)
        )
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Agent)
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
        userService
          .createUser(
            UserGenerator
              .individual("foo", groupId = "group1")
              .withAssignedPrincipalEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
            "testPlanet",
            Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", "IR-SA~UTR~12345678")

        result should haveStatus(204)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.principalEnrolments.isEmpty shouldBe true
      }

      "deallocate delegated enrolment from the group identified by groupId (and unassign enrolment from all users)" in {
        val enrolmentKey = EnrolmentKey("IR-SA~UTR~12345678")
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo1", groupId = "group1")
              .withAssignedDelegatedEnrolment(enrolmentKey),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        // create another user belonging to the same group, with the same assigned delegated enrolment
        userService
          .createUser(
            UserGenerator
              .agent(userId = "foo2", groupId = "group1", credentialRole = User.CR.Assistant)
              .withAssignedDelegatedEnrolment(enrolmentKey),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        // create a third user belonging to a DIFFERENT group, with the same assigned delegated enrolment
        userService
          .createUser(
            UserGenerator
              .agent(userId = "anotherFoo", groupId = "anotherGroup")
              .withAssignedDelegatedEnrolment(enrolmentKey),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1", planetId = "testPlanet")

        // check that the two users to which we have assigned the delegated enrolment, show up in the query
        userService
          .findUserIdsByAssignedDelegatedEnrolmentKey(enrolmentKey, "testPlanet")
          .futureValue
          .toSet shouldBe Set("foo1", "foo2", "anotherFoo")

        val result = EnrolmentStoreProxyStub.deallocateEnrolmentFromGroup("group1", enrolmentKey.tag)

        result should haveStatus(204)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments.isEmpty shouldBe true

        // Check that the two group users to which we have assigned the delegated enrolment, no longer have the assignment.
        // However, it should have been unassigned only from the users of the group from which we unallocated the enrolment:
        // if someone in a DIFFERENT group had that enrolment assigned, they should still have it
        userService
          .findUserIdsByAssignedDelegatedEnrolmentKey(enrolmentKey, "testPlanet")
          .futureValue
          .toSet shouldBe Set("anotherFoo")
      }

      "deallocate delegated enrolment from the group identified by legacy-AgentCode" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Agent),
          agentCode = Some("ABCDEF")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678", `legacy-agentCode` = Some("ABCDEF"))

        result should haveStatus(204)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments.isEmpty shouldBe true
      }

      "fail if groupId is not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Agent),
          agentCode = Some("ABCDEF")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678")

        result should haveStatus(400)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments.isEmpty should not be true
      }

      "fail if legacy-AgentCode is not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        Users.create(
          UserGenerator
            .agent(userId = "foo1", groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          affinityGroup = Some(AG.Agent),
          agentCode = Some("ABCDEF")
        )

        val result = EnrolmentStoreProxyStub
          .deallocateEnrolmentFromGroup("group2", "IR-SA~UTR~12345678", `legacy-agentCode` = Some("FFFFFFF"))

        result should haveStatus(400)

        val group: Group = await(groupsService.findByGroupId("group1", session.planetId)).get
        group.delegatedEnrolments.isEmpty should not be true
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
        userService
          .createUser(UserGenerator.individual("foo"), planetId = "testPlanet", Some(AG.Individual))
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.getUserEnrolments("foo")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of principal enrolments if assigned to the user" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        val enrolmentKey = Enrolment("IR-SA", "UTR", "12345678").toEnrolmentKey.get
        userService
          .updateUser(session.userId, session.planetId, _.withAssignedPrincipalEnrolment(enrolmentKey))
          .futureValue

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
        Groups.create(
          GroupGenerator
            .generate(session.planetId, affinityGroup = AG.Agent, groupId = Some("group1"))
            .copy(delegatedEnrolments = Seq(enrolment))
        )
        Users.create(
          UserGenerator
            .individual(userId = session.userId, groupId = "group1")
            .copy(assignedDelegatedEnrolments = Seq.empty),
          Some(AG.Agent)
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
        val enrolments = Seq(
          Enrolment("IR-SA", "UTR", "12345678"),
          Enrolment("IR-SA", "UTR", "12345670")
        )

        val user = userService
          .createUser(
            UserGenerator
              .agent("testUserId")
              .copy(assignedDelegatedEnrolments = enrolments.map(_.toEnrolmentKey.get)),
            "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue
        groupsService.updateGroup(user.groupId.get, "testPlanet", _.copy(agentCode = Some("ABCDEF")))

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("testUserId", planetId = "testPlanet")

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
        Groups.create(
          GroupGenerator
            .generate(session.planetId, affinityGroup = AG.Agent, groupId = Some("group1"))
            .copy(delegatedEnrolments = Seq(Enrolment("IR-SA", "UTR", "12345678")))
        )
        Users.create(
          UserGenerator.agent(userId = session.userId, groupId = "group1"),
          affinityGroup = Some(AG.Agent),
          agentCode = Some("ABCDEF")
        )

        val result = EnrolmentStoreProxyStub.getUserEnrolments(session.userId, `type` = "delegated")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a paginated list of delegated enrolments assigned to the user" in {
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
        userService
          .createUser(
            UserGenerator
              .agent("foo")
              .copy(assignedDelegatedEnrolments = enrolments.map(_.toEnrolmentKey.get)),
            "testPlanet",
            Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub
          .getUserEnrolments("foo", `type` = "delegated", `start-record` = Some(3), `max-records` = Some(12))

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

    //ES3
    "GET /enrolment-store/groups/:groupId/enrolments" should {
      "return 204 with an empty list of principal enrolments" in {
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo", groupId = "group1"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of principal enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.create(
          UserGenerator
            .individual(userId = session.userId, groupId = "group1")
            .withAssignedPrincipalEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Individual)
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
        userService
          .createUser(
            UserGenerator
              .individual(userId = "foo", groupId = "group1"),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Individual)
          )
          .futureValue
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1", `type` = "delegated")

        result should haveStatus(204)
        result.body shouldBe empty
      }

      "return 200 with a list of delegated enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.create(
          UserGenerator
            .agent(userId = session.userId, groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678"))
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345670")),
          Some(AG.Agent),
          agentCode = Some("ABCDEF")
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

      "return 200 with a list of delegated primary and secondary enrolments" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.create(
          UserGenerator
            .agent(userId = session.userId, groupId = "group1")
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678"))
            .withAssignedDelegatedEnrolment(EnrolmentKey("HMRC-MTD-IT-SUPP~MTDITID~ZIZI45093893553")),
          Some(AG.Agent),
          agentCode = Some("ABCDEF")
        )

        val result = EnrolmentStoreProxyStub.getGroupEnrolments("group1", `type` = "delegated")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Int]("startRecord", be(1)) and
            haveProperty[Int]("totalRecords", be(2)) and
            havePropertyArrayOf[JsObject](
              "enrolments",
              haveProperty[String]("service", oneOfValues("IR-SA", "HMRC-MTD-IT-SUPP")) and
                haveProperty[String]("state") and
                havePropertyArrayOf[JsObject](
                  "identifiers",
                  haveProperty[String]("key", oneOfValues("UTR", "MTDITID")) and
                    haveProperty[String]("value", oneOfValues("12345678", "ZIZI45093893553"))
                )
            )
        )
      }

      "return 200 with a paginated list of delegated enrolments" in {
        val user =
          UserGenerator
            .agent(userId = "testUser", groupId = "group1")
            .copy(
              assignedDelegatedEnrolments = Seq(
                EnrolmentKey("IR-SA~UTR~12345670"),
                EnrolmentKey("IR-SA~UTR~12345671"),
                EnrolmentKey("IR-SA~UTR~12345672"),
                EnrolmentKey("IR-SA~UTR~12345673"),
                EnrolmentKey("IR-SA~UTR~12345674"),
                EnrolmentKey("IR-SA~UTR~12345675"),
                EnrolmentKey("IR-SA~UTR~12345676"),
                EnrolmentKey("IR-SA~UTR~12345677"),
                EnrolmentKey("IR-SA~UTR~12345678"),
                EnrolmentKey("IR-SA~UTR~12345679"),
                EnrolmentKey("IR-SA~UTR~12345680"),
                EnrolmentKey("IR-SA~UTR~12345681"),
                EnrolmentKey("IR-SA~UTR~12345682"),
                EnrolmentKey("IR-SA~UTR~12345683"),
                EnrolmentKey("IR-SA~UTR~12345684"),
                EnrolmentKey("IR-SA~UTR~12345685"),
                EnrolmentKey("IR-SA~UTR~12345686"),
                EnrolmentKey("IR-SA~UTR~12345687"),
                EnrolmentKey("IR-SA~UTR~12345688"),
                EnrolmentKey("IR-SA~UTR~12345689"),
                EnrolmentKey("IR-SA~UTR~12345690"),
                EnrolmentKey("IR-SA~UTR~12345691")
              )
            )
        userService.createUser(user, "testPlanet", affinityGroup = Some(AG.Agent)).futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("testUser", planetId = "testPlanet")

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
//      "update a principal enrolment belonging to the groupId with the friendlyName specified in the request" in {
//        userService
//          .createUser(
//            UserGenerator
//              .individual(userId = "testUserId", groupId = "group2")
//              .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("IR-SA~UTR~12345678"))),
//            planetId = "testPlanet",
//            affinityGroup = Some(AG.Individual)
//          )
//          .futureValue
//        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("testUserId", planetId = "testPlanet")
//
//        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
//          "group2",
//          "IR-SA~UTR~12345678",
//          Json.parse("""{"friendlyName": "friendlyHugs"}""")
//        )
//
//        result should haveStatus(204)
//
//      }

      "update a delegated enrolment belonging to the groupId with the friendlyName specified in the request" in {
        userService
          .createUser(
            UserGenerator
              .agent(userId = "testUserId", groupId = "group2", credentialRole = User.CR.Admin)
              .copy(assignedDelegatedEnrolments = Seq(EnrolmentKey("IR-SA~UTR~12345678"))),
            planetId = "testPlanet",
            affinityGroup = Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("testUserId", planetId = "testPlanet")

        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"friendlyName": "friendlyHugs"}""")
        )

        result should haveStatus(204)
      }

      "return 400 BadRequest if the payload is invalid with wrong key " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"somethingElse": "..."}""")
        )
        result should haveStatus(400)
      }

      "return 400 BadRequest if the payload is invalid with illegal char " in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        val result = EnrolmentStoreProxyStub.setEnrolmentFriendlyName(
          "group2",
          "IR-SA~UTR~12345678",
          Json.parse("""{"friendlyName": "H & R Higgins"}""")
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
            .withAssignedDelegatedEnrolment(EnrolmentKey("IR-SA~UTR~12345678")),
          Some(AG.Agent)
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
      val enrolment = Enrolment.from(EnrolmentKey(enrolmentKey))
      val adminUser = UserGenerator.agent(userId = "testAdmin", groupId = "testGroup", credentialRole = "Admin")
      val assistantUser =
        UserGenerator.agent(userId = "testAssistant", groupId = "testGroup", credentialRole = "Assistant")
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
        Groups.create(
          GroupGenerator
            .generate(session.planetId, AG.Agent, groupId = Some("testGroup"))
            .copy(delegatedEnrolments = Seq(enrolment))
        )
        Users.create(adminUser, Some(AG.Agent))
        Users.create(assistantUser, Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(201)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey(enrolmentKey))
      }
      "return 400 Bad Request if the user was already assigned the enrolment" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Groups.create(
          GroupGenerator
            .generate(session.planetId, AG.Agent, groupId = Some("testGroup"))
            .copy(delegatedEnrolments = Seq(enrolment))
        )
        Users.create(adminUser, Some(AG.Agent))
        Users.create(assistantUser.copy(assignedDelegatedEnrolments = Seq(EnrolmentKey(enrolmentKey))), Some(AG.Agent))

        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(400)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.assignedDelegatedEnrolments should contain.only(EnrolmentKey(enrolmentKey))
      }
      "return 403 Forbidden if the enrolment is not allocated to the user's group" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Groups.create(
          GroupGenerator.generate(session.planetId, AG.Agent, groupId = Some("testGroup"))
        )
        Users.create(adminUser, Some(AG.Agent))
        Users.create(assistantUser, Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", enrolmentKey)
        result should haveStatus(403)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.assignedDelegatedEnrolments should be(empty)
      }
      "return 404 Not Found if the user id does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Groups.create(
          GroupGenerator.generate(session.planetId, AG.Agent, groupId = Some("testGroup"))
        )
        Users.create(adminUser, Some(AG.Agent))
        Users.create(assistantUser, Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.assignUser("bar", enrolmentKey)
        result should haveStatus(404)
      }
      "return 404 Not Found if the enrolment key does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Groups.create(
          GroupGenerator
            .generate(session.planetId, AG.Agent, groupId = Some("testGroup"))
            .copy(delegatedEnrolments = Seq(enrolment))
        )
        Users.create(adminUser, Some(AG.Agent))
        Users.create(assistantUser, Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.assignUser("testAssistant", anotherEnrolmentKey)
        result should haveStatus(404)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.assignedDelegatedEnrolments should be(empty)
      }
    }
    "DELETE /tax-enrolments/users/:userId/enrolments/:enrolmentKey (ES12)" should {
      val enrolmentKey = "HMRC-MTD-VAT~VRN~123456789"
      val enrolment: Enrolment = Enrolment.from(EnrolmentKey(enrolmentKey))
      val adminUser = UserGenerator.agent(userId = "testAdmin", groupId = "testGroup", credentialRole = "Admin")
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
        Users.create(adminUser.withAssignedDelegatedEnrolment(EnrolmentKey(enrolmentKey)), Some(AG.Agent))
        Users.create(assistantUser.copy(assignedDelegatedEnrolments = Seq(EnrolmentKey(enrolmentKey))), Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.deassignUser("testAssistant", enrolmentKey)
        result should haveStatus(204)

        val user = await(userService.findByUserId("testAssistant", session.planetId)).get
        user.assignedDelegatedEnrolments should be(empty)
      }
      "return 204 No Content (but no error) if the enrolment was not assigned to the user in the first place" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Groups.create(
          GroupGenerator
            .agent(planetId = session.planetId, groupId = Some("group1"), delegatedEnrolments = Seq(enrolment))
        )
        Users.create(
          adminUser.copy(assignedDelegatedEnrolments = Seq(enrolment.toEnrolmentKey.get)),
          affinityGroup = Some(AG.Agent)
        )
        Users.create(assistantUser.copy(assignedDelegatedEnrolments = Seq.empty), affinityGroup = Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.deassignUser("testAssistant", enrolmentKey)
        result should haveStatus(204)
      }
      "return 404 Not Found if the user id does not exist" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()
        Users.create(adminUser.copy(assignedDelegatedEnrolments = Seq(enrolment.toEnrolmentKey.get)), Some(AG.Agent))
        val result = EnrolmentStoreProxyStub.deassignUser("bar", enrolmentKey)
        result should haveStatus(404)
      }

    }

    "POST /enrolment-store-proxy/enrolment-store/enrolments (ES20)" should {
      val cbcId = "XECBC0666272111"
      val enrolmentKey = s"HMRC-CBC-ORG~cbcId~$cbcId~UTR~8989040376"

      def setKnownFacts()(implicit ac: AuthContext) = EnrolmentStoreProxyStub
        .setKnownFacts(
          enrolmentKey,
          SetKnownFactsRequest
            .generate(enrolmentKey, _ => None)
            .getOrElse(throw new Exception("Could not generate known facts"))
        )

      "return OK with matching identifiers and verifiers" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")
        setKnownFacts()

        val result = EnrolmentStoreProxyStub.queryKnownFacts(
          EnrolmentsFromKnownFactsRequest(
            "HMRC-CBC-ORG",
            Seq(Identifier("cbcId", cbcId))
          )
        )

        result should haveStatus(OK)
        // add test for JSON

      }

      "return NoContent if nothing found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo1")

        val result = EnrolmentStoreProxyStub.queryKnownFacts(
          EnrolmentsFromKnownFactsRequest("HMRC-CBC-ORG", Seq(Identifier("cbcId", cbcId)))
        )

        result should haveStatus(NO_CONTENT)
      }
    }
  }
}
