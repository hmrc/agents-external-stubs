package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.Enrolment
import uk.gov.hmrc.agentsexternalstubs.repository.UsersRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrievalService @Inject()(usersRepository: UsersRepository) {

  def principalEnrolments(userId: String)(implicit ec: ExecutionContext): Future[Seq[Enrolment]] =
    for {
      user <- usersRepository.findByUserId(userId)
    } yield user.map(_.principalEnrolments).getOrElse(Seq.empty)

  def delegatedEnrolments(userId: String)(implicit ec: ExecutionContext): Future[Seq[Enrolment]] =
    for {
      user <- usersRepository.findByUserId(userId)
    } yield user.map(_.delegatedEnrolments).getOrElse(Seq.empty)
}
