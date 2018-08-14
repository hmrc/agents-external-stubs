package uk.gov.hmrc.agentsexternalstubs.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, ServerBaseISpec, TestRequests}

class EnrolmentStoreProxyStubControllerISpec
    extends ServerBaseISpec with MongoDbPerSuite with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

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

        result.status shouldBe 200
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

        result.status shouldBe 200
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

        result.status shouldBe 200
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

        result.status shouldBe 204
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

        result.status shouldBe 200
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

        result.status shouldBe 200
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

        result.status shouldBe 200
        val json = result.json
        (json \ "principalGroupIds").as[Seq[String]] should contain.only("group1")
        (json \ "delegatedGroupIds").as[Seq[String]] should contain.only("group2")
      }
    }
  }
}
