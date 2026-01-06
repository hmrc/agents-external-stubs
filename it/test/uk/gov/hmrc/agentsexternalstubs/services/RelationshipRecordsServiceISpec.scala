/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.services

import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.support._

import java.time.LocalDate

class RelationshipRecordsServiceISpec extends AppBaseISpec {

  lazy val repo = app.injector.instanceOf[RecordsRepository]
  lazy val service = app.injector.instanceOf[RelationshipRecordsService]

  "RelationshipRecordsService" should {
    "find relationships by key" in {
      await(
        repo
          .store(RelationshipRecord(regime = "A", arn = "B1", refNumber = "C1", idType = "D", active = true), "saturn")
      )
      await(
        repo
          .store(RelationshipRecord(regime = "A", arn = "B2", refNumber = "C2", idType = "D", active = true), "saturn")
      )
      await(
        repo
          .store(
            RelationshipRecord(
              regime = "B",
              arn = "B1",
              refNumber = "C1",
              idType = "D",
              active = true,
              authProfile = Some("ITSAS001")
            ),
            "saturn"
          )
      )

      val result = await(service.findByKey("A", "saturn"))
      result.size shouldBe 2
      result.map(_.arn) should contain.only("B1", "B2")
      result.head.id shouldBe defined

      await(service.findByKey("A", "juniper")).size shouldBe 0

      await(service.findByKey(RelationshipRecord.agentKey("A", "B1"), "saturn")).size shouldBe 1
      await(service.findByKey(RelationshipRecord.clientKey("A", "D", "C2"), "saturn")).size shouldBe 1
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "saturn")).size shouldBe 1
      await(
        service.findByKey(RelationshipRecord.clientWithAuthProfileKey("A", "D", "C2", "ALL00001"), "saturn")
      ).size shouldBe 1
      await(
        service.findByKey(RelationshipRecord.clientWithAuthProfileKey("B", "D", "C1", "ITSAS001"), "saturn")
      ).size shouldBe 1

      await(service.findByKey(RelationshipRecord.agentKey("B", "B2"), "saturn")).size shouldBe 0
      await(service.findByKey(RelationshipRecord.clientKey("B", "D", "C2"), "saturn")).size shouldBe 0
      await(service.findByKey(RelationshipRecord.fullKey("B", "B2", "D", "C2"), "saturn")).size shouldBe 0
      await(
        service.findByKey(RelationshipRecord.clientWithAuthProfileKey("A", "D", "C2", "ALL00002"), "saturn")
      ).size shouldBe 0
    }

    "authorise new relationship" in {
      await(service.authorise(RelationshipRecord("A", "B1", "D", "C1"), "juniper"))
      val records = await(service.findByKey(RelationshipRecord.fullKey("A", "B1", "D", "C1"), "juniper"))
      records.size shouldBe 1
      records.head.active shouldBe true
      records.head.startDate shouldBe defined
    }

    "authorise new main relationship and deactivate existing main relationship" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          "mars",
          isExclusiveAgent = true
        )
      )

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

    "authorise new main relationship with authProfile None and deactivate existing main relationship" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = None),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )

      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          "mars",
          isExclusiveAgent = true
        )
      )

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

    "authorise new main relationship and deactivate existing main relationship with authProfile None" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )

      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = None),
          "mars",
          isExclusiveAgent = true
        )
      )

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

    "authorise new main relationship with authProfile None and deactivate existing main relationship with authProfile None" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = None),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )

      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = None),
          "mars",
          isExclusiveAgent = true
        )
      )

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

    "authorise new supp relationship and do not deactivate existing supp relationship" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = Some("ITSAS001")),
          planetId = "mars",
          isExclusiveAgent = false
        )
      )
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ITSAS001")),
          "mars",
          isExclusiveAgent = false
        )
      )

      val oldRecords = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars"))
      oldRecords.size shouldBe 1
      oldRecords.head.active shouldBe true
      oldRecords.head.endDate should not be defined

      val newRecord = await(service.findByKey(RelationshipRecord.fullKey("A", "B3", "D", "C2"), "mars"))
      newRecord.size shouldBe 1
      newRecord.head.active shouldBe true
      newRecord.head.startDate shouldBe defined
      newRecord.head.endDate should not be defined

      val allRecords = await(service.findByKey("A", "mars"))
      allRecords.size shouldBe 2
    }

    "authorise new supp relationship and do not deactivate existing main  relationship" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ITSAS001")),
          "mars",
          isExclusiveAgent = false
        )
      )

      val oldRecords = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars"))
      oldRecords.size shouldBe 1
      oldRecords.head.active shouldBe true
      oldRecords.head.startDate shouldBe defined
      oldRecords.head.endDate should not be defined

      val newRecord = await(service.findByKey(RelationshipRecord.fullKey("A", "B3", "D", "C2"), "mars"))
      newRecord.size shouldBe 1
      newRecord.head.active shouldBe true
      newRecord.head.startDate shouldBe defined
      newRecord.head.endDate should not be defined

      val allRecords = await(service.findByKey("A", "mars"))
      allRecords.size shouldBe 2
    }

    "authorise new supp relationship and do not deactivate existing main  relationship with AuthProfile None" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = None),
          planetId = "mars",
          isExclusiveAgent = true
        )
      )
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ITSAS001")),
          "mars",
          isExclusiveAgent = false
        )
      )

      val oldRecords = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars"))
      oldRecords.size shouldBe 1
      oldRecords.head.active shouldBe true
      oldRecords.head.startDate shouldBe defined
      oldRecords.head.endDate should not be defined

      val newRecord = await(service.findByKey(RelationshipRecord.fullKey("A", "B3", "D", "C2"), "mars"))
      newRecord.size shouldBe 1
      newRecord.head.active shouldBe true
      newRecord.head.startDate shouldBe defined
      newRecord.head.endDate should not be defined

      val allRecords = await(service.findByKey("A", "mars"))
      allRecords.size shouldBe 2
    }

    "authorise new main relationship and do not deactivate existing supp relationship" in {
      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B2", idType = "D", refNumber = "C2", authProfile = Some("ITSAS001")),
          planetId = "mars",
          isExclusiveAgent = false
        )
      )
      await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars")).size shouldBe 1

      await(
        service.authorise(
          RelationshipRecord(regime = "A", arn = "B3", idType = "D", refNumber = "C2", authProfile = Some("ALL00001")),
          "mars",
          isExclusiveAgent = true
        )
      )

      val oldRecords = await(service.findByKey(RelationshipRecord.fullKey("A", "B2", "D", "C2"), "mars"))
      oldRecords.size shouldBe 1
      oldRecords.head.active shouldBe true
      oldRecords.head.startDate shouldBe defined
      oldRecords.head.endDate should not be defined

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

    "only de-authorise relationships that match the supplied authProfile" in {
      // Create an active relationship for authProfile ALL00001
      await(
        service.authorise(
          RelationshipRecord(
            regime = "A",
            arn = "B9",
            idType = "D",
            refNumber = "C9",
            authProfile = Some("ALL00001")
          ),
          planetId = "neptune",
          isExclusiveAgent = true
        )
      )

      val before = await(service.findByKey(RelationshipRecord.fullKey("A", "B9", "D", "C9"), "neptune"))
      before.size shouldBe 1
      before.head.active shouldBe true

      // De-authorise with a mismatched authProfile – nothing should change
      val mismatchResult = await(
        service.deAuthorise(
          RelationshipRecord(
            regime = "A",
            arn = "B9",
            idType = "D",
            refNumber = "C9",
            authProfile = Some("ITSAS001")
          ),
          "neptune"
        )
      )
      mismatchResult shouldBe empty

      val afterMismatch = await(service.findByKey(RelationshipRecord.fullKey("A", "B9", "D", "C9"), "neptune"))
      afterMismatch.size shouldBe 1
      afterMismatch.head.active shouldBe true

      // De-authorise with the correct authProfile – relationship should be ended
      val matchResult = await(
        service.deAuthorise(
          RelationshipRecord(
            regime = "A",
            arn = "B9",
            idType = "D",
            refNumber = "C9",
            authProfile = Some("ALL00001")
          ),
          "neptune"
        )
      )
      matchResult should not be empty

      val afterMatch = await(service.findByKey(RelationshipRecord.fullKey("A", "B9", "D", "C9"), "neptune"))
      afterMatch.size shouldBe 1
      afterMatch.head.active shouldBe false
      afterMatch.head.endDate shouldBe defined
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
          "pluto"
        )
      )
      allOfAgent.size shouldBe 3

      val activeOfAgent = await(
        service.findByQuery(
          RelationshipRecordQuery(regime = "R1", agent = true, arn = Some("A1"), idType = "D", activeOnly = true),
          "pluto"
        )
      )
      activeOfAgent.size shouldBe 2
      activeOfAgent.map(_.refNumber) should contain.only("C2", "C3")

      val allOfClient = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = false,
            refNumber = Some("C1"),
            idType = "D",
            activeOnly = false
          ),
          "pluto"
        )
      )
      allOfClient.size shouldBe 2
      allOfClient.map(_.arn) should contain.only("A1", "A2")

      val activeOfClient = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = false,
            refNumber = Some("C1"),
            idType = "D",
            activeOnly = true
          ),
          "pluto"
        )
      )
      activeOfClient.size shouldBe 1
      activeOfClient.map(_.arn) should contain.only("A2")
    }

    "find relationships by query with dates" in {
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C1", active = true, startDate = Some(LocalDate.parse("2002-01-01"))),
          "uranus"
        )
      )
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C2", active = true, startDate = Some(LocalDate.parse("2002-06-15"))),
          "uranus"
        )
      )
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C3", active = false, startDate = Some(LocalDate.parse("2001-05-15"))),
          "uranus"
        )
      )
      await(
        repo.store(
          RelationshipRecord("R1", "A1", "E", "C4", active = true, startDate = Some(LocalDate.parse("2000-01-31"))),
          "uranus"
        )
      )

      val recordsAfter = await(
        service.findByQuery(
          RelationshipRecordQuery(
            regime = "R1",
            agent = true,
            arn = Some("A1"),
            idType = "E",
            activeOnly = false,
            from = Some(LocalDate.parse("2002-01-02"))
          ),
          "uranus"
        )
      )
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
            to = Some(LocalDate.parse("2002-01-01"))
          ),
          "uranus"
        )
      )
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
            to = Some(LocalDate.parse("2002-01-01"))
          ),
          "uranus"
        )
      )
      allActiveIgnoreDates.size shouldBe 3
      allActiveIgnoreDates.map(_.refNumber) should contain.only("C1", "C2", "C4")
    }
  }
}
