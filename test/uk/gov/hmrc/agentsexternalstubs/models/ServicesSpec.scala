package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ServicesSpec extends UnitSpec {

  "Services" should {
    "read services definitions at bootstrap" in {
      Services.services should not be empty
    }

    "serialize services back to json" in {
      val entity = Services(services = Services.services)
      val json = Json.toJson(entity)
      (json \ "services").as[JsArray].value should not be empty
    }
  }
}
