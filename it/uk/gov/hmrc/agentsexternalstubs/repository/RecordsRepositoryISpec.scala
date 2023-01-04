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

import org.mongodb.scala.model.Filters

import java.util.UUID
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.LegacyRelationshipRecordsService
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

class RecordsRepositoryISpec extends AppBaseISpec {

  lazy val repo: RecordsRepository = app.injector.instanceOf[RecordsRepository]

  lazy val underlyingRepo: PlayMongoRepository[Record] =
    repo.asInstanceOf[PlayMongoRepository[Record]]

  "RecordsRepository" should {
    "store a RelationshipRecord entities" in {
      val planetId = UUID.randomUUID().toString
      val registration1 = RelationshipRecord(regime = "A", arn = "B1", refNumber = "C1", idType = "D", active = true)
      await(repo.store(registration1, planetId))
      val registration2 = RelationshipRecord(regime = "A", arn = "B2", refNumber = "C2", idType = "D", active = false)
      await(repo.store(registration2, planetId))

      val recordsBefore = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      recordsBefore.size shouldBe 2

      val record1 = recordsBefore.find(_.asInstanceOf[RelationshipRecord].arn == "B1").get
      record1.id should not be empty
      await(repo.store(record1.asInstanceOf[RelationshipRecord].copy(arn = "B3"), planetId))

      val recordsAfter = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      recordsAfter.size shouldBe 2
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B3") should not be empty
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B2") should not be empty
      recordsAfter.find(_.asInstanceOf[RelationshipRecord].arn == "B1") shouldBe empty
    }

    "store a LegacyAgentRecord entities" in {
      val planetId = UUID.randomUUID().toString
      val registration1 = LegacyAgentRecord(agentId = "A1", agentName = "Agent1", address1 = "a11", address2 = "a12")
      await(repo.store(registration1, planetId))

      val registration2 = LegacyAgentRecord(agentId = "A2", agentName = "Agent2", address1 = "a21", address2 = "a22")
      await(repo.store(registration2, planetId))

      val records = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      records.size shouldBe 2
      records.map(_.asInstanceOf[LegacyAgentRecord].agentId) should contain.only("A1", "A2")
    }

    "store a LegacyRelationshipRecord entities" in {
      val planetId = UUID.randomUUID().toString
      val registration1 =
        LegacyRelationshipRecord(nino = Some("A"), utr = Some("U1"), agentId = "1")
      await(repo.store(registration1, planetId))

      val registration2 = LegacyRelationshipRecord(
        nino = Some("B"),
        utr = Some("U2"),
        agentId = "2",
        `Auth_i64-8` = Some(true),
        `Auth_64-8` = Some(true)
      )
      await(repo.store(registration2, planetId))

      val records = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      records.size shouldBe 2
      records.map(_.asInstanceOf[LegacyRelationshipRecord].agentId) should contain.only("1", "2")
      records.map(_.asInstanceOf[LegacyRelationshipRecord].nino.get) should contain.only("A", "B")
      records.map(_.asInstanceOf[LegacyRelationshipRecord].utr.get) should contain.only("U1", "U2")
      records.map(_.asInstanceOf[LegacyRelationshipRecord].`Auth_64-8`) should contain.only(Some(true), None)
      records.map(_.asInstanceOf[LegacyRelationshipRecord].`Auth_i64-8`) should contain.only(Some(true), None)

      val relationshipOpt = await(
        app.injector
          .instanceOf[LegacyRelationshipRecordsService]
          .getLegacyRelationshipByAgentIdAndUtr("2", "U2", planetId)
      )
      relationshipOpt.flatMap(_.utr) shouldBe Some("U2")
      relationshipOpt.flatMap(_.`Auth_64-8`) shouldBe Some(true)
      relationshipOpt.flatMap(_.`Auth_i64-8`) shouldBe Some(true)
    }

    "store a BusinessDetails entities" in {
      val planetId = UUID.randomUUID().toString
      val businessDetails1 = BusinessDetailsRecord.generate("foo")
      await(repo.store(businessDetails1, planetId))

      val records = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      records.size shouldBe 1
    }

    "store a BusinessPartnerRecord entities" in {
      val planetId = UUID.randomUUID().toString
      val businessPartnerRecord = BusinessPartnerRecord.generate("foo")
      await(repo.store(businessPartnerRecord, planetId))

      val records = await(underlyingRepo.collection.find(Filters.equal("_planetId", planetId)).toFuture)
      records.size shouldBe 1
    }

    "find a record by its ID" in {
      val planetId = UUID.randomUUID().toString
      val businessDetails = BusinessDetailsRecord.generate("foo")
      val recordId = await(repo.store(businessDetails, planetId))

      val record = await(repo.findById(recordId, planetId))
      record shouldBe defined
    }

    "remove a record by its ID" in {
      val planetId = UUID.randomUUID().toString
      val businessDetails = BusinessDetailsRecord.generate("foo")
      val recordId = await(repo.store(businessDetails, planetId))

      val record1 = await(repo.findById(recordId, planetId))
      record1 shouldBe defined

      await(repo.remove(recordId, planetId))

      val record2 = await(repo.findById(recordId, planetId))
      record2 shouldBe None
    }
  }
}
