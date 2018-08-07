package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserValidatorSpec extends UnitSpec {

  "UserValidator" should {
    "validate only when affinityGroup is none or one of [Individual, Organisation, Agent]" in {
      User.validate(User("foo", affinityGroup = Some(User.AG.Individual))).isValid shouldBe true
      User.validate(User("foo", affinityGroup = Some(User.AG.Organisation))).isValid shouldBe true
      User.validate(UserGenerator.agent("foo")).isValid shouldBe true
      User.validate(User("foo", affinityGroup = None)).isValid shouldBe true

      User.validate(User("foo", affinityGroup = Some("Foo"))).isValid shouldBe false
      User.validate(User("foo", affinityGroup = Some(""))).isValid shouldBe false
    }

    "validate only when confidenceLevel is none, or one of [50,100,200,300] and user is Individual and NINO is not empty" in {
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(50),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(100),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(300),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true

      User
        .validate(
          User("foo", confidenceLevel = Some(200), affinityGroup = Some(User.AG.Agent), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Organisation),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
      User
        .validate(User("foo", confidenceLevel = Some(200), affinityGroup = Some(User.AG.Individual), nino = None))
        .isValid shouldBe false
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(55),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(0),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
    }

    "validate only when credentialStrength is none, or one of [weak, strong]" in {
      User.validate(User("foo", credentialStrength = Some("weak"))).isValid shouldBe true
      User.validate(User("foo", credentialStrength = Some("strong"))).isValid shouldBe true
      User.validate(User("foo", credentialStrength = None)).isValid shouldBe true

      User.validate(User("foo", credentialStrength = Some("very strong"))).isValid shouldBe false
      User.validate(User("foo", credentialStrength = Some("little weak"))).isValid shouldBe false
      User.validate(User("foo", credentialStrength = Some(""))).isValid shouldBe false
    }

    "validate only when credentialRole is none, or one of [User, Assistant] for Individual or Agent" in {
      User
        .validate(User("foo", credentialRole = Some(User.CR.User), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true
      User
        .validate(UserGenerator.agent("foo", credentialRole = Some(User.CR.User)))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true
      User
        .validate(UserGenerator.agent("foo", credentialRole = Some("Assistant")))
        .isValid shouldBe true
      User
        .validate(UserGenerator.agent("foo", credentialRole = None))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialRole = None, affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true

      User
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when nino is none or set for an Individual" in {
      User
        .validate(
          User(
            "foo",
            nino = Some(Nino("HW827856C")),
            affinityGroup = Some(User.AG.Individual),
            confidenceLevel = Some(200)))
        .isValid shouldBe true
      User
        .validate(User("foo", nino = None, affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe true

      User
        .validate(
          User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Individual), confidenceLevel = None))
        .isValid shouldBe false
      User
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Agent)))
        .isValid shouldBe false
      User
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "validate only when dateOfBirth is none or set for an Individual" in {
      val now = LocalDate.now()
      User
        .validate(UserGenerator.individual(userId = "foo", dateOfBirth = "1975-03-29"))
        .isValid shouldBe true

      User
        .validate(User("foo", affinityGroup = Some(User.AG.Organisation), dateOfBirth = Some(now)))
        .isValid shouldBe false
    }

    "validate only when agentCode is none or set for an Agent" in {
      User
        .validate(UserGenerator.agent(userId = "foo", agentCode = "LMNOPQ234568"))
        .isValid shouldBe true

      User
        .validate(User("foo", affinityGroup = Some(User.AG.Agent)))
        .isValid shouldBe false
    }

    "validate only when delegatedEnrolments are empty or user is an Agent" in {
      User.validate(User("foo", delegatedEnrolments = Seq.empty)).isValid shouldBe true
      User
        .validate(UserGenerator.agent("foo", delegatedEnrolments = Seq(Enrolment("A"))))
        .isValid shouldBe true

      User
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some(User.AG.Individual)))
        .isValid shouldBe false
      User
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some(User.AG.Organisation)))
        .isValid shouldBe false
    }

    "skip validation if isNonStandardFlag is set to true" in {
      User
        .validate(User("foo", affinityGroup = Some("Foo"), isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(User("foo", affinityGroup = Some(""), isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Agent),
            nino = Some(Nino("HW827856C")),
            isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Organisation),
            nino = Some(Nino("HW827856C")),
            isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some(User.AG.Individual),
            nino = None,
            isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(55),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C")),
            isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(0),
            affinityGroup = Some(User.AG.Individual),
            nino = Some(Nino("HW827856C")),
            isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialStrength = Some("very strong"), isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialStrength = Some("little weak"), isNonCompliant = Some(true)))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialStrength = Some(""), isNonCompliant = Some(true)))
        .isValid shouldBe true
    }
  }

}
