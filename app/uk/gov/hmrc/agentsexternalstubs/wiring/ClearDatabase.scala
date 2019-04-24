package uk.gov.hmrc.agentsexternalstubs.wiring

import akka.actor.{Actor, ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.repository._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ClearDatabase @Inject()(
  appConfig: AppConfig,
  usersRepository: UsersRepository,
  recordsRepository: RecordsRepository,
  knownFactsRepository: KnownFactsRepository,
  authenticatedSessionsRepository: AuthenticatedSessionsRepository,
  specialCasesRepository: SpecialCasesRepository,
  actorSystem: ActorSystem)(implicit ec: ExecutionContext) {

  val interval = Duration(24, "h")
  val now = DateTime.now
  val taskDateTime = DateTime.now.withTimeAtStartOfDay().withHourOfDay(12).withMinuteOfHour(30)
  val initialDelay =
    FiniteDuration(
      (if (now.isBefore(taskDateTime)) new Interval(now, taskDateTime) else new Interval(now, taskDateTime.plusDays(1))).toDurationMillis,
      "ms")

  class ClearDatabaseTaskActor(olderThanMilliseconds: Long) extends Actor {

    override val receive: Receive = {
      case "clear" =>
        clearDatabase(System.currentTimeMillis() - olderThanMilliseconds).recover { case _ => }
    }
  }

  val taskActorRef = actorSystem.actorOf(Props(new ClearDatabaseTaskActor(interval.toMillis)))

  if (appConfig.clearOldMongoDbDocumentsDaily) {
    Logger(getClass).info(
      s"Clear database task will start in ${initialDelay.toMinutes} minutes and will run every ${interval.toHours}")
    actorSystem.scheduler.schedule(initialDelay, interval, taskActorRef, "clear")
  } else {
    Logger(getClass).info("Clear database daily task is switched off.")
  }

  def clearDatabase(timestamp: Long): Future[Int] =
    Future
      .sequence(
        Seq(
          usersRepository.deleteAll(timestamp),
          recordsRepository.deleteAll(timestamp),
          knownFactsRepository.deleteAll(timestamp),
          specialCasesRepository.deleteAll(timestamp),
          authenticatedSessionsRepository.deleteAll(timestamp)
        )
      )
      .map(_.sum)
      .andThen {
        case Success(count) =>
          Logger(getClass).info(s"Total number of $count outdated documents has been removed from the database.")
        case Failure(exception) =>
          Logger(getClass).error("Daily outdated documents removal task has failed", exception)
      }

}
