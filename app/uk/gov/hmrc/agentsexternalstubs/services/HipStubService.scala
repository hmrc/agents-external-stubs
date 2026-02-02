/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.agentsexternalstubs.models.Validator.{Validator, _}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.models.identifiers._
import uk.gov.hmrc.domain.TaxIdentifier

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{Instant, LocalDate}
import javax.inject.{Inject, Singleton}

@Singleton
class HipStubService @Inject() extends Logging {

  private val requestCouldNotBeProcessed = "Request could not be processed"

  def validateBaseHeaders(
    transmittingSystem: Option[String],
    originatingSystem: Option[String],
    correlationid: Option[String],
    receiptDate: Option[String]
  ): Either[Errors, Boolean] =
    if (!transmittingSystem.getOrElse("").equals("HIP")) {
      logger.error("transmittingSystem header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (!originatingSystem.getOrElse("").equals("MDTP")) {
      logger.error("originatingSystem header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (
      !correlationid
        .getOrElse("")
        .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$")
    ) {
      logger.error("correlationid header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (!isValidTimestamp(receiptDate.getOrElse(""))) {
      logger.error("receiptDate header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else {
      Right(true)
    }

  def validateGetITSABusinessDetailsHeaders(
    xMessageType: Option[String],
    xRegimeType: Option[String]
  ): Either[Errors, Boolean] =
    if (xMessageType.getOrElse("") != "TaxpayerDisplay") {
      logger.error("messageType header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (xRegimeType.getOrElse("") != "ITSA") {
      logger.error("regimeType header missing or invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else Right(true)

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

  def processDisplayRelationshipsQueryParameters(
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
      logger.error("Missing SAP Number or Regime")
      Left(Errors("001", "Missing SAP Number or Regime"))
    } else if (!regime.get.matches("^.{1,10}$")) {
      logger.error("Invalid Regime Type")
      Left(Errors("002", "Invalid Regime Type"))
    } else if (isAnAgent.isEmpty) {
      logger.error("isAnAgent NOT SUPPLIED")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (activeOnly.isEmpty) {
      logger.error("activeOnly NOT SUPPLIED")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (isAnAgent.get.toBoolean && arn.isEmpty) {
      logger.error("Missing ARN Number")
      Left(Errors("008", "Missing ARN Number"))
    } else if (isAnAgent.get.toBoolean && !arn.get.matches("^[A-Z]ARN[0-9]{7}$")) {
      logger.error("Invalid ARN value")
      Left(Errors("004", "Invalid ARN value"))
    } else if (!isAnAgent.get.toBoolean && !refNumber.getOrElse("").matches("^.{1,15}$")) {
      logger.error("Reference number is missing or invalid")
      Left(Errors("003", "Reference number is missing or invalid"))
    } else if (idType.nonEmpty && !idType.get.matches("^.{1,6}$")) {
      logger.error("idType INVALID")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (!activeOnly.get.toBoolean && (dateTo.isEmpty || dateFrom.isEmpty)) {
      logger.error("'dateTo' and 'dateFrom' mandatory if 'activeOnly' is false")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (!activeOnly.get.toBoolean && (!isValidDate(dateTo.get) || !isValidDate(dateFrom.get))) {
      logger.error("'dateTo' or 'dateFrom' is invalid")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (relationshipType.nonEmpty && !relationshipType.get.matches("^ZA01$")) {
      logger.error("relationshipType INVALID")
      Left(Errors("006", requestCouldNotBeProcessed))
    } else if (authProfile.nonEmpty && !authProfile.get.matches("^(ALL00001|ITSAS001)$")) {
      logger.error("authProfile INVALID")
      Left(Errors("006", requestCouldNotBeProcessed))
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

  def processItsaTaxpayerBusinessDetailsQueryParameters(
    mtdReference: Option[String] = None,
    nino: Option[String] = None
  ): Either[Errors, TaxIdentifier] =
    (mtdReference, nino) match {
      case (Some(mtdId), Some(nino)) =>
        if (MtdItId.isValid(mtdId) && NinoWithoutSuffix.isValid(nino)) Right(NinoWithoutSuffix(nino))
        else {
          logger.error("mtdItId or nino is invalid")
          Left(Errors("006", requestCouldNotBeProcessed))
        }
      case (Some(mtdId), None) =>
        if (MtdItId.isValid(mtdId)) Right(MtdItId(mtdId))
        else {
          logger.error("mtdItId is invalid")
          Left(Errors("006", requestCouldNotBeProcessed))
        }
      case (None, Some(nino)) =>
        if (NinoWithoutSuffix.isValid(nino)) Right(NinoWithoutSuffix(nino))
        else {
          logger.error("nino is invalid")
          Left(Errors("006", requestCouldNotBeProcessed))
        }
      case (None, None) =>
        logger.error("either nino or mtdId must be supplied")
        Left(Errors("006", requestCouldNotBeProcessed))
    }

  private def isValidDate(date: String): Boolean =
    try {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      LocalDate.parse(date, formatter)
      true
    } catch {
      case _: DateTimeParseException => false
    }

  def validateUpdateRelationshipPayload(payload: UpdateRelationshipPayload): Either[Errors, UpdateRelationshipPayload] =
    if (!List("VATC", "ITSA", "CGT", "PPT", "TRS", "PLR", "CBC").contains(payload.regime))
      Left(Errors("002", "Invalid Regime Type"))
    else if (!payload.refNumber.matches("^.{1,15}$")) Left(Errors("003", "Reference number is missing or invalid"))
    else if (!validIdTypeForRegime(payload.regime, payload.refNumber).contains(payload.idType.getOrElse("")))
      Left(Errors("013", "ID Type is invalid or missing"))
    else if (!payload.arn.matches("^[A-Z]ARN[0-9]{7}$")) Left(Errors("004", "Invalid ARN value"))
    else if (!List("0001", "0002").contains(payload.action)) Left(Errors("???", "unrecognised action"))
    else if (!validateRelationshipType(payload.regime, payload.relationshipType))
      Left(Errors("012", "Relationship type is invalid or missing"))
    else
      payload.authProfile.fold[Either[Errors, UpdateRelationshipPayload]](
        if (Seq("TRS", "CBC").contains(payload.regime)) Right(payload)
        else Left(Errors("005", "Relationship Authorisation Profile missing"))
      )(authProfile =>
        if (!validateAuthProfile(payload.regime, authProfile))
          Left(Errors("004", "Incorrect Relationship Authorisation Profile"))
        else Right(payload)
      )

  private def validIdTypeForRegime(regime: String, refNumber: String): Option[String] =
    regime match {
      case "VATC" => Some("VRN")
      case "ITSA" => Some("MTDBSA")
      case "CGT"  => Some("ZCGT")
      case "PPT"  => Some("ZPPT")
      case "TRS" =>
        if (refNumber.matches("^((?i)[a-z]{2}trust[0-9]{8})$")) Some("URN")
        else if (refNumber.matches("^\\d{10}$")) Some("UTR")
        else None
      case "PLR" => Some("ZPLR")
      case "CBC" => Some("CBC")
      case _     => None
    }

  private def validateRelationshipType(regime: String, relationshipType: Option[String]): Boolean =
    regime match {
      case "VATC" | "ITSA" | "CGT" | "PPT" | "PLR" => relationshipType.contains("ZA01")
      case "CBC" | "TRS"                           => relationshipType.isEmpty
      case _                                       => false
    }

  private def validateAuthProfile(regime: String, authProfile: String): Boolean =
    regime match {
      case "ITSA" => List("ALL00001", "ITSAS001").contains(authProfile)
      case _      => authProfile == "ALL00001"
    }

  def validateArn(arn: String): Either[Errors, Arn] =
    if  (Arn.isValid(arn)) Right(Arn(arn)) else Left(Errors("004", "Invalid ARN value"))

}
