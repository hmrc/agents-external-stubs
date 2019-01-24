package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.connectors.ApiPlatformTestUserConnector
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.models.{Planet, User}
import uk.gov.hmrc.domain.{Nino, SaUtr, Vrn}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalUserService @Inject()(
  apiPlatformTestUserConnector: ApiPlatformTestUserConnector,
  @Named("api-platform-test-user.sync-users-all-planets") syncUsersAllPlanets: Boolean) {

  def maybeSyncExternalUserIdentifiedBy[S](
    userIdentifier: S,
    planetId: String,
    createUser: (User, String) => Future[User])(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier): Future[Option[User]] =
    (userIdentifier match {
      case nino: Nino =>
        apiPlatformTestUserConnector.getIndividualUserByNino(nino.value)
      case utr: Utr =>
        apiPlatformTestUserConnector.getIndividualUserBySaUtr(utr.value)
      case utr: SaUtr =>
        apiPlatformTestUserConnector.getIndividualUserBySaUtr(utr.value)
      case vrn: Vrn =>
        apiPlatformTestUserConnector
          .getOrganisationUserByVrn(vrn.value)
          .flatMap {
            case None => apiPlatformTestUserConnector.getIndividualUserByVrn(vrn.value)
            case r    => Future.successful(r)
          }
      case _ =>
        Future.failed(
          new IllegalArgumentException(s"Unknown identifier $userIdentifier, expected one of: nino, utr, vrn"))
    }).flatMap(apiUserOpt => {
        apiUserOpt.map(TestUser.asUser).map(user => createUser(user, planetId)) match {
          case Some(f) => f.map(Some.apply)
          case None    => Future.successful(None)
        }
      })
      .recover {
        case NonFatal(e) =>
          Logger(getClass).error(s"External user sync failed with ${e.getMessage}")
          None
      }

  def tryLookupExternalUserIfMissing[S, T](
    userIdentifier: S,
    planetId: String,
    createUser: (User, String) => Future[User])(maybeResult: S => Future[Option[T]])(
    implicit ec: ExecutionContext): Future[Option[T]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    maybeResult(userIdentifier).flatMap {
      case None if syncUsersAllPlanets || planetId == Planet.DEFAULT =>
        maybeSyncExternalUserIdentifiedBy(userIdentifier, planetId, createUser)
          .flatMap(_.map(_ => maybeResult(userIdentifier)) match {
            case Some(f) => f
            case None    => Future.successful(None)
          })

      case result => Future.successful(result)
    }
  }

}
