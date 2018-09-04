package uk.gov.hmrc.agentsexternalstubs.support
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.{Lock, ReentrantLock}

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import play.api.Application
import uk.gov.hmrc.agentsexternalstubs.repository.{AuthenticatedSessionsRepository, RecordsRepositoryMongo, UsersRepositoryMongo}
import uk.gov.hmrc.mongo.{Awaiting, MongoConnector}

import scala.concurrent.duration.{Duration, _}

trait MongoDB extends BeforeAndAfterAll {
  me: TestSuite =>

  private implicit val timeout: Duration = 5.seconds

  def app: Application

  override def beforeAll(): Unit = {
    super.beforeAll()
    MongoDB.initializeMongo(app)
  }
}

object MongoDB extends Awaiting {

  private val lock: Lock = new ReentrantLock()
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  val databaseName: String = "agents-external-stubs-tests"
  val uri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  def initializeMongo(app: Application): Unit =
    if (lock.tryLock()) try {
      if (!initialized.get()) {
        print("Initializing MongoDB ... ")
        val mongo = MongoConnector(uri).db()
        await(mongo.drop())
        await(app.injector.instanceOf[AuthenticatedSessionsRepository].ensureIndexes)
        await(app.injector.instanceOf[UsersRepositoryMongo].ensureIndexes)
        await(app.injector.instanceOf[RecordsRepositoryMongo].ensureIndexes)
        initialized.set(true)
        println("ready.")
      }
    } finally { lock.unlock() }

}
