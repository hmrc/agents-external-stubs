/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentsexternalstubs.models.PPTSubscriptionDisplayRecord.LegalEntityDetails.CustomerDetails
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class PPTSubscriptionDisplayRecordSpec extends BaseUnitSpec {

  val seeds: Seq[String] = "foeba".permutations.toSeq

  "CustomerDetails" should {

    "be generated correctly" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = CustomerDetails.generate(seed)
        entity.customerType match {
          case "Individual" =>
            entity.individualDetails shouldBe defined
            entity.organisationDetails shouldBe None
          case "Organisation" =>
            entity.individualDetails shouldBe None
            entity.organisationDetails shouldBe defined
          case _ => fail(s"Unknown customer type ${entity.customerType}")
        }
      }
    }

  }

}
