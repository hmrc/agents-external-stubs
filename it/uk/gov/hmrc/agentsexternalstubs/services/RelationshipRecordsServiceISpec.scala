package uk.gov.hmrc.agentsexternalstubs.services

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.support._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RelationshipRecordsServiceISpec extends UnitSpec with OneAppPerSuite with MongoDbPerSuite {

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
    "find relationships by key" in {
      await(
        repo
          .store(RelationshipRecord(regime = "A", arn = "B1", refNumber = "C1", idType = "D", active = true), "saturn"))
      await(
        repo
          .store(RelationshipRecord(regime = "A", arn = "B2", refNumber = "C2", idType = "D", active = true), "saturn"))
      await(
        repo
          .store(RelationshipRecord(regime = "B", arn = "B1", refNumber = "C1", idType = "D", active = true), "saturn"))

      val result = await(service.findByKey("A", "saturn"))
      result.size shouldBe 2
      result.map(_.arn) should contain.only("B1", "B2")
      result.head.id shouldBe defined

      await(service.findByKey("A", "juniper")).size shouldBe 0

      await(service.findByKey(RelationshipRecord.agentKey("A", "B1"), "saturn")).size shouldBe 1
      await(service.findByKey(RelationshipRecord.clientKey("A", "D", "C2"), "saturn")).size shouldBe 1
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "saturn")).size shouldBe 1

      await(service.findByKey(RelationshipRecord.agentKey("B", "B2"), "saturn")).size shouldBe 0
      await(service.findByKey(RelationshipRecord.clientKey("B", "D", "C2"), "saturn")).size shouldBe 0
      await(service.findByKey(RelationshipRecord.fullKey("B", "B2", "D", "C2"), "saturn")).size shouldBe 0
    }

    "authorise new relationship" in {
      await(service.authorise(RelationshipRecord("A", "B1", "D", "C1"), "juniper"))
      val records = await(service.findByKey(RelationshipRecord.fullKey("A", "B1", "D", "C1"), "juniper"))
      records.size shouldBe 1
      records.head.active shouldBe true
      records.head.startDate shouldBe defined
    }

    "authorise new relationship and deactivate existing one" in {
      await(service.authorise(RelationshipRecord("A", "B2", "D", "C2"), "mars"))
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(service.authorise(RelationshipRecord("A", "B3", "D", "C2"), "mars"))

      val oldRecords = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars"))
      oldRecords.size shouldBe 1
      oldRecords.head.active shouldBe false
      oldRecords.head.endDate shouldBe defined

      val newRecord = await(service.findByKey(RelationshipRecord.fullKey("A", "B3", "D", "C2"), "mars"))
      newRecord.size shouldBe 1
      newRecord.head.active shouldBe true
      newRecord.head.startDate shouldBe defined
      newRecord.head.endDate should not be defined

      val allRecords = await(service.findByKey("A", "mars"))
      allRecords.size shouldBe 2
    }

    "de-authorise an existing relationship" in {
      await(service.authorise(RelationshipRecord("A", "B2", "D", "C2"), "venus"))
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "venus")).size shouldBe 1

      await(service.deAuthorise(RelationshipRecord("A", "B2", "D", "C2"), "venus"))
      val newRecord = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "venus"))
      newRecord.size shouldBe 1
      newRecord.head.active shouldBe false
      newRecord.head.startDate shouldBe defined
      newRecord.head.endDate shouldBe defined

      val allRecords = await(service.findByKey("A", "venus"))
      allRecords.size shouldBe 1
    }

    "not fail when de-authorising missing relationship" in {
      await(service.deAuthorise(RelationshipRecord("A", "B2", "D", "C2"), "mercury"))

      val allRecords = await(service.findByKey("A", "mercury"))
      allRecords.size shouldBe 0
    }

    "find relationships by query" in {
      await(service.authorise(RelationshipRecord("R3", "A1", "D", "C3"), "pluto"))
      await(service.authorise(RelationshipRecord("R1", "A1", "D", "C1"), "pluto"))
      await(service.authorise(RelationshipRecord("R1", "A1", "D", "C2"), "pluto"))
      await(service.authorise(RelationshipRecord("R1", "A1", "D", "C3"), "pluto"))
      await(service.authorise(RelationshipRecord("R2", "A1", "D", "C1"), "pluto"))
      await(service.authorise(RelationshipRecord("R1", "A2", "D", "C1"), "pluto")) //replaces #2

      val allOfAgent = await(
        service.findByQuery(
          RelationshipRecordQuery(regime = "R1", agent = true, arn = Some("A1"), idType = "D", activeOnly = false),
          "pluto"))
      allOfAgent.size shouldBe 3

      val activeOfAgent = await(
        service.findByQuery(
          RelationshipRecordQuery(regime = "R1", agent = true, arn = Some("A1"), idType = "D", activeOnly = true),
          "pluto"))
      activeOfAgent.size shouldBe 2
      activeOfAgent.map(_.refNumber) should contain.only("C2", "C3")

      val allOfClient = await(service.findByQuery(
        RelationshipRecordQuery(regime = "R1", agent = false, refNumber = Some("C1"), idType = "D", activeOnly = false),
        "pluto"))
      allOfClient.size shouldBe 2
      allOfClient.map(_.arn) should contain.only("A1", "A2")

      val activeOfClient = await(service.findByQuery(
        RelationshipRecordQuery(regime = "R1", agent = false, refNumber = Some("C1"), idType = "D", activeOnly = true),
        "pluto"))
      activeOfClient.size shouldBe 1
      activeOfClient.map(_.arn) should contain.only("A2")
    }

    "find relationships by query with dates" in {
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C1", active = true, startDate = Some(LocalDate.parse("2002-01-01"))),
          "uranus"))
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C2", active = true, startDate = Some(LocalDate.parse("2002-06-15"))),
          "uranus"))
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C3", active = false, startDate = Some(LocalDate.parse("2001-05-15"))),
          "uranus"))
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C4", active = true, startDate = Some(LocalDate.parse("2000-01-31"))),
          "uranus"))

      val recordsAfter = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = true,
            arn = Some("A1"),
            idType = "E",
            activeOnly = false,
            from = Some(LocalDate.parse("2002-01-02"))),
          "uranus"))
      recordsAfter.size shouldBe 1
      recordsAfter.map(_.refNumber) should contain.only("C2")

      val recordsBetween = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = true,
            arn = Some("A1"),
            idType = "E",
            activeOnly = false,
            from = Some(LocalDate.parse("2001-05-15")),
            to = Some(LocalDate.parse("2002-01-01"))),
          "uranus"
        ))
      recordsBetween.size shouldBe 2
      recordsBetween.map(_.refNumber) should contain.only("C1", "C3")

      val allActiveIgnoreDates = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = true,
            arn = Some("A1"),
            idType = "E",
            activeOnly = true,
            from = Some(LocalDate.parse("2001-05-15")),
            to = Some(LocalDate.parse("2002-01-01"))),
          "uranus"
        ))
      allActiveIgnoreDates.size shouldBe 3
      allActiveIgnoreDates.map(_.refNumber) should contain.only("C1", "C2", "C4")
    }
  }
}
