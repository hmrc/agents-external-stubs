package uk.gov.hmrc.agentsexternalstubs.support
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

  override def beforeEach() = {
    super.beforeEach()
    MongoDB.initializeMongo(
      app,
      force = true
    ) // TODO - This slows down the tests. Is there a quicker way to wipe the db?
  }
}

object MongoDB extends Logging {

  private val lock: Lock = new ReentrantLock()
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  val databaseName: String = "agents-external-stubs-tests"
  val uri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  def initializeMongo(app: Application, force: Boolean = false): Unit =
    if (lock.tryLock()) try if (!initialized.get() || force) {
      logger.debug("Initializing MongoDB ... ")
      val mongoComponent: MongoComponent = MongoComponent(uri)
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
}
