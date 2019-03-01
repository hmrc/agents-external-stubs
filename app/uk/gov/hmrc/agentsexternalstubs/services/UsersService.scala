package uk.gov.hmrc.agentsexternalstubs.services

import cats.data.Validated.{Invalid, Valid}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{User, _}
import uk.gov.hmrc.agentsexternalstubs.repository.{KnownFactsRepository, UsersRepository}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UsersService @Inject()(
  usersRepository: UsersRepository,
  userRecordsService: UserToRecordsSyncService,
  knownFactsRepository: KnownFactsRepository,
  externalUserService: ExternalUserService) {

  def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    usersRepository.findByUserId(userId, planetId)

  def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    externalUserService.tryLookupExternalUserIfMissingForIdentifier(Nino(nino), planetId, createUser)(id =>
      usersRepository.findByNino(id.value, planetId))

  def findByPlanetId(planetId: String, affinityGroup: Option[String])(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] = {
    require(affinityGroup.isEmpty || affinityGroup.exists(User.AG.all))
    usersRepository.findByPlanetId(planetId, affinityGroup)(limit)
  }

  def findByGroupId(groupId: String, planetId: String)(limit: Int)(implicit ec: ExecutionContext): Future[Seq[User]] =
    usersRepository.findByGroupId(groupId, planetId)(limit)

  def findAdminByGroupId(groupId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    usersRepository.findAdminByGroupId(groupId, planetId)

  def findByAgentCode(agentCode: String, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    usersRepository.findByAgentCode(agentCode, planetId)(limit)

  def findAdminByAgentCode(agentCode: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    usersRepository.findAdminByAgentCode(agentCode, planetId)

  def findByPrincipalEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[User]] =
    externalUserService.tryLookupExternalUserIfMissingForEnrolmentKey(enrolmentKey, planetId, createUser) {
      usersRepository.findByPrincipalEnrolmentKey(enrolmentKey, planetId)
    }

  def findByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[User]] =
    usersRepository.findByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  def findUserIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[String]] =
    usersRepository.findUserIdsByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  def findGroupIdsByDelegatedEnrolmentKey(enrolmentKey: EnrolmentKey, planetId: String)(limit: Int)(
    implicit ec: ExecutionContext): Future[Seq[Option[String]]] =
    usersRepository.findGroupIdsByDelegatedEnrolmentKey(enrolmentKey, planetId)(limit)

  def createUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
    for {
      refined   <- refineAndValidateUser(user, planetId)
      _         <- usersRepository.create(refined, planetId)
      maybeUser <- findByUserId(refined.userId, planetId)
      _         <- updateKnownFacts(refined, planetId)
      _         <- userRecordsService.syncUserToRecords(addRecordId(maybeUser, planetId))(maybeUser)
      newUser = maybeUser.getOrElse(throw new Exception(s"User $user creation failed."))
    } yield newUser

  def tryCreateUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Try[User]] =
    for {
      maybeUser <- findByUserId(user.userId, planetId)
      result <- maybeUser match {
                 case Some(_) => Future.successful(Failure(new Exception(s"User ${user.userId} already exists")))
                 case None =>
                   for {
                     refined <- refineAndValidateUser(user, planetId)
                     _       <- usersRepository.create(refined, planetId)
                     newUser <- findByUserId(refined.userId, planetId)
                     _       <- updateKnownFacts(refined, planetId)
                     _       <- userRecordsService.syncUserToRecords(addRecordId(maybeUser, planetId))(newUser)
                   } yield newUser.map(Success.apply).getOrElse(Failure(new Exception(s"User $user creation failed")))
               }
    } yield result

  def updateUser(userId: String, planetId: String, modify: User => User)(implicit ec: ExecutionContext): Future[User] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      updatedUser <- maybeUser match {
                      case Some(existingUser) =>
                        val modified = modify(existingUser).copy(userId = userId, planetId = Some(planetId))
                        if (modified != existingUser) for {
                          refined   <- refineAndValidateUser(modified, planetId)
                          _         <- usersRepository.update(refined, planetId)
                          maybeUser <- findByUserId(userId, planetId)
                          _         <- updateKnownFacts(refined, planetId)
                          _         <- userRecordsService.syncUserToRecords(addRecordId(maybeUser, planetId))(maybeUser)
                        } yield maybeUser.getOrElse(throw new Exception)
                        else Future.successful(existingUser)
                      case None => Future.failed(new NotFoundException(s"User $userId not found"))
                    }
    } yield updatedUser

  def addRecordId(userOpt: Option[User], planetId: String)(recordId: String)(
    implicit ec: ExecutionContext): Future[Unit] =
    userOpt match {
      case Some(user) => usersRepository.addRecordId(user.userId, recordId, planetId).map(_ => ())
      case None       => Future.successful(())
    }

  def deleteUser(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      _ <- maybeUser match {
            case Some(user) =>
              for {
                _ <- checkCanRemoveUser(user, planetId)
                _ <- usersRepository.delete(user.userId, planetId)
                _ <- deleteKnownFacts(user, planetId)
                _ <- userRecordsService.syncAfterUserRemoved(user)
              } yield ()
            case None => Future.successful(())
          }
    } yield ()

  private def refineAndValidateUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
    if (user.isNonCompliant.contains(true)) {
      User.validate(user) match {
        case Right(u)     => Future.successful(u.copy(isNonCompliant = None, complianceIssues = None))
        case Left(issues) => Future.successful(user.copy(isNonCompliant = Some(true), complianceIssues = Some(issues)))
      }
    } else
      for {
        sanitized <- Future(UserSanitizer.sanitize(user.userId)(user))
        validated <- User
                      .validate(sanitized)
                      .fold(errors => Future.failed(new BadRequestException(errors.mkString(", "))), Future.successful)
        accepted <- checkCanAcceptUser(validated, planetId).flatMap(
                     _.fold(
                       errors => Future.failed(new BadRequestException(errors.mkString(", "))),
                       user => Future.successful(user)
                     ))
      } yield accepted

  private def updateKnownFacts(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    Future
      .sequence(user.principalEnrolments
        .map(_.toEnrolmentKey.flatMap(ek => KnownFacts.generate(ek, user.userId, user.facts)))
        .collect { case Some(x) => x }
        .map(knownFacts => knownFactsRepository.upsert(knownFacts.applyProperties(User.knownFactsOf(user)), planetId)))
      .map(_ => ())

  private def deleteKnownFacts(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    Future
      .sequence(
        user.principalEnrolments
          .map(_.toEnrolmentKey)
          .collect { case Some(x) => x }
          .map(enrolmentKey => knownFactsRepository.delete(enrolmentKey, planetId)))
      .map(_ => ())

  private def checkCanAcceptUser(user: User, planetId: String)(
    implicit ec: ExecutionContext): Future[Either[List[String], User]] =
    user.groupId match {
      case None => Future.successful(Right(user))
      case Some(groupId) =>
        findByGroupId(groupId, planetId)(101).map { users =>
          val maybeAdmin =
            if (!user.credentialRole.contains(User.CR.Assistant) && (!users.exists(_.isAdmin) || users
                  .find(_.isAdmin)
                  .map(_.userId)
                  .contains(user.userId)))
              user.copy(credentialRole = Some(User.CR.Admin))
            else user
          GroupValidator
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
        findByGroupId(groupId, planetId)(101) flatMap { users =>
          GroupValidator
            .validate(users.filterNot(_.userId == user.userId))
            .fold(
              errors => Future.failed(new BadRequestException(errors.mkString(", "))),
              _ => Future.successful(())
            )
        }
    }

  /* Group enrolment is assigned to the unique Admin user of the group */
  def allocateEnrolmentToGroup(
    userId: String,
    groupId: String,
    enrolmentKey: EnrolmentKey,
    enrolmentType: String,
    agentCodeOpt: Option[String],
    planetId: String)(implicit ec: ExecutionContext): Future[User] =
    knownFactsRepository.findByEnrolmentKey(enrolmentKey, planetId).flatMap {
      case None => Future.failed(new NotFoundException("ALLOCATION_DOES_NOT_EXIST"))
      case Some(_) =>
        findByUserId(userId, planetId)
          .flatMap {
            case Some(user) =>
              (if (user.credentialRole.contains(User.CR.Assistant))
                 Future.failed(UnsupportedCredentialRole("INVALID_CREDENTIAL_TYPE"))
               else {
                 agentCodeOpt match {
                   case None =>
                     findAdminByGroupId(groupId, planetId)
                       .map(_.getOrElse(throw new BadRequestException("INVALID_GROUP_ID")))
                   case Some(agentCode) =>
                     findAdminByAgentCode(agentCode, planetId)
                       .map(_.getOrElse(throw new BadRequestException("INVALID_AGENT_FORMAT")))
                 }
               }).flatMap { admin =>
                if (enrolmentType == "principal")
                  updateUser(
                    admin.userId,
                    planetId,
                    u => u.copy(principalEnrolments = u.principalEnrolments :+ Enrolment.from(enrolmentKey)))
                else if (enrolmentType == "delegated" && admin.affinityGroup.contains(User.AG.Agent))
                  findByPrincipalEnrolmentKey(enrolmentKey, planetId).flatMap {
                    case Some(owner) if !owner.affinityGroup.contains(User.AG.Agent) =>
                      updateUser(
                        admin.userId,
                        planetId,
                        u => u.copy(delegatedEnrolments = u.delegatedEnrolments :+ Enrolment.from(enrolmentKey)))
                    case None =>
                      Future.failed(throw new BadRequestException("INVALID_QUERY_PARAMETERS"))
                  } else Future.failed(throw new BadRequestException("INVALID_QUERY_PARAMETERS"))
              }
            case None => Future.failed(throw new BadRequestException("INVALID_JSON_BODY"))
          }
    }

  /* Group enrolment is de-assigned from the unique Admin user of the group */
  def deallocateEnrolmentFromGroup(
    groupId: String,
    enrolmentKey: EnrolmentKey,
    agentCodeOpt: Option[String],
    keepAgentAllocations: Option[String],
    planetId: String)(implicit ec: ExecutionContext): Future[User] =
    agentCodeOpt match {
      case None =>
        findAdminByGroupId(groupId, planetId)
          .flatMap {
            case Some(admin) if admin.credentialRole.contains(User.CR.Admin) =>
              admin.affinityGroup match {
                case Some(User.AG.Agent) =>
                  updateUser(
                    admin.userId,
                    planetId,
                    u => u.copy(delegatedEnrolments = removeEnrolment(u.delegatedEnrolments, enrolmentKey)))
                case _ =>
                  updateUser(
                    admin.userId,
                    planetId,
                    u => u.copy(principalEnrolments = removeEnrolment(u.principalEnrolments, enrolmentKey)))
              }

            case _ => Future.failed(throw new BadRequestException("INVALID_GROUP_ID"))
          }
      case Some(agentCode) =>
        findAdminByAgentCode(agentCode, planetId)
          .flatMap {
            case Some(admin) if admin.credentialRole.contains(User.CR.Admin) =>
              updateUser(
                admin.userId,
                planetId,
                u => u.copy(delegatedEnrolments = removeEnrolment(u.delegatedEnrolments, enrolmentKey)))
            case _ => Future.failed(throw new BadRequestException("INVALID_AGENT_FORMAT"))
          }
    }

  private def removeEnrolment(enrolments: Seq[Enrolment], key: EnrolmentKey): Seq[Enrolment] =
    enrolments.filterNot(_.matches(key))

  def checkAndFixUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
    user.affinityGroup match {
      case Some(User.AG.Agent) =>
        CheckAndFix.checkAndFixAgentCode(user, planetId)

      case Some(User.AG.Individual) =>
        CheckAndFix.checkAndFixNino(user, planetId)

      case _ => Future.successful(user)
    }

  object CheckAndFix {

    def checkAndFixAgentCode(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
      user.agentCode
        .map(ac =>
          for {
            duplicateAgentCode <- usersRepository.findByAgentCode(ac, planetId)(1).map(_.nonEmpty)
          } yield if (duplicateAgentCode) user.copy(agentCode = Some(UserGenerator.agentCode(user.userId))) else user)
        .getOrElse(Future.successful(user))

    def checkAndFixNino(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
      user.nino
        .map(nino =>
          for {
            duplicatedNino <- usersRepository.findByNino(nino.value, planetId).map(_.isDefined)
          } yield if (duplicatedNino) user.copy(nino = Some(Generator.ninoNoSpaces(user.userId))) else user)
        .getOrElse(Future.successful(user))

  }

}
