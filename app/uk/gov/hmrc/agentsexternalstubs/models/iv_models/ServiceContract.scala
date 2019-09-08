package uk.gov.hmrc.agentsexternalstubs.models.iv_models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.json.Mappings

sealed trait JourneyType

object JourneyType {
  object UpliftNino extends JourneyType
  object UpliftNoNino extends JourneyType

  val mapping = Mappings.mapEnum(UpliftNino, UpliftNoNino)

  implicit val format = mapping.jsonFormat
}

case class JourneyCreation(serviceContract: ServiceContract, journeyType: JourneyType)

object JourneyCreation {
  implicit val format: OFormat[JourneyCreation] = Json.format[JourneyCreation]
}

case class ServiceContract(origin: String, completionURL: String, failureURL: String, confidenceLevel: Int)

object ServiceContract {

//  implicit val reads: Reads[ServiceContract] = (
//    (__ \ "origin").read[String] and
//      (__ \ "completionURL").read[String] and
//      (__ \ "failureURL").read[String] and
//      (__ \ "confidenceLevel").read[Int]
//  ) { (origin: String, completionURL: String, failureURL: String, confidenceLevel: Int) =>
//    ServiceContract(origin, completionURL, failureURL, confidenceLevel)
//  }
//
//  implicit val writes: Writes[ServiceContract] = new Writes[ServiceContract] {
//    override def writes(o: ServiceContract): JsValue =
//      Json.obj(
//        "origin"          -> o.origin,
//        "completionURL"   -> o.completionURL,
//        "failureURL"      -> o.failureURL,
//        "confidenceLevel" -> o.confidenceLevel)
//  }

  implicit val format: OFormat[ServiceContract] = Json.format[ServiceContract]
}

case class Journey(journeyId: String, journeyType: JourneyType, serviceContract: ServiceContract)

object Journey {

  val JOURNEYID = "journeyId"

  implicit val format = Json.format[Journey]
}
