package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataStreamStubController @Inject()(ecp: ExecutionContextProvider) extends BaseController {

  implicit val ec: ExecutionContext = ecp.get()

  val writeEvent: Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.foreach(println)
    Future.successful(NoContent)
  }

}
