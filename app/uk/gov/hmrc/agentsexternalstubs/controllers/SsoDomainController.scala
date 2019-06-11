package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import play.api.mvc.Action
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import scala.concurrent.Future.successful

@Singleton
class SsoDomainController @Inject()(cc: MessagesControllerComponents)(implicit val configuration: Configuration)
    extends FrontendController(cc) {

  def validate(domain: String): Action[AnyContent] = Action {
    if (domain != "www.google.com") NoContent else BadRequest
  }

  def digitalFlag(flag: String): Action[AnyContent] = Action {
    Ok
  }

  def getDomains: Action[AnyContent] = Action.async { implicit request =>
    successful(Ok(domainsJson))
  }

  def domainsJson =
    s"""
       {
       |   "internalDomains" : [
       |      "localhost"
       |   ],
       |   "externalDomains" : [
       |      "127.0.0.1",
       |      "online-qa.ibt.hmrc.gov.uk",
       |      "ibt.hmrc.gov.uk"
       |   ]
       |}
       |
   """.stripMargin
}
