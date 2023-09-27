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

import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class ServicesSpec extends BaseUnitSpec with ValidatedMatchers {

  "Services" should {
    "read services definitions at bootstrap" in {
      Services.services should not be empty
    }

    "serialize services back to json" in {
      val entity = Services(services = Services.services)
      val json = Json.toJson(entity)
      (json \ "services").as[JsArray].value should not be empty
    }

    import org.scalatest.Inspectors._
    "have Enrolment generator and validator" in {
      forAll(Seq("foo", "bar", "baz", "zoo", "zig", "zag", "doc", "dot", "abc", "xyz")) { seed: String =>
        Services.services.foreach { s =>
          val enrolment = Generator.get(s.generator)(seed).get
          Enrolment.validate(enrolment) should be_Valid
        }
      }
    }

    "have knownFacts generator and validator" in {
      forAll(Seq("foo", "bar", "baz", "zoo", "zig", "zag", "doc", "dot", "abc", "xyz")) { seed: String =>
        Services.services.foreach { s =>
          s.knownFacts.foreach { kf =>
            val value = Generator.get(kf.valueGenerator)(seed).get
            kf.validate(value).isRight shouldBe true
          }
        }
      }
    }
  }
}
