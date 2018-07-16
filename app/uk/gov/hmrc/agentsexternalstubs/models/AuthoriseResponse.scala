package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.agentsexternalstubs.services.RetrievalService

import scala.concurrent.Future

case class AuthoriseResponse(
  credentials: Option[Credentials] = None,
  authProviderId: Option[GGCredId] = None
)

object AuthoriseResponse {
  implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
}

sealed trait Retrieve {
  def key: String
  def fill(response: AuthoriseResponse, retrievalService: RetrievalService): Future[Either[String, AuthoriseResponse]]
}

object Retrieve {

  val supportedRetrievals: Set[Retrieve] = Set(CredentialsRetrieve, AuthProviderIdRetrieve)

  def of(key: String): Retrieve =
    supportedRetrievals.find(_.key == key).getOrElse(UnsupportedRetrieve(key))
}

case class UnsupportedRetrieve(key: String) extends Retrieve {
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService): Future[Either[String, AuthoriseResponse]] =
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
    retrievalService: RetrievalService): Future[Either[String, AuthoriseResponse]] =
    Future.successful(
      Right(response.copy(credentials = Some(Credentials(retrievalService.userId, "GovernmentGateway")))))
}

case class GGCredId(ggCredId: String)
object GGCredId {
  implicit val format: Format[GGCredId] = Json.format[GGCredId]
}

case object AuthProviderIdRetrieve extends Retrieve {
  val key = "authProviderId"
  override def fill(
    response: AuthoriseResponse,
    retrievalService: RetrievalService): Future[Either[String, AuthoriseResponse]] =
    Future.successful(Right(response.copy(authProviderId = Some(GGCredId(retrievalService.userId)))))
}
