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

package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.agentsexternalstubs.connectors.AgentAccessControlConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import uk.gov.hmrc.agentsexternalstubs.services.{GroupsService, UsersService}

import java.time.LocalDate

trait AuthoriseContext {

  def request: AuthoriseRequest

  def userId: String
  def providerType: String
  def principalEnrolments: Seq[Enrolment]
  def delegatedEnrolments: Seq[Enrolment]
  def strideRoles: Seq[String]
  def affinityGroup: Option[String]
  def confidenceLevel: Option[Int]
  def credentialStrength: Option[String]
  def credentialRole: Option[String]
  def nino: Option[Nino]
  def groupId: Option[String]
  def name: Option[String]
  def dateOfBirth: Option[LocalDate]
  def agentCode: Option[String]
  def agentFriendlyName: Option[String]
  def agentId: Option[String]
  def planetId: Option[String]
  def email: Option[String]
  def internalId: Option[String]

  def hasDelegatedAuth(rule: String, identifiers: Seq[Identifier]): Boolean

  lazy val authorisedServices: Set[String] = request.authorise.collect { case EnrolmentPredicate(service, _, _) =>
    service
  }.toSet
}

abstract class AuthoriseUserContext(user: User, group: Option[Group]) extends AuthoriseContext {

  implicit val ec: ExecutionContext

  final val timeout: Duration = 30.seconds

  override def userId: String = user.userId

  def strideRoles: Seq[String] = user.strideRoles

  override def affinityGroup: Option[String] = group.map(_.affinityGroup)

  override def confidenceLevel: Option[Int] = user.confidenceLevel

  override def credentialStrength: Option[String] = user.credentialStrength

  override def credentialRole: Option[String] = user.credentialRole

  override def nino: Option[Nino] = user.nino

  override def groupId: Option[String] = user.groupId

  override def name: Option[String] = user.name

  override def dateOfBirth: Option[LocalDate] = user.dateOfBirth

  override def agentCode: Option[String] = group.flatMap(_.agentCode)

  override def agentFriendlyName: Option[String] = group.flatMap(_.agentFriendlyName)

  override def agentId: Option[String] = group.flatMap(_.agentId)

  override def email: Option[String] = Some(s"event-agents-external-aaaadghuc4fueomsg3kpkvdmry@hmrcdigital.slack.com")

  override def internalId: Option[String] = Some(s"${user.userId}@${user.planetId.getOrElse("hmrc")}")

  val userService: UsersService
  val groupsService: GroupsService

  override def principalEnrolments: Seq[Enrolment] = {
    val enrolments =
      if (
        group.exists(g => g.affinityGroup == AG.Individual || g.affinityGroup == AG.Organisation) && user.nino.isDefined
      )
        group.map(_.principalEnrolments).getOrElse(Seq.empty) :+ Enrolment(
          "HMRC-NI",
          "NINO",
          nino.get.value
        ) // TODO should we use here (and in the rest of this class) the group's enrolments or the user's assigned ones?
      else group.map(_.principalEnrolments).getOrElse(Seq.empty)
    if (user.isAdmin) enrolments
    else {
      enrolments.toSet
        .union((user.groupId, user.planetId) match {
          case (Some(groupId), Some(planetId)) =>
            Await
              .result(
                groupsService
                  .findByGroupId(groupId, planetId)
                  .map(_.map(_.principalEnrolments).getOrElse(Seq.empty)),
                timeout
              )
              .toSet
          case _ => Set.empty[Enrolment]
        })
        .toSeq
    }
  }

  override def delegatedEnrolments: Seq[Enrolment] = {
    val enrolments = group.map(_.delegatedEnrolments).getOrElse(Seq.empty)
    if (user.isAdmin) enrolments
    else {
      enrolments.toSet
        .union((user.groupId, user.planetId) match {
          case (Some(groupId), Some(planetId)) =>
            Await
              .result(
                groupsService
                  .findByGroupId(groupId, planetId)
                  .map(_.map(_.delegatedEnrolments).getOrElse(Seq.empty)),
                timeout
              )
              .toSet
          case _ => Set.empty[Enrolment]
        })
        .toSeq
    }
  }
}

case class FullAuthoriseContext(
  user: User,
  group: Option[Group],
  userService: UsersService,
  groupsService: GroupsService,
  authenticatedSession: AuthenticatedSession,
  request: AuthoriseRequest,
  agentAccessControlConnector: AgentAccessControlConnector
)(implicit val ec: ExecutionContext, hc: HeaderCarrier)
    extends AuthoriseUserContext(user, group) {

  override def providerType: String = authenticatedSession.providerType
  override def planetId: Option[String] = Some(authenticatedSession.planetId)

  def hasDelegatedAuth(rule: String, identifiers: Seq[Identifier]): Boolean =
    rule match {
      case "epaye-auth" =>
        (for {
          ac          <- agentCode
          taxOfficeNo <- identifiers.find(_.key == "TaxOfficeNumber").map(_.value)
          employerRef <- identifiers.find(_.key == "TaxOfficeReference").map(_.value)
        } yield Await.result(
          agentAccessControlConnector.isAuthorisedForPaye(ac, s"$taxOfficeNo/$employerRef"),
          timeout
        ))
          .getOrElse(false)

      case "sa-auth" =>
        (for {
          ac    <- agentCode
          saUtr <- identifiers.find(_.key == "UTR").map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForSa(ac, saUtr), timeout))
          .getOrElse(false)

      case "mtd-it-auth" =>
        (for {
          ac      <- agentCode
          mtdItId <- identifiers.find(_.key == "MTDITID").map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForMtdIt(ac, mtdItId), timeout))
          .getOrElse(false)

      case "mtd-vat-auth" =>
        (for {
          ac  <- agentCode
          vrn <- identifiers.find(_.key == "VRN").map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForMtdVat(ac, vrn), timeout))
          .getOrElse(false)

      case "afi-auth" =>
        (for {
          ac   <- agentCode
          nino <- identifiers.headOption.map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForAfi(ac, nino), timeout))
          .getOrElse(false)

      case "trust-auth" =>
        (for {
          ac  <- agentCode
          utr <- identifiers.headOption.map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForTrust(ac, utr), timeout))
          .getOrElse(false)

      case "cgt-auth" =>
        (for {
          ac     <- agentCode
          cgtRef <- identifiers.headOption.map(_.value)
        } yield Await.result(agentAccessControlConnector.isAuthorisedForCgt(ac, cgtRef), timeout))
          .getOrElse(false)

      case _ => true
    }

}
