package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json

case class VatKnownFacts(vrn: String, dateOfReg: Option[String])

object VatKnownFacts {

  implicit val format = Json.format[VatKnownFacts]

  def fromVatCustomerInformationRecord(
    vatCustomerInformationRecord: VatCustomerInformationRecord): Option[VatKnownFacts] =
    Some(
      VatKnownFacts(
        vrn = vatCustomerInformationRecord.vrn,
        dateOfReg =
          vatCustomerInformationRecord.approvedInformation.map(_.customerDetails).flatMap(_.effectiveRegistrationDate)
      ))
}
