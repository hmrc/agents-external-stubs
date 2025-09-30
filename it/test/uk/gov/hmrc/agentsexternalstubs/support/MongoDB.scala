/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import play.api.{Application, Logging}
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait MongoDB extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: TestSuite =>

  def app: Application

  override def beforeAll(): Unit = {
    super.beforeAll()
    MongoDB.initializeMongo(app)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    MongoDB.deleteAllDocumentsFromAllCollections()
  }
}

object MongoDB extends Logging {

  private val lock: Lock = new ReentrantLock()
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  val databaseName: String = "agents-external-stubs-tests"
  val uri: String = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mongoComponent: MongoComponent = MongoComponent(uri)

  def initializeMongo(app: Application, force: Boolean = false): Unit =
    if (lock.tryLock()) try if (!initialized.get() || force) {
      logger.debug("Initializing MongoDB ... ")
      Await.result(mongoComponent.database.drop().toFuture, Duration("10s"))
      val repos: Seq[PlayMongoRepository[_]] = Seq(
        app.injector.instanceOf[AuthenticatedSessionsRepository],
        app.injector.instanceOf[UsersRepositoryMongo],
        app.injector.instanceOf[GroupsRepositoryMongo],
        app.injector.instanceOf[RecordsRepositoryMongo],
        app.injector.instanceOf[KnownFactsRepositoryMongo],
        app.injector.instanceOf[SpecialCasesRepositoryMongo]
      )
      Await.result(Future.sequence(repos.map(_.ensureIndexes)), Duration("10s"))
      initialized.set(true)
      logger.debug("MongoDB ready.")
    } finally lock.unlock()

  def deleteAllDocumentsFromAllCollections(): Unit = {
    val collectionNames = Seq("authenticated-sessions", "groups", "knownFacts", "records", "specialCases", "users")
    collectionNames.foreach { collectionName =>
      Await.result(
        mongoComponent.database
          .getCollection(collectionName)
          .deleteMany(BsonDocument())
          .toFuture(),
        Duration("10s")
      )
    }
  }
}
