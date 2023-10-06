/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.models.NinoClStoreEntry
import uk.gov.hmrc.agentsexternalstubs.models.admin.User
import uk.gov.hmrc.agentsexternalstubs.repository.DuplicateUserException
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationController @Inject() (
  cc: ControllerComponents,
  usersService: UsersService,
  val authenticationService: AuthenticationService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  private def addNinoToUser(nino: Nino)(user: User): User = user.copy(nino = Some(nino))

  def storeNino(credId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[NinoClStoreEntry] { entry =>
      if (entry.credId != credId)
        Future.successful(BadRequest)
      else {
        withCurrentSession { session =>
          usersService
            .updateUser(session.userId, session.planetId, addNinoToUser(entry.nino))
            .map(theUser => Created(s"Current user ${theUser.userId} has been updated"))
            .recover {
              case DuplicateUserException(msg, _) => Conflict(msg)
              case e: NotFoundException           => notFound("USER_NOT_FOUND", e.getMessage)
            }
        }(SessionRecordNotFound)
      }
    }
  }

}
