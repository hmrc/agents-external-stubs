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

import org.scalacheck.Gen
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

import java.time.LocalDateTime

sealed trait AgencyDetailsStatusValue { def value: String }

object AgencyDetailsStatusValue {
  case object Accepted extends AgencyDetailsStatusValue { val value = "ACCEPTED" }
  case object Rejected extends AgencyDetailsStatusValue { val value = "REJECTED" }
  case object Pending extends AgencyDetailsStatusValue { val value = "PENDING" }
  case object Required extends AgencyDetailsStatusValue { val value = "REQUIRED" }

  val values: Seq[AgencyDetailsStatusValue] = Seq(Accepted, Rejected, Pending, Required)

  implicit val reads: Reads[AgencyDetailsStatusValue] =
    Reads.StringReads.collect(JsonValidationError("Invalid AgencyDetailsStatusValue")) {
      case "ACCEPTED" => Accepted
      case "REJECTED" => Rejected
      case "PENDING"  => Pending
      case "REQUIRED" => Required
    }

  implicit val writes: Writes[AgencyDetailsStatusValue] = Writes(v => JsString(v.value))
  implicit val format: Format[AgencyDetailsStatusValue] = Format(reads, writes)

  val gen: Gen[AgencyDetailsStatusValue] = Gen.oneOf(values)

  val validate: Validator[AgencyDetailsStatusValue] =
    Validator.check(values.contains, "Invalid AgencyDetailsStatusValue")

  val sanitizer: String => AgencyDetailsStatusValue => AgencyDetailsStatusValue =
    _ => v => validate(v).fold(_ => Pending, _ => v)

  def fromString(s: String): AgencyDetailsStatusValue =
    s.trim.toUpperCase match {
      case "ACCEPTED" => Accepted
      case "REJECTED" => Rejected
      case "PENDING"  => Pending
      case "REQUIRED" => Required
      case _          => Pending
    }

}

sealed trait AgencyDetailsStatus {
  val status: AgencyDetailsStatusValue
  val lastUpdated: LocalDateTime
  val lastSuccessfullyCompleted: LocalDateTime
}

object AgencyDetailsStatus {

  implicit val format: Format[AgencyDetailsStatus] = Json.format[AgencyDetailsStatus]

  private val validateDateTime: Validator[LocalDateTime] =
    Validator.check(_ => true, "Invalid LocalDateTime")

  private def validateFor[A <: AgencyDetailsStatus]: Validator[A] =
    Validator(
      Validator.checkProperty(_.status, AgencyDetailsStatusValue.validate),
      Validator.checkProperty(_.lastUpdated, validateDateTime),
      Validator.checkProperty(_.lastSuccessfullyCompleted, validateDateTime)
    )

  private def genFor[A](ctor: (AgencyDetailsStatusValue, LocalDateTime, LocalDateTime) => A): Gen[A] =
    AgencyDetailsStatusValue.gen.map(s => ctor(s, LocalDateTime.now(), LocalDateTime.now()))

  private def sanitizerFor[A <: AgencyDetailsStatus](
    rebuild: (A, AgencyDetailsStatusValue, LocalDateTime, LocalDateTime) => A
  ): String => A => A =
    seed =>
      entity =>
        rebuild(
          entity,
          AgencyDetailsStatusValue.sanitizer(seed)(entity.status),
          Option(entity.lastUpdated).getOrElse(LocalDateTime.now()),
          Option(entity.lastSuccessfullyCompleted).getOrElse(LocalDateTime.now())
        )

  def asRecordUtils[A <: AgencyDetailsStatus](
    ctor: (AgencyDetailsStatusValue, LocalDateTime, LocalDateTime) => A,
    rebuild: (A, AgencyDetailsStatusValue, LocalDateTime, LocalDateTime) => A
  ): RecordUtils[A] =
    new RecordUtils[A] {
      override val gen: Gen[A] = genFor(ctor)
      override val validate: Validator[A] = validateFor[A]
      override val sanitizers: Seq[Update] = Seq(seed => e => sanitizerFor(rebuild)(seed)(e))
    }

  def toResponseField(prefix: String, statusOpt: Option[AgencyDetailsStatus]): JsObject =
    statusOpt match {
      case None =>
        Json.obj()

      case Some(s) =>
        Json.obj(
          s"${prefix}Status"                   -> s.status.value,
          s"${prefix}LastUpdated"              -> s.lastUpdated,
          s"${prefix}LastSuccessfullyComplete" -> s.lastSuccessfullyCompleted
        )
    }
}

final case class UpdateDetailsStatus(
  status: AgencyDetailsStatusValue,
  lastUpdated: LocalDateTime = LocalDateTime.now(),
  lastSuccessfullyCompleted: LocalDateTime = LocalDateTime.now()
) extends AgencyDetailsStatus

object UpdateDetailsStatus extends RecordUtils[UpdateDetailsStatus] {
  private val base = AgencyDetailsStatus.asRecordUtils[UpdateDetailsStatus](
    UpdateDetailsStatus.apply,
    (e, s, lu, lsc) => e.copy(status = s, lastUpdated = lu, lastSuccessfullyCompleted = lsc)
  )
  override val gen: Gen[UpdateDetailsStatus] = base.gen
  override val validate: Validator[UpdateDetailsStatus] = base.validate
  override val sanitizers: Seq[Update] = base.sanitizers
  implicit val format: OFormat[UpdateDetailsStatus] = Json.format[UpdateDetailsStatus]
}

final case class AmlSupervisionUpdateStatus(
  status: AgencyDetailsStatusValue,
  lastUpdated: LocalDateTime = LocalDateTime.now(),
  lastSuccessfullyCompleted: LocalDateTime = LocalDateTime.now()
) extends AgencyDetailsStatus

object AmlSupervisionUpdateStatus extends RecordUtils[AmlSupervisionUpdateStatus] {
  private val base = AgencyDetailsStatus.asRecordUtils[AmlSupervisionUpdateStatus](
    AmlSupervisionUpdateStatus.apply,
    (e, s, lu, lsc) => e.copy(status = s, lastUpdated = lu, lastSuccessfullyCompleted = lsc)
  )
  override val gen: Gen[AmlSupervisionUpdateStatus] = base.gen
  override val validate: Validator[AmlSupervisionUpdateStatus] = base.validate
  override val sanitizers: Seq[Update] = base.sanitizers
  implicit val format: OFormat[AmlSupervisionUpdateStatus] = Json.format[AmlSupervisionUpdateStatus]
}

final case class DirectorPartnerUpdateStatus(
  status: AgencyDetailsStatusValue,
  lastUpdated: LocalDateTime = LocalDateTime.now(),
  lastSuccessfullyCompleted: LocalDateTime = LocalDateTime.now()
) extends AgencyDetailsStatus

object DirectorPartnerUpdateStatus extends RecordUtils[DirectorPartnerUpdateStatus] {
  private val base = AgencyDetailsStatus.asRecordUtils[DirectorPartnerUpdateStatus](
    DirectorPartnerUpdateStatus.apply,
    (e, s, lu, lsc) => e.copy(status = s, lastUpdated = lu, lastSuccessfullyCompleted = lsc)
  )
  override val gen: Gen[DirectorPartnerUpdateStatus] = base.gen
  override val validate: Validator[DirectorPartnerUpdateStatus] = base.validate
  override val sanitizers: Seq[Update] = base.sanitizers
  implicit val format: OFormat[DirectorPartnerUpdateStatus] = Json.format[DirectorPartnerUpdateStatus]
}

final case class AcceptNewTermsStatus(
  status: AgencyDetailsStatusValue,
  lastUpdated: LocalDateTime = LocalDateTime.now(),
  lastSuccessfullyCompleted: LocalDateTime = LocalDateTime.now()
) extends AgencyDetailsStatus

object AcceptNewTermsStatus extends RecordUtils[AcceptNewTermsStatus] {
  private val base = AgencyDetailsStatus.asRecordUtils[AcceptNewTermsStatus](
    AcceptNewTermsStatus.apply,
    (e, s, lu, lsc) => e.copy(status = s, lastUpdated = lu, lastSuccessfullyCompleted = lsc)
  )
  override val gen: Gen[AcceptNewTermsStatus] = base.gen
  override val validate: Validator[AcceptNewTermsStatus] = base.validate
  override val sanitizers: Seq[Update] = base.sanitizers
  implicit val format: OFormat[AcceptNewTermsStatus] = Json.format[AcceptNewTermsStatus]
}

final case class ReriskStatus(
  status: AgencyDetailsStatusValue,
  lastUpdated: LocalDateTime = LocalDateTime.now(),
  lastSuccessfullyCompleted: LocalDateTime = LocalDateTime.now()
) extends AgencyDetailsStatus

object ReriskStatus extends RecordUtils[ReriskStatus] {
  private val base = AgencyDetailsStatus.asRecordUtils[ReriskStatus](
    ReriskStatus.apply,
    (e, s, lu, lsc) => e.copy(status = s, lastUpdated = lu, lastSuccessfullyCompleted = lsc)
  )
  override val gen: Gen[ReriskStatus] = base.gen
  override val validate: Validator[ReriskStatus] = base.validate
  override val sanitizers: Seq[Update] = base.sanitizers
  implicit val format: OFormat[ReriskStatus] = Json.format[ReriskStatus]
}
