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
import uk.gov.hmrc.agentsexternalstubs.support.{AppBaseISpec, MongoDB}
import uk.gov.hmrc.agentsexternalstubs.wiring.ClearDatabase

import scala.concurrent.Future
import scala.util.Random

class ClearDatabaseISpec extends AppBaseISpec with MongoDB with Eventually {

  lazy val clearDatabase = app.injector.instanceOf[ClearDatabase]
  lazy val usersRepo = app.injector.instanceOf[UsersRepositoryMongo]
  lazy val recordsRepo = app.injector.instanceOf[RecordsRepositoryMongo]
  lazy val actorSystem = app.injector.instanceOf[ActorSystem]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(usersRepo.drop)
  }

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

      await(usersRepo.count) should be >= 100
      await(recordsRepo.count) should be >= 200

      await(clearDatabase.clearDatabase(System.currentTimeMillis()))

      await(usersRepo.count) shouldBe 0
      await(recordsRepo.count) shouldBe 0
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

      await(usersRepo.count) should be >= 100
      await(recordsRepo.count) should be >= 200

      actorSystem.actorOf(Props(new clearDatabase.ClearDatabaseTaskActor(0))) ! "clear"

      eventually {
        await(usersRepo.count) shouldBe 0
        await(recordsRepo.count) shouldBe 0
      }
    }
  }
}
