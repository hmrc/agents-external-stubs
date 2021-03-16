package uk.gov.hmrc.agentsexternalstubs.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.UkAddress
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.Nino

import scala.concurrent.duration._

class UserToRecordsSyncServiceISpec extends AppBaseISpec with MongoDB {

  override implicit val defaultTimeout = 60.seconds

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val userRecordsService = app.injector.instanceOf[UserToRecordsSyncService]
  lazy val businessDetailsRecordsService = app.injector.instanceOf[BusinessDetailsRecordsService]
  lazy val vatCustomerInformationRecordsService = app.injector.instanceOf[VatCustomerInformationRecordsService]
  lazy val businessPartnerRecordsService = app.injector.instanceOf[BusinessPartnerRecordsService]
  lazy val legacyRelationshipRecordsService = app.injector.instanceOf[LegacyRelationshipRecordsService]
  lazy val employerAuthsRecordsService = app.injector.instanceOf[EmployerAuthsRecordsService]
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
      result2.flatMap(_.businessData).flatMap(_.head.businessAddressDetails).map {
        case BusinessDetailsRecord
              .UkAddress(addressLine1, addressLine2, _, _, postalCode, countryCode) =>
          addressLine1 shouldBe theUser.address.get.line1.get
          addressLine2 shouldBe theUser.address.get.line2
          postalCode shouldBe theUser.address.flatMap(_.postcode).get
          countryCode shouldBe "GB"
        case BusinessDetailsRecord
              .ForeignAddress(addressLine1, addressLine2, _, _, postalCode, countryCode) =>
          addressLine1 shouldBe theUser.address.get.line1.get
          addressLine2 shouldBe theUser.address.get.line2
          postalCode shouldBe theUser.address.flatMap(_.postcode)
          countryCode shouldBe theUser.address.get.countryCode.get
      }

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
        "ABC 123"
      )
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.lastName))) shouldBe Some(
        "345"
      )

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("123456789", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.firstName))) shouldBe Some(
        "Foo"
      )
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.lastName))) shouldBe Some(
        "Bar"
      )
      result2.flatMap(
        _.approvedInformation
          .map(_.PPOB.address.asInstanceOf[VatCustomerInformationRecord.UkAddress].postCode)
      ) shouldBe theUser.address
        .flatMap(_.postcode)

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
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> "923456788"), planetId)
      )
      val dateOpt = knownFacts.flatMap(
        _.getVerifierValue("VATRegistrationDate")
          .map(LocalDate.parse(_, formatter1))
          .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
      )

      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")
      result.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("Foo Bar")
      result2.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      result2.flatMap(
        _.approvedInformation
          .map(_.PPOB.address.asInstanceOf[VatCustomerInformationRecord.UkAddress].postCode)
      ) shouldBe theUser.address
        .flatMap(_.postcode)

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync hmce-vat-agnt agent to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", name = "ABC123 Corp.")
        .withPrincipalEnrolment("HMCE-VAT-AGNT", "AgentRefNo", "923456788")
      await(usersService.createUser(user, planetId))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMCE-VAT-AGNT", "AgentRefNo" -> "923456788"), planetId)
      )
      val dateOpt = knownFacts.flatMap(
        _.getVerifierValue("IREFFREGDATE")
          .map(LocalDate.parse(_, formatter1))
          .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
      )

      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")
      result.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 = await(vatCustomerInformationRecordsService.getCustomerInformation("923456788", planetId))
      result2.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("Foo Bar")
      result2.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      result2.flatMap(
        _.approvedInformation
          .map(_.PPOB.address.asInstanceOf[VatCustomerInformationRecord.UkAddress].postCode)
      ) shouldBe theUser.address
        .flatMap(_.postcode)

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync hmrc-as-agent agent to agent records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "XARN0001230")

      val theUser = await(usersService.createUser(user, planetId))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> "XARN0001230"), planetId)
      )
      val postcodeOpt = knownFacts.flatMap(_.getVerifierValue("AgencyPostcode"))
      postcodeOpt shouldBe theUser.address.flatMap(_.postcode)

      val result = await(businessPartnerRecordsService.getBusinessPartnerRecord(Arn("XARN0001230"), planetId))
      val id = result.flatMap(_.id)
      result.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("ABC123")
      result.flatMap(
        _.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))
      ) shouldBe postcodeOpt
      result.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe postcodeOpt

      await(usersService.updateUser(user.userId, planetId, user => user.copy(agentFriendlyName = Some("foobar"))))
      val id2 = result.flatMap(_.id)
      id shouldBe id2
      val result2 = await(businessPartnerRecordsService.getBusinessPartnerRecord(Arn("XARN0001230"), planetId))
      result2.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("foobar")
      result2.flatMap(
        _.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))
      ) shouldBe postcodeOpt
      result2.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe postcodeOpt

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync ir-sa-agent agent to agent records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-SA-AGENT"))

      val theUser = await(usersService.createUser(user, planetId))
      val saAgentReference = theUser.findIdentifierValue("IR-SA-AGENT", "IRAgentReference").get

      val result = await(legacyRelationshipRecordsService.getLegacyAgentByAgentId(saAgentReference, planetId)).get
      result.agentId shouldBe saAgentReference
      result.govAgentId shouldBe theUser.agentId
      result.postcode shouldBe theUser.address.flatMap(_.postcode)
      result.address1 shouldBe theUser.address.flatMap(_.line1).get
      result.address2 shouldBe theUser.address.flatMap(_.line2).get
      result.agentName shouldBe theUser.agentFriendlyName.get
    }

    "sync ir-sa-agent agent to agent records and create legacy relationships" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-SA-AGENT"))
        .withDelegatedEnrolment(Enrolment("IR-SA"))

      val theUser = await(usersService.createUser(user, planetId))
      val saAgentReference = theUser.findIdentifierValue("IR-SA-AGENT", "IRAgentReference").get
      val utr = theUser.findDelegatedIdentifierValues("IR-SA", "UTR").head

      val agent = await(legacyRelationshipRecordsService.getLegacyAgentByAgentId(saAgentReference, planetId)).get
      agent.agentId shouldBe saAgentReference
      agent.govAgentId shouldBe theUser.agentId
      agent.postcode shouldBe theUser.address.flatMap(_.postcode)
      agent.address1 shouldBe theUser.address.flatMap(_.line1).get
      agent.address2 shouldBe theUser.address.flatMap(_.line2).get
      agent.agentName shouldBe theUser.agentFriendlyName.get

      val relationship = await(
        legacyRelationshipRecordsService.getLegacyRelationshipByAgentIdAndUtr(saAgentReference, utr, planetId)
      ).get
      relationship.agentId shouldBe saAgentReference
      relationship.utr shouldBe Some(utr)
    }

    "sync ir-ct organisation to business partner records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo")
        .withPrincipalEnrolment("IR-CT", "UTR", "7355748439")

      await(usersService.createUser(user, planetId))

      val result = await(businessPartnerRecordsService.getBusinessPartnerRecord(Utr("7355748439"), planetId))
      val id = result.flatMap(_.id)
      result.flatMap(_.eori) shouldBe None
      result.flatMap(_.utr) shouldBe Some("7355748439")
      result.map(_.isAnOrganisation) shouldBe Some(true)
      result.map(_.isAnIndividual) shouldBe Some(false)
      result.map(_.isAnAgent) shouldBe Some(false)
      result.map(_.isAnASAgent) shouldBe Some(false)

      await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("foobar"))))
      val result2 = await(businessPartnerRecordsService.getBusinessPartnerRecord(Utr("7355748439"), planetId))
      val id2 = result2.flatMap(_.id)
      id2 shouldBe id

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync ir-paye-agent agent to records when record does not exist" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-PAYE-AGENT"))
        .withDelegatedEnrolment("IR-PAYE~TaxOfficeNumber~123~TaxOfficeReference~123456789")
        .withDelegatedEnrolment("IR-PAYE~TaxOfficeNumber~321~TaxOfficeReference~987654321")

      val theUser = await(usersService.createUser(user, planetId))

      val result = await(employerAuthsRecordsService.getEmployerAuthsByAgentCode(theUser.agentCode.get, planetId)).get
      result.empAuthList should have size 2
    }

    "do not sync ir-paye-agent agent to records when record does not exist and no delegated enrolments" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-PAYE-AGENT"))

      val theUser = await(usersService.createUser(user, planetId))

      await(employerAuthsRecordsService.getEmployerAuthsByAgentCode(theUser.agentCode.get, planetId)) shouldBe None
    }

    "sync ir-paye-agent agent to records when record exists" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-PAYE-AGENT"))
        .withDelegatedEnrolment("IR-PAYE~TaxOfficeNumber~123~TaxOfficeReference~123456789")
        .withDelegatedEnrolment("IR-PAYE~TaxOfficeNumber~321~TaxOfficeReference~987654321")

      await(
        employerAuthsRecordsService
          .store(
            EmployerAuths(
              agentCode = user.agentCode.get,
              empAuthList = Seq(
                EmployerAuths.EmpAuth(
                  empRef = EmployerAuths.EmpAuth.EmpRef("456", "123456789"),
                  aoRef = EmployerAuths.EmpAuth.AoRef("456", "1", "2", "123456789"),
                  true,
                  false
                )
              )
            ),
            false,
            planetId
          )
      )

      val theUser = await(usersService.createUser(user, planetId))

      val result = await(employerAuthsRecordsService.getEmployerAuthsByAgentCode(theUser.agentCode.get, planetId)).get
      result.empAuthList should have size 3
    }

    "sync ir-paye-agent agent to records when record exists and no delegated enrolments" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", agentFriendlyName = "ABC123")
        .withPrincipalEnrolment(Enrolment("IR-PAYE-AGENT"))

      val empAuthList = Seq(
        EmployerAuths.EmpAuth(
          empRef = EmployerAuths.EmpAuth.EmpRef("456", "123456789"),
          aoRef = EmployerAuths.EmpAuth.AoRef("456", "1", "2", "123456789"),
          true,
          false
        )
      )

      await(
        employerAuthsRecordsService
          .store(
            EmployerAuths(
              agentCode = user.agentCode.get,
              empAuthList = empAuthList
            ),
            false,
            planetId
          )
      )

      val theUser = await(usersService.createUser(user, planetId))

      val result = await(employerAuthsRecordsService.getEmployerAuthsByAgentCode(theUser.agentCode.get, planetId)).get
      result.empAuthList shouldBe empAuthList
    }
  }
}
