/*
 * Copyright 2017 HM Revenue & Customs
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
package uk.gov.hmrc.agentsexternalstubs.repository

import akka.actor.{ActorSystem, Props}
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, UserGenerator, VatCustomerInformationRecord}
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec
import uk.gov.hmrc.agentsexternalstubs.wiring.ClearDatabase
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.util.Random

class ClearDatabaseISpec extends AppBaseISpec with Eventually {

  lazy val clearDatabase = app.injector.instanceOf[ClearDatabase]
  lazy val usersRepo = app.injector.instanceOf[UsersRepositoryMongo]
  lazy val recordsRepo = app.injector.instanceOf[RecordsRepositoryMongo]
  lazy val actorSystem = app.injector.instanceOf[ActorSystem]

  "clearDatabase" should {
    "remove all documents older than specified timestamp" in {
      val fixture = Stream
        .continually(Random.nextString(32))
        .take(100)
        .map { seed =>
          for {
            _ <- usersRepo.create(UserGenerator.individual(seed), seed)
            _ <- recordsRepo.store(BusinessPartnerRecord.generate(seed), seed)
            _ <- recordsRepo.store(VatCustomerInformationRecord.generate(seed), seed)
          } yield ()
        }
      await(Future.sequence(fixture))

      await(usersRepo.collection.countDocuments.toFuture) should be >= 100L
      await(recordsRepo.collection.countDocuments.toFuture) should be >= 200L

      await(clearDatabase.clearDatabase(System.currentTimeMillis()))

      await(usersRepo.collection.countDocuments.toFuture) shouldBe 0L
      await(recordsRepo.collection.countDocuments.toFuture) shouldBe 0L
    }

    "run a task actor to clear database" in {
      val fixture = Stream
        .continually(Random.nextString(32))
        .take(100)
        .map { seed =>
          for {
            _ <- usersRepo.create(UserGenerator.individual(seed), seed)
            _ <- recordsRepo.store(BusinessPartnerRecord.generate(seed), seed)
            _ <- recordsRepo.store(VatCustomerInformationRecord.generate(seed), seed)
          } yield ()
        }
      await(Future.sequence(fixture))

      await(usersRepo.collection.countDocuments.toFuture) should be >= 100L
      await(recordsRepo.collection.countDocuments.toFuture) should be >= 200L

      actorSystem.actorOf(Props(new clearDatabase.ClearDatabaseTaskActor(0))) ! "clear"

      eventually {
        await(usersRepo.collection.countDocuments.toFuture) shouldBe 0L
        await(recordsRepo.collection.countDocuments.toFuture) shouldBe 0L
      }
    }
  }
}
