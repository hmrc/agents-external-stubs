package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataStreamStubController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  val writeEvent: Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson
      .map(Json.prettyPrint)
      .foreach(Logger(getClass).info(_))
    Future.successful(NoContent)
  }

}
