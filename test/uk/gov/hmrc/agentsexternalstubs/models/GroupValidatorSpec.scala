package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.play.test.UnitSpec

class GroupValidatorSpec extends UnitSpec {

  "GroupValidator" should {
    "validate empty group" in {
      GroupValidator.validate(Seq()) shouldBe Right(())
    }
    "validate group with users without affinity" in {
      GroupValidator.validate(Seq(User("foo", credentialRole = Some("Admin")))) shouldBe Right(())
      GroupValidator.validate(Seq(User("foo", credentialRole = Some("Admin")), User("bar"))) shouldBe Right(())
    }
    "validate only when group is empty or have one and at most one Admin" in {
      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"))) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"), User("foo"))) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"))) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), User("foo"))) shouldBe Right(())

      GroupValidator.validate(Seq(
        UserGenerator.individual(credentialRole = "Admin"),
        UserGenerator.individual(credentialRole = "User"))) shouldBe Right(())
      GroupValidator.validate(Seq(
        UserGenerator.individual(credentialRole = "Admin"),
        UserGenerator.individual(credentialRole = "Assistant"))) shouldBe Right(())
      GroupValidator.validate(
        Seq(
          UserGenerator.individual(credentialRole = "Admin"),
          UserGenerator.individual(credentialRole = "User"),
          UserGenerator.individual(credentialRole = "Assistant"))) shouldBe Right(())

      GroupValidator.validate(Seq(
        UserGenerator.agent(credentialRole = "Admin"),
        UserGenerator.agent(credentialRole = "User"))) shouldBe Right(())
      GroupValidator.validate(Seq(
        UserGenerator.agent(credentialRole = "Admin"),
        UserGenerator.agent(credentialRole = "Assistant"))) shouldBe Right(())
      GroupValidator.validate(
        Seq(
          UserGenerator.agent(credentialRole = "Admin"),
          UserGenerator.agent(credentialRole = "User"),
          UserGenerator.agent(credentialRole = "Assistant"))) shouldBe Right(())

      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "User"))).isLeft shouldBe true
    }
    "validate only if group have at most one Organisation" in {
      GroupValidator.validate(Seq(UserGenerator.organisation())) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "User"))) shouldBe Right(
        ())
      GroupValidator.validate(Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))) shouldBe Right(
        ())

      GroupValidator.validate(Seq(UserGenerator.organisation(), UserGenerator.organisation())).isLeft shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.organisation(), UserGenerator.individual()))
        .isLeft shouldBe true
    }
    "validate only if group is not only consisting of Assistants" in {
      GroupValidator.validate(Seq(
        UserGenerator.individual(credentialRole = "Admin"),
        UserGenerator.individual(credentialRole = "Assistant"))) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))) shouldBe Right(
        ())

      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "Assistant")))
        .isLeft shouldBe true
      GroupValidator
        .validate(
          Seq(
            UserGenerator.individual(credentialRole = "Assistant"),
            UserGenerator.individual(credentialRole = "Assistant")))
        .isLeft shouldBe true
    }
    "validate if agents are not in the group with Organisation and Individuals" in {
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"))) shouldBe Right(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), UserGenerator.agent())) shouldBe Right(
        ())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), User("foo"))) shouldBe Right(())

      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "User"), UserGenerator.agent(credentialRole = "Admin")))
        .isLeft shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.agent(credentialRole = "User")))
        .isLeft shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.agent(credentialRole = "User")))
        .isLeft shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.agent(credentialRole = "Assistant")))
        .isLeft shouldBe true
    }
  }

}
