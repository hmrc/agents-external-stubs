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
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

final class AgentSubscriptionDisplayResponseFormatSpec extends AnyFreeSpec with Matchers {
  val updateDetailsLastUpdatedStr = "2026-02-05T10:11:12"
  val updateDetailsLastSuccessfullyCompleteStr = "2026-02-05T10:11:13"
  val updateDetails = UpdateDetailsStatus(
    status = AgencyDetailsStatusValue.Accepted,
    lastUpdated = LocalDateTime.parse(updateDetailsLastUpdatedStr),
    lastSuccessfullyCompleted = LocalDateTime.parse(updateDetailsLastSuccessfullyCompleteStr)
  )

  val aml = AmlSupervisionUpdateStatus(
    status = AgencyDetailsStatusValue.Accepted,
    lastUpdated = LocalDateTime.parse("2026-02-05T10:12:12"),
    lastSuccessfullyCompleted = LocalDateTime.parse("2026-02-05T10:12:13")
  )

  val directorPartner = DirectorPartnerUpdateStatus(
    status = AgencyDetailsStatusValue.Accepted,
    lastUpdated = LocalDateTime.parse("2026-02-05T10:13:12"),
    lastSuccessfullyCompleted = LocalDateTime.parse("2026-02-05T10:13:13")
  )

  val acceptNewTerms = AcceptNewTermsStatus(
    status = AgencyDetailsStatusValue.Accepted,
    lastUpdated = LocalDateTime.parse("2026-02-05T10:14:12"),
    lastSuccessfullyCompleted = LocalDateTime.parse("2026-02-05T10:14:13")
  )

  val rerisk = ReriskStatus(
    status = AgencyDetailsStatusValue.Accepted,
    lastUpdated = LocalDateTime.parse("2026-02-05T10:15:12"),
    lastSuccessfullyCompleted = LocalDateTime.parse("2026-02-05T10:15:13")
  )

  "AgentSubscriptionDisplayResponse OFormat" - {

    "write then read should round-trip and flatten status fields" in {

      val original = AgentSubscriptionDisplayResponse(
        processingDate = "2026-02-05T10:00:00Z",
        utr = Some("1234567890"),
        name = "Night Manager",
        addr1 = "Alps Hotel",
        addr2 = Some("Swiss Alps"),
        addr3 = None,
        addr4 = None,
        postcode = None,
        country = "CH",
        phone = Some("01234567890"),
        email = "test@example.com",
        suspensionStatus = "T",
        regime = Some(Seq("VAT", "SA")),
        supervisoryBody = Some("Supervisory Body"),
        membershipNumber = Some("MEM-123"),
        evidenceObjectReference = Some("evidence-ref-123"),
        updateDetailsStatus = updateDetails,
        amlSupervisionUpdateStatus = aml,
        directorPartnerUpdateStatus = directorPartner,
        acceptNewTermsStatus = acceptNewTerms,
        reriskStatus = rerisk
      )

      val json = Json.toJson(original).as[JsObject]

      (json \ "regime").as[Seq[String]] mustBe Seq("VAT", "SA")

      (json \ "updateDetailsStatus").as[String] mustBe updateDetails.status.value
      (json \ "updateDetailsLastUpdated").as[String] mustBe updateDetailsLastUpdatedStr
      (json \ "updateDetailsLastSuccessfullyComplete").as[String] mustBe updateDetailsLastSuccessfullyCompleteStr

      val roundTripped = json.as[AgentSubscriptionDisplayResponse]

      roundTripped.processingDate mustBe original.processingDate
      roundTripped.utr mustBe original.utr
      roundTripped.name mustBe original.name
      roundTripped.addr1 mustBe original.addr1
      roundTripped.addr2 mustBe original.addr2
      roundTripped.addr3 mustBe original.addr3
      roundTripped.addr4 mustBe original.addr4
      roundTripped.postcode mustBe original.postcode
      roundTripped.country mustBe original.country
      roundTripped.phone mustBe original.phone
      roundTripped.email mustBe original.email
      roundTripped.suspensionStatus mustBe original.suspensionStatus
      roundTripped.regime mustBe original.regime
      roundTripped.supervisoryBody mustBe original.supervisoryBody
      roundTripped.membershipNumber mustBe original.membershipNumber
      roundTripped.evidenceObjectReference mustBe original.evidenceObjectReference

      roundTripped.updateDetailsStatus.status mustBe original.updateDetailsStatus.status
      roundTripped.updateDetailsStatus.lastUpdated mustBe original.updateDetailsStatus.lastUpdated
      roundTripped.updateDetailsStatus.lastSuccessfullyCompleted mustBe original.updateDetailsStatus.lastSuccessfullyCompleted

    }

    "write should omit optional non-required fields when None" in {
      val original = AgentSubscriptionDisplayResponse(
        processingDate = "2026-02-05T10:00:00Z",
        utr = None,
        name = "Test Name",
        addr1 = "Line 1",
        addr2 = None,
        addr3 = None,
        addr4 = None,
        postcode = None,
        country = "GB",
        phone = None,
        email = "test@example.com",
        suspensionStatus = "F",
        regime = None,
        supervisoryBody = None,
        membershipNumber = None,
        evidenceObjectReference = None,
        updateDetailsStatus = updateDetails,
        amlSupervisionUpdateStatus = aml,
        directorPartnerUpdateStatus = directorPartner,
        acceptNewTermsStatus = acceptNewTerms,
        reriskStatus = rerisk
      )

      val json = Json.toJson(original).as[JsObject]

      (json \ "utr").toOption mustBe empty
      (json \ "addr2").toOption mustBe empty
      (json \ "postcode").toOption mustBe empty
      (json \ "phone").toOption mustBe empty
      (json \ "regime").toOption mustBe empty
      (json \ "supervisoryBody").toOption mustBe empty
      (json \ "membershipNumber").toOption mustBe empty
      (json \ "evidenceObjectReference").toOption mustBe empty
    }
  }
}
