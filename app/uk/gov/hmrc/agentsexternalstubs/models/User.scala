package uk.gov.hmrc.agentsexternalstubs.models

import cats.{Semigroup, SemigroupK}
import cats.data.{NonEmptyList, Validated}
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  planetId: Option[String] = None
)

object User {

  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: Writes[User] = Json
    .writes[User]
    .transform(addNormalizedUserIndexKey _)
    .transform(addNormalizedNinoIndexKey _)

  val formats = Format(reads, writes)

  val user_index_key = "user_index_key"
  val nino_index_key = "nino_index_key"

  def userIndexKey(userId: String, planetId: String): String = s"$userId@$planetId"
  def ninoIndexKey(nino: String, planetId: String): String = s"${nino.replace(" ", "")}@$planetId"

  private def addNormalizedUserIndexKey(json: JsObject): JsObject = {
    val userId = (json \ "userId").as[String]
    val planetId = (json \ "planetId").asOpt[String].getOrElse("hmrc")
    json + (user_index_key, JsString(userIndexKey(userId, planetId)))
  }

  private def addNormalizedNinoIndexKey(json: JsObject): JsObject =
    (json \ "nino")
      .asOpt[String]
      .map(nino => {
        val planetId = (json \ "planetId").asOpt[String].getOrElse("hmrc")
        json + (nino_index_key, JsString(ninoIndexKey(nino, planetId)))
      })
      .getOrElse(json)

  def individual(userId: String, confidenceLevel: Int, nino: String): User =
    User(
      userId = userId,
      affinityGroup = Some("Individual"),
      confidenceLevel = Some(confidenceLevel),
      nino = Some(Nino(nino)))

  implicit val nelS: Semigroup[NonEmptyList[String]] =
    SemigroupK[NonEmptyList].algebra[String]

  implicit val userS: Semigroup[Unit] = Semigroup.instance((_, _) => ())

  def validate(user: User): Validated[NonEmptyList[String], Unit] =
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
    case Some("User") | Some("Assistant") if user.affinityGroup.exists(Set("Individual", "Agent").contains) => Valid(())
    case None                                                                                               => Valid(())
    case _ =>
      Invalid(
        NonEmptyList.of("credentialRole must be none, or one of [\"User\",\"Assistant\"] for Individual or Agent"))
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

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None)

object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class Identifier(key: String, value: String)

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}
