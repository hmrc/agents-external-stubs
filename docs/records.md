## GET /agents-external-stubs/records

Returns all records on the planet grouped by the type.

    {
      "VatCustomerInformationRecord": [
        {
          "vrn": "123456789",
          "approvedInformation": {
            "customerDetails": {
              "organisationName": "Ancient Antiques",
              "individual": {
                "title": "0001",
                "firstName": "Fred",
                "middleName": "M",
                "lastName": "Flintstone"
              },
              "dateOfBirth": "2006-08-16",
              "tradingName": "a",
              "mandationStatus": "1",
              "registrationReason": "0001",
              "effectiveRegistrationDate": "1967-08-13",
              "businessStartDate": "1967-08-13"
            },
            "PPOB": {
              "address": {
                "line1": "VAT ADDR 1",
                "line2": "VAT ADDR 2",
                "line3": "VAT ADDR 3",
                "line4": "VAT ADDR 4",
                "postCode": "SW1A 2BQ",
                "countryCode": "ES"
              },
              "RLS": "0001",
              "contactDetails": {
                "primaryPhoneNumber": "01257162661",
                "mobileNumber": "07128126712 ",
                "faxNumber": "01268712671 ",
                "emailAddress": "antiques@email.com"
              },
              "websiteAddress": "4ljihgtcjmLuPuhPKadbopv1ok6qrt5yhhcfssqttujzhtasbcxVaub"
            },
            "correspondenceContactDetails": {
              "address": {
                "line1": "VAT ADDR 1",
                "line2": "VAT ADDR 2",
                "line3": "VAT ADDR 3",
                "line4": "VAT ADDR 4",
                "postCode": "SW1A 2BQ",
                "countryCode": "ES"
              },
              "RLS": "0001",
              "contactDetails": {
                "primaryPhoneNumber": "01257162661",
                "mobileNumber": "07128126712",
                "faxNumber": "01268712671",
                "emailAddress": "antiques@email.com"
              }
            },
            "bankDetails": {
              "IBAN": "a",
              "BIC": "a",
              "accountHolderName": "Flintstone Quarry",
              "bankAccountNumber": "00012345",
              "sortCode": "010103",
              "buildingSocietyNumber": "12312345",
              "bankBuildSocietyName": "a"
            },
            "businessActivities": {
              "primaryMainCode": "00000",
              "mainCode2": "00000",
              "mainCode3": "00000",
              "mainCode4": "00000"
            },
            "flatRateScheme": {
              "FRSCategory": "001",
              "FRSPercentage": 123.12,
              "startDate": "2001-01-01",
              "limitedCostTrader": true
            },
            "deregistration": {
              "deregistrationReason": "0001",
              "effectDateOfCancellation": "2001-01-01",
              "lastReturnDueDate": "2001-01-01"
            },
            "returnPeriod": {
              "stdReturnPeriod": "MA",
              "nonStdTaxPeriods": {
                "period01": "2001-01-01",
                "period02": "2001-01-01",
                "period03": "2001-01-01",
                "period04": "2001-01-01",
                "period05": "2001-01-01",
                "period06": "2001-01-01",
                "period07": "2001-01-01",
                "period08": "2001-01-01",
                "period09": "2001-01-01",
                "period10": "2001-01-01",
                "period11": "2001-01-01",
                "period12": "2001-01-01",
                "period13": "2001-01-01",
                "period14": "2001-01-01",
                "period15": "2001-01-01",
                "period16": "2001-01-01",
                "period17": "2001-01-01",
                "period18": "2001-01-01",
                "period19": "2001-01-01",
                "period20": "2001-01-01",
                "period21": "2001-01-01",
                "period22": "2001-01-01"
              }
            },
            "groupOrPartnerMbrs": [
              {
                "typeOfRelationship": "01",
                "organisationName": "abcdefghijklmn",
                "individual": {
                  "title": "0001",
                  "firstName": "abcdefghijklmnopq",
                  "middleName": "abcdefg",
                  "lastName": "abcdefghijklm"
                },
                "SAP_Number": "012345678901234567890123456789012345678912"
              }
            ]
          },
          "inFlightInformation": {
            "changeIndicators": {
              "customerDetails": true,
              "PPOBDetails": false,
              "correspContactDetails": false,
              "bankDetails": true,
              "businessActivities": true,
              "flatRateScheme": false,
              "deRegistrationInfo": false,
              "returnPeriods": true,
              "groupOrPartners": true
            },
            "inflightChanges": {
              "customerDetails": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "organisationName": "Ancient Antiques",
                "individual": {
                  "title": "0001",
                  "firstName": "Fred",
                  "middleName": "M",
                  "lastName": "Flintstone"
                },
                "dateOfBirth": "2006-08-16",
                "tradingName": "a",
                "mandationStatus": "1",
                "registrationReason": "0001",
                "effectiveRegistrationDate": "1967-08-13"
              },
              "PPOBDetails": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "address": {
                  "line1": "VAT ADDR 1",
                  "line2": "VAT ADDR 2",
                  "line3": "VAT ADDR 3",
                  "line4": "VAT ADDR 4",
                  "postCode": "SW1A 2BQ",
                  "countryCode": "ES"
                },
                "contactDetails": {
                  "primaryPhoneNumber": "01257162661",
                  "mobileNumber": "07128126712",
                  "faxNumber": "01268712671",
                  "emailAddress": "antiques@email.com"
                },
                "websiteAddress": "abcdefghijklmn"
              },
              "correspondenceContactDetails": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "address": {
                  "line1": "VAT ADDR 1",
                  "line2": "VAT ADDR 2",
                  "line3": "VAT ADDR 3",
                  "line4": "VAT ADDR 4",
                  "postCode": "SW1A 2BQ",
                  "countryCode": "ES"
                },
                "contactDetails": {
                  "primaryPhoneNumber": "01257162661",
                  "mobileNumber": "07128126712",
                  "faxNumber": "01268712671",
                  "emailAddress": "antiques@email.com"
                }
              },
              "bankDetails": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "IBAN": "a",
                "BIC": "a",
                "accountHolderName": "Flintstone Quarry",
                "bankAccountNumber": "00012345",
                "sortCode": "010103",
                "buildingSocietyNumber": "12312345",
                "bankBuildSocietyName": "a"
              },
              "businessActivities": {
                "formInformation": {
                  "formBundle": "040438784565",
                  "dateReceived": "1995-07-21"
                },
                "primaryMainCode": "47359",
                "mainCode2": "04043",
                "mainCode3": "04043",
                "mainCode4": "04043"
              },
              "flatRateScheme": {
                "formInformation": {
                  "formBundle": "040438784565",
                  "dateReceived": "1995-07-21"
                },
                "FRSCategory": "029",
                "FRSPercentage": 805.49,
                "startDate": "1970-06-25",
                "limitedCostTrader": true
              },
              "deregister": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "deregistrationReason": "0001",
                "deregDate": "2001-01-01",
                "deregDateInFuture": "2001-01-01"
              },
              "returnPeriod": {
                "formInformation": {
                  "formBundle": "012345678912",
                  "dateReceived": "2001-01-01"
                },
                "changeReturnPeriod": false,
                "nonStdTaxPeriodsRequested": false,
                "ceaseNonStdTaxPeriods": false,
                "stdReturnPeriod": "MA",
                "nonStdTaxPeriods": {
                  "period01": "2001-01-01",
                  "period02": "2001-01-01",
                  "period03": "2001-01-01",
                  "period04": "2001-01-01",
                  "period05": "2001-01-01",
                  "period06": "2001-01-01",
                  "period07": "2001-01-01",
                  "period08": "2001-01-01",
                  "period09": "2001-01-01",
                  "period10": "2001-01-01",
                  "period11": "2001-01-01",
                  "period12": "2001-01-01",
                  "period13": "2001-01-01",
                  "period14": "2001-01-01",
                  "period15": "2001-01-01",
                  "period16": "2001-01-01",
                  "period17": "2001-01-01",
                  "period18": "2001-01-01",
                  "period19": "2001-01-01",
                  "period20": "2001-01-01",
                  "period21": "2001-01-01",
                  "period22": "2001-01-01"
                }
              },
              "groupOrPartner": [
                {
                  "formInformation": {
                    "formBundle": "012345678912",
                    "dateReceived": "2001-01-01"
                  },
                  "action": "1",
                  "SAP_Number": "012345678901234567890123456789012345678912",
                  "typeOfRelationship": "01",
                  "makeGrpMember": false,
                  "makeControllingBody": false,
                  "isControllingBody": false,
                  "organisationName": "abcdefg",
                  "tradingName": "abcdefghijkl",
                  "individual": {
                    "title": "0001",
                    "firstName": "abcdefghijk",
                    "middleName": "abcdefghijklmno",
                    "lastName": "abcdefg"
                  },
                  "PPOB": {
                    "address": {
                      "line1": "75 Braemar Road",
                      "line2": "The Garthwaite House",
                      "postCode": "NE62 7MU",
                      "countryCode": "GB"
                    },
                    "RLS": "0002",
                    "contactDetails": {
                      "primaryPhoneNumber": "01322 530550",
                      "mobileNumber": "00404 387845",
                      "faxNumber": "00404 387845",
                      "emailAddress": "c@z.com"
                    },
                    "websiteAddress": "4ljihgtcjmLuPuhPKadbopv1ok6qrt5yhhcfssqttujzhtasbcxVaub"
                  }
                }
              ]
            }
          },
          "id": "5b91a5481300000f00a0f738"
        }
      ],
      "LegacyAgentRecord": [
        {
          "agentId": "SA6012",
          "agentOwnRef": "abcdefghij",
          "hasAgent": false,
          "isRegisteredAgent": false,
          "govAgentId": "6WKC9BTJUTPH",
          "agentName": "Mr SA AGT_022",
          "agentPhoneNo": "03002003319",
          "address1": "Plaza 2",
          "address2": "Ironmasters Way",
          "address3": "Telford",
          "address4": "Shropshire",
          "postcode": "TF3 4NT",
          "isAgentAbroad": false,
          "agentCeasedDate": "2001-01-01",
          "id": "5b91a5481300001000a0f739"
        }
      ],
      "BusinessPartnerRecord": [
        {
          "businessPartnerExists": true,
          "safeId": "XE0001234567890",
          "agentReferenceNumber": "AARN1234567",
          "utr": "0123456789",
          "isAnAgent": true,
          "isAnASAgent": true,
          "isAnIndividual": true,
          "individual": {
            "firstName": "Stephen",
            "lastName": "Wood",
            "dateOfBirth": "1990-04-03"
          },
          "isAnOrganisation": false,
          "addressDetails": {
            "addressLine1": "100 SuttonStreet",
            "addressLine2": "Wokingham",
            "addressLine3": "Surrey",
            "addressLine4": "London",
            "postalCode": "DH14EJ",
            "countryCode": "GB"
          },
          "contactDetails": {
            "phoneNumber": "01332752856",
            "mobileNumber": "07782565326",
            "faxNumber": "01332754256",
            "emailAddress": "stephen@manncorpone.co.uk"
          },
          "agencyDetails": {
            "agencyName": "HMRC",
            "agencyAddress": {
              "addressLine1": "Plaza 2",
              "addressLine2": "Ironmasters Way",
              "addressLine3": "Telford",
              "addressLine4": "Shropshire",
              "postalCode": "TF3 4NT",
              "countryCode": "GB"
            },
            "agencyEmail": "hmrc@hmrc.gsi.gov.uk"
          },
          "id": "5b91a5481300000e00a0f73b"
        }
      ],
      "LegacyRelationshipRecord": [
        {
          "agentId": "SA6012",
          "nino": "AA123456A",
          "utr": "1234567890",
          "id": "5b91a5481300000f00a0f73a"
        }
      ],
      "BusinessDetailsRecord": [
        {
          "safeId": "XE00001234567890",
          "nino": "AA123456A",
          "mtdId": "ZZZZ56789012345",
          "propertyIncome": false,
          "businessData": [
            {
              "incomeSourceId": "123456789012345",
              "accountingPeriodStartDate": "2001-01-01",
              "accountingPeriodEndDate": "2001-01-01",
              "tradingName": "RCDTS",
              "businessAddressDetails": {
                "addressLine1": "100 SuttonStreet",
                "addressLine2": "Wokingham",
                "addressLine3": "Surrey",
                "addressLine4": "London",
                "postalCode": "DH14EJ",
                "countryCode": "GB"
              },
              "businessContactDetails": {
                "phoneNumber": "01332752856",
                "mobileNumber": "07782565326",
                "faxNumber": "01332754256",
                "emailAddress": "stephen@manncorpone.co.uk"
              },
              "tradingStartDate": "2001-01-01",
              "cashOrAccruals": true,
              "seasonal": true,
              "cessationDate": "2001-01-01",
              "cessationReason": "002",
              "paperLess": true
            }
          ],
          "propertyData": {
            "incomeSourceId": "idr58ssnzmUV6cp",
            "accountingPeriodStartDate": "1992-06-15",
            "accountingPeriodEndDate": "2011-06-07",
            "numPropRented": "61",
            "numPropRentedUK": "61",
            "numPropRentedEEA": "61",
            "numPropRentedNONEEA": "61",
            "emailAddress": "u@P8ssnzmUV6cp0hwvvosrorNdlvtjuaurBynV1vrtpxybTavvuagweziqopptkqvJmzXywjiuy4ct5sul9f4snjz4n.eu",
            "cessationDate": "2015-12-02",
            "cessationReason": "003",
            "paperLess": true
          },
          "id": "5b91a5471300000f00a0f737"
        }
      ],
      "_links": []
    }