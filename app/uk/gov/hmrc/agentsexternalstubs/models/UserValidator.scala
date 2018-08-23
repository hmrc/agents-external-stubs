package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import cats.{Semigroup, SemigroupK}

object UserValidator {

  type UserConstraint = User => Validated[String, Unit]

  val validateAffinityGroup: UserConstraint = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) | Some(User.AG.Organisation) | Some(User.AG.Agent) | None => Valid(())
      case _ =>
        Invalid("affinityGroup must be none, or one of [Individual, Organisation, Agent]")
  }

  val validateConfidenceLevel: UserConstraint = user =>
    user.confidenceLevel match {
      case Some(50) | Some(100) | Some(200) | Some(300)
          if user.affinityGroup.contains(User.AG.Individual) && user.nino.isDefined =>
        Valid(())
      case None => Valid(())
      case _ =>
        Invalid("confidenceLevel can only be set for Individuals and has to be one of [50, 100, 200, 300]")
  }

  val validateCredentialStrength: UserConstraint = user =>
    user.credentialStrength match {
      case Some("weak") | Some("strong") | None => Valid(())
      case _ =>
        Invalid("credentialStrength must be none, or one of [weak, strong]")
  }

  val validateCredentialRole: UserConstraint = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual | User.AG.Agent) =>
        if (user.credentialRole.isEmpty || user.credentialRole.exists(User.CR.all)) Valid(())
        else
          Invalid("credentialRole must be none, or one of [Admin, User, Assistant] for Individual or Agent")
      case Some(User.AG.Organisation) =>
        if (user.credentialRole.contains(User.CR.Admin)) Valid(())
        else Invalid("credentialRole must be Admin for Organisation")
      case _ => Valid(())
  }

  val validateNino: UserConstraint = user =>
    user.nino match {
      case Some(_) if user.affinityGroup.contains(User.AG.Individual) => Valid(())
      case None                                                       => Valid(())
      case _                                                          => Invalid("NINO can be only set for Individual")
  }

  val validateConfidenceLevelAndNino: UserConstraint = user =>
    (user.affinityGroup, user.nino, user.confidenceLevel) match {
      case (Some(User.AG.Individual), Some(_), Some(_)) => Valid(())
      case (Some(User.AG.Individual), None, Some(_)) =>
        Invalid("confidenceLevel must be accompanied by NINO")
      case (Some(User.AG.Individual), Some(_), None) =>
        Invalid("NINO must be accompanied by confidenceLevel")
      case _ => Valid(())
  }

  val validateDelegatedEnrolments: UserConstraint = user =>
    user.delegatedEnrolments match {
      case s if s.isEmpty                                  => Valid(())
      case _ if user.affinityGroup.contains(User.AG.Agent) => Valid(())
      case _                                               => Invalid("Only Agents can have delegated enrolments")
  }

  val validateDateOfBirth: UserConstraint = user =>
    user.dateOfBirth match {
      case Some(_) if user.affinityGroup.contains(User.AG.Individual) => Valid(())
      case None                                                       => Valid(())
      case _                                                          => Invalid("dateOfBirth can be only set for Individual")
  }

  val validateAgentCode: UserConstraint = user =>
    user.agentCode match {
      case Some(_) if user.affinityGroup.contains(User.AG.Agent) => Valid(())
      case None if user.affinityGroup.contains(User.AG.Agent) =>
        Invalid("agentCode is required for Agent")
      case _ => Valid(())
  }

  private val constraints: Seq[UserConstraint] = Seq(
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

  val validate: User => Validated[List[String], Unit] = Validator.validate(constraints: _*)

}
