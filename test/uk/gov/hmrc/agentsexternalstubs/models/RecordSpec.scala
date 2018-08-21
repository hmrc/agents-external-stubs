package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class RecordSpec extends UnitSpec {

  val registrationEntity =
    RelationshipRecord(
      regime = "ITSA",
      arn = "ZARN1234567",
      refNumber = "012345678901234",
      idType = "none",
      active = true)
  val registrationJson =
    """{"regime":"ITSA","arn":"ZARN1234567","idType":"none","refNumber":"012345678901234","active":true,"_keys":["RelationshipRecord/ITSA/ZARN1234567/none/012345678901234","ITSA","ZARN1234567","012345678901234"],"_type":"RelationshipRecord"}"""

  "Record" should {
    "have json writes for RelationshipRecord" in {
      Json
        .toJson[Record](registrationEntity)
        .toString shouldBe registrationJson
    }

    "have json reads for RelationshipRecord" in {
      Json.parse(registrationJson).as[Record] shouldBe registrationEntity
    }
  }

}
