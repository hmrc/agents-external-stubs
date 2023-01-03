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

package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.connectors.ApiPlatformTestUserConnector
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Planet, User}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.{Nino, SaUtr, Vrn}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalUserService @Inject() (apiPlatformTestUserConnector: ApiPlatformTestUserConnector, appConfig: AppConfig) {

  def maybeSyncExternalUserIdentifiedBy[S](
    userIdentifier: S,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[User]] =
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
          new IllegalArgumentException(s"Unknown identifier $userIdentifier, expected one of: nino, utr, vrn")
        )
    }).flatMap { apiUserOpt =>
      apiUserOpt.map(testUser =>
        createUser(
          TestUser.asUserAndGroup(testUser)._1,
          planetId,
          Some(testUser.affinityGroup)
        )
      /* TODO (maybe): We don't create the group explicitly as the logic in UsersService should take care of creating a group for us.
        By doing this we may be losing some detail (e.g. if an agent friendly name was given). Should we create the group explicitly? */
      ) match {
        case Some(f) =>
          f.map { user =>
            Logger(getClass).info(
              s"External user id=name=${user.userId} ${user.name.getOrElse("")} definition successfully imported."
            )
            Option(user)
          }
        case None =>
          Logger(getClass).warn(s"External user definition not found for $userIdentifier.")
          Future.successful(None)
      }
    }.recover { case NonFatal(e) =>
      Logger(getClass).error(s"External user sync failed with ${e.getMessage}")
      None
    }

  def tryLookupExternalUserIfMissingForIdentifier[S, T](
    userIdentifier: S,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(maybeResult: S => Future[Option[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    maybeResult(userIdentifier).flatMap {
      case None if appConfig.syncUsersAllPlanets || planetId == Planet.DEFAULT =>
        maybeSyncExternalUserIdentifiedBy(userIdentifier, planetId, createUser)
          .flatMap(_.map(_ => maybeResult(userIdentifier)) match {
            case Some(f) => f
            case None    => Future.successful(None)
          })

      case result => Future.successful(result)
    }
  }

  def tryLookupExternalUserIfMissingForEnrolmentKey[T](
    enrolmentKey: EnrolmentKey,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(maybeResult: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    maybeResult.flatMap {
      case None if appConfig.syncUsersAllPlanets || planetId == Planet.DEFAULT =>
        identifierFor(enrolmentKey) match {
          case None => Future.successful(None)
          case Some(userIdentifier) =>
            maybeSyncExternalUserIdentifiedBy(userIdentifier, planetId, createUser)
              .flatMap(_.map(_ => maybeResult) match {
                case Some(f) => f
                case None    => Future.successful(None)
              })
        }
      case result => Future.successful(result)
    }
  }

  private def identifierFor(enrolmentKey: EnrolmentKey): Option[AnyRef] = enrolmentKey.service match {
    case "HMRC-NI"      => enrolmentKey.identifiers.headOption.map(i => Nino(i.value))
    case "HMRC-MTD-VAT" => enrolmentKey.identifiers.headOption.map(i => Vrn(i.value))
    case _              => None
  }

}
