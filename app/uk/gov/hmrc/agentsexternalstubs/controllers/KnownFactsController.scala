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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json, OWrites}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, KnownFact, KnownFacts, User}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class KnownFactsController @Inject() (
  knownFactsRepository: KnownFactsRepository,
  usersRepository: UsersRepository,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import KnownFactsController._

  def getKnownFacts(enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      knownFactsRepository
        .findByEnrolmentKey(enrolmentKey, session.planetId)
        .flatMap {
          case None => notFoundF("NOT_FOUND")
          case Some(kf) =>
            for {
              maybeUser <- usersRepository.findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
              agents    <- usersRepository.findByDelegatedEnrolmentKey(enrolmentKey, session.planetId)(1000)
            } yield ok(EnrolmentInfo(enrolmentKey.tag, kf.verifiers, maybeUser, agents))
        }
    }(SessionRecordNotFound)
  }

  val createKnownFacts: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[KnownFacts](knownFacts =>
        knownFactsRepository
          .upsert(KnownFacts.sanitize(knownFacts.enrolmentKey.tag)(knownFacts), session.planetId)
          .map(_ =>
            Created(s"Known facts ${knownFacts.enrolmentKey.tag} has been created.")
              .withHeaders(
                HeaderNames.LOCATION -> routes.KnownFactsController.getKnownFacts(knownFacts.enrolmentKey).url
              )
          )
      )
    }(SessionRecordNotFound)
  }

  def upsertKnownFacts(enrolmentKey: EnrolmentKey): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[KnownFacts](knownFacts =>
          knownFactsRepository
            .findByEnrolmentKey(enrolmentKey, session.planetId)
            .flatMap {
              case None =>
                knownFactsRepository
                  .upsert(
                    KnownFacts.sanitize(enrolmentKey.tag)(knownFacts.copy(enrolmentKey = enrolmentKey)),
                    session.planetId
                  )
                  .map(_ =>
                    Created(s"Known facts ${knownFacts.enrolmentKey.tag} has been created.")
                      .withHeaders(HeaderNames.LOCATION -> routes.KnownFactsController.getKnownFacts(enrolmentKey).url)
                  )
              case Some(_) =>
                knownFactsRepository
                  .upsert(
                    KnownFacts.sanitize(enrolmentKey.tag)(knownFacts.copy(enrolmentKey = enrolmentKey)),
                    session.planetId
                  )
                  .map(_ =>
                    Accepted(s"Known facts ${knownFacts.enrolmentKey.tag} has been updated.")
                      .withHeaders(HeaderNames.LOCATION -> routes.KnownFactsController.getKnownFacts(enrolmentKey).url)
                  )
            }
        )
      }(SessionRecordNotFound)
  }

  def upsertKnownFactVerifier(enrolmentKey: EnrolmentKey): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[KnownFact](knownFact =>
          knownFactsRepository
            .findByEnrolmentKey(enrolmentKey, session.planetId)
            .flatMap {
              case None =>
                notFoundF("KNOWN_FACTS_NOT_FOUND", s"Could not found known facts $enrolmentKey")
              case Some(knownFacts) =>
                knownFactsRepository
                  .upsert(
                    KnownFacts.sanitize(enrolmentKey.tag)(
                      knownFacts.copy(
                        enrolmentKey = enrolmentKey,
                        verifiers = KnownFactsController.addVerifier(knownFacts.verifiers, knownFact)
                      )
                    ),
                    session.planetId
                  )
                  .map(_ =>
                    Accepted(s"Known facts ${knownFacts.enrolmentKey.tag} verifiers has been updated.")
                      .withHeaders(HeaderNames.LOCATION -> routes.KnownFactsController.getKnownFacts(enrolmentKey).url)
                  )
            }
        )
      }(SessionRecordNotFound)
  }

  def deleteKnownFacts(enrolmentKey: EnrolmentKey): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      knownFactsRepository
        .findByEnrolmentKey(enrolmentKey, session.planetId)
        .flatMap {
          case Some(_) => knownFactsRepository.delete(enrolmentKey, session.planetId).map(_ => NoContent)
          case None    => notFoundF("KNOWN_FACTS_NOT_FOUND", s"Could not found known facts $enrolmentKey")
        }
    }(SessionRecordNotFound)
  }

}

object KnownFactsController {

  case class EnrolmentInfo(enrolmentKey: String, verifiers: Seq[KnownFact], user: Option[User], agents: Seq[User])

  object EnrolmentInfo {
    implicit val writes: OWrites[EnrolmentInfo] = Json.writes[EnrolmentInfo]
  }

  def addVerifier(verifiers: Seq[KnownFact], knownFact: KnownFact): Seq[KnownFact] =
    verifiers.filterNot(_.key == knownFact.key) :+ knownFact
}
