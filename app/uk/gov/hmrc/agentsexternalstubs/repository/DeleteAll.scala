package uk.gov.hmrc.agentsexternalstubs.repository

import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

trait DeleteAll[E] {
  self: ReactiveRepository[E, BSONObjectID] =>

  import ImplicitBSONHandlers._

  val UPDATED: String

  def deleteAll(lastUpdatedBefore: Long)(implicit ec: ExecutionContext): Future[Int] =
    collection
      .delete()
      .element(
        q = BSONDocument(UPDATED -> BSONDocument("$lt" -> BSONLong(lastUpdatedBefore))),
        limit = None,
        collation = None)
      .flatMap(e => collection.delete(ordered = false).many(Seq(e)))
      .map(_.n)

}
