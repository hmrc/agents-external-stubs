package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class ErrorResponse(code: String, reason: Option[String])

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]
}

trait HttpHelpers {

  def success[T](value: T): Future[T] = Future.successful(value)

  def okF[T: Writes](entity: T, links: Link*): Future[Result] =
    success(ok(entity, links: _*))

  def ok[T: Writes](entity: T, links: Link*): Result =
    Results.Ok(RestfulResponse(entity, links: _*))

  def okF[T: Writes](entity: T, fix: JsValue => JsValue): Future[Result] =
    success(ok(entity, fix))

  def ok[T: Writes](entity: T, fix: JsValue => JsValue): Result =
    Results.Ok(fix(Json.toJson(entity)))

  def errorMessage(code: String, reason: Option[String]): JsValue =
    Json.toJson(ErrorResponse(code, reason))

  def unauthorizedF(reason: String): Future[Result] =
    success(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Results
      .Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

  def badRequestF(code: String, reason: String = null): Future[Result] =
    success(badRequest(code, reason))

  def badRequest(code: String, reason: String = null): Result =
    Results.BadRequest(errorMessage(code, Option(reason)))

  def forbiddenF(code: String, reason: String = null): Future[Result] =
    success(forbidden(code, reason))

  def forbidden(code: String, reason: String = null): Result =
    Results.Forbidden(errorMessage(code, Option(reason)))

  def notFoundF(code: String, reason: String = null): Future[Result] =
    success(notFound(code, reason))

  def notFound(code: String, reason: String = null): Result =
    Results.NotFound(errorMessage(code, Option(reason)))

  def conflictF(code: String, reason: String = null): Future[Result] =
    success(conflict(code, reason))

  def conflict(code: String, reason: String = null): Result =
    Results.Conflict(errorMessage(code, Option(reason)))

  def internalServerErrorF(code: String, reason: String = null): Future[Result] =
    success(internalServerError(code, reason))

  def internalServerError(code: String, reason: String = null): Result =
    Results.InternalServerError(errorMessage(code, Option(reason)))

  val SessionRecordNotFound: Future[Result] = unauthorizedF("SessionRecordNotFound")

  def withPayloadOrDefault[T](default: T)(f: T => Future[Result])(
    implicit request: Request[AnyContent],
    reads: Reads[T],
    ec: ExecutionContext): Future[Result] =
    request.body.asJson.map(j => validate(j)(f)).getOrElse(f(default))

  def withPayload[T](
    f: T => Future[Result])(implicit request: Request[JsValue], reads: Reads[T], ec: ExecutionContext): Future[Result] =
    validate(request.body)(f)

  def validate[T](body: JsValue)(
    f: T => Future[Result])(implicit reads: Reads[T], ec: ExecutionContext): Future[Result] =
    Try(body.validate[T]) match {
      case Success(validationResult) =>
        whenSuccess(f)(validationResult)
      case Failure(e) =>
        Future.failed(new BadRequestException(s"Could not parse body due to ${e.getMessage}"))
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

  override def errorMessage(code: String, reason: Option[String]): JsValue =
    Json.toJson(DesErrorResponse(code, reason))

  override def unauthorized(reason: String): Result =
    Results.Unauthorized(errorMessage("UNAUTHORIZED", Option(reason)))
}
