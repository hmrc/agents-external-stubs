package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json._
import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class ErrorResponse(code: String, message: Option[String])

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]
}

trait HttpHelpers {

  def success[T](value: T): Future[T] = Future.successful(value)

  def okF[T: Writes](entity: T, links: Link*): Future[Result] =
    success(ok(entity, links: _*))

  def ok[T: Writes](entity: T, links: Link*): Result =
    Results.Ok(RestfulResponse(entity, links: _*))

  def errorMessage(code: String, message: Option[String]): JsValue =
    Json.toJson(ErrorResponse(code, message))

  def unauthorizedF(reason: String): Future[Result] =
    success(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Results
      .Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

  def badRequestF(code: String, message: String = null): Future[Result] =
    success(badRequest(code, message))

  def badRequest(code: String, message: String = null): Result =
    Results.BadRequest(errorMessage(code, Option(message)))

  def forbiddenF(code: String, message: String = null): Future[Result] =
    success(forbidden(code, message))

  def forbidden(code: String, message: String = null): Result =
    Results.Forbidden(errorMessage(code, Option(message)))

  def notFoundF(code: String, message: String = null): Future[Result] =
    success(notFound(code, message))

  def notFound(code: String, message: String = null): Result =
    Results.NotFound(errorMessage(code, Option(message)))

  def internalServerErrorF(code: String, message: String = null): Future[Result] =
    success(internalServerError(code, message))

  def internalServerError(code: String, message: String = null): Result =
    Results.InternalServerError(errorMessage(code, Option(message)))

  val SessionRecordNotFound: Future[Result] = unauthorizedF("SessionRecordNotFound")

  def withPayload[T](
    f: T => Future[Result])(implicit request: Request[JsValue], reads: Reads[T], ec: ExecutionContext): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(validationResult) => whenSuccess(f)(validationResult)
      case Failure(e)                => Future.failed(new BadRequestException(s"Could not parse body due to ${e.getMessage}"))
    }

  def whenSuccess[T](f: T => Future[Result])(jsResult: JsResult[T]): Future[Result] = jsResult match {
    case JsSuccess(payload, _) => f(payload)
    case JsError(errs) =>
      Future.failed(new BadRequestException(s"Invalid payload: Parser failed ${errs
        .map {
          case (path, errors) =>
            s"at path $path with ${errors.map(e => e.messages.mkString(", ")).mkString(", ")}"
        }
        .mkString(", and ")}"))
  }

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
