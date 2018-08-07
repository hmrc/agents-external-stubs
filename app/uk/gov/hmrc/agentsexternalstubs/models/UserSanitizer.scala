package uk.gov.hmrc.agentsexternalstubs.models

object UserSanitizer {

  def sanitize(user: User): User =
    if (user.isNonStandardUser.contains(true)) user else userSanitizers.foldLeft(user)((u, fx) => fx(u))

  private val ensureUserHaveName: User => User = user =>
    if (user.name.isEmpty)
      user.affinityGroup match {
        case Some(User.AG.Individual)   => user.copy(name = Some(UserGenerator.nameForIndividual(user.userId)))
        case Some(User.AG.Agent)        => user.copy(name = Some(UserGenerator.nameForAgent(user.userId)))
        case Some(User.AG.Organisation) => user.copy(name = Some(UserGenerator.nameForOrganisation(user.userId)))
        case None                       => user
      } else user

  private val ensureIndividualUserHaveDateOfBirth: User => User = user =>
    if (user.affinityGroup.contains(User.AG.Individual) && user.dateOfBirth.isEmpty)
      user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(user.userId)))
    else user

  private val ensureOnlyIndividualUserHaveNINO: User => User = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.nino.isEmpty) user.copy(nino = Some(UserGenerator.nino(user.userId))) else user
      case _ => user.copy(nino = None)
  }

  private val ensureOnlyIndividualUserHaveConfidenceLevel: User => User = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.confidenceLevel.isEmpty)
          user.copy(confidenceLevel = Some(50))
        else user
      case _ => user.copy(confidenceLevel = None)
  }

  private val ensureUserCredentialRole: User => User = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual | User.AG.Agent) =>
        if (user.credentialRole.isEmpty) user.copy(credentialRole = Some(User.CR.User)) else user
      case _ => user.copy(credentialRole = None)
  }

  private val ensureOnlyIndividualUserHaveDateOfBirth: User => User = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.dateOfBirth.isEmpty)
          user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(user.userId)))
        else user
      case _ => user.copy(dateOfBirth = None)
  }

  private val ensureUserHaveGroupIdentifier: User => User = user =>
    if (user.groupId.isEmpty) user.copy(groupId = Some(UserGenerator.groupId(user.userId))) else user

  private val userSanitizers =
    Seq(
      ensureUserHaveName,
      ensureIndividualUserHaveDateOfBirth,
      ensureOnlyIndividualUserHaveNINO,
      ensureOnlyIndividualUserHaveConfidenceLevel,
      ensureUserCredentialRole,
      ensureOnlyIndividualUserHaveDateOfBirth,
      ensureUserHaveGroupIdentifier
    )

}
