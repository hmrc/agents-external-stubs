package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models.UserGenerator
import uk.gov.hmrc.agentsexternalstubs.support._

class UserRecordsSyncServiceISpec extends AppBaseISpec with MongoDB {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val userRecordsService = app.injector.instanceOf[UserRecordsSyncService]
  lazy val businessDetailsRecordsService = app.injector.instanceOf[BusinessDetailsRecordsService]
  lazy val vatCustomerInformationRecordsService = app.injector.instanceOf[VatCustomerInformationRecordsService]

  "UserRecordsSyncService" should {
    "sync mtd-it individual to business details records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withPrincipalEnrolment("HMRC-MTD-IT", "MTDITID", "123456789098765")
      val theUser = await(usersService.createUser(user, planetId))
      val result1 = await(businessDetailsRecordsService.getBusinessDetails(theUser.nino.get, theUser.planetId.get))
      result1 shouldBe defined
      val result2 =
        await(businessDetailsRecordsService.getBusinessDetails(MtdItId("123456789098765"), theUser.planetId.get))
      result2 shouldBe defined
    }

    "sync mtd-vat individual to vat customer information records" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withPrincipalEnrolment("HMRC-MTD-VAT", "VRN", "123456789")
      val theUser = await(usersService.createUser(user, planetId))
      val result = await(vatCustomerInformationRecordsService.getCustomerInformation("123456789", theUser.planetId.get))
      result shouldBe defined
    }
  }
}
