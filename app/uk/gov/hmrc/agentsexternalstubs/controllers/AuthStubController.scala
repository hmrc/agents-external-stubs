package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionRepository
import uk.gov.hmrc.agentsexternalstubs.services.{RetrievalService, RetrievalServiceProvider}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthStubController @Inject()(
  authSessionRepository: AuthenticatedSessionRepository,
  retrievalServiceProvider: RetrievalServiceProvider)
    extends BaseController {

  def authorise(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authSessionRepository.findByAuthToken(authToken)
          response <- maybeSession match {
                       case Some(authSession) =>
                         request.body.validate[AuthoriseRequest] match {
                           case JsSuccess(authoriseRequest, _) =>
                             for {
                               maybeResponse <- prepareResponse(authoriseRequest, retrievalServiceProvider(authSession))
                             } yield
                               maybeResponse.fold(error => unauthorized(error), response => Ok(Json.toJson(response)))
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

  type MaybeResponse = Future[Either[String, AuthoriseResponse]]

  def prepareResponse(request: AuthoriseRequest, retrievalService: RetrievalService)(
    implicit ex: ExecutionContext): MaybeResponse =
    for {
      status <- checkPredicates(request.authorise)
      response <- status.fold(
                   error => Future.successful(Left(error)),
                   _ => retrieveDetails(request.retrieve, retrievalService))
    } yield response

  def checkPredicates(predicates: Seq[Predicate])(implicit ex: ExecutionContext): Future[Either[String, Unit]] =
    Future.successful(Right(()))

  def retrieveDetails(retrieve: Seq[String], retrievalService: RetrievalService)(
    implicit ex: ExecutionContext): MaybeResponse =
    retrieve.foldLeft[MaybeResponse](Future.successful(Right(AuthoriseResponse())))(
      (fr, retrieve) =>
        fr.flatMap(
          _.fold(
            error => Future.successful(Left(error)),
            response => addDetailToResponse(response, retrieve, retrievalService))))

  def addDetailToResponse(response: AuthoriseResponse, retrieve: String, retrievalService: RetrievalService)(
    implicit ex: ExecutionContext): MaybeResponse = Retrieve.of(retrieve).fill(response, retrievalService)

  def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  def unauthorized(reason: String): Result =
    Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")

}
