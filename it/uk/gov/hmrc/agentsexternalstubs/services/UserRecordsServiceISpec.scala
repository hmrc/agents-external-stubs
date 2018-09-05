package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import uk.gov.hmrc.agentsexternalstubs.models.UserGenerator
import uk.gov.hmrc.agentsexternalstubs.support._

class UserRecordsServiceISpec extends AppBaseISpec with MongoDB {

  lazy val usersService = app.injector.instanceOf[UsersService]
  lazy val userRecordsService = app.injector.instanceOf[UserRecordsService]
  lazy val businessDetailsRecordsService = app.injector.instanceOf[BusinessDetailsRecordsService]

  "UserRecordsService" should {
    "find relationships by key" in {
      val planetId = UUID.randomUUID().toString
      val user = UserGenerator
        .individual("foo")
        .withPrincipalEnrolment("HMRC-MTD-IT", "MTDITID", "")
      val theUser = await(usersService.createUser(user, planetId))
      val result = await(businessDetailsRecordsService.getBusinessDetails(theUser.nino.get, theUser.planetId.get))
      result shouldBe defined
    }
  }
}
