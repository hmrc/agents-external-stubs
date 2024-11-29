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

package uk.gov.hmrc.agentsexternalstubs.services

import uk.gov.hmrc.agentsexternalstubs.models.Errors
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

import java.time.LocalDate
import java.util.UUID

class HipStubServiceSpec extends BaseUnitSpec {

  private val requestCouldNotBeProcessed = "Request could not be processed"

  "HipStubService.validateHeaders" should {
    "return an Either Right when all mandatory values provided and are valid" in {
      validateHeaders().getOrElse(false) shouldBe true
    }

    "return an error if transmittingSystemHeader is missing" in {
      val result = validateHeaders(transmittingSystemHeader = None).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if transmittingSystemHeader is invalid" in {
      val result = validateHeaders(transmittingSystemHeader = Some("apple")).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if originatingSystemHeader is missing" in {
      val result = validateHeaders(originatingSystemHeader = None).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if originatingSystemHeader is invalid" in {
      val result = validateHeaders(originatingSystemHeader = Some("apple")).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if correlationIdHeader is missing" in {
      val result = validateHeaders(correlationIdHeader = None).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if correlationIdHeader is invalid" in {
      val result = validateHeaders(correlationIdHeader = Some("apple")).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if receiptDateHeader is missing" in {
      val result = validateHeaders(receiptDateHeader = None).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if receiptDateHeader is not a date" in {
      val result = validateHeaders(receiptDateHeader = Some("apple")).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if receiptDateHeader is an invalid date" in {
      val result = validateHeaders(receiptDateHeader = Some("2024-99-22T12:54:24Z")).swap.getOrElse(Errors())
      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }
  }

  "HipStubService.processQueryParameters" should {
    "return an Either Right when all mandatory values provided and are valid" in {
      processQueryParameters(
        activeOnly = Some("false"),
        dateFrom = Some("2024-01-01"),
        dateTo = Some("9999-12-31")
      ) shouldBe Right(
        RelationshipRecordQuery(
          "VAT",
          Some("AARN1234567"),
          "none",
          None,
          None,
          false,
          true,
          Some(LocalDate.parse("2024-01-01")),
          Some(LocalDate.parse("9999-12-31")),
          None,
          None
        )
      )
    }

    "return an error if regime query parameter is missing" in {
      val result = processQueryParameters(regime = None).swap.getOrElse(Errors())

      result.text shouldBe "Missing SAP Number or Regime"
      result.code shouldBe "001"
    }

    "return an error if regime query parameter is invalid" in {
      val result = processQueryParameters(regime = Some("12345678901")).swap.getOrElse(Errors())

      result.text shouldBe "Invalid Regime Type"
      result.code shouldBe "002"
    }

    "return an error if isAnAgent query parameter is missing" in {
      val result =
        processQueryParameters(isAnAgent = None).swap.getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if activeOnly query parameter is missing" in {
      val result =
        processQueryParameters(activeOnly = None).swap.getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if isAnAgent was true and arn query parameter is missing" in {
      val result = processQueryParameters(isAnAgent = Some("true"), arn = None).swap
        .getOrElse(Errors())

      result.text shouldBe "Missing ARN Number"
      result.code shouldBe "008"
    }

    "return an error if isAnAgent was true and arn query parameter is invalid" in {
      val result =
        processQueryParameters(isAnAgent = Some("true"), arn = Some("apple")).swap
          .getOrElse(Errors())

      result.text shouldBe "Invalid ARN value"
      result.code shouldBe "004"
    }

    "return an error if activeOnly was false and dateFrom query parameter is missing" in {
      val result = processQueryParameters(
        activeOnly = Some("false"),
        dateFrom = None,
        dateTo = Some("1999-05-26")
      ).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if activeOnly was false and dateFrom query parameter is invalid" in {
      val result = processQueryParameters(
        activeOnly = Some("false"),
        dateFrom = Some("apple"),
        dateTo = Some("1999-05-26")
      ).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if activeOnly was false and dateTo query parameter is missing" in {
      val result = processQueryParameters(
        activeOnly = Some("false"),
        dateFrom = Some("1999-05-26"),
        dateTo = None
      ).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if activeOnly was false and dateTo query parameter is invalid" in {
      val result = processQueryParameters(
        activeOnly = Some("false"),
        dateFrom = Some("1999-05-26"),
        dateTo = Some("apple")
      ).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if isAnAgent was false and refNumber query parameter is missing" in {
      val result =
        processQueryParameters(isAnAgent = Some("false"), refNumber = None).swap.getOrElse(Errors())

      result.text shouldBe "Reference number is missing or invalid"
      result.code shouldBe "003"
    }

    "return an error if isAnAgent was false and refNumber query parameter is invalid" in {
      val result = processQueryParameters(
        isAnAgent = Some("false"),
        refNumber = Some("1234567890123456")
      ).swap
        .getOrElse(Errors())

      result.text shouldBe "Reference number is missing or invalid"
      result.code shouldBe "003"
    }

    "return an error if idType query parameter is invalid" in {
      val result = processQueryParameters(idType = Some("1234567")).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if relationshipType query parameter is invalid" in {
      val result = processQueryParameters(relationshipType = Some("apple")).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }

    "return an error if authProfile query parameter is invalid" in {
      val result = processQueryParameters(authProfile = Some("apple")).swap
        .getOrElse(Errors())

      result.text shouldBe requestCouldNotBeProcessed
      result.code shouldBe "006"
    }
  }

  val service = new HipStubService

  private def validateHeaders(
    transmittingSystemHeader: Option[String] = Some("HIP"),
    originatingSystemHeader: Option[String] = Some("MDTP"),
    correlationIdHeader: Option[String] = Some(UUID.randomUUID().toString),
    receiptDateHeader: Option[String] = Some("2024-11-22T12:54:24Z")
  ) = service.validateHeaders(transmittingSystemHeader, originatingSystemHeader, correlationIdHeader, receiptDateHeader)

  private def processQueryParameters(
    regime: Option[String] = Some("VAT"),
    isAnAgent: Option[String] = Some("true"),
    activeOnly: Option[String] = Some("true"),
    idType: Option[String] = None,
    refNumber: Option[String] = None,
    arn: Option[String] = Some("AARN1234567"),
    dateFrom: Option[String] = None,
    dateTo: Option[String] = None,
    relationshipType: Option[String] = None,
    authProfile: Option[String] = None
  ): Either[Errors, RelationshipRecordQuery] = service.processQueryParameters(
    regime,
    refNumber,
    idType,
    arn,
    isAnAgent,
    activeOnly,
    dateFrom,
    dateTo,
    relationshipType,
    authProfile
  )

}
