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
    """{"regime":"ITSA","arn":"ZARN1234567","idType":"none","refNumber":"012345678901234","active":true,"_record_type":"RelationshipRecord","_id":{"$oid":"abc"}}"""

  "Record" should {
    "have json writes for RelationshipRecord" in {
      Json
        .toJson[Record](registrationEntity)
        .toString() should (include("regime") and include("arn") and include("idType") and include("refNumber") and include(
        "active") and include(Record.ID))
    }

    "have json reads for RelationshipRecord" in {
      Json.parse(registrationJson).as[Record] shouldBe registrationEntity
    }
  }

}
