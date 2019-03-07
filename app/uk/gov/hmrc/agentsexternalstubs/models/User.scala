package uk.gov.hmrc.agentsexternalstubs.models

import cats.data.Validated.{Invalid, Valid}
import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.User.AdditionalInformation
import uk.gov.hmrc.domain.Nino

import scala.util.Random

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
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty
) {

  def isIndividual: Boolean = affinityGroup.contains(User.AG.Individual)
  def isOrganisation: Boolean = affinityGroup.contains(User.AG.Organisation)
  def isAgent: Boolean = affinityGroup.contains(User.AG.Agent)

  def isAdmin: Boolean = credentialRole.contains(User.CR.Admin)
  def isUser: Boolean = credentialRole.contains(User.CR.User)
  def isAssistant: Boolean = credentialRole.contains(User.CR.Assistant)

  lazy val firstName: Option[String] =
    name
      .map(_.split(" ").dropRight(1))
      .map(a => if (a.nonEmpty) a.mkString(" ") else Generator.get(Generator.forename())(userId).getOrElse("John"))

  def lastName: Option[String] = name.map(_.split(" ").last)

  def withRecordId(recordId: String): User = copy(recordIds = recordIds :+ recordId)

  final val facts: String => Option[String] = {
    case n if n.toLowerCase.contains("postcode") => address.flatMap(_.postcode)
    case _                                       => None
  }

  def findIdentifierValue(serviceName: String, identifierName: String): Option[String] =
    principalEnrolments
      .find(_.key == serviceName)
      .flatMap(_.identifiers.flatMap(_.find(_.key == identifierName)))
      .map(_.value)

  def findDelegatedIdentifierValues(serviceName: String, identifierName: String): Seq[String] =
    delegatedEnrolments
      .filter(_.key == serviceName)
      .flatMap(_.identifiers.flatMap(_.find(_.key == identifierName)))
      .map(_.value)
}

object User {

  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaReads._

  case class Address(
    line1: Option[String] = None,
    line2: Option[String] = None,
    line3: Option[String] = None,
    line4: Option[String] = None,
    postcode: Option[String] = None,
    countryCode: Option[String] = None) {

    def isUKAddress: Boolean = countryCode.contains("GB")

  }

  object Address {
    implicit lazy val formats: Format[Address] = Json.format[Address]
  }

  case class AdditionalInformation(vatRegistrationDate: Option[LocalDate] = None)

  object AdditionalInformation {
    implicit lazy val formats: Format[AdditionalInformation] = Json.format[AdditionalInformation]
  }

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

  object Individual {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Individual) Some(user) else None)
  }

  object Organisation {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Organisation) Some(user) else None)
  }

  object Agent {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Agent) Some(user) else None)
  }

  object Matches {
    def apply(affinityGroupMatches: String => Boolean, serviceName: String): MatchesAffinityGroupAndEnrolment =
      MatchesAffinityGroupAndEnrolment(affinityGroupMatches, serviceName)
  }

  case class MatchesAffinityGroupAndEnrolment(affinityGroupMatches: String => Boolean, serviceName: String) {
    def unapply(user: User): Option[(User, String)] =
      user.affinityGroup
        .flatMap(ag => if (affinityGroupMatches(ag)) Some(user) else None)
        .flatMap(_.principalEnrolments.find(_.key == serviceName))
        .flatMap(_.identifiers)
        .flatMap(_.map(_.value).headOption)
        .map((user, _))
  }

  def validate(user: User): Either[List[String], User] = UserValidator.validate(user) match {
    case Valid(())       => Right(user)
    case Invalid(errors) => Left(errors)
  }

  implicit class UserBuilder(val user: User) extends AnyVal {
    def withPrincipalEnrolment(enrolment: Enrolment): User =
      user.copy(principalEnrolments = user.principalEnrolments :+ enrolment)

    def withPrincipalEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withPrincipalEnrolment(Enrolment(service, identifierKey, identifierValue))

    def withPrincipalEnrolment(enrolmentKey: String): User =
      withPrincipalEnrolment(
        Enrolment.from(EnrolmentKey.parse(enrolmentKey).fold(e => throw new Exception(e), identity)))

    def withStrideRole(role: String): User = user.copy(strideRoles = user.strideRoles :+ role)

    def withDelegatedEnrolment(enrolment: Enrolment): User =
      user.copy(delegatedEnrolments = user.delegatedEnrolments :+ enrolment)

    def withDelegatedEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withDelegatedEnrolment(Enrolment(service, Some(Seq(Identifier(identifierKey, identifierValue)))))

    def withDelegatedEnrolment(enrolmentKey: String): User =
      withDelegatedEnrolment(
        Enrolment.from(EnrolmentKey.parse(enrolmentKey).fold(e => throw new Exception(e), identity)))
  }

  type Transformer = JsObject => JsObject

  val user_index_key = "_user_index_key"
  val nino_index_key = "_nino_index_key"
  val ttl_index_key = "_ttl_index_key"
  val principal_enrolment_keys = "_principal_enrolment_keys"
  val delegated_enrolment_keys = "_delegated_enrolment_keys"
  val group_id_index_key = "_group_id_index_key"
  val agent_code_index_key = "_agent_code_index_key"

  def userIndexKey(userId: String, planetId: String): String = s"$userId@$planetId"
  def ninoIndexKey(nino: String, planetId: String): String = s"${nino.replace(" ", "")}@$planetId"
  def enrolmentIndexKey(key: String, planetId: String): String = s"$key@$planetId"
  def groupIdIndexKey(groupId: String, planetId: String): String = s"$groupId@$planetId"
  def agentCodeIndexKey(agentCode: String, planetId: String): String = s"$agentCode@$planetId"

  private def planetIdOf(json: JsObject): String =
    (json \ "planetId").asOpt[String].getOrElse(Planet.DEFAULT)

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

  import play.api.libs.functional.syntax._

  implicit val reads: Reads[User] = (
    (JsPath \ "userId").readNullable[String].map(_.getOrElse(UserIdGenerator.nextUserId)) and
      (JsPath \ "groupId").readNullable[String] and
      (JsPath \ "affinityGroup").readNullable[String] and
      (JsPath \ "confidenceLevel").readNullable[Int] and
      (JsPath \ "credentialStrength").readNullable[String] and
      (JsPath \ "credentialRole").readNullable[String] and
      (JsPath \ "nino").readNullable[Nino] and
      (JsPath \ "principalEnrolments").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "delegatedEnrolments").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "dateOfBirth").readNullable[LocalDate] and
      (JsPath \ "agentCode").readNullable[String] and
      (JsPath \ "agentFriendlyName").readNullable[String] and
      (JsPath \ "agentId").readNullable[String] and
      (JsPath \ "planetId").readNullable[String] and
      (JsPath \ "isNonCompliant").readNullable[Boolean] and
      (JsPath \ "complianceIssues").readNullable[Seq[String]] and
      (JsPath \ "recordIds").readNullable[Seq[String]].map(_.map(_.distinct).getOrElse(Seq.empty)) and
      (JsPath \ "address").readNullable[Address] and
      (JsPath \ "additionalInformation").readNullable[AdditionalInformation] and
      (JsPath \ "strideRoles").readNullable[Seq[String]].map(_.getOrElse(Seq.empty))
  )(User.apply _)

  val plainWrites: OWrites[User] = Json.writes[User]

  implicit val writes: Writes[User] = plainWrites.transform(addIndexedFields)

  val formats = Format(reads, writes)

  def parseUserIdAtPlanetId(credId: String, defaultPlanetId: => String): (String, String) = {
    val at = credId.indexOf('@')
    if (at >= 0) (credId.substring(0, at), credId.substring(at + 1))
    else (credId, defaultPlanetId)
  }

  def knownFactsOf(user: User): Map[String, String] = {
    val postcodeOpt = user.address.flatMap(_.postcode)
    val vatRegDateOpt = user.additionalInformation
      .flatMap(_.vatRegistrationDate.map(_.toString("dd/MM/yy")))
    Seq(
      "PostCode"            -> postcodeOpt,
      "BusinessPostcode"    -> postcodeOpt,
      "businesspostcode"    -> postcodeOpt,
      "Postcode"            -> postcodeOpt,
      "POSTCODE"            -> postcodeOpt,
      "AgencyPostcode"      -> postcodeOpt,
      "IRAgentPostcode"     -> postcodeOpt,
      "IRPCODE"             -> postcodeOpt,
      "IREFFREGDATE"        -> vatRegDateOpt,
      "VATRegistrationDate" -> vatRegDateOpt,
      "CountryCode"         -> user.address.flatMap(_.countryCode)
    ).collect {
        case (a, Some(b)) => (a, b)
      }
      .toMap[String, String]
  }

}
