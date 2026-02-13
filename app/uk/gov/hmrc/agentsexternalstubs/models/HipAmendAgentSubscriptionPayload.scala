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

import play.api.libs.json.{Format, Json}

case class HipAmendAgentSubscriptionPayload(
  name: Option[String],
  addr1: Option[String],
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: Option[String],
  phone: Option[String],
  email: Option[String],
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: Option[String],
  amlSupervisionUpdateStatus: Option[String],
  directorPartnerUpdateStatus: Option[String],
  acceptNewTermsStatus: Option[String],
  reriskStatus: Option[String]
) {
  def isEmpty: Boolean =
    List(
      name,
      addr1,
      addr2,
      addr3,
      addr4,
      postcode,
      country,
      phone,
      email,
      supervisoryBody,
      membershipNumber,
      evidenceObjectReference,
      updateDetailsStatus,
      amlSupervisionUpdateStatus,
      directorPartnerUpdateStatus,
      acceptNewTermsStatus,
      reriskStatus
    ).forall(_.isEmpty)

  def addressProvided: Boolean =
    List(
      addr1,
      addr2,
      addr3,
      addr4,
      postcode,
      country
    ).exists(_.isDefined)

}

object HipAmendAgentSubscriptionPayload {

  import Validator._

  private val validationError =
    Errors("003", "Request could not be processed")

  private val validStatuses: Set[String] =
    Set("ACCEPTED", "REJECTED", "PENDING", "REQUIRED")

  private type PayloadValidator = Validator[HipAmendAgentSubscriptionPayload]

  private def strLenValidator(
    extract: HipAmendAgentSubscriptionPayload => String,
    min: Int,
    max: Int
  ): PayloadValidator =
    checkProperty(extract, check[String](_.lengthMinMaxInclusive(min, max), validationError.text))

  private def optEnumValidator(
    extract: HipAmendAgentSubscriptionPayload => Option[String]
  ): PayloadValidator =
    checkObjectIfSome(
      extract,
      check[String](s => validStatuses.contains(s.trim.toUpperCase), validationError.text)
    )

  private def optStrLenValidator(
    extract: HipAmendAgentSubscriptionPayload => Option[String],
    min: Int,
    max: Int
  ): PayloadValidator =
    checkObjectIfSome(
      extract,
      check[String](_.lengthMinMaxInclusive(min, max), validationError.text)
    )

  private val validatePayload: PayloadValidator =
    Validator(
      optStrLenValidator(_.name, 1, 40),
      optStrLenValidator(_.addr1, 1, 35),
      optStrLenValidator(_.addr2, 1, 35),
      optStrLenValidator(_.addr3, 1, 35),
      optStrLenValidator(_.addr4, 1, 35),
      optStrLenValidator(_.postcode, 1, 10),
      optStrLenValidator(_.country, 2, 2),
      optStrLenValidator(_.phone, 1, 24),
      optStrLenValidator(_.email, 1, 132),
      optStrLenValidator(_.supervisoryBody, 1, 100),
      optStrLenValidator(_.membershipNumber, 1, 100),
      optStrLenValidator(_.evidenceObjectReference, 1, 36),
      optEnumValidator(_.updateDetailsStatus),
      optEnumValidator(_.amlSupervisionUpdateStatus),
      optEnumValidator(_.directorPartnerUpdateStatus),
      optEnumValidator(_.acceptNewTermsStatus),
      optEnumValidator(_.reriskStatus)
    )

  def validateAmendAgentSubscriptionPayload(
    payload: HipAmendAgentSubscriptionPayload
  ): Either[Errors, HipAmendAgentSubscriptionPayload] =
    validatePayload(payload)
      .leftMap(_ => validationError)
      .toEither
      .map(_ => payload)

  implicit val format: Format[HipAmendAgentSubscriptionPayload] =
    Json.format[HipAmendAgentSubscriptionPayload]
}
