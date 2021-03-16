/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import org.scalatest.Inspectors
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers
import uk.gov.hmrc.play.test.UnitSpec

class RecordSpec extends UnitSpec with ScalaCheckPropertyChecks with ValidatedMatchers {

  val seeds = "foeba".permutations.toSeq

  val registrationEntity =
    RelationshipRecord(
      regime = "ITSA",
      arn = "ZARN1234567",
      refNumber = "012345678901234",
      idType = "none",
      active = true,
      id = Some("abc")
    )

  implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

  val registrationJson =
    """{"regime":"ITSA","arn":"ZARN1234567","idType":"none","refNumber":"012345678901234","active":true,"_record_type":"RelationshipRecord","_id":{"$oid":"abc"}}"""

  "Record" should {
    "have json writes for RelationshipRecord" in {
      Json
        .toJson[Record](registrationEntity)
        .toString() should (include("regime") and include("arn") and include("idType") and include(
        "refNumber"
      ) and include("active") and include(Record.ID))
    }

    "have json reads for RelationshipRecord" in {
      Json.parse(registrationJson).as[Record] shouldBe registrationEntity
    }
  }

  "RelationshipRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = RelationshipRecord.generate(seed)
        RelationshipRecord.validate(entity) should beValid
      }
    }
  }

  "BusinessDetailsRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = BusinessDetailsRecord.generate(seed)
        BusinessDetailsRecord.validate(entity) should beValid
      }
    }
  }

  "LegacyAgentRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = LegacyAgentRecord.generate(seed)
        LegacyAgentRecord.validate(entity) should beValid
      }
    }
  }

  "LegacyRelationshipRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = LegacyRelationshipRecord.generate(seed)
        LegacyRelationshipRecord.validate(entity) should beValid
      }
    }
  }

  "VatCustomerInformationRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = VatCustomerInformationRecord.generate(seed)
        VatCustomerInformationRecord.validate(entity) should beValid
      }
    }
  }

  "GetBusinessPartnerRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = BusinessPartnerRecord.generate(seed)
        BusinessPartnerRecord.validate(entity) should beValid
      }
    }
  }

  "EmployerAuths" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = EmployerAuths.generate(seed)
        EmployerAuths.validate(entity) should beValid
      }
    }
  }

}
