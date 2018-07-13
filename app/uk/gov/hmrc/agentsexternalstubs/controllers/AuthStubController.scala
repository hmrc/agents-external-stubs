package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AuthStubController @Inject()(authSessionRepository: AuthenticatedSessionRepository) extends BaseController {

  def authorise(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authSessionRepository.findByAuthToken(authToken)
          response <- maybeSession match {
                       case Some(authSession) =>
                         withJsonBody[AuthoriseRequest] { authoriseRequest =>
                           for {
                             maybeResponse <- prepareResponse(authoriseRequest, authSession)
                           } yield
                             maybeResponse.fold(error => unauthorized(error), response => Ok(Json.toJson(response)))
                         } recover {
                           case NonFatal(e) => unauthorized(e.getMessage)
                         }
                       case None => unauthorizedF("SessionRecordNotFound")
                     }
        } yield response
      case Some(_) => unauthorizedF("InvalidBearerToken")
      case None    => unauthorizedF("MissingBearerToken")
    }
  }

  type MaybeResponse = Future[Either[String, AuthoriseResponse]]

  def prepareResponse(request: AuthoriseRequest, session: AuthenticatedSession)(
    implicit ex: ExecutionContext): MaybeResponse =
    for {
      status   <- checkPredicates(request.authorise)
      response <- status.fold(error => Future.successful(Left(error)), _ => retrieveDetails(request.retrieve, session))
    } yield response

  def checkPredicates(predicates: Seq[Predicate])(implicit ex: ExecutionContext): Future[Either[String, Unit]] =
    Future.successful(Right(()))

  def retrieveDetails(retrieve: Seq[String], session: AuthenticatedSession)(
    implicit ex: ExecutionContext): MaybeResponse =
    retrieve.foldLeft[MaybeResponse](Future.successful(Right(AuthoriseResponse())))((fr, retrieve) =>
      fr.flatMap(
        _.fold(error => Future.successful(Left(error)), response => addDetailToResponse(response, retrieve, session))))

  def addDetailToResponse(response: AuthoriseResponse, retrieve: String, session: AuthenticatedSession)(
    implicit ex: ExecutionContext): MaybeResponse = Retrieve.of(retrieve).fill(response, session)

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

}
