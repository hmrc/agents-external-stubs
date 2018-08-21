package uk.gov.hmrc.agentsexternalstubs.services

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RelationshipRecordRecordsServiceISpec extends UnitSpec with OneAppPerSuite with MongoDbPerSuite {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri"   -> mongoUri,
        "proxies.start" -> "false"
      )

  override implicit lazy val app: Application = appBuilder.build()

  val repo = app.injector.instanceOf[RecordsRepository]
  val service = app.injector.instanceOf[RelationshipRecordsService]

  "RelationshipRecordsService" should {
    "findRelationships by regime" in {
      await(repo
        .create(RelationshipRecord(regime = "A", arn = "B1", refNumber = "C1", idType = "D", active = true), "saturn"))
      await(repo
        .create(RelationshipRecord(regime = "A", arn = "B2", refNumber = "C2", idType = "D", active = true), "saturn"))
      await(repo
        .create(RelationshipRecord(regime = "B", arn = "B1", refNumber = "C1", idType = "D", active = true), "saturn"))

      val result = await(service.findByKey("A", "saturn"))
      result.size shouldBe 2
      result.map(_.arn) should contain.only("B1", "B2")

      val result2 = await(service.findByKey("A", "juniper"))
      result2.size shouldBe 0
    }
  }
}
