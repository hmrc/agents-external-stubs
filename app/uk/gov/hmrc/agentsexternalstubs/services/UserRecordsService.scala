package uk.gov.hmrc.agentsexternalstubs.services
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessDetailsRecord, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class UserRecordsService @Inject()(businessDetailsRecordsService: BusinessDetailsRecordsService) {

  type UserSync = PartialFunction[User, Future[Unit]]

  val businessDetailsForMtdItIndividual: UserSync = {
    case User.Individual(user) if user.principalEnrolments.exists(_.key == "HMRC-MTD-IT") =>
      businessDetailsRecordsService.store(
        BusinessDetailsRecord
          .seed(user.userId)
          .withNino(
            user.nino
              .map(_.value.replace(" ", ""))
              .getOrElse(throw new IllegalStateException("Expected user with NINO.")))
          .withMtdbsa(
            user.principalEnrolments
              .find(_.key == "HMRC-MTD-IT")
              .flatMap(_.identifiers.flatMap(_.find(_.key == "MTDITID")))
              .map(_.value)
              .getOrElse(throw new IllegalStateException("Expected user with MTDITID identifier."))),
        autoFill = true,
        user.planetId.get
      )
  }

  val userSyncOperations: Seq[UserSync] = Seq(
    businessDetailsForMtdItIndividual
  )

  final val syncUserToRecords: Option[User] => Future[Unit] = {
    case None => Future.successful(())
    case Some(user) =>
      Future
        .sequence(
          userSyncOperations
            .map(f => if (f.isDefinedAt(user)) f(user) else Future.successful(())))
        .map(_.reduce((_, _) => ()))
  }

  final val syncAfterUserRemoved: User => Future[Unit] =
    user => Future.successful(())

}
