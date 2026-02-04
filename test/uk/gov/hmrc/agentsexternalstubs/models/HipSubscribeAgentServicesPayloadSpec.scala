/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HipSubscribeAgentServicesPayloadSpec extends AnyFreeSpec with Matchers {

  private val requestCouldNotBeProcessed = "Request could not be processed"

  private val minimumValidPayload = HipSubscribeAgentServicesPayload(
    name = "Moneypenny",
    addr1 = "River House",
    addr2 = None,
    addr3 = None,
    addr4 = None,
    postcode = None,
    country = "UK",
    phone = None,
    email = "miss.moneypenny@mi6.co.uk",
    supervisoryBody = None,
    membershipNumber = None,
    evidenceObjectReference = None,
    updateDetailsStatus = "ACCEPTED",
    amlSupervisionUpdateStatus = "ACCEPTED",
    directorPartnerUpdateStatus = "PENDING",
    acceptNewTermsStatus = "REQUIRED",
    reriskStatus = "REJECTED"
  )

  private val fullValidPayload = minimumValidPayload.copy(
    addr2 = Some("The Thames"),
    addr3 = Some("Whitehall"),
    addr4 = Some("London"),
    phone = Some("0123456789"),
    supervisoryBody = Some("Mi6"),
    membershipNumber = Some("1"),
    evidenceObjectReference = Some("ref")
  )

  private def expectInvalid(payload: HipSubscribeAgentServicesPayload): Errors =
    HipSubscribeAgentServicesPayload
      .validateCreateAgentSubscriptionPayload(payload)
      .swap
      .getOrElse(fail("Expected Left(Errors) but got Right(...)"))

  "validateCreateAgentSubscriptionPayload" - {

    "a minimum payload" - {

      "return an Either Right when valid" in {
        val result =
          HipSubscribeAgentServicesPayload.validateCreateAgentSubscriptionPayload(minimumValidPayload)

        result mustBe Right(minimumValidPayload)
      }

      "return Either Left with errors because a required field is too short" in {
        val payload = minimumValidPayload.copy(name = "")
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors because a required field is too long" in {
        val payload = minimumValidPayload.copy(addr1 = "Field name that is way too long to be an address line 1")
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors when multiple required fields are incorrect" in {
        val payload =
          minimumValidPayload.copy(
            name = "",
            addr1 = "Field name that is way too long to be an address line 1"
          )

        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors when an enum field is incorrect" in {
        val payload = minimumValidPayload.copy(updateDetailsStatus = "NOT_A_VALID_VALUE")
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }
    }

    "a full payload" - {

      "return an Either Right when valid" in {
        val result =
          HipSubscribeAgentServicesPayload.validateCreateAgentSubscriptionPayload(fullValidPayload)

        result mustBe Right(fullValidPayload)
      }

      "return Either Left with errors because an optional field is too short" in {
        val payload = minimumValidPayload.copy(addr2 = Some(""))
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors because a optional field is too long" in {
        val payload = minimumValidPayload.copy(addr3 = Some("Field name that is way too long to be an address line 3"))
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors when multiple optional fields are incorrect" in {
        val payload = minimumValidPayload.copy(
          addr4 = Some(""),
          postcode = Some("Field name that is way too long to be a postcode")
        )

        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }
    }
  }
}
