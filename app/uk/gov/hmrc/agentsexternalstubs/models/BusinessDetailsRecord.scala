package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.BusinessData

case class BusinessDetailsRecord(
  safeId: String,
  nino: String,
  mtdbsa: String,
  propertyIncome: Option[Boolean] = None,
  businessData: Option[Seq[BusinessData]] = None,
  id: Option[String] = None)
    extends Record {

  override def keys: Seq[String] = Seq()
  override def withId(id: Option[String]): BusinessDetailsRecord = copy(id = id)
}

object BusinessDetailsRecord {

  case class BusinessAddress(
    addressLine1: String,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postalCode: Option[String] = None,
    countryCode: String)

  case class BusinessContactDetails(
    phoneNumber: Option[String] = None,
    mobileNumber: Option[String] = None,
    faxNumber: Option[String] = None,
    emailAddress: Option[String] = None)

  case class BusinessData(
    incomeSourceId: String,
    accountingPeriodStartDate: LocalDate,
    accountingPeriodEndDate: LocalDate,
    tradingName: Option[String] = None,
    businessAddressDetails: Option[BusinessAddress] = None,
    businessContactDetails: Option[BusinessContactDetails] = None,
    tradingStartDate: Option[LocalDate] = None,
    cashOrAccruals: Option[String] = None,
    seasonal: Option[String] = None,
    cessationDate: Option[LocalDate] = None,
    cessationReason: Option[String] = None,
    paperLess: Option[Boolean] = None
  )

  implicit val formats1: Format[BusinessAddress] = Json.format[BusinessAddress]
  implicit val formats2: Format[BusinessContactDetails] = Json.format[BusinessContactDetails]
  implicit val formats3: Format[BusinessData] = Json.format[BusinessData]
  implicit val formats: Format[BusinessDetailsRecord] = Json.format[BusinessDetailsRecord]

}
