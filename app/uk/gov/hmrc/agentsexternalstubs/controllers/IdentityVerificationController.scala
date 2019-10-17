package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class IdentityVerificationController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def storeNino(credId: String): Action[AnyContent] = Action {
    Created
  }

}
