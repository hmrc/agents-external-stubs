package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserSanitizerSpec extends UnitSpec {

  "UserSanitizer" should {
    "add missing name to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).name shouldBe Some(
        "Kaylee Phillips")
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).name shouldBe Some(
        "Nicholas Isnard")
    }

    "add missing name to the Agent" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).name shouldBe Some("Avery Goulding")
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Agent))).name shouldBe Some("Isaac Pearson")
    }

    "add missing name to the Organisation" in {
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Organisation))).name shouldBe Some(
        "Petroleum Inc.")
      UserSanitizer.sanitize(User("zoo", affinityGroup = Some(User.AG.Organisation))).name shouldBe Some(
        "Markets Mutual FedEx Holdings")
    }

    "add missing dateOfBirth to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).dateOfBirth.isDefined shouldBe true
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).dateOfBirth.isDefined shouldBe true
    }

    "add missing NINO to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).nino shouldBe Some(
        Nino("XC 93 60 45 D"))
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).nino shouldBe Some(
        Nino("HW 82 78 56 C"))
    }

    "remove NINO if not Individual" in {
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe None
    }

    "add missing ConfidenceLevel to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).confidenceLevel shouldBe Some(50)
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).confidenceLevel shouldBe Some(50)
    }

    "remove ConfidenceLevel if not Individual" in {
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), confidenceLevel = Some(100)))
        .confidenceLevel shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), confidenceLevel = Some(100)))
        .confidenceLevel shouldBe None
    }

    "add or remove CredentialRole when appropriate" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).credentialRole shouldBe Some(
        User.CR.User)
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).credentialRole shouldBe Some(
        User.CR.User)
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Organisation))).credentialRole shouldBe None
      UserSanitizer.sanitize(User("foo", affinityGroup = None)).credentialRole shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), credentialRole = Some("User")))
        .credentialRole shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = None, credentialRole = Some("User")))
        .credentialRole shouldBe None
    }

    "remove DateOfBirth if not Individual" in {
      val now = LocalDate.now()
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Individual), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe None
    }

    "add missing GroupIdentifier" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Organisation))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = None)).groupId.isDefined shouldBe true
    }
  }

}
