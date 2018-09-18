package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository
import uk.gov.hmrc.agentsexternalstubs.services.UsersService
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthStubController @Inject()(authSessionRepository: AuthenticatedSessionsRepository, usersService: UsersService)
    extends BaseController {

  def authorise(): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authSessionRepository.findByAuthToken(authToken)
          response <- maybeSession match {
                       case Some(authenticatedSession) =>
                         request.body.validate[AuthoriseRequest] match {
                           case JsSuccess(authoriseRequest, _) =>
                             for {
                               maybeUser <- usersService
                                             .findByUserId(authenticatedSession.userId, authenticatedSession.planetId)
                               result <- Future(maybeUser match {
                                          case Some(user) =>
                                            prepareAuthoriseResponse(
                                              FullAuthoriseContext(user, authenticatedSession, authoriseRequest))
                                          case None =>
                                            Left("SessionRecordNotFound")
                                        }) map (_.fold(
                                          error => unauthorized(error),
                                          response => Ok(Json.toJson(response))))
                             } yield result
                           case JsError(errors) =>
                             Future.successful(
                               BadRequest(errors
                                 .map { case (p, ve) => s"$p -> [${ve.map(v => v.message).mkString(",")}]" }
                                 .mkString("\n")))
                         }
                       case None => unauthorizedF("SessionRecordNotFound")
                     }
        } yield response
      case Some(_) => unauthorizedF("InvalidBearerToken")
      case None    => unauthorizedF("MissingBearerToken")
    }
  }

  def prepareAuthoriseResponse(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
    checkPredicates(context).fold(error => Left(error), _ => retrieveDetails(context))

  def checkPredicates(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Either[String, Unit] =
    context.request.authorise.foldLeft[Either[String, Unit]](Right(()))(
      (result, p: Predicate) => result.fold(error => Left(error), _ => p.validate(context))
    )

  def retrieveDetails(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
    context.request.retrieve.foldLeft[Retrieve.MaybeResponse](Right(AuthoriseResponse()))((result, r: String) =>
      result.fold(error => Left(error), response => addDetailToResponse(response, r, context)))

  def addDetailToResponse(response: AuthoriseResponse, retrieve: String, context: AuthoriseContext)(
    implicit ex: ExecutionContext): Retrieve.MaybeResponse =
    Retrieve.of(retrieve).fill(response, context)

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

}
