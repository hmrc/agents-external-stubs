package uk.gov.hmrc.agentsexternalstubs.models

import org.scalatest.Inspectors
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers
import uk.gov.hmrc.play.test.UnitSpec

class RecordSpec extends UnitSpec with PropertyChecks with ValidatedMatchers {

  val seeds = "fooba".permutations.toSeq

  val registrationEntity =
    RelationshipRecord(
      regime = "ITSA",
      arn = "ZARN1234567",
      refNumber = "012345678901234",
      idType = "none",
      active = true,
      id = Some("abc"))

  implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome

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

  "BusinessDetailsRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = BusinessDetailsRecord.generate(seed)
        BusinessDetailsRecord.validate(entity) should beValid
      }
    }
    "sanitize provided entity" in {
      val entity = BusinessDetailsRecord(
        safeId = "9090909",
        nino = "HW827856C",
        mtdbsa = "123456789098765"
      )
      BusinessDetailsRecord.validate(entity) should beValid

      val sanitized = BusinessDetailsRecord.sanitize(entity.safeId)(entity)

      BusinessDetailsRecord.validate(sanitized) should beValid
      sanitized.safeId === "9090909"
      sanitized.mtdbsa === "123456789098765"
      sanitized.nino === "HW827856C"
    }
  }

  "LegacyAgentRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = LegacyAgentRecord.generate(seed)
        LegacyAgentRecord.validate(entity) should beValid
      }
    }
  }

  "LegacyRelationshipRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = LegacyRelationshipRecord.generate(seed)
        LegacyRelationshipRecord.validate(entity) should beValid
      }
    }
  }

  "VatCustomerInformationRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = VatCustomerInformationRecord.generate(seed)
        VatCustomerInformationRecord.validate(entity) should beValid
      }
    }
  }

  "GetBusinessPartnerRecord" should {
    "generate valid entity from seed" in {
      Inspectors.forAll(seeds) { seed: String =>
        val entity = BusinessPartnerRecord.generate(seed)
        BusinessPartnerRecord.validate(entity) should beValid
      }
    }
  }

}
