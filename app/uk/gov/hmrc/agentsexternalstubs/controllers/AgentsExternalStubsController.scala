package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentsexternalstubs.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AgentsExternalStubsController @Inject()(val authConnector: MicroserviceAuthConnector, val env: Environment)(
  implicit val configuration: Configuration)
    extends BaseController with AuthActions {

  def testAuthAgentMtd: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      Future.successful(Ok(toJson(arn)))
    }
  }

  def testAuthClientMtdIt: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { mtdItId =>
      Future.successful(Ok(toJson(mtdItId)))
    }
  }

}
