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

class HipAmendAgentSubscriptionPayloadSpec extends AnyFreeSpec with Matchers {

  private val requestCouldNotBeProcessed = "Request could not be processed"

  private val emptyPayload = HipAmendAgentSubscriptionPayload(
    name = None,
    addr1 = None,
    addr2 = None,
    addr3 = None,
    addr4 = None,
    postcode = None,
    country = None,
    phone = None,
    email = None,
    supervisoryBody = None,
    membershipNumber = None,
    evidenceObjectReference = None,
    updateDetailsStatus = None,
    amlSupervisionUpdateStatus = None,
    directorPartnerUpdateStatus = None,
    acceptNewTermsStatus = None,
    reriskStatus = None
  )

  private val fullValidPayload = HipAmendAgentSubscriptionPayload(
    name = Some("Moneypenny"),
    addr1 = Some("River House"),
    addr2 = Some("The Thames"),
    addr3 = Some("Whitehall"),
    addr4 = Some("London"),
    postcode = Some("SW1A1AA"),
    country = Some("UK"),
    phone = Some("0123456789"),
    email = Some("miss.moneypenny@mi6.co.uk"),
    supervisoryBody = Some("Mi6"),
    membershipNumber = Some("1"),
    evidenceObjectReference = Some("ref"),
    updateDetailsStatus = Some("ACCEPTED"),
    amlSupervisionUpdateStatus = Some("PENDING"),
    directorPartnerUpdateStatus = Some("REQUIRED"),
    acceptNewTermsStatus = Some("REJECTED"),
    reriskStatus = Some("ACCEPTED")
  )

  private def expectInvalid(payload: HipAmendAgentSubscriptionPayload): Errors =
    HipAmendAgentSubscriptionPayload
      .validateAmendAgentSubscriptionPayload(payload)
      .swap
      .getOrElse(fail("Expected Left(Errors) but got Right(...)"))

  "validateAmendAgentSubscriptionPayload" - {

    "an empty payload" - {

      "return an Either Right when valid" in {
        val result =
          HipAmendAgentSubscriptionPayload.validateAmendAgentSubscriptionPayload(emptyPayload)

        result mustBe Right(emptyPayload)
      }
    }

    "a full payload" - {

      "return an Either Right when valid" in {
        val result =
          HipAmendAgentSubscriptionPayload.validateAmendAgentSubscriptionPayload(fullValidPayload)

        result mustBe Right(fullValidPayload)
      }

      "return Either Left with errors because an optional field is too short" in {
        val payload = emptyPayload.copy(name = Some(""))
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors because an optional field is too long" in {
        val payload =
          emptyPayload.copy(addr1 = Some("Field name that is way too long to be an address line 1"))
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors when multiple optional fields are incorrect" in {
        val payload = emptyPayload.copy(
          postcode = Some("Field name that is way too long to be a postcode"),
          phone = Some("")
        )

        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }

      "return Either Left with errors when an enum field is incorrect" in {
        val payload = emptyPayload.copy(updateDetailsStatus = Some("NOT_A_VALID_VALUE"))
        val result = expectInvalid(payload)

        result.text mustBe requestCouldNotBeProcessed
        result.code mustBe "003"
      }
    }
  }
}
