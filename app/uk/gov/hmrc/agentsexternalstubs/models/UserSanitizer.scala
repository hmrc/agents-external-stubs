package uk.gov.hmrc.agentsexternalstubs.models

object UserSanitizer {

  def sanitize(user: User): User =
    if (user.isNonStandardUser.contains(true)) user else userSanitizers.foldLeft(user)((u, fx) => fx(u))

  private val ensureIndividualUserHaveName: User => User = user =>
    if (user.affinityGroup.contains(User.AG.Individual) && user.name.isEmpty)
      user.copy(name = Some(UserGenerator.name(user.userId)))
    else user

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
    if (user.credentialRole.isEmpty)
      user.copy(credentialRole = user.affinityGroup match {
        case Some(User.AG.Individual | User.AG.Agent) => Some(User.CR.User)
        case _                                        => None
      })
    else user

  private val userSanitizers =
    Seq(
      ensureIndividualUserHaveName,
      ensureIndividualUserHaveDateOfBirth,
      ensureOnlyIndividualUserHaveNINO,
      ensureOnlyIndividualUserHaveConfidenceLevel,
      ensureUserCredentialRole
    )

}
