/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.wiring

import akka.actor.{Actor, ActorSystem, Props}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.repository._

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ClearDatabase @Inject() (
  appConfig: AppConfig,
  usersRepository: UsersRepository,
  recordsRepository: RecordsRepository,
  knownFactsRepository: KnownFactsRepository,
  authenticatedSessionsRepository: AuthenticatedSessionsRepository,
  specialCasesRepository: SpecialCasesRepository,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) {

  val interval = Duration(24, "h")
  val now = Instant.now
  val taskDateTime: Instant = LocalDateTime
    .now()
    .withHour(12)
    .withMinute(30)
    .withSecond(0)
    .toInstant(ZoneOffset.UTC)
  val initialDelayMillis = (if (now.isBefore(taskDateTime)) java.time.Duration.between(now, taskDateTime)
                            else java.time.Duration.between(now, taskDateTime.plus(1, ChronoUnit.DAYS))).toMillis
  val initialDelay = Duration(initialDelayMillis, TimeUnit.MILLISECONDS)

  class ClearDatabaseTaskActor(olderThanMilliseconds: Long) extends Actor {

    override val receive: Receive = { case "clear" =>
      clearDatabase(System.currentTimeMillis() - olderThanMilliseconds).recover { case _ => -1 }
    }
  }

  val taskActorRef = actorSystem.actorOf(Props(new ClearDatabaseTaskActor(interval.toMillis)))

  if (appConfig.clearOldMongoDbDocumentsDaily) {
    Logger(getClass).info(
      s"Clear database task will start in ${initialDelay.toMinutes} minutes and will run every ${interval.toHours} hours."
    )
    actorSystem.scheduler.schedule(initialDelay, interval, taskActorRef, "clear")
  } else {
    Logger(getClass).info("Clear database daily task is switched off.")
  }

  def clearDatabase(timestamp: Long): Future[Long] =
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
