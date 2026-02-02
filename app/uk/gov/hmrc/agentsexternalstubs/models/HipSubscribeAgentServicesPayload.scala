package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}

case class HipSubscribeAgentServicesPayload(
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: String,
  phone: Option[String],
  email: String,
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: String,
  amlSupervisionUpdateStatus: String,
  directorPartnerUpdateStatus: String,
  acceptNewTermsStatus: String,
  reriskStatus: String
)

object HipSubscribeAgentServicesPayload {

  import Validator._

  private val validationError =
    Errors("003", "Request could not be processed")

  private val validStatuses: Set[String] =
    Set("ACCEPTED", "REJECTED", "PENDING", "REQUIRED")

  private type PayloadValidator = Validator[HipSubscribeAgentServicesPayload]

  private def strLenValidator(
    extract: HipSubscribeAgentServicesPayload => String,
    min: Int,
    max: Int
  ): PayloadValidator =
    checkProperty(extract, check[String](_.lengthMinMaxInclusive(min, max), validationError.text))

  private def optStrLenValidator(
    extract: HipSubscribeAgentServicesPayload => Option[String],
    min: Int,
    max: Int
  ): PayloadValidator =
    checkObjectIfSome(extract, check[String](_.lengthMinMaxInclusive(min, max), validationError.text))

  private def enumValidator(
    extract: HipSubscribeAgentServicesPayload => String
  ): PayloadValidator =
    checkProperty(extract, check[String](s => validStatuses.contains(s.trim.toUpperCase), validationError.text))

  private val validatePayload: PayloadValidator =
    Validator(
      strLenValidator(_.name, 1, 40),
      strLenValidator(_.addr1, 1, 35),
      optStrLenValidator(_.addr2, 1, 35),
      optStrLenValidator(_.addr3, 1, 35),
      optStrLenValidator(_.addr4, 1, 35),
      optStrLenValidator(_.postcode, 1, 10),
      strLenValidator(_.country, 2, 2),
      optStrLenValidator(_.phone, 1, 24),
      strLenValidator(_.email, 1, 132),
      optStrLenValidator(_.supervisoryBody, 1, 100),
      optStrLenValidator(_.membershipNumber, 1, 100),
      optStrLenValidator(_.evidenceObjectReference, 1, 36),
      enumValidator(_.updateDetailsStatus),
      enumValidator(_.amlSupervisionUpdateStatus),
      enumValidator(_.directorPartnerUpdateStatus),
      enumValidator(_.acceptNewTermsStatus),
      enumValidator(_.reriskStatus)
    )

  def validateCreateAgentSubscriptionPayload(
    payload: HipSubscribeAgentServicesPayload
  ): Either[Errors, HipSubscribeAgentServicesPayload] =
    validatePayload(payload)
      .leftMap(_ => validationError)
      .toEither
      .map(_ => payload)

  implicit val format: Format[HipSubscribeAgentServicesPayload] = Json.format[HipSubscribeAgentServicesPayload]
}
