package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

import scala.concurrent.Future

trait HttpHelpers {

  case class ErrorResponse(code: String, message: Option[String])

  object ErrorResponse {
    implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]
  }

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Results
      .Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

  def badRequestF(code: String, message: String = null): Future[Result] =
    Future.successful(badRequest(code, message))

  def badRequest(code: String, message: String = null): Result =
    Results.BadRequest(Json.toJson(ErrorResponse(code, Option(message))))

  def notFoundF(code: String, message: String = null): Future[Result] =
    Future.successful(notFound(code, message))

  def notFound(code: String, message: String = null): Result =
    Results.NotFound(Json.toJson(ErrorResponse(code, Option(message))))

  val SessionRecordNotFound = unauthorizedF("SessionRecordNotFound")

}
