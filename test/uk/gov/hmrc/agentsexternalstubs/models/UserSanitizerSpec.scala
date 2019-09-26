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
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).name shouldBe Some("Kaylee Hastings")
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Agent))).name shouldBe Some("Nicholas Bates")
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
        Nino("AB 61 73 12 C"))
    }

    "remove NINO for Business" in {
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe Some(Nino("XC 93 60 45 D"))
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Individual), nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe Some(Nino("XC 93 60 45 D"))
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
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Organisation))).credentialRole shouldBe Some(
        User.CR.Admin)
      UserSanitizer.sanitize(User("foo", affinityGroup = None)).credentialRole shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), credentialRole = Some(User.CR.User)))
        .credentialRole shouldBe Some(User.CR.Admin)
      UserSanitizer
        .sanitize(User("foo", affinityGroup = None, credentialRole = Some(User.CR.User)))
        .credentialRole shouldBe None
    }

    "remove DateOfBirth for Business" in {
      val now = LocalDate.now()
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Individual), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe None
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
    }

    "add missing GroupIdentifier" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Organisation))).groupId.isDefined shouldBe true
      UserSanitizer.sanitize(User("foo", affinityGroup = None)).groupId.isDefined shouldBe true
    }

    "add missing AgentCode to Agent" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).agentCode.isDefined shouldBe true
    }

    "remove AgentCode from Individual or Organisation" in {
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Individual), agentCode = Some("foo")))
        .agentCode
        .isDefined shouldBe false
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Organisation), agentCode = Some("foo")))
        .agentCode
        .isDefined shouldBe false
    }

    "add missing friendly name to Agent" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).agentFriendlyName.isDefined shouldBe true
    }

    "add missing identifiers to principal enrolments" in {
      UserSanitizer
        .sanitize(
          User("foo", affinityGroup = Some(User.AG.Individual), principalEnrolments = Seq(Enrolment("HMRC-MTD-IT"))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("MTDITID")
      UserSanitizer
        .sanitize(
          User("foo", affinityGroup = Some(User.AG.Agent), principalEnrolments = Seq(Enrolment("HMRC-AS-AGENT"))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("AgentReferenceNumber")
    }

    "remove affinity group, principal and delegated enrolments and other data if STRIDE role present" in {
      val sanitized = UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Individual),
            principalEnrolments = Seq(Enrolment("HMRC-AS-AGENT")),
            delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT")),
            strideRoles = Seq("FOO")
          )
        )
      sanitized.principalEnrolments shouldBe empty
      sanitized.delegatedEnrolments shouldBe empty
      sanitized.strideRoles shouldBe Seq("FOO")
      sanitized.affinityGroup shouldBe None
      sanitized.nino shouldBe None
      sanitized.credentialRole shouldBe None
      sanitized.credentialStrength shouldBe None
      sanitized.confidenceLevel shouldBe None
    }

    "remove redundant dangling principal enrolment keys" in {
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Individual),
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT"), Enrolment("HMRC-MTD-IT"))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) shouldBe Seq("MTDITID")
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Individual),
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT"), Enrolment("HMRC-MTD-VAT"))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain theSameElementsAs Seq("MTDITID", "VRN")
      UserSanitizer
        .sanitize(User(
          "foo",
          affinityGroup = Some(User.AG.Individual),
          principalEnrolments = Seq(
            Enrolment("HMRC-MTD-IT"),
            Enrolment("HMRC-MTD-IT", "MTDITID", "CNOB96766112368"),
            Enrolment("HMRC-MTD-IT"))
        ))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.value)) shouldBe Seq("CNOB96766112368")
    }

    "add missing identifier names to principal enrolments" in {
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Individual),
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT", "", "123456789"))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.key))
        .filter(_.nonEmpty) should contain.only("MTDITID")
    }

    "add missing identifier values to principal enrolments" in {
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Individual),
            principalEnrolments = Seq(Enrolment("HMRC-MTD-IT", "MTDITID", ""))))
        .principalEnrolments
        .flatMap(_.identifiers.get.map(_.value))
        .filter(_.nonEmpty) should not be empty
    }

    "add missing identifiers to delegated enrolments" in {
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT"))))
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("MTDITID")
      UserSanitizer
        .sanitize(User("foo", affinityGroup = Some(User.AG.Agent), delegatedEnrolments = Seq(Enrolment("IR-SA"))))
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key)) should contain.only("UTR")
    }

    "add missing identifier names to delegated enrolments" in {
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Agent),
            delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT", "", "123456789"))))
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.key))
        .filter(_.nonEmpty) should contain.only("MTDITID")
    }

    "add missing identifier values to delegated enrolments" in {
      UserSanitizer
        .sanitize(
          User(
            "foo",
            affinityGroup = Some(User.AG.Agent),
            delegatedEnrolments = Seq(Enrolment("HMRC-MTD-IT", "MTDITID", ""))))
        .delegatedEnrolments
        .flatMap(_.identifiers.get.map(_.value))
        .filter(_.nonEmpty) should not be empty
    }

    "add missing address" in {
      val address = UserSanitizer.sanitize(User("foo")).address
      address shouldBe defined
      address.get.postcode shouldBe defined
      address.get.line1 shouldBe defined
      address.get.line2 shouldBe defined
      address.get.countryCode shouldBe defined
    }

    "add missing address fields" in {
      val address = UserSanitizer.sanitize(User("foo", address = Some(User.Address(line1 = Some("foo"))))).address
      address shouldBe defined
      address.get.postcode shouldBe defined
      address.get.line1 shouldBe defined
      address.get.line2 shouldBe defined
      address.get.countryCode shouldBe defined
    }
  }

}
