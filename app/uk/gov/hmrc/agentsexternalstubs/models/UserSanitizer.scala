package uk.gov.hmrc.agentsexternalstubs.models

object UserSanitizer {

  def sanitizeUser(user: User): User = userSanitizers.foldLeft(user)((u, fx) => fx(u))

  private val ensureIndividualUserHaveName: User => User = user =>
    if (user.affinityGroup.contains(User.AG.Individual) && user.name.isEmpty)
      user.copy(name = Some(UserGenerator.name(user.userId)))
    else user

  private val ensureIndividualUserHaveDateOfBirth: User => User = user =>
    if (user.affinityGroup.contains(User.AG.Individual) && user.dateOfBirth.isEmpty)
      user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(user.userId)))
    else user

  private val userSanitizers = Seq(ensureIndividualUserHaveName, ensureIndividualUserHaveDateOfBirth)

}
