package uk.gov.hmrc.agentsexternalstubs.connectors

import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.admin.{AG, Group, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}

/*

  This test suite has been copy-pasted from agent-client-relationship
  to prove the external stubs works as expected by the service.

 */

class EnrolmentStoreProxyConnectorISpec extends ServerBaseISpec with EnrolmentStoreProxyHelper with MockitoSugar {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val connector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  "EnrolmentStoreProxy" should {

    "return some agent's groupId for given ARN" in {
      implicit val session = givenAuthenticatedSession()
      givenPrincipalGroupIdExistsFor(Arn("foo"), "bar")
      await(connector.getPrincipalGroupIdFor(Arn("foo"))) shouldBe "bar"
    }

    "return RelationshipNotFound Exception when ARN not found" in {
      implicit val session = givenAuthenticatedSession()
      an[Exception] shouldBe thrownBy {
        await(connector.getPrincipalGroupIdFor(Arn("foo")))
      }
    }

    "return some agents's groupIds for given MTDITID" in {
      implicit val session = givenAuthenticatedSession()
      givenDelegatedGroupIdsExistFor(MtdItId("foo"), Set("bar", "car", "dar"))
      await(connector.getDelegatedGroupIdsFor(MtdItId("foo"))) should contain("bar")
    }

    "return Empty when MTDITID not found" in {
      implicit val session = givenAuthenticatedSession()
      await(connector.getDelegatedGroupIdsFor(MtdItId("foo"))) should be(empty)
    }

    "return some agents's groupIds for given VRN" in {
      implicit val session = givenAuthenticatedSession()
      givenDelegatedGroupIdsExistFor(Vrn("123456789"), Set("bar", "car", "dar"))
      await(connector.getDelegatedGroupIdsFor(Vrn("123456789"))) should contain("bar")
    }

    "return some agents's groupIds for given VATRegNo" in {
      implicit val session = givenAuthenticatedSession()
      givenDelegatedGroupIdsExistForKey("HMCE-VATDEC-ORG~VATREGNO~123", Set("bar", "car", "dar"))
      await(connector.getDelegatedGroupIdsForHMCEVATDECORG(Vrn("123"))) should contain("bar")
    }

    "return Empty when VRN not found" in {
      implicit val session = givenAuthenticatedSession()
      await(connector.getDelegatedGroupIdsFor(Vrn("345"))) should be(empty)
    }

    "return some clients userId for given MTDITID" in {
      implicit val session = givenAuthenticatedSession()
      givenPrincipalUserIdExistFor(MtdItId("123456789098765"), "bar")
      await(connector.getPrincipalUserIdFor(MtdItId("123456789098765"))) shouldBe "bar"
    }

    "return RelationshipNotFound Exception when MTDITID not found" in {
      implicit val session = givenAuthenticatedSession()
      an[Exception] shouldBe thrownBy {
        await(connector.getPrincipalUserIdFor(MtdItId("123456789098765")))
      }
    }

    "return some clients userId for given VRN" in {
      implicit val session = givenAuthenticatedSession()
      givenPrincipalUserIdExistFor(Vrn("123456789"), "bar")
      await(connector.getPrincipalUserIdFor(Vrn("123456789"))) shouldBe "bar"
    }

    "return RelationshipNotFound Exception when VRN not found" in {
      implicit val session = givenAuthenticatedSession()
      an[Exception] shouldBe thrownBy {
        await(connector.getPrincipalUserIdFor(Vrn("123456789")))
      }
    }
  }

  "TaxEnrolments" should {

    "allocate an enrolment to an agent" in {
      implicit val session = givenAuthenticatedSession()
      val clientCreation = Users.create(
        UserGenerator
          .individual()
          .copy(assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-MTD-IT~MTDITID~123456789098765"))),
        Some(AG.Individual)
      )
      clientCreation should haveStatus(201)
      val agentCreation = Users.create(
        UserGenerator.agent(userId = "user1", groupId = "group1"),
        Some(AG.Agent),
        agentCode = Some("bar")
      )
      agentCreation should haveStatus(201)

      await(connector.allocateEnrolmentToAgent("group1", "user1", MtdItId("123456789098765"), AgentCode("bar")))
      Groups.get("group1").json.as[Group].delegatedEnrolments should contain(
        Enrolment.from(EnrolmentKey("HMRC-MTD-IT~MTDITID~123456789098765"))
      )
    }

    "de-allocate an enrolment from an agent" in {
      implicit val session = givenAuthenticatedSession()
      givenEnrolmentDeallocationSucceeds("group1", "HMRC-MTD-IT", "MTDITID", "123456789098765", "bar")
      await(connector.deallocateEnrolmentFromAgent("group1", MtdItId("123456789098765"), AgentCode("bar")))
      verifyEnrolmentDeallocationAttempt("group1", "HMRC-MTD-IT~MTDITID~123456789098765", "bar")
    }
  }
}

trait EnrolmentStoreProxyHelper extends TestRequests with TestStubs with Matchers with WSResponseMatchers {
  this: Suite =>

  def givenAuthenticatedSession(): AuthenticatedSession =
    SignIn.signInAndGetSession("foo")

  private def asEnrolment(identifier: TaxIdentifier): Enrolment = identifier match {
    case _: Arn     => Enrolment("HMRC-AS-AGENT", Some(Seq(Identifier("AgentReferenceNumber", identifier.value))))
    case _: MtdItId => Enrolment("HMRC-MTD-IT", Some(Seq(Identifier("MTDITID", identifier.value))))
    case _: Vrn     => Enrolment("HMRC-MTD-VAT", Some(Seq(Identifier("VRN", identifier.value))))
    case _          => throw new IllegalArgumentException(s"Tax identifier not supported $identifier")
  }

  def givenPrincipalGroupIdExistsFor(taxIdentifier: TaxIdentifier, groupId: String)(implicit
    authContext: AuthContext
  ): Unit = taxIdentifier match {
    case _: Arn =>
      Users.create(
        UserGenerator
          .agent(groupId = groupId)
          .withAssignedPrincipalEnrolment(asEnrolment(taxIdentifier).toEnrolmentKey.get),
        Some(AG.Agent)
      )
    case _ =>
      Users.create(
        UserGenerator
          .individual(groupId = groupId)
          .withAssignedPrincipalEnrolment(asEnrolment(taxIdentifier).toEnrolmentKey.get),
        Some(AG.Individual)
      )
  }

  def givenDelegatedGroupIdsExistFor(taxIdentifier: TaxIdentifier, groupIds: Set[String])(implicit
    authContext: AuthContext
  ): Unit = for (groupId <- groupIds) {
    val result = Users.create(
      UserGenerator
        .agent(groupId = groupId)
        .withAssignedDelegatedEnrolment(asEnrolment(taxIdentifier).toEnrolmentKey.get),
      Some(AG.Agent)
    )
    result should haveStatus(201)
  }

  def givenDelegatedGroupIdsExistForKey(enrolmentKey: String, groupIds: Set[String])(implicit
    authContext: AuthContext
  ): Unit = {
    val enrolment = Enrolment.from(EnrolmentKey.parse(enrolmentKey).right.get)
    for (groupId <- groupIds) {
      val result = Users.create(
        UserGenerator
          .agent(groupId = groupId)
          .withAssignedDelegatedEnrolment(enrolment.toEnrolmentKey.get),
        Some(AG.Agent)
      )
      result should haveStatus(201)
    }
  }

  def givenPrincipalUserIdExistFor(taxIdentifier: TaxIdentifier, userId: String)(implicit
    authContext: AuthContext
  ): Unit = taxIdentifier match {
    case _: Arn =>
      val result = Users.create(
        UserGenerator
          .agent(userId = userId)
          .withAssignedPrincipalEnrolment(asEnrolment(taxIdentifier).toEnrolmentKey.get),
        Some(AG.Agent)
      )
      result should haveStatus(201)
    case _ =>
      val result = Users.create(
        UserGenerator
          .individual(userId = userId)
          .withAssignedPrincipalEnrolment(asEnrolment(taxIdentifier).toEnrolmentKey.get),
        Some(AG.Individual)
      )
      result should haveStatus(201)
  }

  def givenEnrolmentDeallocationSucceeds(
    groupId: String,
    key: String,
    identifier: String,
    value: String,
    agentCode: String
  )(implicit authContext: AuthContext): Unit =
    Users.create(
      UserGenerator
        .agent(
          groupId = groupId
        )
        .copy(assignedDelegatedEnrolments = Seq(Enrolment(key, identifier, value).toEnrolmentKey.get)),
      Some(AG.Agent),
      agentCode = Some(agentCode)
    )

  def verifyEnrolmentDeallocationAttempt(groupId: String, enrolmentKey: String, agentCode: String)(implicit
    authContext: AuthContext
  ) =
    Groups
      .get(groupId)
      .json
      .as[Group]
      .delegatedEnrolments
      .map(_.toEnrolmentKeyTag.get) should not contain enrolmentKey

}
