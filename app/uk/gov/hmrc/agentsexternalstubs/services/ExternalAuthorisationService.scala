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

import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.controllers.BearerToken
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalAuthorisationService @Inject() (
  usersService: UsersService,
  groupsService: GroupsService,
  val authConnector: AuthConnector,
  appConfig: AppConfig
) extends AuthorisedFunctions {

  private val retrievals = Retrievals.credentials and Retrievals.credentialRole and Retrievals.credentialStrength and
    Retrievals.nino and Retrievals.groupIdentifier and Retrievals.dateOfBirth and Retrievals.name and
    Retrievals.allEnrolments and Retrievals.confidenceLevel and Retrievals.agentInformation and Retrievals.affinityGroup

  final def maybeExternalSession(
    _planetId: String,
    createNewAuthentication: AuthenticateRequest => Future[Option[AuthenticatedSession]]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[AuthenticatedSession]] =
    if (appConfig.isProxyMode) {
      Future.successful(None)
    } else {
      authorised()
        .retrieve(retrievals) {
          case credentials ~ credentialRole ~ credentialStrength ~ nino ~ groupIdentifier ~ dateOfBirth ~
              name ~ allEnrolments ~ confidenceLevel ~ agentInformation ~ affinityGroup =>
            val creds = credentials.getOrElse(throw new Exception("Missing credentials"))
            val (userId, planetId) = User.parseUserIdAtPlanetId(creds.providerId, _planetId)
            val user = User(
              userId = userId,
              groupId = groupIdentifier,
              confidenceLevel = Some(confidenceLevel.level),
              credentialStrength = credentialStrength,
              credentialRole = credentialRole.map(_.toString),
              nino = nino.map(Nino(_)),
              name = name.map(n => Name(n.name, n.lastName).toString),
              dateOfBirth = dateOfBirth,
              assignedPrincipalEnrolments = allEnrolments.enrolments.toSeq
                .filterNot(_.key == "HMRC-NI")
                .map(enrolment =>
                  Enrolment(
                    enrolment.key,
                    Some(enrolment.identifiers.map(id => Identifier(id.key, id.value))),
                    enrolment.state
                  )
                )
                .flatMap(_.toEnrolmentKey)
            )
            val maybeGroup = affinityGroup.map(ag =>
              Group(
                groupId = groupIdentifier.getOrElse(""),
                planetId = planetId,
                affinityGroup = ag.toString,
                principalEnrolments = allEnrolments.enrolments.toSeq
                  .filterNot(_.key == "HMRC-NI")
                  .map(enrolment =>
                    Enrolment(
                      enrolment.key,
                      Some(enrolment.identifiers.map(id => Identifier(id.key, id.value))),
                      enrolment.state
                    )
                  ),
                agentCode = agentInformation.agentCode,
                agentFriendlyName = agentInformation.agentFriendlyName,
                agentId = agentInformation.agentId
              )
            )
            for {
              maybeSession <- createNewAuthentication(
                                AuthenticateRequest(
                                  sessionId = hc.sessionId.map(_.value).getOrElse(UUID.randomUUID().toString),
                                  userId = userId,
                                  password = "p@ssw0rd",
                                  providerType = creds.providerType,
                                  planetId = planetId,
                                  authTokenOpt = hc.authorization.map(a =>
                                    BearerToken
                                      .unapply(a.value)
                                      .getOrElse(
                                        throw new IllegalStateException(
                                          s"Unsupported authorization token format ${a.value}"
                                        )
                                      )
                                  )
                                )
                              )
              _ <- maybeSession match {
                     case Some(session) =>
                       usersService.findByUserId(userId, planetId).flatMap {
                         case Some(existingUser) =>
                           (for {
                             _ <- (existingUser.groupId, maybeGroup) match {
                                    case (Some(groupId), Some(group)) =>
                                      groupsService
                                        .updateGroup(groupId, session.planetId, existing => merge(existing, group))
                                    case _ => Future.successful(())
                                  }
                             _ <- usersService
                                    .updateUser(session.userId, session.planetId, existing => merge(existing, user))
                           } yield ())
                             .andThen { case _ =>
                               Logger(getClass).info(
                                 s"Creating user '$userId' updated on the planet '$planetId' based on external" +
                                   s" authorisation and headers ${report(hc)}"
                               )
                             }
                         case None =>
                           (for {
                             fixed <- usersService.checkAndFixUser(
                                        user,
                                        planetId,
                                        affinityGroup.get.toString
                                      ) // TODO Use of Option.get is unsafe. Is affinityGroup guaranteed to be defined?
                             maybeNewGroup <- maybeGroup.fold(Future.successful(Option.empty[Group]))(group =>
                                                groupsService.createGroup(group, session.planetId).map(Some(_))
                                              )
                             user <- usersService.createUser(
                                       fixed.copy(userId = session.userId, groupId = maybeNewGroup.map(_.groupId)),
                                       session.planetId,
                                       affinityGroup.map(_.toString)
                                     )
                             _ =
                               Logger(getClass).info(
                                 s"Creating user '$userId' on the planet '$planetId' based on external authorisation" +
                                   s" and headers ${report(hc)}"
                               )
                           } yield user)
                             .recover { case NonFatal(e) =>
                               Logger(getClass).warn(
                                 s"Creating user '$userId' on the planet '$planetId' failed with [$e] for an external" +
                                   s" authorisation and headers ${report(hc)}"
                               )
                               None
                             }
                       }
                     case _ =>
                       Logger(getClass).warn(
                         s"Creating user '$userId' on the planet '$planetId' failed for an external authorisation" +
                           s" and headers ${report(hc)}"
                       )
                       Future.successful(None)
                   }
            } yield maybeSession
          case _ => Future.successful(None)
        }
        .recover { case NonFatal(_) =>
          None
        }
    }

  def report(hc: HeaderCarrier): String =
    s"""Authorization:${hc.authorization
      .map(_.value)
      .getOrElse("-")} X-Session-ID:${hc.sessionId.getOrElse("-")} ForwardedFor:${hc.forwarded
      .map(_.value)
      .getOrElse("-")} RequestId:${hc.requestId.map(_.value).getOrElse("-")}"""

  private def merge(first: User, second: User): User = User(
    userId = first.userId,
    groupId = first.groupId.orElse(second.groupId),
    confidenceLevel = first.confidenceLevel.orElse(second.confidenceLevel),
    credentialStrength = first.credentialStrength.orElse(second.credentialStrength),
    credentialRole = first.credentialRole.orElse(second.credentialRole),
    nino = first.nino.orElse(second.nino),
    name = first.name.orElse(second.name),
    dateOfBirth = first.dateOfBirth.orElse(second.dateOfBirth),
    recordIds = (first.recordIds ++ second.recordIds).distinct,
    isNonCompliant = first.isNonCompliant,
    planetId = first.planetId
  )

  private def merge(first: Group, second: Group): Group = Group(
    groupId = first.groupId,
    planetId = first.planetId,
    affinityGroup = {
      require(first.affinityGroup == second.affinityGroup)
      first.affinityGroup
    },
    agentId = first.agentId.orElse(second.agentId),
    agentCode = first.agentCode.orElse(second.agentCode),
    agentFriendlyName = first.agentFriendlyName.orElse(second.agentFriendlyName),
    principalEnrolments = (first.principalEnrolments ++ second.principalEnrolments).distinct,
    delegatedEnrolments = (first.delegatedEnrolments ++ second.delegatedEnrolments).distinct,
    suspendedRegimes = first.suspendedRegimes ++ second.suspendedRegimes
  )
}
