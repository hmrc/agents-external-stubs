package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.models.Pillar2Record

trait ExampleDesPayloads {

  val trustDetailsPayload =
    """
      |{
      |  "acknowledgmentReference": "A1BCDEFG1HIJKLNOPQRSTUVWXYZ12346",
      |   "refNumber": "XXTRUST80000001",
      |   "agentReferenceNumber": "PARN0876123",
      |   "idType": "URN"
      |   "regime": "TRS",
      |   "authorisation": {
      |     "action": "Authorise",
      |     "isExclusiveAgent": true
      |
      |}
    """.stripMargin

  val validLegacyAgentPayload =
    """
      |{
      |    "agentId": "SA6012",
      |    "agentOwnRef": "abcdefghij",
      |    "hasAgent": false,
      |    "isRegisteredAgent": false,
      |    "govAgentId": "6WKC9BTJUTPH",
      |    "agentName": "Mr SA AGT_022",
      |    "agentPhoneNo": "03002003319",
      |    "address1": "Plaza 2",
      |    "address2": "Ironmasters Way",
      |    "address3": "Telford",
      |    "address4": "Shropshire",
      |    "postcode": "TF3 4NT",
      |    "isAgentAbroad": false,
      |    "agentCeasedDate": "2001-01-01"
      |}
    """.stripMargin

  val validLegacyRelationshipPayload =
    """
      |{
      |    "agentId": "SA6012",
      |    "nino": "AA123456A",
      |    "utr": "1234567890"
      |}
    """.stripMargin

  val validBusinessDetailsPayload =
    """
      |{
      |    "safeId": "XE00001234567890",
      |    "nino": "AA123456A",
      |    "mtdId": "ZZZZ56789012345",
      |    "propertyIncome": false,
      |    "businessData": [
      |        {
      |            "incomeSourceId": "123456789012345",
      |            "accountingPeriodStartDate": "2001-01-01",
      |            "accountingPeriodEndDate": "2001-01-01",
      |            "tradingName": "RCDTS",
      |            "businessAddressDetails":
      |            {
      |                "addressLine1": "100 SuttonStreet",
      |                "addressLine2": "Wokingham",
      |                "addressLine3": "Surrey",
      |                "addressLine4": "London",
      |                "postalCode": "DH14EJ",
      |                "countryCode": "GB"
      |            },
      |            "businessContactDetails":
      |            {
      |                "phoneNumber": "01332752856",
      |                "mobileNumber": "07782565326",
      |                "faxNumber": "01332754256",
      |                "emailAddress": "stephen@manncorpone.co.uk"
      |            },
      |            "tradingStartDate": "2001-01-01",
      |            "cashOrAccruals": true,
      |            "seasonal": true,
      |            "cessationDate": "2001-01-01",
      |            "cessationReason": "002",
      |            "paperLess": true
      |        }
      |    ]
      |}
                                    """.stripMargin

  val validVatCustomerInformationPayload =
    """{
      |  "vrn": "123456789",
      |  "approvedInformation": {
      |    "customerDetails": {
      |      "organisationName": "Ancient Antiques",
      |      "individual": {
      |        "title": "0001",
      |        "firstName": "Fred",
      |        "middleName": "M",
      |        "lastName": "Flintstone"
      |      },
      |      "tradingName": "a",
      |      "mandationStatus": "1",
      |      "registrationReason": "0001",
      |      "effectiveRegistrationDate": "1967-08-13",
      |      "businessStartDate": "1967-08-13"
      |    },
      |    "PPOB": {
      |      "address": {
      |        "line1": "VAT ADDR 1",
      |        "line2": "VAT ADDR 2",
      |        "line3": "VAT ADDR 3",
      |        "line4": "VAT ADDR 4",
      |        "postCode": "SW1A 2BQ",
      |        "countryCode": "ES"
      |      },
      |      "RLS": "0001",
      |      "contactDetails": {
      |        "primaryPhoneNumber": "01257162661",
      |        "mobileNumber": "07128126712 ",
      |        "faxNumber": "01268712671 ",
      |        "emailAddress": "antiques@email.com"
      |      }
      |    },
      |    "correspondenceContactDetails": {
      |      "address": {
      |        "line1": "VAT ADDR 1",
      |        "line2": "VAT ADDR 2",
      |        "line3": "VAT ADDR 3",
      |        "line4": "VAT ADDR 4",
      |        "postCode": "SW1A 2BQ",
      |        "countryCode": "ES"
      |      },
      |      "RLS": "0001",
      |      "contactDetails": {
      |        "primaryPhoneNumber": "01257162661",
      |        "mobileNumber": "07128126712",
      |        "faxNumber": "01268712671",
      |        "emailAddress": "antiques@email.com"
      |      }
      |    },
      |    "bankDetails": {
      |      "IBAN": "a",
      |      "BIC": "a",
      |      "accountHolderName": "Flintstone Quarry",
      |      "bankAccountNumber": "00012345",
      |      "sortCode": "010103",
      |      "buildingSocietyNumber": "12312345",
      |      "bankBuildSocietyName": "a"
      |    },
      |    "businessActivities": {
      |      "primaryMainCode": "00000",
      |      "mainCode2": "00000",
      |      "mainCode3": "00000",
      |      "mainCode4": "00000"
      |    },
      |    "flatRateScheme": {
      |      "FRSCategory": "001",
      |      "FRSPercentage": 123.12,
      |      "limitedCostTrader": true,
      |      "startDate": "2001-01-01"
      |    },
      |    "deregistration": {
      |      "deregistrationReason": "0001",
      |      "effectDateOfCancellation": "2001-01-01",
      |      "lastReturnDueDate": "2001-01-01"
      |    },
      |    "returnPeriod": {
      |      "stdReturnPeriod": "MA",
      |      "nonStdTaxPeriods": {
      |        "period01": "2001-01-01",
      |        "period02": "2001-01-01",
      |        "period03": "2001-01-01",
      |        "period04": "2001-01-01",
      |        "period05": "2001-01-01",
      |        "period06": "2001-01-01",
      |        "period07": "2001-01-01",
      |        "period08": "2001-01-01",
      |        "period09": "2001-01-01",
      |        "period10": "2001-01-01",
      |        "period11": "2001-01-01",
      |        "period12": "2001-01-01",
      |        "period13": "2001-01-01",
      |        "period14": "2001-01-01",
      |        "period15": "2001-01-01",
      |        "period16": "2001-01-01",
      |        "period17": "2001-01-01",
      |        "period18": "2001-01-01",
      |        "period19": "2001-01-01",
      |        "period20": "2001-01-01",
      |        "period21": "2001-01-01",
      |        "period22": "2001-01-01",
      |        "period23": "2001-01-01",
      |        "period24": "2001-01-01"
      |      }
      |    },
      |    "groupOrPartnerMbrs": [
      |      {
      |        "typeOfRelationship": "01",
      |        "organisationName": "abcdefghijklmn",
      |        "individual": {
      |          "title": "0001",
      |          "firstName": "abcdefghijklmnopq",
      |          "middleName": "abcdefg",
      |          "lastName": "abcdefghijklm"
      |        },
      |        "SAP_Number": "012345678901234567890123456789012345678912"
      |      }
      |    ]
      |  },
      |  "inFlightInformation": {
      |    "changeIndicators": {
      |      "customerDetails": true,
      |      "PPOBDetails": false,
      |      "correspContactDetails": false,
      |      "bankDetails": true,
      |      "businessActivities": true,
      |      "flatRateScheme": false,
      |      "deRegistrationInfo": false,
      |      "returnPeriods": true,
      |      "groupOrPartners": true
      |    },
      |    "inflightChanges": {
      |      "customerDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "organisationName": "Ancient Antiques",
      |        "individual": {
      |          "title": "0001",
      |          "firstName": "Fred",
      |          "middleName": "M",
      |          "lastName": "Flintstone"
      |        },
      |        "tradingName": "a",
      |        "mandationStatus": "1",
      |        "registrationReason": "0001",
      |        "effectiveRegistrationDate": "1967-08-13"
      |      },
      |      "PPOBDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "address": {
      |          "line1": "VAT ADDR 1",
      |          "line2": "VAT ADDR 2",
      |          "line3": "VAT ADDR 3",
      |          "line4": "VAT ADDR 4",
      |          "postCode": "SW1A 2BQ",
      |          "countryCode": "ES"
      |        },
      |        "contactDetails": {
      |          "primaryPhoneNumber": "01257162661",
      |          "mobileNumber": "07128126712",
      |          "faxNumber": "01268712671",
      |          "emailAddress": "antiques@email.com"
      |        },
      |        "websiteAddress": "abcdefghijklmn"
      |      },
      |      "correspondenceContactDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "address": {
      |          "line1": "VAT ADDR 1",
      |          "line2": "VAT ADDR 2",
      |          "line3": "VAT ADDR 3",
      |          "line4": "VAT ADDR 4",
      |          "postCode": "SW1A 2BQ",
      |          "countryCode": "ES"
      |        },
      |        "contactDetails": {
      |          "primaryPhoneNumber": "01257162661",
      |          "mobileNumber": "07128126712",
      |          "faxNumber": "01268712671",
      |          "emailAddress": "antiques@email.com"
      |        }
      |      },
      |      "bankDetails": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "IBAN": "a",
      |        "BIC": "a",
      |        "accountHolderName": "Flintstone Quarry",
      |        "bankAccountNumber": "00012345",
      |        "sortCode": "010103",
      |        "buildingSocietyNumber": "12312345",
      |        "bankBuildSocietyName": "a"
      |      },
      |      "groupOrPartner": [
      |        {
      |          "formInformation": {
      |            "formBundle": "012345678912",
      |            "dateReceived": "2001-01-01"
      |          },
      |          "action": "1",
      |          "SAP_Number": "012345678901234567890123456789012345678912",
      |          "typeOfRelationship": "01",
      |          "makeGrpMember": false,
      |          "makeControllingBody": false,
      |          "isControllingBody": false,
      |          "organisationName": "abcdefg",
      |          "tradingName": "abcdefghijkl",
      |          "individual": {
      |            "title": "0001",
      |            "firstName": "abcdefghijk",
      |            "middleName": "abcdefghijklmno",
      |            "lastName": "abcdefg"
      |          }
      |        }
      |      ],
      |      "deregister": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "deregistrationReason": "0001",
      |        "deregDate": "2001-01-01",
      |        "deregDateInFuture": "2001-01-01"
      |      },
      |      "returnPeriod": {
      |        "formInformation": {
      |          "formBundle": "012345678912",
      |          "dateReceived": "2001-01-01"
      |        },
      |        "changeReturnPeriod": false,
      |        "nonStdTaxPeriodsRequested": false,
      |        "ceaseNonStdTaxPeriods": false,
      |        "stdReturnPeriod": "MA",
      |        "nonStdTaxPeriods": {
      |          "period01": "2001-01-01",
      |          "period02": "2001-01-01",
      |          "period03": "2001-01-01",
      |          "period04": "2001-01-01",
      |          "period05": "2001-01-01",
      |          "period06": "2001-01-01",
      |          "period07": "2001-01-01",
      |          "period08": "2001-01-01",
      |          "period09": "2001-01-01",
      |          "period10": "2001-01-01",
      |          "period11": "2001-01-01",
      |          "period12": "2001-01-01",
      |          "period13": "2001-01-01",
      |          "period14": "2001-01-01",
      |          "period15": "2001-01-01",
      |          "period16": "2001-01-01",
      |          "period17": "2001-01-01",
      |          "period18": "2001-01-01",
      |          "period19": "2001-01-01",
      |          "period20": "2001-01-01",
      |          "period21": "2001-01-01",
      |          "period22": "2001-01-01",
      |          "period23": "2001-01-01",
      |          "period24": "2001-01-01"
      |        }
      |      }
      |    }
      |  }
      |}""".stripMargin

  val validBusinessPartnerRecordPayload =
    """
      |{
      |  "businessPartnerExists": true,
      |  "safeId": "XE0001234567890",
      |  "agentReferenceNumber": "AARN1234567",
      |  "utr": "0123456789",
      |  "crn": "AA123456",
      |  "isAnAgent": true,
      |  "isAnASAgent": true,
      |  "isAnIndividual": true,
      |  "individual": {
      |    "firstName": "Stephen",
      |    "lastName": "Wood",
      |    "dateOfBirth": "1990-04-03"
      |  },
      |  "isAnOrganisation": false,
      |  "addressDetails": {
      |    "addressLine1": "100 SuttonStreet",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "DH14EJ",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "01332752856",
      |    "mobileNumber": "07782565326",
      |    "faxNumber": "01332754256",
      |    "emailAddress": "stephen@manncorpone.co.uk"
      |  },
      |  "agencyDetails": {
      |    "agencyName": "HMRC",
      |    "agencyAddress": {
      |      "addressLine1": "Plaza 2",
      |      "addressLine2": "Ironmasters Way",
      |      "addressLine3": "Telford",
      |      "addressLine4": "Shropshire",
      |      "postalCode": "TF3 4NT",
      |      "countryCode": "GB"
      |    },
      |    "agencyEmail": "hmrc@hmrc.gsi.gov.uk"
      |  }
      |}
    """.stripMargin

  val validAgentSubmission =
    """{
      |  "safeId": "XE0001234567890",
      |  "agencyName": "HMRC",
      |  "agencyAddress": {
      |    "addressLine1": "Plaza 2",
      |    "addressLine2": "Ironmasters Way",
      |    "addressLine3": "Telford",
      |    "addressLine4": "Shropshire",
      |    "postalCode": "TF3 4NT",
      |    "countryCode": "GB"
      |  },
      |  "telephoneNumber": "01332752856",
      |  "agencyEmail": "hmrc@hmrc.gsi.gov.uk"
      |}""".stripMargin

  val validIndividualSubmission =
    """{
      |  "regime": "ITSA",
      |  "requiresNameMatch": true,
      |  "isAnAgent": false,
      |  "individual": {
      |    "firstName": "Stephen",
      |    "lastName": "Wood",
      |    "dateOfBirth": "1990-04-03"
      |  }
      |}""".stripMargin

  val validRelationshipPayload =
    """{
      |  "regime" : "VATC",
      |  "arn" : "DARN6768822",
      |  "idType" : "vrn",
      |  "refNumber" : "170332692",
      |  "active" : true,
      |  "relationshipType" : "ZA02",
      |  "authProfile" : "CORR0001",
      |  "startDate" : "1992-05-14"
      |}""".stripMargin

  val validIndividualWithoutIDSubmission =
    """
      |{
      |    "regime":"AGSV",
      |    "acknowledgementReference":"8bc4feee-be04-4e84-ba0f-djgg11",
      |    "isAnAgent":false,
      |    "isAGroup":false,
      |    "individual": {
      |      "firstName": "Stephen",
      |      "lastName": "Wood",
      |      "dateOfBirth": "1990-04-03"
      |    },
      |    "address":{
      |        "addressLine1":"Mandatory Address Line 1",
      |        "addressLine2":"Mandatory Address Line 2",
      |        "addressLine3":"Optional Address Line 3",
      |        "addressLine4":"Optional Address Line 4",
      |        "countryCode":"IE"
      |    },
      |    "contactDetails":{
      |        "phoneNumber":"12345678"
      |    }
      |}
    """.stripMargin

  val invalidWithoutIDSubmission =
    """
      |{
      |    "regime":"AGSV",
      |    "acknowledgementReference":"8bc4feee-be04-4e84-ba0f-djgg11",
      |    "isAnAgent":false,
      |    "isAGroup":false
      |}
    """.stripMargin

  val validOrganisationWithoutIDSubmission =
    """
      |{
      |    "regime":"AGSV",
      |    "acknowledgementReference":"8bc4feee-be04-4e84-ba0f-djgg11",
      |    "isAnAgent":false,
      |    "isAGroup":false,
      |    "organisation":{"organisationName":"Test Organisation Name"},
      |    "address":{
      |        "addressLine1":"Mandatory Address Line 1",
      |        "addressLine2":"Mandatory Address Line 2",
      |        "addressLine3":"Optional Address Line 3",
      |        "addressLine4":"Optional Address Line 4",
      |        "countryCode":"IE"
      |    },
      |    "contactDetails":{
      |        "phoneNumber":"12345678"
      |    }
      |}
    """.stripMargin

  val validPPTSubscriptionDisplayPayload =
    s"""
      |{
      |  "pptReference": "XAPPT0001234567",
      |    "legalEntityDetails": {
      |        "dateOfApplication": "2004-08-21",
      |        "customerDetails": {
      |            "customerType": "Individual",
      |            "individualDetails": {
      |                "firstName": "Jacob",
      |                "lastName": "Smellie"
      |            }
      |        }
      |    },
      |    "changeOfCircumstanceDetails": {
      |        "deregistrationDetails": {
      |            "deregistrationDate": "1991-09-01"
      |        }
      |    },
      |    "id": "6167e9b0f7000054fab88f17"
      |}
      |""".stripMargin

  val validPillar2SubscriptionPayload = Json
    .toJson(
      Pillar2Record(
        "XAPLR2222222222",
        Pillar2Record.UpeDetails(None, None, "OrgName", "2000-01-01"),
        Pillar2Record.AccountingPeriod("2023-01-01", "2023-12-31"),
        Pillar2Record.UpeCorrespAddressDetails("1 North Street", countryCode = "GB"),
        Pillar2Record.ContactDetails("Some Name", None, "name@email.com")
      )
    )
    .toString
}
