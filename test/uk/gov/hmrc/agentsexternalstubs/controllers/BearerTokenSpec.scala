/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.controllers

import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

class BearerTokenSpec extends BaseUnitSpec {

  "BearerToken" should {
    "extract token when only Bearer supplied" in {
      BearerToken.unapply("Bearer secret") shouldBe Some("secret")
    }

    "extract token when Bearer supplied along with GNAP, with Bearer first" in {
      BearerToken.unapply("Bearer secret, GNAP 12bG") shouldBe Some("secret")
    }

    "extract token when Bearer supplied along with GNAP, with Bearer last" in {
      BearerToken.unapply("GNAP 12bG, Bearer secret") shouldBe Some("secret")
    }

    "return None when supplied value does not match regex" in {
      BearerToken.unapply("something") shouldBe None
    }
  }
}
