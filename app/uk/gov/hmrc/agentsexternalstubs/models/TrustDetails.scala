/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Result
import uk.gov.hmrc.agentsexternalstubs.controllers.HttpHelpers
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

object TrustDetailsResponse extends HttpHelpers {
  implicit val format: Format[TrustDetailsResponse] = Json.format[TrustDetailsResponse]

  def getErrorResponseFor(utr: String): Result =
    if (utr == "3887997235") {
      badRequest(
        "INVALID_TRUST_STATE",
        "The remote endpoint has indicated that the Trust/Estate is Closed and playback is not possible.")
    } else if (utr == "5786221775") {
      badRequest(
        "INVALID_TRUST_STATE",
        "The remote endpoint has indicated that there are Pending changes yet to be processed and playback is not yet possible.")
    } else if (utr == "6028812143") {
      badRequest("INVALID_REGIME", "The remote endpoint has indicated that the REGIME provided is invalid.")
    } else {
      notFound(
        "RESOURCE_NOT_FOUND",
        "The remote endpoint has indicated that no resource can be returned for the UTR provided and playback is not possible.")
    }
}
