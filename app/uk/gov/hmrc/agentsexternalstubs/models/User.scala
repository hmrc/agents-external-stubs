package uk.gov.hmrc.agentsexternalstubs.models

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import org.joda.time.LocalDate
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
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  agentId: Option[String] = None,
  planetId: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  isPermanent: Option[Boolean] = None
) {

  def isIndividual: Boolean = affinityGroup.contains(User.AG.Individual)
  def isOrganisation: Boolean = affinityGroup.contains(User.AG.Organisation)
  def isAgent: Boolean = affinityGroup.contains(User.AG.Agent)
}

object User {

  object AG {
    final val Individual = "Individual"
    final val Organisation = "Organisation"
    final val Agent = "Agent"

    val contains = Set(Individual, Organisation, Agent).contains _
  }

  object CR {
    final val Admin = "Admin"
    final val User = "User"
    final val Assistant = "Assistant"
  }

  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: Writes[User] = Json
    .writes[User]
    .transform(addNormalizedUserIndexKey _)
    .transform(addNormalizedNinoIndexKey _)
    .transform(addTTLIndexKey _)

  val formats = Format(reads, writes)

  val user_index_key = "user_index_key"
  val nino_index_key = "nino_index_key"
  val ttl_index_key = "ttl_index_key"

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

  private def addTTLIndexKey(json: JsObject): JsObject =
    (json \ "isPermanent")
      .asOpt[Boolean] match {
      case None | Some(false) =>
        val planetId = (json \ "planetId").asOpt[String].getOrElse("hmrc")
        json + (ttl_index_key, JsString(planetId))
      case _ => json
    }

  def validate(user: User): Validated[NonEmptyList[String], Unit] = UserValidator.validate(user)

  def validateAndFlagCompliance(user: User): Validated[String, User] = validate(user) match {
    case Valid(_) => Valid(user.copy(isNonCompliant = None, complianceIssues = None))
    case Invalid(errors) =>
      if (user.isNonCompliant.contains(true))
        Valid(user.copy(isNonCompliant = Some(true), complianceIssues = Some(errors.toList)))
      else Invalid(errors.toList.mkString(", "))
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
