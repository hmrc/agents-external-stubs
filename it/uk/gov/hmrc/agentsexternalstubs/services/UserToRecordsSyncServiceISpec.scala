package uk.gov.hmrc.agentsexternalstubs.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, CbcId, MtdItId, PlrId, PptRef, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.UkAddress
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.domain.{AgentCode, Nino, Vrn}
import play.api.test.Helpers._

import scala.concurrent.duration._

class UserToRecordsSyncServiceISpec extends AppBaseISpec {

  implicit val defaultTimeout: FiniteDuration = 60.seconds

  lazy val usersService: UsersService = app.injector.instanceOf[UsersService]
  lazy val groupsService: GroupsService = app.injector.instanceOf[GroupsService]
  lazy val userRecordsService: UserToRecordsSyncService = app.injector.instanceOf[UserToRecordsSyncService]
  lazy val legacyRelationshipRecordsService: LegacyRelationshipRecordsService =
    app.injector.instanceOf[LegacyRelationshipRecordsService]
  lazy val knownFactsRepository: KnownFactsRepository = app.injector.instanceOf[KnownFactsRepository]
  lazy val recordsService: RecordsService =
    app.injector.instanceOf[RecordsService]

  private val formatter1 = DateTimeFormatter.ofPattern("dd/MM/yy")
  private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  "UserToRecordsSyncService" should {
    "sync mtd-it individual to business details records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withAssignedPrincipalEnrolment("HMRC-MTD-IT", "MTDITID", "123456789098765")

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Individual)))
      val result1 = await(recordsService.getRecord[BusinessDetailsRecord, Nino](theUser.nino.get, planetId))
      result1.map(_.mtdbsa) shouldBe Some("123456789098765")
      val result2 =
        await(recordsService.getRecord[BusinessDetailsRecord, MtdItId](MtdItId("123456789098765"), planetId))
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

      val result3 = await(recordsService.getRecord[BusinessDetailsRecord, Nino](Nino("HW827856C"), planetId))
      result3.map(_.mtdbsa) shouldBe Some("123456789098765")

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty

    }

    "sync mtd-vat individual to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo", name = "ABC 123 345")
        .withAssignedPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "123456789")

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Individual)))

      val result = await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("123456789"), planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.firstName))) shouldBe Some(
        "ABC 123"
      )
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.individual.flatMap(_.lastName))) shouldBe Some(
        "345"
      )

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 =
        await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("123456789"), planetId))
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
        .withAssignedPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "923456788")
      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Organisation)))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-MTD-VAT", "VRN" -> "923456788"), planetId)
      )
      val dateOpt = knownFacts.flatMap(
        _.getVerifierValue("VATRegistrationDate")
          .map(LocalDate.parse(_, formatter1))
          .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
      )

      val result = await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("923456788"), planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")
      result.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 =
        await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("923456788"), planetId))
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

    "sync hmrc-ppt-org individual to ppt subscription display records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withAssignedPrincipalEnrolment("HMRC-PPT-ORG", "EtmpRegistrationNumber", "XAPPT0004567890")

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Individual)))
      val result1 =
        await(
          recordsService.getRecord[PPTSubscriptionDisplayRecord, PptRef](PptRef("XAPPT0004567890"), planetId)
        )
      result1.map(_.pptReference) shouldBe Some("XAPPT0004567890")
    }

    "sync hmce-vat-agnt agent to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo", name = "ABC123 Corp.")
        .withAssignedPrincipalEnrolment("HMCE-VAT-AGNT", "AgentRefNo", "923456788")
      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMCE-VAT-AGNT", "AgentRefNo" -> "923456788"), planetId)
      )
      val dateOpt = knownFacts.flatMap(
        _.getVerifierValue("IREFFREGDATE")
          .map(LocalDate.parse(_, formatter1))
          .map(date => if (date.isAfter(LocalDate.now())) date.minusYears(100) else date)
      )

      val result = await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("923456788"), planetId))
      result.flatMap(_.approvedInformation.flatMap(_.customerDetails.organisationName)) shouldBe Some("ABC123 Corp.")
      result.flatMap(
        _.approvedInformation
          .flatMap(
            _.customerDetails.effectiveRegistrationDate
              .map(LocalDate.parse(_, formatter2))
          )
      ) shouldBe dateOpt

      val theUser = await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("Foo Bar"))))

      val result2 =
        await(recordsService.getRecord[VatCustomerInformationRecord, Vrn](Vrn("923456788"), planetId))
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
        .agent("foo")
        .withAssignedPrincipalEnrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "XARN0001230")

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))
      await(groupsService.updateGroup(theUser.groupId.get, planetId, _.copy(agentFriendlyName = Some("ABC123"))))

      val knownFacts = await(
        knownFactsRepository
          .findByEnrolmentKey(EnrolmentKey.from("HMRC-AS-AGENT", "AgentReferenceNumber" -> "XARN0001230"), planetId)
      )
      val postcodeOpt = knownFacts.flatMap(_.getVerifierValue("AgencyPostcode"))
      postcodeOpt shouldBe theUser.address.flatMap(_.postcode)

      val result = await(recordsService.getRecord[BusinessPartnerRecord, Arn](Arn("XARN0001230"), planetId))
      val id = result.flatMap(_.id)
      result.flatMap(_.agencyDetails.flatMap(_.agencyName)) shouldBe Some("ABC123")
      result.flatMap(
        _.agencyDetails.flatMap(_.agencyAddress.map(_.asInstanceOf[UkAddress].postalCode))
      ) shouldBe postcodeOpt
      result.map(_.addressDetails.asInstanceOf[UkAddress].postalCode) shouldBe postcodeOpt

      await(groupsService.updateGroup(user.groupId.get, planetId, _.copy(agentFriendlyName = Some("foobar"))))
      val id2 = result.flatMap(_.id)
      id shouldBe id2
      val result2 = await(recordsService.getRecord[BusinessPartnerRecord, Arn](Arn("XARN0001230"), planetId))
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
      val groupId = "testGroupId"
      val group = groupsService
        .createGroup(
          GroupGenerator
            .agent(planetId = planetId, groupId = Some(groupId))
            .copy(principalEnrolments = Seq(Enrolment("IR-SA-AGENT"))),
          planetId = planetId
        )
        .futureValue
      val user = UserGenerator
        .agent("foo", groupId = groupId)
        .copy(assignedPrincipalEnrolments = group.principalEnrolments.map(_.toEnrolmentKey.get))

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))

      val saAgentReference = group.findIdentifierValue("IR-SA-AGENT", "IRAgentReference").get

      val result = await(legacyRelationshipRecordsService.getLegacyAgentByAgentId(saAgentReference, planetId)).get
      result.agentId shouldBe saAgentReference
      result.govAgentId shouldBe group.agentId
      result.postcode shouldBe theUser.address.flatMap(_.postcode)
      result.address1 shouldBe theUser.address.flatMap(_.line1).get
      result.address2 shouldBe theUser.address.flatMap(_.line2).get
      result.agentName shouldBe group.agentFriendlyName.get
    }

    "sync ir-sa-agent agent to agent records and create legacy relationships" in {
      val planetId = UUID.randomUUID().toString
      val groupId = "testGroupId"
      val group = groupsService
        .createGroup(
          GroupGenerator
            .agent(planetId = planetId, groupId = Some(groupId))
            .copy(principalEnrolments = Seq(Enrolment("IR-SA-AGENT")), delegatedEnrolments = Seq(Enrolment("IR-SA"))),
          planetId = planetId
        )
        .futureValue

      val user = UserGenerator
        .agent("foo", groupId = groupId)
        .copy(
          assignedPrincipalEnrolments = group.principalEnrolments.map(_.toEnrolmentKey.get),
          assignedDelegatedEnrolments = group.delegatedEnrolments.map(_.toEnrolmentKey.get)
        )

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))

      val saAgentReference = group.findIdentifierValue("IR-SA-AGENT", "IRAgentReference").get
      val utr = group.findDelegatedIdentifierValues("IR-SA", "UTR").head

      val agent = await(legacyRelationshipRecordsService.getLegacyAgentByAgentId(saAgentReference, planetId)).get
      agent.agentId shouldBe saAgentReference
      agent.govAgentId shouldBe group.agentId
      agent.postcode shouldBe theUser.address.flatMap(_.postcode)
      agent.address1 shouldBe theUser.address.flatMap(_.line1).get
      agent.address2 shouldBe theUser.address.flatMap(_.line2).get
      agent.agentName shouldBe group.agentFriendlyName.get

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
        .withAssignedPrincipalEnrolment("IR-CT", "UTR", "7355748439")

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Organisation)))

      val result = await(recordsService.getRecord[BusinessPartnerRecord, Utr](Utr("7355748439"), planetId))
      val id = result.flatMap(_.id)
      result.flatMap(_.eori) shouldBe None
      result.flatMap(_.utr) shouldBe Some("7355748439")
      result.map(_.isAnOrganisation) shouldBe Some(true)
      result.map(_.isAnIndividual) shouldBe Some(false)
      result.map(_.isAnAgent) shouldBe Some(false)
      result.map(_.isAnASAgent) shouldBe Some(false)

      await(usersService.updateUser(user.userId, planetId, user => user.copy(name = Some("foobar"))))
      val result2 = await(recordsService.getRecord[BusinessPartnerRecord, Utr](Utr("7355748439"), planetId))
      val id2 = result2.flatMap(_.id)
      id2 shouldBe id

      val userWithRecordId = await(usersService.findByUserId(user.userId, planetId))
      userWithRecordId.map(_.recordIds).get should not be empty
    }

    "sync ir-paye-agent agent to records when record does not exist" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo")
        .withAssignedPrincipalEnrolment(EnrolmentKey("IR-PAYE-AGENT", Seq.empty))
        .withAssignedDelegatedEnrolment(EnrolmentKey("IR-PAYE~TaxOfficeNumber~123~TaxOfficeReference~123456789"))
        .withAssignedDelegatedEnrolment(EnrolmentKey("IR-PAYE~TaxOfficeNumber~321~TaxOfficeReference~987654321"))

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))
      val theGroup =
        await(
          groupsService.updateGroup(
            theUser.groupId.get,
            planetId,
            _.copy(
              agentFriendlyName = Some("ABC123")
            )
          )
        )

      val result = await(
        recordsService.getRecord[EmployerAuths, AgentCode](AgentCode(theGroup.agentCode.get), planetId)
      ).get
      result.empAuthList should have size 2
    }

    "do not sync ir-paye-agent agent to records when record does not exist and no delegated enrolments" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo")
        .withAssignedPrincipalEnrolment(EnrolmentKey("IR-PAYE-AGENT", Seq.empty))

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))
      val theGroup =
        await(groupsService.updateGroup(theUser.groupId.get, planetId, _.copy(agentFriendlyName = Some("ABC123"))))

      await(
        recordsService.getRecord[EmployerAuths, AgentCode](AgentCode(theGroup.agentCode.get), planetId)
      ) shouldBe None
    }

    "sync ir-paye-agent agent to records when record exists" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo")
        .withAssignedPrincipalEnrolment(EnrolmentKey("IR-PAYE-AGENT", Seq.empty))
        .withAssignedDelegatedEnrolment(EnrolmentKey("IR-PAYE~TaxOfficeNumber~123~TaxOfficeReference~123456789"))
        .withAssignedDelegatedEnrolment(EnrolmentKey("IR-PAYE~TaxOfficeNumber~321~TaxOfficeReference~987654321"))

      val testAgentCode = "AGENTCODE123"

      await(
        recordsService
          .store(
            EmployerAuths(
              agentCode = testAgentCode,
              empAuthList = Seq(
                EmployerAuths.EmpAuth(
                  empRef = EmployerAuths.EmpAuth.EmpRef("456", "123456789"),
                  aoRef = EmployerAuths.EmpAuth.AoRef("456", "1", "2", "123456789"),
                  `Auth_64-8` = true,
                  Auth_OAA = false
                )
              )
            ),
            autoFill = false,
            planetId
          )
      )

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))
      await(
        groupsService.updateGroup(
          theUser.groupId.get,
          planetId,
          _.copy(agentFriendlyName = Some("ABC123"), agentCode = Some(testAgentCode))
        )
      )

      val result =
        await(recordsService.getRecord[EmployerAuths, AgentCode](AgentCode(testAgentCode), planetId)).get
      result.empAuthList should have size 3
    }

    "sync ir-paye-agent agent to records when record exists and no delegated enrolments" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .agent("foo")
        .withAssignedPrincipalEnrolment(EnrolmentKey("IR-PAYE-AGENT", Seq.empty))

      val testAgentCode = "AGENTCODE123"

      val empAuthList = Seq(
        EmployerAuths.EmpAuth(
          empRef = EmployerAuths.EmpAuth.EmpRef("456", "123456789"),
          aoRef = EmployerAuths.EmpAuth.AoRef("456", "1", "2", "123456789"),
          `Auth_64-8` = true,
          Auth_OAA = false
        )
      )

      await(
        recordsService
          .store(
            EmployerAuths(
              agentCode = testAgentCode,
              empAuthList = empAuthList
            ),
            autoFill = false,
            planetId
          )
      )

      val theUser = await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Agent)))
      await(
        groupsService.updateGroup(
          theUser.groupId.get,
          planetId,
          _.copy(agentFriendlyName = Some("ABC123"), agentCode = Some(testAgentCode))
        )
      )

      val result =
        await(recordsService.getRecord[EmployerAuths, AgentCode](AgentCode(testAgentCode), planetId)).get
      result.empAuthList shouldBe empAuthList
    }

    "sync hmrc-cbc-org organisation to cbc subscription records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo")
        .withAssignedPrincipalEnrolment(
          EnrolmentKey("HMRC-CBC-ORG", Seq(Identifier("UTR", "4478078113"), Identifier("cbcId", "XACBC4940653845")))
        )

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Organisation)))
      val result1 =
        await(recordsService.getRecord[CbcSubscriptionRecord, CbcId](CbcId("XACBC4940653845"), planetId))
      result1.map(_.cbcId) shouldBe Some("XACBC4940653845")

      val result2 = await(usersService.findByUserId(user.userId, planetId))
      result2.map(_.recordIds.size).get shouldBe 1
    }

    "sync hmrc-cbc-nonuk-org organisation to cbc subscription records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo")
        .withAssignedPrincipalEnrolment(
          EnrolmentKey("HMRC-CBC-NONUK-ORG", Seq(Identifier("cbcId", "XACBC4940653849")))
        )

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Organisation)))
      val result1 =
        await(recordsService.getRecord[CbcSubscriptionRecord, CbcId](CbcId("XACBC4940653849"), planetId))
      result1.map(_.cbcId) shouldBe Some("XACBC4940653849")

      val result2 = await(usersService.findByUserId(user.userId, planetId))
      result2.map(_.recordIds.size).get shouldBe 1
    }

    "sync hmrc-pillar2-org organisation to cbc subscription records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .organisation("foo")
        .withAssignedPrincipalEnrolment(
          EnrolmentKey("HMRC-PILLAR2-ORG", Seq(Identifier("plrId", "XAPLR2222222222")))
        )

      await(usersService.createUser(user, planetId, affinityGroup = Some(AG.Organisation)))
      val result1 =
        await(recordsService.getRecord[Pillar2Record, PlrId](PlrId("XAPLR2222222222"), planetId))
      result1.map(_.plrReference) shouldBe Some("XAPLR2222222222")

      val result2 = await(usersService.findByUserId(user.userId, planetId))
      result2.map(_.recordIds.size).get shouldBe 1
    }

  }
}
