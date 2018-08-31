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
package uk.gov.hmrc.agentsexternalstubs.repository

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.MongoDbPerTest
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RecordsRepositoryISpec extends UnitSpec with OneAppPerSuite with MongoDbPerTest {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri"   -> mongoUri,
        "proxies.start" -> "false"
      )

  override implicit lazy val app: Application = appBuilder.build()

  lazy val repo: RecordsRepository = app.injector.instanceOf[RecordsRepository]

  lazy val underlyingRepo: ReactiveRepository[Record, BSONObjectID] =
    repo.asInstanceOf[ReactiveRepository[Record, BSONObjectID]]

  "RecordsRepository" should {
    "store a RelationshipRecord entities" in {
      val registration1 = RelationshipRecord(regime = "A", arn = "B1", refNumber = "C1", idType = "D", active = true)
      await(repo.store(registration1, "saturn"))
      val registration2 = RelationshipRecord(regime = "A", arn = "B2", refNumber = "C2", idType = "D", active = false)
      await(repo.store(registration2, "saturn"))

      val recordsBefore = await(underlyingRepo.findAll())
      recordsBefore.size shouldBe 2

      val record1 = recordsBefore.find(_.asInstanceOf[RelationshipRecord].arn == "B1").get
      record1.id should not be empty
      await(repo.store(record1.asInstanceOf[RelationshipRecord].copy(arn = "B3"), "saturn"))

      val recordsAfter = await(underlyingRepo.findAll())
      recordsAfter.size shouldBe 2
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B3") should not be empty
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B2") should not be empty
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B1") shouldBe empty
    }

    "store a LegacyAgentRecord entities" in {
      val registration1 = LegacyAgentRecord(agentId = "A1", agentName = "Agent1", address1 = "a11", address2 = "a12")
      await(repo.store(registration1, "saturn"))

      val registration2 = LegacyAgentRecord(agentId = "A2", agentName = "Agent2", address1 = "a21", address2 = "a22")
      await(repo.store(registration2, "saturn"))

      val records = await(underlyingRepo.findAll())
      records.size shouldBe 2
      records.map(_.asInstanceOf[LegacyAgentRecord].agentId) should contain.only("A1", "A2")
    }

    "store a LegacyRelationshipRecord entities" in {
      val registration1 = LegacyRelationshipRecord(nino = Some("A"), utr = Some("U1"), agentId = "1")
      await(repo.store(registration1, "saturn"))

      val registration2 = LegacyRelationshipRecord(nino = Some("B"), utr = Some("U2"), agentId = "2")
      await(repo.store(registration2, "saturn"))

      val records = await(underlyingRepo.findAll())
      records.size shouldBe 2
      records.map(_.asInstanceOf[LegacyRelationshipRecord].agentId) should contain.only("1", "2")
      records.map(_.asInstanceOf[LegacyRelationshipRecord].nino.get) should contain.only("A", "B")
      records.map(_.asInstanceOf[LegacyRelationshipRecord].utr.get) should contain.only("U1", "U2")
    }

    "store a BusinessDetails entities" in {
      val businessDetails1 = BusinessDetailsRecord.generate("foo")
      await(repo.store(businessDetails1, "saturn"))

      val records = await(underlyingRepo.findAll())
      records.size shouldBe 1
    }
  }
}
