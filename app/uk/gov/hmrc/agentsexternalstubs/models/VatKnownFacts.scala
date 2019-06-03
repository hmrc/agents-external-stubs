package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json

case class VatKnownFacts(vrn: String, dateOfReg: String)

object VatKnownFacts {

  implicit val format = Json.format[VatKnownFacts]

  def fromVatCustomerInformationRecord(
    vrn: String,
    vatRecord: Option[VatCustomerInformationRecord]): Option[VatKnownFacts] =
    vatRecord
      .flatMap(_.approvedInformation)
      .map(_.customerDetails)
      .flatMap(_.effectiveRegistrationDate)
      .map(VatKnownFacts(vrn, _))
}
