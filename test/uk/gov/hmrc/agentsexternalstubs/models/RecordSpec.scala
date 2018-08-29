package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord.{BusinessContact, BusinessData}
import uk.gov.hmrc.play.test.UnitSpec

class RecordSpec extends UnitSpec with PropertyChecks {

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

  "BusinessDetailsRecord" should {
    "generate valid entity from seed" in {
      forAll { seed: String =>
        val entity = BusinessDetailsRecord.generate(seed)
        BusinessDetailsRecord.validate(entity).isValid shouldBe true
        entity.businessData shouldBe defined
        entity.businessData.get.head.tradingName shouldBe defined
        entity.businessData.get.head.businessAddressDetails shouldBe defined
      }
    }
    "sanitize provided entity" in {
      val entity = BusinessDetailsRecord(
        safeId = "9090909",
        nino = "HW827856C",
        mtdbsa = "123456789098765",
        businessData = Some(
          Seq(BusinessData(
            incomeSourceId = "123456789012345",
            accountingPeriodEndDate = LocalDate.now,
            accountingPeriodStartDate = LocalDate.now,
            businessContactDetails = Some(BusinessContact(emailAddress = Some("a@a.com")))
          )))
      )
      BusinessDetailsRecord.validate(entity).isValid shouldBe true

      val sanitized = BusinessDetailsRecord.sanitize(entity)
      BusinessDetailsRecord.validate(sanitized).isValid shouldBe true
      sanitized.safeId === "9090909"
      sanitized.mtdbsa === "123456789098765"
      sanitized.nino === "HW827856C"
      sanitized.businessData.get.head.incomeSourceId === "123456789012345"
      sanitized.businessData.get.head.businessContactDetails.get.emailAddress === "a@a.com"
      sanitized.businessData.get.head.tradingName shouldBe defined
      sanitized.businessData.get.head.businessAddressDetails shouldBe defined
    }
  }

  "LegacyAgentRecord" should {
    "generate valid entity from seed" in {
      forAll { seed: String =>
        val entity = LegacyAgentRecord.generate(seed)
        LegacyAgentRecord.validate(entity).isValid shouldBe true
      }
    }
  }

  "LegacyRelationshipRecord" should {
    "generate valid entity from seed" in {
      forAll { seed: String =>
        val entity = LegacyRelationshipRecord.generate(seed)
        LegacyRelationshipRecord.validate(entity).isValid shouldBe true
      }
    }
  }

}
