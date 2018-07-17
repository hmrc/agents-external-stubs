package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubs.repository.UsersRepository
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersService @Inject()(usersRepository: UsersRepository) {

  def findByUserId(userId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    usersRepository.findByUserId(userId)

  def createUser(user: User)(implicit ec: ExecutionContext): Future[User] =
    for {
      _         <- usersRepository.create(user)
      maybeUser <- findByUserId(user.userId)
      newUser = maybeUser.getOrElse(throw new Exception(s"User $user creation failed."))
    } yield newUser

  def updateUser(userId: String, modify: User => User)(implicit ec: ExecutionContext): Future[User] =
    for {
      maybeUser <- findByUserId(userId)
      updatedUser <- maybeUser match {
                      case Some(existingUser) =>
                        val modified = modify(existingUser).copy(userId = userId)
                        if (modified != existingUser) for {
                          _         <- usersRepository.update(modified)
                          maybeUser <- usersRepository.findByUserId(userId)
                        } yield maybeUser.getOrElse(throw new Exception)
                        else Future.successful(existingUser)
                      case None => Future.failed(new NotFoundException(s"User $userId not found"))
                    }
    } yield updatedUser

  def addEnrolment(userId: String, service: String, identifierKey: String, identifierValue: String)(
    implicit ec: ExecutionContext): Future[User] =
    updateUser(
      userId,
      user =>
        user.copy(
          principalEnrolments = user.principalEnrolments :+ Enrolment(
            service,
            Some(Seq(Identifier(identifierKey, identifierValue))))))

}
