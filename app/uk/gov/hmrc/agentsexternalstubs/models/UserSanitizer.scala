package uk.gov.hmrc.agentsexternalstubs.models
import org.scalacheck.Gen
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

object UserSanitizer extends RecordUtils[User] {

  override val gen: Gen[User] =
    for (userId <- Gen.uuid.map(_.toString)) yield User(userId = userId)

  private val ensureUserHaveName: Update = user =>
    if (user.name.isEmpty)
      user.affinityGroup match {
        case Some(User.AG.Individual) => user.copy(name = Some(UserGenerator.nameForIndividual(user.userId)))
        case Some(User.AG.Agent) =>
          user.copy(name = Some(UserGenerator.nameForAgent(user.userId, user.groupId.getOrElse(user.userId))))
        case Some(_) => user.copy(name = Some(UserGenerator.nameForOrganisation(user.userId)))
        case None    => user
      } else user

  private val ensureIndividualUserHaveDateOfBirth: Update = user =>
    if (user.affinityGroup.contains(User.AG.Individual) && user.dateOfBirth.isEmpty)
      user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(user.userId)))
    else user

  private val ensureOnlyIndividualUserHaveNINO: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.nino.isEmpty) user.copy(nino = Some(UserGenerator.ninoWithSpaces(user.userId))) else user
      case _ => user.copy(nino = None)
  }

  private val ensureOnlyIndividualUserHaveConfidenceLevel: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.confidenceLevel.isEmpty)
          user.copy(confidenceLevel = Some(50))
        else user
      case _ => user.copy(confidenceLevel = None)
  }

  private val ensureUserHaveCredentialRole: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual | User.AG.Agent) =>
        if (user.credentialRole.isEmpty) user.copy(credentialRole = Some(User.CR.User)) else user
      case Some(User.AG.Organisation) =>
        user.copy(credentialRole = Some(User.CR.Admin))
      case _ => user.copy(credentialRole = None)
  }

  private val ensureOnlyIndividualUserHaveDateOfBirth: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Individual) =>
        if (user.dateOfBirth.isEmpty)
          user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(user.userId)))
        else user
      case _ => user.copy(dateOfBirth = None)
  }

  private val ensureUserHaveGroupIdentifier: Update = user =>
    if (user.groupId.isEmpty) user.copy(groupId = Some(UserGenerator.groupId(user.userId))) else user

  private val ensureAgentHaveAgentCode: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Agent) =>
        if (user.agentCode.isEmpty)
          user.copy(agentCode = Some(UserGenerator.agentCode(user.groupId.getOrElse(user.userId))))
        else user
      case _ => user.copy(agentCode = None)
  }

  private val ensureAgentHaveAgentId: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Agent) =>
        if (user.agentId.isEmpty)
          user.copy(agentId = Some(UserGenerator.agentId(user.groupId.getOrElse(user.userId))))
        else user
      case _ => user.copy(agentId = None)
  }

  private val ensureAgentHaveFriendlyName: Update = user =>
    user.affinityGroup match {
      case Some(User.AG.Agent) =>
        if (user.agentFriendlyName.isEmpty)
          user.copy(agentFriendlyName = Some(UserGenerator.agentFriendlyName(user.groupId.getOrElse(user.userId))))
        else user
      case _ => user.copy(agentFriendlyName = None)
  }

  private val ensurePrincipalEnrolmentsHaveIdentifiers: Update = user => {
    val modifiedPrincipalEnrolments = user.principalEnrolments.map(
      e =>
        if (e.identifiers.isEmpty) Services(e.key).flatMap(s => Generator.get(s.generator)(user.userId)).getOrElse(e)
        else
          e.copy(identifiers = e.identifiers.map(_.map(i => {
            val key: String =
              if (i.key.isEmpty) Services(e.key).flatMap(s => s.identifiers.headOption.map(_.name)).getOrElse("")
              else i.key
            val value: String =
              if (i.value.isEmpty)
                Services(e.key)
                  .flatMap(s => s.getIdentifier(key).flatMap(i => Generator.get(i.valueGenerator)(user.userId)))
                  .getOrElse("")
              else i.value
            Identifier(key, value)
          }))))
    user.copy(principalEnrolments = modifiedPrincipalEnrolments)
  }

  private val ensureDelegatedEnrolmentsHaveIdentifiers: Update = user => {
    val modifiedDelegatedEnrolments = user.delegatedEnrolments.map(
      e =>
        if (e.identifiers.isEmpty) Services(e.key).flatMap(s => Generator.get(s.generator)(user.userId)).getOrElse(e)
        else
          e.copy(identifiers = e.identifiers.map(_.map(i => {
            val key: String =
              if (i.key.isEmpty) Services(e.key).flatMap(s => s.identifiers.headOption.map(_.name)).getOrElse("")
              else i.key
            val value: String =
              if (i.value.isEmpty)
                Services(e.key)
                  .flatMap(s => s.getIdentifier(key).flatMap(i => Generator.get(i.valueGenerator)(user.userId)))
                  .getOrElse("")
              else i.value
            Identifier(key, value)
          }))))
    user.copy(delegatedEnrolments = modifiedDelegatedEnrolments)
  }

  override val sanitizers: Seq[Update] =
    Seq(
      ensureUserHaveGroupIdentifier,
      ensureUserHaveName,
      ensureIndividualUserHaveDateOfBirth,
      ensureOnlyIndividualUserHaveNINO,
      ensureOnlyIndividualUserHaveConfidenceLevel,
      ensureUserHaveCredentialRole,
      ensureOnlyIndividualUserHaveDateOfBirth,
      ensureAgentHaveAgentCode,
      ensureAgentHaveAgentId,
      ensureAgentHaveFriendlyName,
      ensurePrincipalEnrolmentsHaveIdentifiers,
      ensureDelegatedEnrolmentsHaveIdentifiers
    )

  override val validate: Validator[User] = UserValidator.validate

}
