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

package uk.gov.hmrc.agentsexternalstubs.support

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.{Lock, ReentrantLock}

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import play.api.Application
import uk.gov.hmrc.agentsexternalstubs.repository.{AuthenticatedSessionsRepository, RecordsRepositoryMongo, UsersRepositoryMongo}
import uk.gov.hmrc.mongo.{Awaiting, MongoSpecSupport}

import scala.concurrent.duration.{Duration, _}

trait MongoDbPerSuite extends BeforeAndAfterAll {
  me: TestSuite =>

  private implicit val timeout: Duration = 5.seconds

  def app: Application

  override def beforeAll(): Unit = {
    super.beforeAll()
    Mongo.initializeMongo(app)
  }
}

object Mongo extends MongoSpecSupport with Awaiting {

  private val lock: Lock = new ReentrantLock()
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  override protected val databaseName: String = "agents-external-stubs-tests"

  val uri: String = mongoUri

  def initializeMongo(app: Application): Unit =
    if (lock.tryLock()) try {
      if (!initialized.get()) {
        println("Initializing MongoDB ...")
        await(mongo().drop())
        await(app.injector.instanceOf[AuthenticatedSessionsRepository].ensureIndexes)
        await(app.injector.instanceOf[UsersRepositoryMongo].ensureIndexes)
        await(app.injector.instanceOf[RecordsRepositoryMongo].ensureIndexes)
        initialized.set(true)
        println("Initialized.")
        mongoUri
      }
    } finally { lock.unlock() }

}
