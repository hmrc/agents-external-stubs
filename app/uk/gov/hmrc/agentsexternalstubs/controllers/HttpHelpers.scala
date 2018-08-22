package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Result, Results}

import scala.concurrent.Future

case class ErrorResponse(code: String, message: Option[String])

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]
}

trait HttpHelpers {

  def errorMessage(code: String, message: Option[String]): JsValue =
    Json.toJson(ErrorResponse(code, message))

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Results
      .Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

  def badRequestF(code: String, message: String = null): Future[Result] =
    Future.successful(badRequest(code, message))

  def badRequest(code: String, message: String = null): Result =
    Results.BadRequest(errorMessage(code, Option(message)))

  def notFoundF(code: String, message: String = null): Future[Result] =
    Future.successful(notFound(code, message))

  def notFound(code: String, message: String = null): Result =
    Results.NotFound(errorMessage(code, Option(message)))

  def internalServerErrorF(code: String, message: String = null): Future[Result] =
    Future.successful(internalServerError(code, message))

  def internalServerError(code: String, message: String = null): Result =
    Results.InternalServerError(errorMessage(code, Option(message)))

  val SessionRecordNotFound: Future[Result] = unauthorizedF("SessionRecordNotFound")

}

case class DesErrorResponse(code: String, reason: Option[String])

object DesErrorResponse {
  implicit val writes: Writes[DesErrorResponse] = Json.writes[DesErrorResponse]
}

trait DesHttpHelpers extends HttpHelpers {

  override def errorMessage(code: String, message: Option[String]): JsValue =
    Json.toJson(DesErrorResponse(code, message))

  override def unauthorized(reason: String): Result =
    Results.Unauthorized(errorMessage("UNAUTHORIZED", Option(reason)))
}
