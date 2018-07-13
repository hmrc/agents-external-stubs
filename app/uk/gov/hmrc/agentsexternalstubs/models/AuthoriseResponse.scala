package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

case class AuthoriseResponse(retrievals: Seq[String] = Seq.empty)

object AuthoriseResponse {
  implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
}

trait Retrieve {
  def key: String
  def fill(response: AuthoriseResponse, session: AuthenticatedSession): Future[Either[String, AuthoriseResponse]]
}

case class UnsupportedRetrieve(key: String) extends Retrieve {
  override def fill(
    response: AuthoriseResponse,
    session: AuthenticatedSession): Future[Either[String, AuthoriseResponse]] =
    Future.successful(Left(s"Retrieval of $key not supported"))
}

object Retrieve {

  val supportedRetrievals: Set[Retrieve] = Set.empty

  def of(key: String): Retrieve =
    supportedRetrievals.find(_.key == key).getOrElse(UnsupportedRetrieve(key))
}
