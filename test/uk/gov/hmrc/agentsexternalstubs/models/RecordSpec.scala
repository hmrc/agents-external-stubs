package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.test.UnitSpec

class RecordSpec extends UnitSpec {

  val registrationEntity =
    RelationshipRecord(
      regime = "ITSA",
      arn = "ZARN1234567",
      refNumber = "012345678901234",
      idType = "none",
      active = true,
      id = Some("abc"))

  val registrationJson =
    """{"regime":"ITSA","arn":"ZARN1234567","idType":"none","refNumber":"012345678901234","active":true,"_keys":["RRFK/ITSA/ZARN1234567/none/012345678901234","ITSA","ZARN1234567","012345678901234"],"_type":"RelationshipRecord","_id":{"$oid":"abc"}}"""

  "Record" should {
    "have json writes for RelationshipRecord" in {
      Json
        .toJson[Record](registrationEntity)
        .toString() should (include("regime") and include("arn") and include("idType") and include("refNumber") and include(
        "active") and include("_keys") and include("_type") and include("_id"))
    }

    "have json reads for RelationshipRecord" in {
      Json.parse(registrationJson).as[Record] shouldBe registrationEntity
    }
  }

}
