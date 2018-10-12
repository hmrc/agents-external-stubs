package uk.gov.hmrc.agentsexternalstubs.wiring

import javax.inject.Inject
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc._
import play.api.routing.Router
import uk.gov.hmrc.agentsexternalstubs.controllers.SpecialCasesController

class SpecialCasesRequestHandler @Inject()(
  router: Router,
  errorHandler: HttpErrorHandler,
  configuration: HttpConfiguration,
  filters: HttpFilters,
  specialCasesController: SpecialCasesController)
    extends uk.gov.hmrc.play.bootstrap.http.RequestHandler(router, errorHandler, configuration, filters) {

  val context = "/agents-external-stubs"
  val health = "/ping"

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) =
    if (request.path.startsWith(context) || request.path.startsWith(health)) {
      super.handlerForRequest(request)
    } else {
      val (requestHeader, handler) = super.handlerForRequest(request)
      (requestHeader, handler match {
        case action: EssentialAction => specialCasesController.maybeSpecialCase(action)
        case _                       => handler
      })
    }

}
