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

package uk.gov.hmrc.agentsexternalstubs.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

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
