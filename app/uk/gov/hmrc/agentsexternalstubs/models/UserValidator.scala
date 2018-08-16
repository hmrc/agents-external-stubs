package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.{Semigroup, SemigroupK}

object UserValidator {

  private implicit val nelS: Semigroup[NonEmptyList[String]] =
    SemigroupK[NonEmptyList].algebra[String]

  private implicit val userS: Semigroup[Unit] = Semigroup.instance((_, _) => ())

  def validate(user: User): Validated[NonEmptyList[String], Unit] =
    userValidators.foldLeft[Validated[NonEmptyList[String], Unit]](Valid(()))((v, fx) => v combine fx(user))

  type UserValidator = User => Validated[NonEmptyList[String], Unit]

  val validateAffinityGroup: UserValidator = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) | Some(User.AG.Organisation) | Some(User.AG.Agent) | None => Valid(())
      case _ =>
        Invalid(NonEmptyList.of("affinityGroup must be none, or one of [Individual, Organisation, Agent]"))
  }

  val validateConfidenceLevel: UserValidator = user =>
    user.confidenceLevel match {
      case Some(50) | Some(100) | Some(200) | Some(300)
          if user.affinityGroup.contains(User.AG.Individual) && user.nino.isDefined =>
        Valid(())
      case None => Valid(())
      case _ =>
        Invalid(
          NonEmptyList.of("confidenceLevel can only be set for Individuals and has to be one of [50, 100, 200, 300]"))
  }

  val validateCredentialStrength: UserValidator = user =>
    user.credentialStrength match {
      case Some("weak") | Some("strong") | None => Valid(())
      case _ =>
        Invalid(NonEmptyList.of("credentialStrength must be none, or one of [weak, strong]"))
  }

  val validateCredentialRole: UserValidator = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual | User.AG.Agent) =>
        if (user.credentialRole.isEmpty || user.credentialRole.exists(User.CR.all)) Valid(())
        else
          Invalid(
            NonEmptyList.of("credentialRole must be none, or one of [Admin, User, Assistant] for Individual or Agent"))
      case Some(User.AG.Organisation) =>
        if (user.credentialRole.contains(User.CR.Admin)) Valid(())
        else Invalid(NonEmptyList.of("credentialRole must be Admin for Organisation"))
      case _ => Valid(())
  }

  val validateNino: UserValidator = user =>
    user.nino match {
      case Some(_) if user.affinityGroup.contains(User.AG.Individual) => Valid(())
      case None                                                       => Valid(())
      case _                                                          => Invalid(NonEmptyList.of("NINO can be only set for Individual"))
  }

  val validateConfidenceLevelAndNino: UserValidator = user =>
    (user.affinityGroup, user.nino, user.confidenceLevel) match {
      case (Some(User.AG.Individual), Some(_), Some(_)) => Valid(())
      case (Some(User.AG.Individual), None, Some(_)) =>
        Invalid(NonEmptyList.of("confidenceLevel must be accompanied by NINO"))
      case (Some(User.AG.Individual), Some(_), None) =>
        Invalid(NonEmptyList.of("NINO must be accompanied by confidenceLevel"))
      case _ => Valid(())
  }

  val validateDelegatedEnrolments: UserValidator = user =>
    user.delegatedEnrolments match {
      case s if s.isEmpty                                  => Valid(())
      case _ if user.affinityGroup.contains(User.AG.Agent) => Valid(())
      case _                                               => Invalid(NonEmptyList.of("Only Agents can have delegated enrolments"))
  }

  val validateDateOfBirth: UserValidator = user =>
    user.dateOfBirth match {
      case Some(_) if user.affinityGroup.contains(User.AG.Individual) => Valid(())
      case None                                                       => Valid(())
      case _                                                          => Invalid(NonEmptyList.of("dateOfBirth can be only set for Individual"))
  }

  val validateAgentCode: UserValidator = user =>
    user.agentCode match {
      case Some(_) if user.affinityGroup.contains(User.AG.Agent) => Valid(())
      case None if user.affinityGroup.contains(User.AG.Agent) =>
        Invalid(NonEmptyList.of("agentCode is required for Agent"))
      case _ => Valid(())
  }

  val userValidators: Seq[UserValidator] = Seq(
    validateAffinityGroup,
    validateConfidenceLevel,
    validateCredentialStrength,
    validateCredentialRole,
    validateNino,
    validateConfidenceLevelAndNino,
    validateDelegatedEnrolments,
    validateDateOfBirth,
    validateAgentCode
  )

}
