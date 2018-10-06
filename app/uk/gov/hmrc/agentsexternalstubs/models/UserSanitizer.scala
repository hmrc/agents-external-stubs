package uk.gov.hmrc.agentsexternalstubs.models
import org.scalacheck.Gen
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

object UserSanitizer extends RecordUtils[User] {

  def sanitize(user: User): User = sanitize(user.userId)(user)

  override val gen: Gen[User] =
    for (userId <- Gen.uuid.map(_.toString)) yield User(userId = userId)

  private val ensureUserHaveName: Update = seed =>
    user =>
      if (user.name.isEmpty)
        user.affinityGroup match {
          case Some(User.AG.Individual) => user.copy(name = Some(UserGenerator.nameForIndividual(seed)))
          case Some(User.AG.Agent) =>
            user.copy(name = Some(UserGenerator.nameForAgent(seed, user.groupId.getOrElse(seed))))
          case Some(_) => user.copy(name = Some(UserGenerator.nameForOrganisation(seed)))
          case None    => user
        } else user

  private val ensureIndividualUserHaveDateOfBirth: Update = seed =>
    user =>
      if (user.affinityGroup.contains(User.AG.Individual) && user.dateOfBirth.isEmpty)
        user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(seed)))
      else user

  private val ensureOnlyIndividualUserHaveNINO: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Individual) =>
          if (user.nino.isEmpty) user.copy(nino = Some(Generator.ninoWithSpaces(seed))) else user
        case _ => user.copy(nino = None)
  }

  private val ensureOnlyIndividualUserHaveConfidenceLevel: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Individual) =>
          if (user.confidenceLevel.isEmpty)
            user.copy(confidenceLevel = Some(50))
          else user
        case _ => user.copy(confidenceLevel = None)
  }

  private val ensureUserHaveCredentialRole: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Individual | User.AG.Agent) =>
          if (user.credentialRole.isEmpty) user.copy(credentialRole = Some(User.CR.User)) else user
        case Some(User.AG.Organisation) =>
          user.copy(credentialRole = Some(User.CR.Admin))
        case _ => user.copy(credentialRole = None)
  }

  private val ensureOnlyIndividualUserHaveDateOfBirth: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Individual) =>
          if (user.dateOfBirth.isEmpty)
            user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(seed)))
          else user
        case _ => user.copy(dateOfBirth = None)
  }

  private val ensureUserHaveGroupIdentifier: Update = seed =>
    user => if (user.groupId.isEmpty) user.copy(groupId = Some(UserGenerator.groupId(seed))) else user

  private val ensureAgentHaveAgentCode: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Agent) =>
          if (user.agentCode.isEmpty)
            user.copy(agentCode = Some(UserGenerator.agentCode(user.groupId.getOrElse(seed))))
          else user
        case _ => user.copy(agentCode = None)
  }

  private val ensureAgentHaveAgentId: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Agent) =>
          if (user.agentId.isEmpty)
            user.copy(agentId = Some(UserGenerator.agentId(user.groupId.getOrElse(seed))))
          else user
        case _ => user.copy(agentId = None)
  }

  private val ensureAgentHaveFriendlyName: Update = seed =>
    user =>
      user.affinityGroup match {
        case Some(User.AG.Agent) =>
          if (user.agentFriendlyName.isEmpty)
            user.copy(agentFriendlyName = Some(UserGenerator.agentFriendlyName(user.groupId.getOrElse(seed))))
          else user
        case _ => user.copy(agentFriendlyName = None)
  }

  private val ensurePrincipalEnrolmentKeysAreDistinct: Update = seed =>
    user => {
      user.copy(
        principalEnrolments = user.principalEnrolments
          .groupBy(_.key)
          .collect {
            case (key, es) if es.size == 1 || Services(key).exists(_.flags.multipleEnrolment) => es
            case (_, es) =>
              Seq(es.maxBy(_.identifiers.map(_.size).getOrElse(0)))
          }
          .flatten
          .toSeq
      )

  }

  private val ensurePrincipalEnrolmentsHaveIdentifiers: Update = seed =>
    user => {
      val modifiedPrincipalEnrolments = user.principalEnrolments
        .groupBy(_.key)
        .flatMap {
          case (_, es) =>
            es.zipWithIndex.map { case (e, i) => ensureEnrolmentHaveIdentifier(Generator.variant(seed, i))(e) }
        }
        .toSeq
      user.copy(principalEnrolments = modifiedPrincipalEnrolments)
  }

  private val ensureDelegatedEnrolmentsHaveIdentifiers: Update = seed =>
    user => {
      val modifiedDelegatedEnrolments = user.delegatedEnrolments
        .groupBy(_.key)
        .flatMap {
          case (_, es) =>
            es.zipWithIndex.map { case (e, i) => ensureEnrolmentHaveIdentifier(Generator.variant(seed, i))(e) }
        }
        .toSeq
      user.copy(delegatedEnrolments = modifiedDelegatedEnrolments)
  }

  private val ensureEnrolmentHaveIdentifier: String => Enrolment => Enrolment = seed =>
    e =>
      if (e.identifiers.isEmpty) Services(e.key).flatMap(s => Generator.get(s.generator)(seed)).getOrElse(e)
      else
        e.copy(identifiers = e.identifiers.map(_.map(i => {
          val key: String =
            if (i.key.isEmpty) Services(e.key).flatMap(s => s.identifiers.headOption.map(_.name)).getOrElse("")
            else i.key
          val value: String =
            if (i.value.isEmpty)
              Services(e.key)
                .flatMap(s => s.getIdentifier(key).flatMap(i => Generator.get(i.valueGenerator)(seed)))
                .getOrElse("")
            else i.value
          Identifier(key, value)
        })))

  private def sanitizeAddress(addressOpt: Option[User.Address], seed: String): Option[User.Address] = {
    val newAddress = Generator.address(seed)
    addressOpt match {
      case None =>
        Some(
          User.Address(
            line1 = Some(newAddress.street.take(35)),
            line2 = Some(newAddress.town.take(35)),
            postcode = Some(newAddress.postcode),
            countryCode = Some("GB")))
      case Some(address) =>
        Some(
          address.copy(
            line1 = address.line1.map(_.take(35)).orElse(Some(newAddress.street.take(35))),
            line2 = address.line2.map(_.take(35)).orElse(Some(newAddress.town.take(35))),
            postcode = UserValidator
              .postalCodeValidator(address.postcode)
              .fold(_ => None, _ => address.postcode)
              .orElse(Generator.get(Generator.postcode)(seed)),
            countryCode = UserValidator
              .countryCodeValidator(address.countryCode)
              .fold(_ => None, _ => address.countryCode)
              .orElse(Some("GB"))
          ))
    }
  }

  private val ensureUserHaveAddress: Update = seed => user => user.copy(address = sanitizeAddress(user.address, seed))

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
      ensureUserHaveAddress,
      ensurePrincipalEnrolmentKeysAreDistinct,
      ensurePrincipalEnrolmentsHaveIdentifiers,
      ensureDelegatedEnrolmentsHaveIdentifiers
    )

  override val validate: Validator[User] = UserValidator.validate

}
