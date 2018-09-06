package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.agentsexternalstubs.models.AgentRecord.UkAddress
import uk.gov.hmrc.agentsexternalstubs.models.UserGenerator
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino

class UserRecordsSyncServiceISpec extends AppBaseISpec with MongoDB {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val userRecordsService = app.injector.instanceOf[UserRecordsSyncService]
  lazy val businessDetailsRecordsService = app.injector.instanceOf[BusinessDetailsRecordsService]
  lazy val vatCustomerInformationRecordsService = app.injector.instanceOf[VatCustomerInformationRecordsService]
  lazy val agentRecordsService = app.injector.instanceOf[AgentRecordsService]

  "UserRecordsSyncService" should {
    "sync mtd-it individual to business details records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withPrincipalEnrolment("HMRC-MTD-IT", "MTDITID", "123456789098765")

      val theUser = await(usersService.createUser(user, planetId))

      val result1 = await(businessDetailsRecordsService.getBusinessDetails(theUser.nino.get, planetId))
      result1.map(_.mtdbsa) shouldBe Some("123456789098765")
      val result2 =
        await(businessDetailsRecordsService.getBusinessDetails(MtdItId("123456789098765"), planetId))
      result2.map(_.nino) shouldBe Some(theUser.nino.get.value.replace(" ", ""))

      await(usersService.updateUser(user.userId, planetId, user => user.copy(nino = Some(Nino("HW827856C")))))

      val result3 = await(businessDetailsRecordsService.getBusinessDetails(Nino("HW827856C"), planetId))
      result3.map(_.mtdbsa) shouldBe Some("123456789098765")

    }

    "sync mtd-vat individual to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo", name = "ABC 123 345")
        .withPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "123456789")

      await(usersService.createUser(user, planetId))

      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("123456789", planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.firstName))) shouldBe Some(
        "ABC 123")
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.lastName))) shouldBe Some(
        "345")

      await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("123456789", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.firstName))) shouldBe Some(
        "Foo")
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.lastName))) shouldBe Some(
        "Bar")
    }

    "sync mtd-vat organisation to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo", name = "ABC123 Corp.")
        .withPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "923456788")
      await(usersService.createUser(user, planetId))
      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")

      await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("Foo Bar")
    }

    "sync hmrc-as-agent agent to agent records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "XARN0001230")

      await(usersService.createUser(user, planetId))

      val result = await(agentRecordsService.getAgentRecord(Arn("XARN0001230"), planetId))
      result.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("ABC123")
      result.flatMap(_.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))) shouldBe defined
      result.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe defined

      await(usersService.updateUser(user.userId, planetId, user => user.copy(agentFriendlyName = Some("foobar"))))
      val result2 = await(agentRecordsService.getAgentRecord(Arn("XARN0001230"), planetId))
      result2.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("foobar")
      result2.flatMap(_.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))) shouldBe defined
      result2.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe defined
    }
  }
}
