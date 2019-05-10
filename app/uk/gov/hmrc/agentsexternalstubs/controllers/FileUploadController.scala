package uk.gov.hmrc.agentsexternalstubs.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadController @Inject()(val authenticationService: AuthenticationService, cc: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def createEnvelope(): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { _ =>
        Future.successful(Created.withHeaders(HeaderNames.LOCATION -> s"/file-upload/envelopes/${UUID.randomUUID()}"))
      }(SessionRecordNotFound)
    }

  def routeEnvelope(): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { _ =>
        Future.successful(Created)
      }(SessionRecordNotFound)
    }

  def uploadFile(envelopeId: String, fileId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { _ =>
        Future.successful(Ok)
      }(SessionRecordNotFound)
    }
}
