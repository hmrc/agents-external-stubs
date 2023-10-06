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

import cats.data.Validated.{Invalid, Valid}
import com.github.blemale.scaffeine.Scaffeine
import play.api.i18n.Lang.logger
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.admin.{AG, Group, GroupGenerator, GroupUsersValidator, User, UserSanitizer}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, ForbiddenException, NotFoundException}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersService @Inject() (
  usersRepository: UsersRepository,
  userRecordsService: UserToRecordsSyncService,
  knownFactsRepository: KnownFactsRepository,
  externalUserService: ExternalUserService,
  groupsService: GroupsService
) {

  def findByUserId(userId: String, planetId: String): Future[Option[User]] =
    usersRepository.findByUserId(userId, planetId)

  def findUserAndGroup(userId: String, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[(Option[User], Option[Group])] =
    for {
      maybeUser <- usersRepository.findByUserId(userId, planetId)
      maybeGroup <- maybeUser
                      .flatMap(_.groupId)
                      .fold(Future.successful(Option.empty[Group]))(gid => groupsService.findByGroupId(gid, planetId))
    } yield (maybeUser, maybeGroup)

  def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    externalUserService.tryLookupExternalUserIfMissingForIdentifier(Nino(nino), planetId, createUser)(id =>
      usersRepository.findByNino(id.value, planetId)
    )

  def findByPlanetId(planetId: String)(limit: Int): Future[Seq[User]] =
    usersRepository.findByPlanetId(planetId)(limit)

  def findByGroupId(groupId: String, planetId: String)(limit: Option[Int]): Future[Seq[User]] =
    usersRepository.findByGroupId(groupId, planetId)(limit)

  def findAdminByGroupId(groupId: String, planetId: String): Future[Option[User]] =
    usersRepository.findAdminByGroupId(groupId, planetId)

  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Option[User]] =
    externalUserService.tryLookupExternalUserIfMissingForEnrolmentKey(enrolmentKey, planetId, createUser) {
      usersRepository.findByPrincipalEnrolmentKey(enrolmentKey, planetId)
    }

  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int): Future[Seq[User]] =
    usersRepository.findByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  def findUserIdsByAssignedDelegatedEnrolmentKey(
    enrolmentKey: EnrolmentKey,
    planetId: String,
    limit: Option[Int] = None
  ): Future[Seq[String]] =
    usersRepository.findUserIdsByAssignedDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  def findUserIdsByAssignedPrincipalEnrolmentKey(
    enrolmentKey: EnrolmentKey,
    planetId: String,
    limit: Option[Int] = None
  ): Future[Seq[String]] =
    usersRepository.findUserIdsByAssignedPrincipalEnrolmentKey(enrolmentKey, planetId)(limit)

  def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    limit: Int
  ): Future[Seq[Option[String]]] =
    usersRepository.findGroupIdsByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  val usersCache = Scaffeine().maximumSize(1000).expireAfterWrite(10.minutes).build[Int, User]()

  /** Create a new user. If the group corresponding to the groupId doesn't exist, it will create a group as well
    * with the affinityGroup provided. (If the group does exist then the affinityGroup parameter has no effect.)
    * If the new user has any assigned enrolments, corresponding group enrolments will be created.
    */
  def createUser(user: User, planetId: String, affinityGroup: Option[String])(implicit
    ec: ExecutionContext
  ): Future[User] = {
    val sanitizedAffinityGroup = affinityGroup.flatMap(AG.sanitize)

    val userKey = user.copy(planetId = None, recordIds = Seq.empty).hashCode()
    for {
      maybeExistingGroup <-
        user.groupId.fold(Future.successful(Option.empty[Group]))(gid => groupsService.findByGroupId(gid, planetId))
      refinedUser <-
        usersCache
          .getIfPresent(userKey)
          .map(u => Future.successful(u.copy(planetId = Some(planetId))))
          .getOrElse(
            refineAndValidateUser(
              user,
              planetId,
              maybeExistingGroup.map(_.affinityGroup).orElse(sanitizedAffinityGroup)
            )
          )
          .map(u => u.copy(planetId = Some(planetId)))

      maybeGroup <- (maybeExistingGroup, sanitizedAffinityGroup) match {
                      // An existing group id is provided: we assume we want to add a new user to the specified (existing) group.
                      case (Some(existingGroup), _) => Future.successful(Some(existingGroup))
                      // If no group id is provided, or doesn't exist, we create a group from scratch
                      case (None, Some(ag)) =>
                        val groupId = user.groupId.getOrElse(GroupGenerator.groupId(seed = UUID.randomUUID().toString))
                        groupsService
                          .createGroup(
                            Group(
                              planetId = planetId,
                              groupId = groupId,
                              affinityGroup = ag,
                              principalEnrolments = refinedUser.assignedPrincipalEnrolments.map(Enrolment.from(_)),
                              delegatedEnrolments = refinedUser.assignedDelegatedEnrolments.map(Enrolment.from(_))
                            ),
                            planetId
                          )
                          .map(Some(_))
                      // If no affinity group is provided, we leave the user without a group (e.g. Stride users)
                      case (None, None) => Future.successful(None)
                    }
      // If we are creating a group from scratch, the credential role MUST be admin as there must be an admin in the group
      credentialRole = if (maybeExistingGroup.isDefined) refinedUser.credentialRole else Some(User.CR.Admin)
      newUser = refinedUser.copy(groupId = maybeGroup.map(_.groupId), credentialRole = credentialRole)
      _ <- usersRepository.create(newUser, planetId)
      _ = usersCache.put(userKey, newUser)
      _ <- maybeGroup.fold(Future.successful(()))(group => updateKnownFacts(refinedUser, group, planetId))
      _ <- maybeGroup.fold(Future.successful(()))(group =>
             userRecordsService.syncUserToRecords(syncRecordId(refinedUser, planetId), refinedUser, group)
           )
    } yield newUser
  }

  /** Update a user.
    * If new assigned enrolments are being added to the user, corresponding group enrolments will be created.
    */
  def updateUser(userId: String, planetId: String, modify: User => User)(implicit ec: ExecutionContext): Future[User] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      updatedUser <- maybeUser match {
                       case Some(existingUser) =>
                         val modified = modify(existingUser).copy(userId = userId, planetId = Some(planetId))
                         if (modified != existingUser) for {
                           maybeGroup <- modified.groupId.fold(Future.successful(Option.empty[Group]))(gid =>
                                           groupsService.findByGroupId(gid, planetId)
                                         )
                           refined <- refineAndValidateUser(modified, planetId, maybeGroup.map(_.affinityGroup))
                           _       <- usersRepository.update(refined, planetId)
                           // now add any principal or delegated enrolments the group needs to have in order to maintain consistency
                           principalEnrolmentsToAdd =
                             maybeGroup
                               .fold(Seq.empty[EnrolmentKey])(grp =>
                                 refined.assignedPrincipalEnrolments.filterNot(ek =>
                                   grp.principalEnrolments.exists(_.toEnrolmentKey.contains(ek))
                                 )
                               )
                               .map(Enrolment.from(_))
                           delegatedEnrolmentsToAdd =
                             maybeGroup
                               .fold(Seq.empty[EnrolmentKey])(grp =>
                                 refined.assignedDelegatedEnrolments.filterNot(ek =>
                                   grp.delegatedEnrolments.exists(_.toEnrolmentKey.contains(ek))
                                 )
                               )
                               .map(Enrolment.from(_))
                           maybeUpdatedGroup <-
                             if (
                               (principalEnrolmentsToAdd.isEmpty && delegatedEnrolmentsToAdd.isEmpty) || maybeGroup.isEmpty
                             ) Future.successful(maybeGroup)
                             else
                               groupsService
                                 .updateGroup(
                                   maybeGroup.get.groupId,
                                   planetId,
                                   grp =>
                                     grp.copy(
                                       principalEnrolments = grp.principalEnrolments ++ principalEnrolmentsToAdd,
                                       delegatedEnrolments = grp.delegatedEnrolments ++ delegatedEnrolmentsToAdd
                                     )
                                 )
                                 .map(Some(_))
                           _ <-
                             maybeUpdatedGroup.fold(Future.successful(()))(group =>
                               updateKnownFacts(refined, group, planetId)
                             )
                           _ <- maybeUpdatedGroup.fold(Future.successful(()))(group =>
                                  userRecordsService.syncUserToRecords(syncRecordId(refined, planetId), refined, group)
                                )
                           _ =
                             AuthorisationCache
                               .updateResultsFor(refined, maybeUpdatedGroup, UsersService.this, groupsService, planetId)
                         } yield refined
                         else Future.successful(existingUser)
                       case None => Future.failed(new NotFoundException(s"User $userId not found"))
                     }
    } yield updatedUser

  def copyUser(userId: String, planetId: String, targetPlanetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      _ <- maybeUser match {
             case Some(user) => usersRepository.create(user.copy(recordIds = Seq.empty), targetPlanetId)
             case None       => Future.failed(new NotFoundException(s"User $userId not found"))
           }
    } yield ()

  def syncRecordId(user: User, planetId: String)(recordId: String)(implicit ec: ExecutionContext): Future[Unit] =
    usersRepository.syncRecordId(user.userId, recordId, planetId).map(_ => ())

  def deleteUser(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      maybeGroup <- maybeUser
                      .flatMap(_.groupId)
                      .fold(Future.successful(Option.empty[Group]))(groupsService.findByGroupId(_, planetId))
      _ <- maybeUser match {
             case Some(user) =>
               for {
                 _ <- checkCanRemoveUser(user, planetId)
                 _ <- usersRepository.delete(user.userId, planetId)
                 _ <- maybeGroup match {
                        case Some(group) => deleteKnownFacts(group, planetId)
                        case None =>
                          logger.warn(s"Deleting user $userId but the associated group was not found!")
                          Future.successful(())
                      }
                 _ <- userRecordsService.syncAfterUserRemoved(user)
               } yield ()
             case None => Future.successful(())
           }
    } yield ()

  private def refineAndValidateUser(user: User, planetId: String, affinityGroup: Option[String])(implicit
    ec: ExecutionContext
  ): Future[User] =
    if (user.isNonCompliant.contains(true)) {
      User.validate(user, affinityGroup) match {
        case Right(u)     => Future.successful(u.copy(isNonCompliant = None, complianceIssues = None))
        case Left(issues) => Future.successful(user.copy(isNonCompliant = Some(true), complianceIssues = Some(issues)))
      }
    } else
      for {
        sanitized <- Future(UserSanitizer(affinityGroup).sanitize(user.userId)(user))
        validated <- User
                       .validate(sanitized, affinityGroup)
                       .fold(errors => Future.failed(new BadRequestException(errors.mkString(", "))), Future.successful)
        accepted <- checkCanAcceptUser(validated, planetId).flatMap(
                      _.fold(
                        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
                        user => Future.successful(user)
                      )
                    )
      } yield accepted

  private def updateKnownFacts(user: User, group: Group, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    Future
      .sequence(
        group.principalEnrolments
          .map(_.toEnrolmentKey.flatMap(ek => KnownFacts.generate(ek, user.userId, user.facts)))
          .collect { case Some(x) => x }
          .map(knownFacts => knownFactsRepository.upsert(knownFacts.applyProperties(User.knownFactsOf(user)), planetId))
      )
      .map(_ => ())

  private def deleteKnownFacts(group: Group, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    Future
      .sequence(
        group.principalEnrolments
          .map(_.toEnrolmentKey)
          .collect { case Some(x) => x }
          .map(enrolmentKey => knownFactsRepository.delete(enrolmentKey, planetId))
      )
      .map(_ => ())

  private def checkCanAcceptUser(user: User, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Either[List[String], User]] =
    user.groupId match {
      case None => Future.successful(Right(user))
      case Some(groupId) =>
        findByGroupId(groupId, planetId)(limit = Some(101)).map { users => // TODO magic number: 101 (limit) Why?
          val maybeAdmin =
            if (
              !user.credentialRole.contains(User.CR.Assistant) && (!users.exists(_.isAdmin) || users
                .find(_.isAdmin)
                .map(_.userId)
                .contains(user.userId))
            )
              user.copy(credentialRole = Some(User.CR.Admin))
            else user
          GroupUsersValidator
            .validate(users.filterNot(_.userId == maybeAdmin.userId) :+ maybeAdmin) match {
            case Valid(())       => Right(maybeAdmin)
            case Invalid(errors) => Left(errors)
          }
        }
    }

  private def checkCanRemoveUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    user.groupId match {
      case None => Future.successful(())
      case Some(groupId) =>
        findByGroupId(groupId, planetId)(limit = Some(101)) flatMap { users => // TODO magic number: 101 (limit) Why?
          GroupUsersValidator
            .validate(users.filterNot(_.userId == user.userId))
            .fold(
              errors => Future.failed(new BadRequestException(errors.mkString(", "))),
              _ => Future.successful(())
            )
        }
    }

  def assignEnrolmentToUser(userId: String, enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId).flatMap {
      case None => Future.failed(new NotFoundException("ALLOCATION_DOES_NOT_EXIST"))
      case Some(_) =>
        findByUserId(userId, planetId)
          .flatMap {
            case None => Future.failed(new NotFoundException("USER_ID_DOES_NOT_EXIST"))
            case Some(user)
                if (user.assignedPrincipalEnrolments ++ user.assignedDelegatedEnrolments)
                  .exists(_.tag.equalsIgnoreCase(enrolmentKey.tag)) =>
              // if the user already has the assignment, return Bad Request as per spec
              Future.failed(new BadRequestException("INVALID_CREDENTIAL_ID"))
            case Some(user) =>
              user.groupId match {
                case None => Future.failed(new BadRequestException("INVALID_CREDENTIAL_ID"))
                case Some(groupId) =>
                  groupsService.findByGroupId(groupId, planetId).flatMap {
                    case None => Future.failed(new BadRequestException("INVALID_CREDENTIAL_ID"))
                    case Some(group) =>
                      val groupHasPrincipalEnrolment = group.principalEnrolments.exists(_.matches(enrolmentKey))
                      val groupHasDelegatedEnrolment = group.delegatedEnrolments.exists(_.matches(enrolmentKey))
                      val groupHasEnrolment = groupHasPrincipalEnrolment || groupHasDelegatedEnrolment
                      val isPrincipal = groupHasPrincipalEnrolment
                      if (groupHasEnrolment) {
                        usersRepository
                          .assignEnrolment(userId, group.groupId, group.planetId, enrolmentKey, isPrincipal)
                      } else {
                        Future.failed(new ForbiddenException("INVALID_CREDENTIAL_ID"))
                      }
                  }
              }
          }
    }

  def deassignEnrolmentFromUser(userId: String, enrolmentKey: EnrolmentKey, planetId: String)(implicit
    ec: ExecutionContext
  ): Future[User] =
    knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId).flatMap {
      case None => Future.failed(new NotFoundException("ALLOCATION_DOES_NOT_EXIST"))
      case Some(_) =>
        findByUserId(userId, planetId)
          .flatMap {
            case None => Future.failed(new NotFoundException("USER_ID_DOES_NOT_EXIST"))
            case Some(user) =>
              updateUser(
                userId,
                planetId,
                u =>
                  u.copy(
                    assignedPrincipalEnrolments = u.assignedPrincipalEnrolments.filterNot(_.tag == enrolmentKey.tag),
                    assignedDelegatedEnrolments = u.assignedDelegatedEnrolments.filterNot(_.tag == enrolmentKey.tag)
                  )
              )
          }
    }

  def checkAndFixUser(user: User, planetId: String, affinityGroup: String)(implicit
    ec: ExecutionContext
  ): Future[User] =
    affinityGroup match {
      case AG.Individual =>
        CheckAndFix.checkAndFixNino(user, planetId)
      case _ => Future.successful(user)
    }

  object CheckAndFix {

    def checkAndFixNino(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
      user.nino
        .map(nino =>
          for {
            duplicatedNino <- usersRepository.findByNino(nino.value, planetId).map(_.isDefined)
          } yield if (duplicatedNino) user.copy(nino = Some(Generator.ninoNoSpaces(user.userId))) else user
        )
        .getOrElse(Future.successful(user))

  }

  def reindexAllUsers: Future[Boolean] = usersRepository.reindexAllUsers

}
