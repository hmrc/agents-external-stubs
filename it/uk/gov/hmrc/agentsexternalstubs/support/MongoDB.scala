package uk.gov.hmrc.agentsexternalstubs.support
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.{Lock, ReentrantLock}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import play.api.{Application, Logging}
import reactivemongo.api.FailoverStrategy
import uk.gov.hmrc.agentsexternalstubs.repository._
import uk.gov.hmrc.mongo.{Awaiting, MongoConnector}

import scala.concurrent.duration._

trait MongoDB extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: TestSuite =>

  def app: Application

  override def beforeAll(): Unit = {
    super.beforeAll()
    MongoDB.initializeMongo(app)
  }

  override def beforeEach() =
    MongoDB.initializeMongo(
      app,
      force = true
    ) // TODO! Is there a quicker way to wipe the db?
}

object MongoDB extends Awaiting with Logging {

  private val lock: Lock = new ReentrantLock()
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  val databaseName: String = "agents-external-stubs-tests"
  val uri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  def initializeMongo(app: Application, force: Boolean = false): Unit =
    if (lock.tryLock()) try if (!initialized.get() || force) {
      logger.debug("Initializing MongoDB ... ")
      val mongo = MongoConnector(
        uri,
        failoverStrategy = Some(FailoverStrategy.default),
        dbTimeout = Some(FiniteDuration.apply(4000, "ms"))
      ).db()
      await(mongo.drop())
      await(app.injector.instanceOf[AuthenticatedSessionsRepository].ensureIndexes)
      await(app.injector.instanceOf[UsersRepositoryMongo].ensureIndexes)
      await(app.injector.instanceOf[GroupsRepositoryMongo].ensureIndexes)
      await(app.injector.instanceOf[RecordsRepositoryMongo].ensureIndexes)
      await(app.injector.instanceOf[KnownFactsRepositoryMongo].ensureIndexes)
      await(app.injector.instanceOf[SpecialCasesRepositoryMongo].ensureIndexes)
      initialized.set(true)
      logger.debug("MongoDB ready.")
    } finally lock.unlock()
}
