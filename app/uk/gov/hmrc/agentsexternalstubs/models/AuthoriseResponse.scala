package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.agentsexternalstubs.services.RetrievalService

import scala.concurrent.{ExecutionContext, Future}

case class AuthoriseResponse(
  credentials: Option[Credentials] = None,
  authProviderId: Option[GGCredId] = None,
  authorisedEnrolments: Seq[Enrolment] = Seq.empty
)

object AuthoriseResponse {
  implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
}

sealed trait Retrieve {
  def key: String
  def fill(response: AuthoriseResponse, retrievalService: RetrievalService, authenticatedSession: AuthenticatedSession)(
    implicit ec: ExecutionContext): Future[Either[String, AuthoriseResponse]]
}

object Retrieve {

  val supportedRetrievals: Set[Retrieve] =
    Set(CredentialsRetrieve, AuthProviderIdRetrieve, AuthorisedEnrolmentsRetrieve)

  def of(key: String): Retrieve =
    supportedRetrievals.find(_.key == key).getOrElse(UnsupportedRetrieve(key))
}

case class UnsupportedRetrieve(key: String) extends Retrieve {
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService,
    authenticatedSession: AuthenticatedSession)(
    implicit ec: ExecutionContext): Future[Either[String, AuthoriseResponse]] =
    Future.successful(Left(s"Retrieval of $key not supported"))
}

case class Credentials(providerId: String, providerType: String)
object Credentials {
  implicit val format: Format[Credentials] = Json.format[Credentials]
}

case object CredentialsRetrieve extends Retrieve {
  val key = "credentials"
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService,
    authenticatedSession: AuthenticatedSession)(
    implicit ec: ExecutionContext): Future[Either[String, AuthoriseResponse]] =
    Future.successful(
      Right(response.copy(credentials = Some(Credentials(authenticatedSession.userId, "GovernmentGateway")))))
}

case class GGCredId(ggCredId: String)
object GGCredId {
  implicit val format: Format[GGCredId] = Json.format[GGCredId]
}

case object AuthProviderIdRetrieve extends Retrieve {
  val key = "authProviderId"
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService,
    authenticatedSession: AuthenticatedSession)(
    implicit ec: ExecutionContext): Future[Either[String, AuthoriseResponse]] =
    Future.successful(Right(response.copy(authProviderId = Some(GGCredId(authenticatedSession.userId)))))
}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None)
object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class Identifier(key: String, value: String)
object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}

case object AuthorisedEnrolmentsRetrieve extends Retrieve {
  val key = "authorisedEnrolments"
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService,
    authenticatedSession: AuthenticatedSession)(
    implicit ec: ExecutionContext): Future[Either[String, AuthoriseResponse]] =
    retrievalService
      .principalEnrolments(authenticatedSession.userId)
      .map(pe => Right(response.copy(authorisedEnrolments = pe)))
}
