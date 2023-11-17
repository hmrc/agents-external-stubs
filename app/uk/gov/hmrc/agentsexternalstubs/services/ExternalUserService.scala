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
import play.api.{Logger, Logging}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.connectors.ApiPlatformTestUserConnector
import uk.gov.hmrc.agentsexternalstubs.models.ApiPlatform.TestUser
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Planet, User}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.{Nino, SaUtr, TaxIdentifier, Vrn}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalUserService @Inject() (apiPlatformTestUserConnector: ApiPlatformTestUserConnector, appConfig: AppConfig)
    extends Logging {

  private def maybeSyncExternalUser(
    userIdentifier: TaxIdentifier,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    def userFromApiPlatform(id: TaxIdentifier): Future[Option[TestUser]] = id match {
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
            case r => Future.successful(r)
          }
      case _ =>
        Future.failed(
          new IllegalArgumentException(s"Unknown identifier $userIdentifier, expected one of: nino, utr, vrn")
        )
    }

    if (appConfig.syncUsersAllPlanets || planetId == Planet.DEFAULT) {
      userFromApiPlatform(userIdentifier)
        .flatMap {
          case None =>
            Logger(getClass).warn(s"External user definition not found for $userIdentifier.")
            Future.successful(None)
          case Some(testUser) =>
            /* TODO (maybe): We don't create the group explicitly as the logic in UsersService should take care of creating a group for us.
        By doing this we may be losing some detail (e.g. if an agent friendly name was given). Should we create the group explicitly? */
            createUser(TestUser.asUserAndGroup(testUser)._1, planetId, Some(testUser.affinityGroup)).map { user =>
              logger.info(
                s"External user id=name=${user.userId} ${user.name.getOrElse("")} definition successfully imported."
              )
              Some(user)
            }
        }
        .recover { case NonFatal(e) =>
          Logger(getClass).error(s"External user sync failed with ${e.getMessage}")
          None
        }
    } else Future.successful(None)
  }

  def lookupExternalUser(
    userIdentifier: TaxIdentifier,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    if (appConfig.syncUsersAllPlanets || planetId == Planet.DEFAULT)
      maybeSyncExternalUser(userIdentifier, planetId, createUser)
    else Future.successful(None)
  }

  def lookupExternalUserByEnrolmentKey(
    enrolmentKey: EnrolmentKey,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User] // (user, planetId, affinityGroup)
  )(implicit ec: ExecutionContext): Future[Option[User]] =
    identifierFor(enrolmentKey) match {
      case None                 => Future.successful(None)
      case Some(userIdentifier) => lookupExternalUser(userIdentifier, planetId, createUser)
    }

  // try the action given in 'block'. If it returns None, do an external user sync for the given id and try again (once)
  def syncAndRetry[A](
    userIdentifier: TaxIdentifier,
    planetId: String,
    createUser: (User, String, Option[String]) => Future[User]
  )(block: () => Future[Option[A]])(implicit ec: ExecutionContext): Future[Option[A]] =
    block().flatMap {
      case Some(a) => Future.successful(Some(a))
      case None =>
        for {
          _      <- lookupExternalUser(userIdentifier, planetId, createUser)
          result <- block()
        } yield result
    }

  private def identifierFor(enrolmentKey: EnrolmentKey): Option[TaxIdentifier] = enrolmentKey.service match {
    case "HMRC-NI"      => enrolmentKey.identifiers.headOption.map(i => Nino(i.value))
    case "HMRC-MTD-VAT" => enrolmentKey.identifiers.headOption.map(i => Vrn(i.value))
    case _              => None
  }

}
