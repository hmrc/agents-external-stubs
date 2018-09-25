package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future

@Singleton
class DataStreamStubController @Inject()() extends BaseController {

  val writeEvent: Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.foreach(println)
    Future.successful(NoContent)
  }

}
