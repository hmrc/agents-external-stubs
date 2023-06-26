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

import org.scalacheck.Gen
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec

import java.util.UUID

class KnownFactsRepositoryISpec extends AppBaseISpec {

  lazy val repo: KnownFactsRepositoryMongo = app.injector.instanceOf[KnownFactsRepositoryMongo]

  val p: Gen[String] = Generator.pattern("9999999999")

  "KnownFactsRepository" should {
    "create or update an entity" in {
      val planetId = UUID.randomUUID().toString
      val knownFacts1: KnownFacts =
        KnownFacts(
          enrolmentKey = EnrolmentKey("IR-SA~UTR~1234567890"),
          identifiers = Seq(Identifier("UTR", "1234567890")),
          verifiers = Seq(KnownFact("Postcode", "TF2 6NU"), KnownFact("NINO", "AB123456X")),
          planetId = Some(planetId)
        )
      await(repo.upsert(knownFacts1, planetId))

      val result1 =
        await(repo.findByEnrolmentKey(enrolmentKey = EnrolmentKey("IR-SA~UTR~1234567890"), planetId))
      result1 shouldBe Some(knownFacts1)

      val knownFacts2: KnownFacts =
        KnownFacts(
          enrolmentKey = EnrolmentKey("IR-SA~UTR~1234567890"),
          identifiers = Seq(Identifier("UTR", "1234567890")),
          verifiers = Seq(KnownFact("NINO", "AB123654X")),
          planetId = Some(planetId)
        )
      await(repo.upsert(knownFacts2, planetId))

      val result2 =
        await(repo.findByEnrolmentKey(enrolmentKey = EnrolmentKey("IR-SA~UTR~1234567890"), planetId))
      result2 shouldBe Some(knownFacts2)
    }

    "throw an exception if entity has unknown verifier" in {
      val planetId = UUID.randomUUID().toString
      val knownFacts: KnownFacts =
        KnownFacts(
          enrolmentKey = EnrolmentKey("IR-SA~UTR~1234567890"),
          identifiers = Seq(Identifier("UTR", "1234567890")),
          verifiers = Seq(KnownFact("DateOfBirth", "2000-01-01"), KnownFact("NINO", "AB123456X")),
          planetId = Some(planetId)
        )

      an[Exception] shouldBe thrownBy {
        await(repo.upsert(knownFacts, planetId))
      }
    }
  }
}
