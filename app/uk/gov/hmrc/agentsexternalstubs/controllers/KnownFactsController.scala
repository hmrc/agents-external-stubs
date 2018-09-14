package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, KnownFact, User}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class KnownFactsController @Inject()(
  knownFactsRepository: KnownFactsRepository,
  usersRepository: UsersRepository,
  val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

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
            } yield Ok(RestfulResponse(EnrolmentInfo(enrolmentKey.tag, kf.verifiers, maybeUser, agents)))
        }
    }(SessionRecordNotFound)
  }

}

object KnownFactsController {

  case class EnrolmentInfo(enrolmentKey: String, verifiers: Seq[KnownFact], user: Option[User], agents: Seq[User])

  object EnrolmentInfo {
    implicit val writes: OWrites[EnrolmentInfo] = Json.writes[EnrolmentInfo]
  }
}
