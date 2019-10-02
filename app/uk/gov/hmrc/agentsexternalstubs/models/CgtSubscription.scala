package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._

case class CgtAddressDetails(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  countryCode: String,
  postalCode: Option[String] = None)

case object CgtAddressDetails {
  implicit val format: Format[CgtAddressDetails] = Json.format[CgtAddressDetails]
}

case class IndividualName(firstName: String, lastName: String)

case object IndividualName {
  implicit val format: Format[IndividualName] = Json.format[IndividualName]
}

case class OrganisationName(name: String)

case object OrganisationName {
  implicit val format: Format[OrganisationName] = Json.format[OrganisationName]
}

case class TypeOfPersonDetails(typeOfPerson: String, name: Either[IndividualName, OrganisationName])

object TypeOfPersonDetails {

  implicit val writes: Writes[TypeOfPersonDetails] = new Writes[TypeOfPersonDetails] {
    override def writes(tpd: TypeOfPersonDetails): JsValue = {

      val namePart = tpd.name match {
        case Left(individualName) =>
          s""""firstName": "${individualName.firstName}", "lastName": "${individualName.lastName}""""
        case Right(organisationName) =>
          s""""organisationName": "${organisationName.name}""""
      }

      Json.parse(s"""{
                    |"typeOfPerson": "${tpd.typeOfPerson}",
                    |$namePart
                    |}""".stripMargin)
    }
  }
}

case class SubscriptionDetails(typeOfPersonDetails: TypeOfPersonDetails, addressDetails: CgtAddressDetails)

object SubscriptionDetails {
  implicit val writes: Writes[SubscriptionDetails] = Json.writes[SubscriptionDetails]
}

case class CgtSubscription(regime: String, subscriptionDetails: SubscriptionDetails)

object CgtSubscription {
  implicit val writes: Writes[CgtSubscription] = Json.writes[CgtSubscription]
}
