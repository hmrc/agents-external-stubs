/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, CursorProducer, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.{Record, RelationshipRecord}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@ImplementedBy(classOf[RecordsRepositoryMongo])
trait RecordsRepository {

  def create(entity: Record, planetId: String)(implicit ec: ExecutionContext): Future[Either[Option[Int], Int]]
  def cursor[T: Projection](key: String, planetId: String)(implicit reads: Reads[T], ec: ExecutionContext): Cursor[T]
}

@Singleton
class RecordsRepositoryMongo @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[Record, BSONObjectID](
      "records",
      mongoComponent.mongoConnector.db,
      Record.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[Record, BSONObjectID] with RecordsRepository {

  import ImplicitBSONHandlers._

  private final val PLANET_ID = "planetId"

  override def indexes =
    Seq(Index(Seq(Record.KEYS -> Ascending, PLANET_ID -> Ascending), Some("Keys")))

  override def cursor[T: Projection](key: String, planetId: String)(
    implicit reads: Reads[T],
    ec: ExecutionContext): Cursor[T] =
    collection
      .find(
        JsObject(Seq(Record.KEYS -> JsString(key), PLANET_ID -> JsString(planetId))),
        Json.obj(implicitly[Projection[T]].fieldNames.map(option => option -> toJsFieldJsValueWrapper(JsNumber(1))): _*)
      )
      .cursor[T](ReadPreference.primaryPreferred)(
        implicitly[collection.pack.Reader[T]],
        ec,
        implicitly[CursorProducer[T]])

  override def create(entity: Record, planetId: String)(
    implicit ec: ExecutionContext): Future[Either[Option[Int], Int]] = {
    val json = Json.toJson[Record](entity).as[JsObject].+(PLANET_ID -> JsString(planetId))
    collection
      .insert(json)
      .map(r => if (!r.ok || r.code.isDefined) Left(r.code) else Right(r.n))
  }
}

trait Projection[T] {
  val fieldNames: Seq[String]
}

object Projection {

  def apply[T](implicit classTag: ClassTag[T]): Projection[T] = {
    val properties = classTag.runtimeClass.getDeclaredFields.map(_.getName)
    new Projection[T] { override val fieldNames: Seq[String] = properties }
  }

  implicit val registration: Projection[RelationshipRecord] = Projection[RelationshipRecord]

}
