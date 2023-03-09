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

import com.github.blemale.scaffeine.Scaffeine
import play.api.i18n.Lang.logger
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.{GroupsRepository, KnownFactsRepository}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupsService @Inject() (
  groupsRepository: GroupsRepository,
  knownFactsRepository: KnownFactsRepository,
  userToRecordsSyncService: UserToRecordsSyncService
) {

  def findByGroupId(groupId: String, planetId: String): Future[Option[Group]] =
    groupsRepository.findByGroupId(groupId, planetId)

  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int): Future[Seq[Group]] = {
    require(affinityGroup.isEmpty || affinityGroup.exists(AG.all))
    groupsRepository.findByPlanetId(planetId, affinityGroup)(limit)
  }

  def findByAgentCode(agentCode: String, planetId: String): Future[Option[Group]] =
    groupsRepository.findByAgentCode(agentCode, planetId)

  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String): Future[Option[Group]] =
    groupsRepository.findByPrincipalEnrolmentKey(enrolmentKey, planetId)

  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int): Future[Seq[Group]] =
    groupsRepository.findByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  val groupsCache = Scaffeine().maximumSize(1000).expireAfterWrite(10.minutes).build[Int, Group]()

  def createGroup(group: Group, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Group] = {
    val groupKey = group.copy(planetId = "").hashCode()
    for {
      refined <- groupsCache
                   .getIfPresent(groupKey)
                   .map(g => Future.successful(g.copy(planetId = planetId)))
                   .getOrElse(refineAndValidateGroup(group, planetId))
                   .map(g => g.copy(planetId = planetId))
      _ <- groupsRepository.create(refined, planetId)
      _ <- userToRecordsSyncService.syncGroup(refined)
      _ <- Future.successful(groupsCache.put(groupKey, refined))
    } yield refined
  }

  def updateGroup(groupId: String, planetId: String, modify: Group => Group)(implicit
    ec: ExecutionContext
  ): Future[Group] =
    for {
      maybeGroup <- findByGroupId(groupId, planetId)
      updatedGroup <- maybeGroup match {
                        case Some(existingGroup) =>
                          val modified = modify(existingGroup).copy(planetId = planetId)
                          if (modified != existingGroup) for {
                            refined <- refineAndValidateGroup(modified, planetId)
                            _       <- groupsRepository.update(refined, planetId)
                          } yield refined
                          else Future.successful(existingGroup)
                        case None => Future.failed(new NotFoundException(s"Group $groupId not found"))
                      }
      _ <- userToRecordsSyncService.syncGroup(updatedGroup)
    } yield updatedGroup

  def deleteGroup(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeGroup <- findByGroupId(groupId, planetId)
      _ <- maybeGroup match {
             case Some(group) =>
               for {
                 _ <- groupsRepository.delete(group.groupId, planetId)
                 _ <- deleteKnownFacts(group, planetId)
                 // TODO should we also delete all the users that were in the group?
               } yield ()
             case None => Future.successful(())
           }
    } yield ()

  def setEnrolmentFriendlyName(groupId: String, planetId: String, enrolmentKey: EnrolmentKey, friendlyName: String)(
    implicit ec: ExecutionContext
  ): Future[Option[Unit]] = {
    logger.info(
      s"Updating friendly name '$friendlyName', enrolment key '$enrolmentKey', group '$groupId', planet '$planetId'"
    )

    groupsRepository.updateFriendlyNameForEnrolment(groupId, planetId, enrolmentKey, friendlyName) flatMap {
      case None    => Future.failed(new NotFoundException("enrolment not found"))
      case Some(_) => Future successful Some(())
    }
  }

  private def refineAndValidateGroup(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Group] =
    for {
      sanitized <- Future(GroupSanitizer.sanitize(group.groupId)(group))
      validated <- Group
                     .validate(sanitized)
                     .fold(errors => Future.failed(new BadRequestException(errors.mkString(", "))), Future.successful)
    } yield validated

  private def deleteKnownFacts(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    Future
      .sequence(
        group.principalEnrolments
          .map(_.toEnrolmentKey)
          .collect { case Some(x) => x }
          .map(enrolmentKey => knownFactsRepository.delete(enrolmentKey, planetId))
      )
      .map(_ => ())

  /* Group enrolment is assigned to the unique Admin user of the group */
  def allocateEnrolmentToGroup(
    user: User,
    groupId: String,
    enrolmentKey: EnrolmentKey,
    enrolmentType: String,
    agentCodeOpt: Option[String],
    planetId: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId).flatMap {
      case None => Future.failed(new NotFoundException("ALLOCATION_DOES_NOT_EXIST"))
      case Some(_) =>
        (if (user.credentialRole.contains(User.CR.Assistant))
           Future.failed(UnsupportedCredentialRole("INVALID_CREDENTIAL_TYPE"))
         else {
           agentCodeOpt match {
             case None =>
               findByGroupId(groupId, planetId)
                 .map(_.getOrElse(throw new BadRequestException("INVALID_GROUP_ID")))
             case Some(agentCode) =>
               findByAgentCode(agentCode, planetId)
                 .map(_.getOrElse(throw new BadRequestException("INVALID_AGENT_FORMAT")))
           }
         }).flatMap { group =>
          if (enrolmentType == "principal")
            updateGroup(
              group.groupId,
              planetId,
              g => {
                val enrolment = Enrolment.from(enrolmentKey)
                if (g.principalEnrolments.contains(enrolment)) throw new EnrolmentAlreadyExists
                else
                  g.copy(principalEnrolments = appendEnrolment(g.principalEnrolments, Enrolment.from(enrolmentKey)))
              }
            )
              .map(_ => ())
          else if (enrolmentType == "delegated" && group.affinityGroup == AG.Agent)
            findByPrincipalEnrolmentKey(enrolmentKey, planetId).flatMap {
              case Some(owner) if !(owner.affinityGroup == AG.Agent) =>
                val enrolment = Enrolment.from(enrolmentKey)
                groupsRepository.findByGroupId(group.groupId, planetId).map { case Some(group) =>
                  if (group.delegatedEnrolments.map(_.toEnrolmentKeyTag.getOrElse("")).contains(enrolmentKey.tag))
                    throw new EnrolmentAlreadyExists
                  else groupsRepository.addDelegatedEnrolment(group.groupId, planetId, enrolment)
                }
              case None =>
                Future.failed(new BadRequestException("INVALID_QUERY_PARAMETERS"))
            }
          else Future.failed(new BadRequestException("INVALID_QUERY_PARAMETERS"))
        }
    }

  def deallocateEnrolmentFromGroup(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    agentCodeOpt: Option[String],
    planetId: String
  )(implicit ec: ExecutionContext): Future[Group] =
    agentCodeOpt match {
      case None =>
        findByGroupId(groupId, planetId)
          .flatMap {
            case Some(group) =>
              group.affinityGroup match {
                case AG.Agent =>
                  updateGroup(
                    group.groupId,
                    planetId,
                    _.copy(delegatedEnrolments = removeEnrolment(group.delegatedEnrolments, enrolmentKey))
                  )
                case _ =>
                  updateGroup(
                    group.groupId,
                    planetId,
                    _.copy(principalEnrolments = removeEnrolment(group.principalEnrolments, enrolmentKey))
                  )
              }
            case _ => Future.failed(new BadRequestException("INVALID_GROUP_ID"))
          }
      case Some(agentCode) =>
        findByAgentCode(agentCode, planetId)
          .flatMap {
            case Some(group) =>
              updateGroup(
                group.groupId,
                planetId,
                _.copy(delegatedEnrolments = removeEnrolment(group.delegatedEnrolments, enrolmentKey))
              )
            case _ => Future.failed(new BadRequestException("INVALID_AGENT_FORMAT"))
          }
    }

  private def appendEnrolment(enrolments: Seq[Enrolment], enrolment: Enrolment): Seq[Enrolment] =
    if (
      enrolments.exists(e => e.key == enrolment.key && e.identifiers.exists(ii => enrolment.identifiers.contains(ii)))
    ) enrolments
    else enrolments :+ enrolment

  private def removeEnrolment(enrolments: Seq[Enrolment], key: EnrolmentKey): Seq[Enrolment] =
    enrolments.filterNot(_.matches(key))

  def checkAndFixGroup(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Group] =
    group.affinityGroup match {
      case AG.Agent =>
        CheckAndFix.checkAndFixAgentCode(group, planetId)
      case _ => Future.successful(group)
    }

  object CheckAndFix {

    def checkAndFixAgentCode(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Group] =
      group.agentCode
        .map(ac =>
          for {
            duplicateAgentCode <- groupsRepository.findByAgentCode(ac, planetId).map(_.nonEmpty)
          } yield
            if (duplicateAgentCode) group.copy(agentCode = Some(UserGenerator.agentCode(seed = group.groupId)))
            else group
        )
        .getOrElse(Future.successful(group))
  }

  def reindexAllGroups: Future[Boolean] = groupsRepository.reindexAllGroups

}

final class EnrolmentAlreadyExists extends Exception
