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

import play.api.libs.json.*

import java.time.{ZoneOffset, ZonedDateTime}

object AgentKnownFactCheckResponse:
  case class AgentSuccessResponse(success: TrustDetailsResponse)

  object AgentSuccessResponse:
    given Format[AgentSuccessResponse] = Json.format[AgentSuccessResponse]

  case class ValidationError(processingDate: ZonedDateTime, errorId: String, text: String)

  object ValidationError:
    given Format[ValidationError] = Json.format[ValidationError]

  case class ValidationErrors(error: ValidationError)

  object ValidationErrors:
    given Format[ValidationErrors] = Json.format[ValidationErrors]

    val invalidUrnOrUtr = ValidationErrors(
      ValidationError(ZonedDateTime.now(ZoneOffset.UTC), "000", "UTR or URN is invalid")
    )
