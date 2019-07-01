package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.User.Address

case class TrustAddress(
  line1: String,
  line2: String,
  line3: Option[String] = None,
  line4: Option[String] = None,
  postcode: Option[String] = None,
  country: String)

object TrustAddress {
  implicit val format: Format[TrustAddress] = Json.format[TrustAddress]

  def apply(userAddress: Option[Address]): TrustAddress =
    userAddress match {
      case Some(address) =>
        TrustAddress(
          address.line1.getOrElse(""),
          address.line2.getOrElse(""),
          address.line3,
          address.line4,
          address.postcode,
          address.countryCode.getOrElse(""))
      case None => TrustAddress("", "", country = "")
    }

}

//#API-1495
case class TrustDetails(utr: String, trustName: String, address: TrustAddress, serviceName: String)

object TrustDetails {
  implicit val format: Format[TrustDetails] = Json.format[TrustDetails]
}

case class TrustDetailsResponse(trustDetails: TrustDetails)

object TrustDetailsResponse {
  implicit val format: Format[TrustDetailsResponse] = Json.format[TrustDetailsResponse]
}
