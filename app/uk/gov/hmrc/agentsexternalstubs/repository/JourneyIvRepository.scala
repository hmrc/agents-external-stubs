package uk.gov.hmrc.agentsexternalstubs.repository
import java.util.UUID

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.agentsexternalstubs.models.iv_models.{Journey, JourneyCreation}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[JourneyIvRepositoryImpl])
trait JourneyIvRepository {

  val journeyId: String

  def createJourneyId(journeyCreation: JourneyCreation)(implicit ec: ExecutionContext): Future[String]

  def getJourneyInfo(journeyId: String)(implicit ec: ExecutionContext): Future[Option[Journey]]

  def deleteJourneyRecord(journeyId: String)(implicit ec: ExecutionContext): Future[Unit]
}

@Singleton
class JourneyIvRepositoryImpl @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[Journey, BSONObjectID](
      "journeyIv",
      mongoComponent.mongoConnector.db,
      Journey.format,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[Journey, BSONObjectID] with JourneyIvRepository
    with DeleteAll[Journey] {

  override def indexes = Seq(
    Index(Seq(Journey.JOURNEYID -> Ascending), Some("JourneyIdKey"), unique = true)
  )

  override val journeyId: String = UUID.randomUUID().toString

  override def createJourneyId(journeyCreation: JourneyCreation)(implicit ec: ExecutionContext): Future[String] = {

    val journey = Journey(journeyId, journeyCreation.journeyType, journeyCreation.serviceContract)

    insert(journey).map(_ => journeyId)
  }

  override def getJourneyInfo(journeyId: String)(implicit ec: ExecutionContext): Future[Option[Journey]] =
    find("journeyId" -> JsString(journeyId)).map(_.headOption)

  override def deleteJourneyRecord(journeyId: String)(implicit ec: ExecutionContext): Future[Unit] =
    remove("journeyId" -> journeyId).map(_ => ())

  override val UPDATED: String = "_last_updated_at"
}
