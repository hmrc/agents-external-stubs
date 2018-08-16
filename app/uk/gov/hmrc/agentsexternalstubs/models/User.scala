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

  def isAdmin: Boolean = credentialRole.contains(User.CR.Admin)
  def isUser: Boolean = credentialRole.contains(User.CR.User)
  def isAssistant: Boolean = credentialRole.contains(User.CR.Assistant)
}

object User {

  object AG {
    final val Individual = "Individual"
    final val Organisation = "Organisation"
    final val Agent = "Agent"

    val all: String => Boolean = Set(Individual, Organisation, Agent).contains
  }

  object CR {
    final val Admin = "Admin"
    final val User = "User"
    final val Assistant = "Assistant"

    val all: String => Boolean = Set(Admin, User, Assistant).contains
  }

  def validate(user: User): Validated[NonEmptyList[String], Unit] = UserValidator.validate(user)

  def validateAndFlagCompliance(user: User): Validated[String, User] = validate(user) match {
    case Valid(_) => Valid(user.copy(isNonCompliant = None, complianceIssues = None))
    case Invalid(errors) =>
      if (user.isNonCompliant.contains(true))
        Valid(user.copy(isNonCompliant = Some(true), complianceIssues = Some(errors.toList)))
      else Invalid(errors.toList.mkString(", "))
  }

  implicit class UserBuilder(val user: User) extends AnyVal {
    def withPrincipalEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      user.copy(principalEnrolments = user.principalEnrolments :+ Enrolment(service, identifierKey, identifierValue))

    def withDelegatedEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      user.copy(
        delegatedEnrolments = user.delegatedEnrolments :+ Enrolment(
          service,
          Some(Seq(Identifier(identifierKey, identifierValue)))))
  }

  type Transformer = JsObject => JsObject

  val user_index_key = "user_index_key"
  val nino_index_key = "nino_index_key"
  val ttl_index_key = "ttl_index_key"
  val principal_enrolment_keys = "principal_enrolment_keys"
  val delegated_enrolment_keys = "delegated_enrolment_keys"
  val group_id_index_key = "group_id_index_key"
  val agent_code_index_key = "agent_code_index_key"

  def userIndexKey(userId: String, planetId: String): String = s"$userId@$planetId"
  def ninoIndexKey(nino: String, planetId: String): String = s"${nino.replace(" ", "")}@$planetId"
  def enrolmentIndexKey(key: String, planetId: String): String = s"$key@$planetId"
  def groupIdIndexKey(groupId: String, planetId: String): String = s"$groupId@$planetId"
  def agentCodeIndexKey(agentCode: String, planetId: String): String = s"$agentCode@$planetId"

  private def planetIdOf(json: JsObject): String =
    (json \ "planetId").asOpt[String].getOrElse("hmrc")

  private final val addNormalizedUserIndexKey: Transformer = json => {
    val userId = (json \ "userId").as[String]
    val planetId = planetIdOf(json)
    json + ((user_index_key, JsString(userIndexKey(userId, planetId))))
  }

  private final val addNormalizedNinoIndexKey: Transformer = json =>
    (json \ "nino")
      .asOpt[String]
      .map(nino => {
        val planetId = planetIdOf(json)
        json + ((nino_index_key, JsString(ninoIndexKey(nino, planetId))))
      })
      .getOrElse(json)

  private final val addTTLIndexKey: Transformer = json =>
    (json \ "isPermanent")
      .asOpt[Boolean] match {
      case None | Some(false) =>
        val planetId = planetIdOf(json)
        json + ((ttl_index_key, JsString(planetId)))
      case _ => json
  }

  private final val addPrincipalEnrolmentKeys: Transformer = json => {
    val enrolments = (json \ "principalEnrolments").as[Seq[Enrolment]]
    if (enrolments.isEmpty) json
    else {
      val planetId = planetIdOf(json)
      val keys =
        enrolments.map(_.toEnrolmentKeyTag).collect { case Some(key) => enrolmentIndexKey(key, planetId) }
      if (keys.isEmpty) json else json + ((principal_enrolment_keys, JsArray(keys.map(JsString))))
    }
  }

  private final val addDelegatedEnrolmentKeys: Transformer = json => {
    val enrolments = (json \ "delegatedEnrolments").as[Seq[Enrolment]]
    if (enrolments.isEmpty) json
    else {
      val planetId = planetIdOf(json)
      val keys = enrolments.map(_.toEnrolmentKeyTag).collect { case Some(key) => enrolmentIndexKey(key, planetId) }
      if (keys.isEmpty) json else json + ((delegated_enrolment_keys, JsArray(keys.map(JsString))))
    }
  }

  private final val addAgentCodeIndexKey: Transformer = json =>
    (json \ "credentialRole").asOpt[String] match {
      case Some(User.CR.Admin) =>
        (json \ "agentCode")
          .asOpt[String]
          .map(agentCode => {
            val planetId = planetIdOf(json)
            json + ((agent_code_index_key, JsString(agentCodeIndexKey(agentCode, planetId))))
          })
          .getOrElse(json)
      case _ => json
  }

  private final val addGroupIdIndexKey: Transformer = json =>
    (json \ "credentialRole").asOpt[String] match {
      case Some(User.CR.Admin) =>
        (json \ "groupId")
          .asOpt[String]
          .map(groupId => {
            val planetId = planetIdOf(json)
            json + ((group_id_index_key, JsString(groupIdIndexKey(groupId, planetId))))
          })
          .getOrElse(json)
      case _ => json
  }

  private final val addIndexedFields: Transformer = addNormalizedUserIndexKey
    .andThen(addNormalizedNinoIndexKey)
    .andThen(addTTLIndexKey)
    .andThen(addPrincipalEnrolmentKeys)
    .andThen(addDelegatedEnrolmentKeys)
    .andThen(addAgentCodeIndexKey)
    .andThen(addGroupIdIndexKey)

  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: Writes[User] = Json
    .writes[User]
    .transform(addIndexedFields)

  val formats = Format(reads, writes)

}
