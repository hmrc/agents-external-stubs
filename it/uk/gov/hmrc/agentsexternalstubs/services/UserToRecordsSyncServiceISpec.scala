package uk.gov.hmrc.agentsexternalstubs.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.UkAddress
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino

class UserToRecordsSyncServiceISpec extends AppBaseISpec with MongoDB {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val userRecordsService = app.injector.instanceOf[UserToRecordsSyncService]
  lazy val businessDetailsRecordsService = app.injector.instanceOf[BusinessDetailsRecordsService]
  lazy val vatCustomerInformationRecordsService = app.injector.instanceOf[VatCustomerInformationRecordsService]
  lazy val businessPartnerRecordsService = app.injector.instanceOf[BusinessPartnerRecordsService]
  lazy val knownFactsRepository = app.injector.instanceOf[KnownFactsRepository]

  private val formatter1 = DateTimeFormatter.ofPattern("dd/MM/yy")
  private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  "UserToRecordsSyncService" should {
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

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty

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

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync mtd-vat organisation to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo", name = "ABC123 Corp.")
        .withPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "923456788")
      await(usersService.createUser(user, planetId))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> "923456788"), planetId))
      val dateOpt = knownFacts.flatMap(_.getVerifierValue("VATRegistrationDate").map(LocalDate.parse(_, formatter1)))

      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")
      result.flatMap(
        _.approvedInformation
          .flatMap(_.customerDetails.effectiveRegistrationDate.map(LocalDate.parse(_, formatter2)))) shouldBe dateOpt

      await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("Foo Bar")
      result2.flatMap(
        _.approvedInformation
          .flatMap(_.customerDetails.effectiveRegistrationDate.map(LocalDate.parse(_, formatter2)))) shouldBe dateOpt

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync hmrc-as-agent agent to agent records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "XARN0001230")

      await(usersService.createUser(user, planetId))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> "XARN0001230"), planetId))
      val postcodeOpt = knownFacts.flatMap(_.getVerifierValue("AgencyPostcode"))

      val result = await(businessPartnerRecordsService.getBusinessPartnerRecord(Arn("XARN0001230"), planetId))
      result.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("ABC123")
      result.flatMap(_.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))) shouldBe postcodeOpt
      result.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe postcodeOpt

      await(usersService.updateUser(user.userId, planetId, user => user.copy(agentFriendlyName = Some("foobar"))))
      val result2 = await(businessPartnerRecordsService.getBusinessPartnerRecord(Arn("XARN0001230"), planetId))
      result2.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("foobar")
      result2.flatMap(_.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))) shouldBe postcodeOpt
      result2.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe postcodeOpt

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }
  }
}
