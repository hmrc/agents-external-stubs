package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext

case class AuthoriseResponse(
  credentials: Option[Credentials] = None,
  authProviderId: Option[GGCredId] = None,
  authorisedEnrolments: Seq[Enrolment] = Seq.empty,
  allEnrolments: Seq[Enrolment] = Seq.empty,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  groupIdentifier: Option[String] = None,
  name: Option[Name] = None,
  dateOfBirth: Option[LocalDate] = None
)

object AuthoriseResponse {
  implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
}

sealed trait Retrieve {
  type MaybeResponse = Retrieve.MaybeResponse

  def key: String
  def fill(response: AuthoriseResponse, context: AuthoriseContext)(implicit ec: ExecutionContext): MaybeResponse
}

object Retrieve {
  type MaybeResponse = Either[String, AuthoriseResponse]

  val supportedRetrievals: Set[Retrieve] =
    Set(
      CredentialsRetrieve,
      AuthProviderIdRetrieve,
      AuthorisedEnrolmentsRetrieve,
      AllEnrolmentsRetrieve,
      AffinityGroupRetrieve,
      ConfidenceLevelRetrieve,
      CredentialStrengthRetrieve,
      NinoRetrieve,
      CredentialRoleRetrieve,
      GroupIdentifierRetrieve,
      NameRetrieve,
      DateOfBirthRetrieve
    )

  def of(key: String): Retrieve =
    supportedRetrievals.find(_.key == key).getOrElse(UnsupportedRetrieve(key))
}

case class UnsupportedRetrieve(key: String) extends Retrieve {
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Left(s"Retrieval of $key not supported")
}

case class Credentials(providerId: String, providerType: String)
object Credentials {
  implicit val format: Format[Credentials] = Json.format[Credentials]
}

case object CredentialsRetrieve extends Retrieve {
  val key = "credentials"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(credentials = Some(Credentials(context.userId, "GovernmentGateway"))))
}

case class GGCredId(ggCredId: String)
object GGCredId {
  implicit val format: Format[GGCredId] = Json.format[GGCredId]
}

case object AuthProviderIdRetrieve extends Retrieve {
  val key = "authProviderId"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(authProviderId = Some(GGCredId(context.userId))))
}

case object AuthorisedEnrolmentsRetrieve extends Retrieve {
  val key = "authorisedEnrolments"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(authorisedEnrolments = context.principalEnrolments.filter(p =>
      context.authorisedServices.contains(p.key))))
}

case object AllEnrolmentsRetrieve extends Retrieve {
  val key = "allEnrolments"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(allEnrolments = context.principalEnrolments))
}

case object AffinityGroupRetrieve extends Retrieve {
  val key = "affinityGroup"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(affinityGroup = context.affinityGroup))
}

case object ConfidenceLevelRetrieve extends Retrieve {
  val key = "confidenceLevel"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(confidenceLevel = context.confidenceLevel))
}

case object CredentialStrengthRetrieve extends Retrieve {
  val key = "credentialStrength"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(credentialStrength = context.credentialStrength))
}

case object NinoRetrieve extends Retrieve {
  val key = "nino"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(nino = context.nino))
}

case object CredentialRoleRetrieve extends Retrieve {
  val key = "credentialRole"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(credentialRole = context.credentialRole))
}

case object GroupIdentifierRetrieve extends Retrieve {
  val key = "groupIdentifier"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(groupIdentifier = context.groupId))
}

case class Name(name: Option[String], lastName: Option[String])
object Name {
  implicit val format: Format[Name] = Json.format[Name]

  def from(name: Option[String]): Name =
    name
      .map(n => {
        val p = n.split(" ")
        if (p.isEmpty) Name(None, None)
        else if (p.length == 1) Name(Some(n), None)
        else {
          Name(Some(p.init.mkString(" ")), Some(p.last))
        }
      })
      .getOrElse(Name(None, None))
}

case object NameRetrieve extends Retrieve {
  val key = "name"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(name = Some(Name.from(context.name))))
}

case object DateOfBirthRetrieve extends Retrieve {
  val key = "dateOfBirth"
  override def fill(response: AuthoriseResponse, context: AuthoriseContext)(
    implicit ec: ExecutionContext): MaybeResponse =
    Right(response.copy(dateOfBirth = context.dateOfBirth))
}
