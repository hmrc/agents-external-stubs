package uk.gov.hmrc.agentsexternalstubs.models
import cats.{Semigroup, SemigroupK}
import cats.data.{NonEmptyList, Validated}
import cats.data.Validated.{Invalid, Valid}

object UserValidator {

  private implicit val nelS: Semigroup[NonEmptyList[String]] =
    SemigroupK[NonEmptyList].algebra[String]

  private implicit val userS: Semigroup[Unit] = Semigroup.instance((_, _) => ())

  def validate(user: User): Validated[NonEmptyList[String], Unit] =
    if (user.isNonStandardUser.contains(true)) Valid(())
    else
      Seq(
        validateAffinityGroup(user),
        validateConfidenceLevel(user),
        validateCredentialStrength(user),
        validateCredentialRole(user),
        validateNino(user),
        validateConfidenceLevelAndNino(user),
        validateDelegatedEnrolments(user)
      ).reduce(_ combine _)

  def validateAffinityGroup(user: User): Validated[NonEmptyList[String], Unit] = user.affinityGroup match {
    case Some("Individual") | Some("Organisation") | Some("Agent") | None => Valid(())
    case _ =>
      Invalid(NonEmptyList.of("affinityGroup must be none, or one of [\"Individual\",\"Organisation\",\"Agent\"]"))
  }

  def validateConfidenceLevel(user: User): Validated[NonEmptyList[String], Unit] = user.confidenceLevel match {
    case Some(50) | Some(100) | Some(200) | Some(300)
        if user.affinityGroup.contains("Individual") && user.nino.isDefined =>
      Valid(())
    case None => Valid(())
    case _ =>
      Invalid(NonEmptyList.of("confidenceLevel can only be set for Individuals and has to be one of [50,100,200,300]"))
  }

  def validateCredentialStrength(user: User): Validated[NonEmptyList[String], Unit] = user.credentialStrength match {
    case Some("weak") | Some("strong") | None => Valid(())
    case _ =>
      Invalid(NonEmptyList.of("credentialStrength must be none, or one of [\"weak\",\"strong\"]"))
  }

  def validateCredentialRole(user: User): Validated[NonEmptyList[String], Unit] = user.credentialRole match {
    case Some("Admin") | Some("User") | Some("Assistant")
        if user.affinityGroup.exists(Set("Individual", "Agent").contains) =>
      Valid(())
    case None => Valid(())
    case _ =>
      Invalid(
        NonEmptyList.of(
          "credentialRole must be none, or one of [\"Admin\",\"User\",\"Assistant\"] for Individual or Agent only"))
  }

  def validateNino(user: User): Validated[NonEmptyList[String], Unit] = user.nino match {
    case Some(_) if user.affinityGroup.contains("Individual") => Valid(())
    case None                                                 => Valid(())
    case _                                                    => Invalid(NonEmptyList.of("NINO can be only set for Individual"))
  }

  def validateConfidenceLevelAndNino(user: User): Validated[NonEmptyList[String], Unit] =
    (user.affinityGroup, user.nino, user.confidenceLevel) match {
      case (Some("Individual"), Some(_), Some(_)) => Valid(())
      case (Some("Individual"), None, Some(_)) =>
        Invalid(NonEmptyList.of("confidenceLevel must be accompanied by NINO"))
      case (Some("Individual"), Some(_), None) =>
        Invalid(NonEmptyList.of("NINO must be accompanied by confidenceLevel"))
      case _ => Valid(())
    }

  def validateDelegatedEnrolments(user: User): Validated[NonEmptyList[String], Unit] = user.delegatedEnrolments match {
    case s if s.isEmpty                            => Valid(())
    case _ if user.affinityGroup.contains("Agent") => Valid(())
    case _                                         => Invalid(NonEmptyList.of("Only Agents can have delegated enrolments"))
  }

}
