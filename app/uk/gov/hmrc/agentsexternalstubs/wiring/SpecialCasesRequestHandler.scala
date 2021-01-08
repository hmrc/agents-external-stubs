/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  specialCasesController: SpecialCasesController,
  appConfig: AppConfig)
    extends uk.gov.hmrc.play.bootstrap.http.RequestHandler(router, errorHandler, configuration, filters) {

  val context = "/agents-external-stubs"
  val health = "/ping"

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) =
    if (appConfig.specialCasesDisabled || request.path.startsWith(context) || request.path.startsWith(health)) {
      super.handlerForRequest(request)
    } else {
      val (requestHeader, handler) = super.handlerForRequest(request)
      (requestHeader, handler match {
        case action: EssentialAction => specialCasesController.maybeSpecialCase(action)
        case _                       => handler
      })
    }

}
