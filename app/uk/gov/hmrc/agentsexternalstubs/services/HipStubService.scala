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

import play.api.Logging
import uk.gov.hmrc.agentsexternalstubs.models._

import java.time.{Instant, LocalDate}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject.{Inject, Singleton}

@Singleton
class HipStubService @Inject() extends Logging {

  def validateHeaders(
    transmittingSystem: Option[String],
    originatingSystem: Option[String],
    correlationid: Option[String],
    receiptDate: Option[String]
  ): Either[Errors, Boolean] =
    if (!transmittingSystem.getOrElse("").equals("HIP")) {
      Left(Errors("TBC", "transmittingSystem header missing or invalid"))
    } else if (!originatingSystem.getOrElse("").equals("MDTP")) {
      Left(Errors("TBC", "originatingSystem header missing or invalid"))
    } else if (
      !correlationid
        .getOrElse("")
        .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$")
    ) {
      Left(Errors("TBC", "correlationid header missing or invalid"))
    } else if (!isValidTimestamp(receiptDate.getOrElse(""))) {
      Left(Errors("TBC", "receiptDate header missing or invalid"))
    } else {
      Right(true)
    }

  //yyyy-MM-ddTHH:mm:ssZ
  private def isValidTimestamp(timestamp: String): Boolean =
    timestamp.matches("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""") && {
      try {
        Instant.parse(timestamp)
        true
      } catch {
        case _: DateTimeParseException => false
      }
    }

  def processQueryParameters(
    regime: Option[String] = None,
    refNumber: Option[String] = None,
    idType: Option[String] = None,
    arn: Option[String] = None,
    isAnAgent: Option[String] = None,
    activeOnly: Option[String] = None,
    dateFrom: Option[String] = None,
    dateTo: Option[String] = None,
    relationshipType: Option[String] = None,
    authProfile: Option[String] = None
  ): Either[Errors, RelationshipRecordQuery] =
    if (regime.forall(_.isEmpty)) {
      Left(Errors("001", "Missing SAP Number or Regime"))
    } else if (!regime.get.matches("^.{1,10}$")) {
      Left(Errors("002", "Invalid Regime Type"))
    } else if (isAnAgent.isEmpty) {
      Left(Errors("TBC", "isAnAgent NOT SUPPLIED"))
    } else if (activeOnly.isEmpty) {
      Left(Errors("TBC", "activeOnly NOT SUPPLIED"))
    } else if (isAnAgent.get.toBoolean && arn.isEmpty) {
      Left(Errors("008", "Missing ARN Number"))
    } else if (isAnAgent.get.toBoolean && !arn.get.matches("^[A-Z]ARN[0-9]{7}$")) {
      Left(Errors("004", "Invalid ARN value"))
    } else if (!isAnAgent.get.toBoolean && !refNumber.getOrElse("").matches("^.{1,15}$")) {
      Left(Errors("003", "Reference number is missing or invalid"))
    } else if (idType.nonEmpty && !idType.get.matches("^.{1,6}$")) {
      Left(Errors("TBC", "idType INVALID"))
    } else if (!activeOnly.get.toBoolean && (dateTo.isEmpty || dateFrom.isEmpty)) {
      Left(Errors("TBC", "'dateTo' and 'dateFrom' mandatory if 'activeOnly' is false"))
    } else if (!activeOnly.get.toBoolean && (!isValidDate(dateTo.get) || !isValidDate(dateFrom.get))) {
      Left(Errors("TBC", "'dateTo' or 'dateFrom' is invalid"))
    } else if (relationshipType.nonEmpty && !relationshipType.get.matches("^ZA01$")) {
      Left(Errors("TBC", "relationshipType INVALID"))
    } else if (authProfile.nonEmpty && !authProfile.get.matches("^(ALL00001|ITSAS001)$")) {
      Left(Errors("TBC", "authProfile INVALID"))
    } else {
      Right(
        RelationshipRecordQuery(
          regime = regime.get,
          arn = arn,
          idType = idType.getOrElse("none"),
          refNumber = None,
          referenceNumber = refNumber,
          activeOnly = activeOnly.get.toBoolean,
          agent = isAnAgent.get.toBoolean,
          from = dateFrom.map(LocalDate.parse(_)),
          to = dateTo.map(LocalDate.parse(_)),
          relationship = relationshipType,
          authProfile = authProfile
        )
      )
    }

  private def isValidDate(date: String): Boolean =
    try {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      LocalDate.parse(date, formatter)
      true
    } catch {
      case _: DateTimeParseException => false
    }

}
